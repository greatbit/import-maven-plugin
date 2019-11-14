package com.testquack.maven.mojo;

import com.testquack.beans.Launch;
import com.testquack.beans.LaunchStatus;
import com.testquack.beans.LaunchTestCase;
import com.testquack.beans.LaunchTestCaseTree;
import com.testquack.maven.client.QuackClient;
import com.testquack.maven.client.QuackClietnUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST;
import static ru.greatbit.utils.string.StringUtils.getMd5String;

@Mojo(name = "junit-results-import", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true, defaultPhase = TEST)
public class QuackJunitResultsImport extends AbstractMojo{

    @Parameter(property = "junitXmlPath", defaultValue = "${project.build.directory}/surefire-reports")
    File junitXmlResource;

    @Parameter(property = "quackProject",name = "quackProject", required = true)
    private String quackProject;

    @Parameter(property = "apiToken", name = "apiToken", required = true)
    private String apiToken;

    @Parameter(property = "apiEndpoint", name = "apiEndpoint", required = true)
    private String apiEndpoint;

    @Parameter(property = "apiTimeout", name = "apiTimeout", defaultValue = "60000")
    private long apiTimeout;

    @Parameter(property = "launchNamePrefix", name = "launchNamePrefix", defaultValue = "Junit Import")
    private String launchNamePrefix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!junitXmlResource.isDirectory()) {
            throw new MojoExecutionException(junitXmlResource + " is not a directory");
        }

        getLog().debug("Checking test results in " + junitXmlResource);
        List<File> reportDirectories = collectReportDirectoriesRecursively(junitXmlResource);
        SurefireReportParser parser = new SurefireReportParser(reportDirectories, Locale.getDefault());
        List<ReportTestSuite> testSuites;
        try {
            testSuites = parser.parseXMLReportFiles();
        } catch (MavenReportException e) {
            throw new MojoExecutionException("Could not parse XML reports", e);
        }
        if (testSuites.isEmpty()) {
            getLog().warn("XML reports not found in " + junitXmlResource);
        }

        List<LaunchTestCase> launchTestCases =
                testSuites.stream().
                        flatMap(testSuite -> testSuite.getTestCases().stream()).
                        filter(Objects::nonNull).
                        map(this::convertTestcase).
                        collect(toList());

        Launch launch = (Launch) new Launch().withName(launchNamePrefix + " " + new Date());
        launch.setTestCaseTree(new LaunchTestCaseTree().withTestCases(launchTestCases));

        getLog().info("Starting launch import to QuAck");
        QuackClient client = QuackClietnUtils.getClient(apiToken, apiEndpoint, apiTimeout);
        try {
            client.createLaunch(quackProject, launch).execute();
        } catch (IOException e) {
            getLog().error("Unable to import launch to QuAck", e);
            throw new RuntimeException(e);
        }
    }

    private LaunchTestCase convertTestcase(ReportTestCase reportTestCase) {
        LaunchTestCase launchTestCase = new LaunchTestCase().
                withDuration(new Float(reportTestCase.getTime()).longValue()).
                withLaunchStatus(convertStatus(reportTestCase)).
                withFailureMessage(reportTestCase.getFailureMessage()).
                withFailureTrace(reportTestCase.getFailureDetail());
        try {
            launchTestCase.setAlias(getMd5String(reportTestCase.getFullName()));


            getLog().info("Import alias " + reportTestCase.getFullName());
            getLog().info("Alias " + launchTestCase.getAlias());
        } catch (NoSuchAlgorithmException e) {
            getLog().warn("Unable to create testcase alias", e);
        }
        return launchTestCase;
    }

    private LaunchStatus convertStatus(ReportTestCase reportTestCase) {
        if (reportTestCase.getFailureType() == null){
            return LaunchStatus.PASSED;
        }
        switch(reportTestCase.getFailureType()) {
            case "skipped":
                return LaunchStatus.SKIPPED;
            default:
                return LaunchStatus.FAILED;
        }

    }

    static List<File> collectReportDirectoriesRecursively(final File rootDirectory) throws MojoExecutionException {
        if (rootDirectory == null) {
            throw new MojoExecutionException("No valid directory provided");
        }
        if (!rootDirectory.exists()) {
            throw new MojoExecutionException("Directory " + rootDirectory + " does not exist");
        }
        if (!rootDirectory.isDirectory()) {
            throw new MojoExecutionException("Directory " + rootDirectory + " is no directory");
        }

        List<File> ret = new ArrayList<>();
        ret.add(rootDirectory);

        for (File child : rootDirectory.listFiles()) {
            if (child.isDirectory()) {
                ret.addAll(collectReportDirectoriesRecursively(child));
            }
        }

        return ret;
    }

    private void logLines(List<String> lines) {
        for (String line : lines) {
            getLog().info(line);
        }
        getLog().info("");
    }
}
