package com.testquack.maven.mojo;

import com.testquack.beans.TestCase;
import com.testquack.maven.client.QuackClient;
import com.testquack.maven.client.QuackClietnUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static ru.greatbit.utils.string.StringUtils.getMd5String;


/**
 * Created by azee on 21.08.19.
 */
@Mojo(name = "junit-import", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class QuackJunitImport extends AbstractMojo{

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject mavenProject;

    @Parameter(defaultValue = "false")
    private boolean failOnError;

    @Parameter(property = "quackProject",name = "quackProject", required = true)
    private String quackProject;

    @Parameter(property = "apiToken", name = "apiToken", required = true)
    private String apiToken;

    @Parameter(property = "apiEndpoint", name = "apiEndpoint", required = true)
    private String apiEndpoint;

    @Parameter(property = "apiTimeout", name = "apiTimeout", defaultValue = "60000")
    private long apiTimeout;

    @Parameter(property = "uploadChunkSize", name = "uploadChunkSize", defaultValue = "20")
    private int uploadChunkSize;

    @Parameter(property = "importResource", name = "importResource", defaultValue = "maven-junit-${project.groupId}-${project.artifactId}")
    private String importResource;


    @Override
    public void execute() {
        getLog().info( "Preparing QuAck Project data" );

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(getTestClasspathUrls())
                .setScanners(new MethodAnnotationsScanner(), new SubTypesScanner(), new TypeAnnotationsScanner())
                .addClassLoader(getClassLoader())
        );

        Thread.currentThread().setContextClassLoader(getClassLoader());

        Set<Method> methods = reflections.getMethodsAnnotatedWith(Test.class);
        List<TestCase> testCases = methods.stream().map(this::convert).collect(toList());
        uploadTestcases(testCases);
    }

    private void uploadTestcases(List<TestCase> testCases) {
        getLog().info(format("Found %s testcases to upload", testCases.size()));
        QuackClient client = QuackClietnUtils.getClient(apiToken, apiEndpoint, apiTimeout);

        getLog().info(format("Marking testcase of import resource %s as obsolete", importResource));
        client.deleteTestcasesByImportResource(quackProject, importResource);

        getLog().info(format("Sending testcases to QuAck in chunks by %s", uploadChunkSize));
        List<List<TestCase>> partitions = ListUtils.partition(testCases, uploadChunkSize);
        for (int i = 0; i < partitions.size(); i++ ) {
            try {
                Response response = client.importTestCases(quackProject, partitions.get(i)).execute();
                if (!response.isSuccessful()){
                    String errorMsg = format("Unable to upload testcases to QuAck, got response %s", response.toString());
                    getLog().error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            } catch (IOException e) {
                getLog().error("Unable to upload testcases to QuAck", e);
                throw new RuntimeException(e);
            }
            getLog().info(format("Uploaded %s %%", Math.min(100, (i + 1) * uploadChunkSize * 100 / testCases.size())));
        }
    }

    private TestCase convert(Method method) {
        TestCase testCase = (TestCase) new TestCase().
                withName(method.getName()).
                withAlias(getHash(method)).
                withImportResource(importResource);
        testCase.getMetaData().put("class", method.getDeclaringClass());
        testCase.getMetaData().put("method", method.getName());
        testCase.getMetaData().put("parameters", Stream.of(method.getParameterTypes()).map(Class::getName).collect(toList()));
        return testCase;
    }

    private String getHash(Method method) {
        try {
            return getMd5String(method.toString() +
                    Stream.of(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create hash for the method " + method, e);
        }
    }


    private ClassLoader getClassLoader() {
        return new URLClassLoader(getTestClasspathUrls(), this.getClass().getClassLoader() );
    }

    private URL[] getTestClasspathUrls() {
        try {
            List<String> classpathElements = mavenProject.getTestCompileSourceRoots();
            classpathElements.addAll(mavenProject.getTestClasspathElements());
            classpathElements.add(mavenProject.getBuild().getTestOutputDirectory());
            getLog().info( "Classpath " + classpathElements );
            return classpathElements.stream().
                    filter(Objects::nonNull).
                    map(File::new).map(File::toURI).map(this::toUrl).
                    filter(Objects::nonNull).
                    toArray(URL[]::new);

        } catch (DependencyResolutionRequiredException e){
            return new URL[0];
        }
    }

    private URL toUrl(URI uri) {
        try {
            return uri.toURL();
        }  catch (MalformedURLException e){
            getLog().debug( "Couldn't get the classloader." );
            return null;
        }
    }


}
