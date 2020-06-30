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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.testquack.beans.LaunchStatus.BROKEN;
import static com.testquack.beans.LaunchStatus.FAILED;
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

    private final Map<String, LaunchTestCase> testcasesByAlias = new HashMap<>();

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

            ///////////////////
            getLog().info("/////////////////// Got testsuites " + testSuites.stream().map(ReportTestSuite::getName));
            ///////////////////

        } catch (MavenReportException e) {
            throw new MojoExecutionException("Could not parse XML reports", e);
        }
        if (testSuites.isEmpty()) {
            getLog().warn("XML reports not found in " + junitXmlResource);
        }

        // Tests might be parametrised.
        // If a single parameter fails - all test is considered to fail.
        testSuites.stream().
                flatMap(testSuite -> testSuite.getTestCases().stream()).
                filter(Objects::nonNull).
                forEach(reportTestCase -> {
                    String alias = getAlias(reportTestCase);
                    LaunchTestCase testCaseToStore = convertTestcase(reportTestCase);
                    LaunchTestCase preservedTestcase = testcasesByAlias.get(alias);
                    if (preservedTestcase == null ||
                            (!isFailed(preservedTestcase) && isFailed(testCaseToStore))){
                        testcasesByAlias.put(alias, testCaseToStore);
                    }
                });


        List<LaunchTestCase> launchTestCases = new ArrayList<>(testcasesByAlias.values());

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

    private boolean isFailed(LaunchTestCase preservedTestcase) {
        return preservedTestcase.getLaunchStatus() == FAILED || preservedTestcase.getLaunchStatus() == BROKEN;
    }

    private String getAlias(ReportTestCase reportTestCase) {
        String fullNameNoParameters = reportTestCase.getFullName().split("\\[")[0];
        try {
            return getMd5String(fullNameNoParameters);
        } catch (NoSuchAlgorithmException e) {
            getLog().warn("Unable to create testcase alias", e);
            return null;
        }
    }

    private LaunchTestCase convertTestcase(ReportTestCase reportTestCase) {

        ///////////////////
        getLog().info("/////////////////// Converting report testcase " + reportTestCase.getFullName());
        ///////////////////

        return (LaunchTestCase) new LaunchTestCase().
                withDuration(new Float(reportTestCase.getTime()).longValue()).
                withLaunchStatus(convertStatus(reportTestCase)).
                withFailureMessage(reportTestCase.getFailureMessage()).
                withFailureTrace(reportTestCase.getFailureDetail()).
                withAlias(getAlias(reportTestCase));
    }

    private LaunchStatus convertStatus(ReportTestCase reportTestCase) {
        if (reportTestCase.getFailureType() == null){
            return LaunchStatus.PASSED;
        }
        switch(reportTestCase.getFailureType()) {
            case "skipped":
                return LaunchStatus.SKIPPED;
            default:
                return FAILED;
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
