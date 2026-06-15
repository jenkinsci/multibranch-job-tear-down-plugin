package com.fuzzpro.multibranchteardown;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import java.time.Duration;
import java.util.List;
import jenkins.branch.BranchProperty;
import jenkins.branch.BranchSource;
import jenkins.branch.DefaultBranchPropertyStrategy;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.awaitility.Awaitility;
import org.htmlunit.html.HtmlForm;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WithGitSampleRepo
class JobTearDownListenerTest {

    private JenkinsRule j;
    private GitSampleRepoRule sampleRepo;

    private static final String DEFAULT_JOB = "job-tear-down-executor";
    private static final String PIPELINE_JOB = "my-custom-pipeline-executor";
    private static final String GLOBAL_JOB = "my-custom-global-executor";

    @BeforeEach
    void setUp(GitSampleRepoRule repo, JenkinsRule rule) throws Exception {
        j = rule;
        sampleRepo = repo;

        sampleRepo.init();
        sampleRepo.write(
                "Jenkinsfile", "echo \"branch=${env.BRANCH_NAME}\"; node {checkout scm; echo readFile('file')}");
        sampleRepo.write("file", "initial content");
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--all", "--message=flow");

        sampleRepo.git("checkout", "-b", "feature");
        sampleRepo.write(
                "Jenkinsfile",
                "echo \"branch=${env.BRANCH_NAME}\"; node {checkout scm; echo readFile('file').toUpperCase()}");
        sampleRepo.write("file", "subsequent content");
        sampleRepo.git("commit", "--all", "--message=tweaked");
    }

    @Test
    void defaultJobConfiguredAndTriggered() throws Exception {
        WorkflowJob tearDownJob = j.jenkins.createProject(WorkflowJob.class, DEFAULT_JOB);
        WorkflowJob p = createBasicJob();

        assertEquals(1, tearDownJob.getNextBuildNumber());
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        p.delete();
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(tearDownJob::getNextBuildNumber, equalTo(2));
        verifyParameters(tearDownJob);
    }

    @Test
    void userDefinedJobTriggered() throws Exception {
        WorkflowJob tearDownJob = j.jenkins.createProject(WorkflowJob.class, DEFAULT_JOB);
        WorkflowJob customJob = j.jenkins.createProject(WorkflowJob.class, GLOBAL_JOB);
        WorkflowJob p = createBasicJob();
        setConfigSettings();

        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(1, customJob.getNextBuildNumber());
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        p.delete();
        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(2, customJob.getNextBuildNumber());
        verifyParameters(customJob);
    }

    @Test
    void pipelineDefinedJobTriggered() throws Exception {
        WorkflowJob tearDownJob = j.jenkins.createProject(WorkflowJob.class, DEFAULT_JOB);
        WorkflowJob customJob = j.jenkins.createProject(WorkflowJob.class, PIPELINE_JOB);
        WorkflowJob p = createCustomJob();

        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(1, customJob.getNextBuildNumber());
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        p.delete();
        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(2, customJob.getNextBuildNumber());
        verifyParameters(customJob);
    }

    @Test
    void pipelineDefinedJobTakesPrecedenceOverGlobalSettings() throws Exception {
        WorkflowJob tearDownJob = j.jenkins.createProject(WorkflowJob.class, DEFAULT_JOB);
        WorkflowJob customJob = j.jenkins.createProject(WorkflowJob.class, GLOBAL_JOB);
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, PIPELINE_JOB);
        WorkflowJob p = createCustomJob();
        setConfigSettings();

        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(1, customJob.getNextBuildNumber());
        assertEquals(1, pipelineJob.getNextBuildNumber());
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        p.delete();
        assertEquals(1, tearDownJob.getNextBuildNumber());
        assertEquals(1, customJob.getNextBuildNumber());
        assertEquals(2, pipelineJob.getNextBuildNumber());
        verifyParameters(pipelineJob);
    }

    @Test
    void teardownIgnoredGlobalSharedLibraries() throws Exception {
        WorkflowJob tearDownJob = j.jenkins.createProject(WorkflowJob.class, DEFAULT_JOB);
        addGlobalLibrary();
        WorkflowJob p = createBasicJob();

        assertEquals(1, tearDownJob.getNextBuildNumber());
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        p.delete();
        assertEquals(2, tearDownJob.getNextBuildNumber());
        verifyParameters(tearDownJob);
    }

    private WorkflowJob scheduleAndFindBranchProject(WorkflowMultiBranchProject mp, String name) throws Exception {
        mp.scheduleBuild2(0).getFuture().get();
        WorkflowJob p = mp.getItem(name);
        mp.getIndexing();
        if (p == null) {
            fail(name + " project not found");
        }
        return p;
    }

    private WorkflowJob createBasicJob() throws Exception {
        WorkflowMultiBranchProject mp = j.jenkins.createProject(WorkflowMultiBranchProject.class, "p");
        mp.getSourcesList()
                .add(new BranchSource(
                        new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));
        WorkflowJob p = scheduleAndFindBranchProject(mp, "feature");
        p.setDefinition(new CpsFlowDefinition("node { checkout scm }", true));
        return p;
    }

    private WorkflowJob createCustomJob() throws Exception {
        WorkflowMultiBranchProject mp = j.jenkins.createProject(WorkflowMultiBranchProject.class, "p");
        mp.getSourcesList()
                .add(new BranchSource(
                        new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));
        WorkflowJob p = scheduleAndFindBranchProject(mp, "feature");
        p.setDefinition(new CpsFlowDefinition(
                "properties([branchTearDownExecutor('" + PIPELINE_JOB + "')])\n" + "node { " + "checkout scm " + "}",
                true));
        return p;
    }

    private void setConfigSettings() throws Exception {
        HtmlForm form = j.createWebClient().goTo("configure").getFormByName("config");
        form.getInputByName("jobteardown.tearDownJob").setValueAttribute(GLOBAL_JOB);
        j.submit(form);
    }

    private void verifyParameters(WorkflowJob job) {
        waitForLastBuild(job);
        WorkflowRun run = job.getLastBuild();
        assertEquals(1, run.number);
        ParametersAction action = run.getAction(ParametersAction.class);
        List<ParameterValue> params = action.getAllParameters();
        assertEquals(2, params.size());
        assertEquals("git_url", params.get(0).getName());
        assertFalse(params.get(0).getValue().toString().isEmpty());
        assertNotEquals(
                "https://github.com/fabric8io/jenkins-pipeline-library",
                params.get(0).getValue());
        assertEquals("branch_name", params.get(1).getName());
        assertEquals(sampleRepo.getRoot().toString(), params.get(0).getValue());
        assertEquals("feature", params.get(1).getValue());
    }

    private void addGlobalLibrary() throws Exception {
        j.configRoundtrip();
        GlobalLibraries gl = GlobalLibraries.get();
        LibraryConfiguration bar = new LibraryConfiguration(
                "bar",
                new SCMSourceRetriever(new GitSCMSource(
                        null,
                        "https://github.com/fabric8io/jenkins-pipeline-library",
                        "",
                        "origin",
                        "+refs/heads/*:refs/remotes/origin/*",
                        "*",
                        "",
                        true)));
        bar.setDefaultVersion("master");
        bar.setImplicit(true);
        bar.setAllowVersionOverride(false);
        gl.setLibraries(List.of(bar));
    }

    private void waitForLastBuild(WorkflowJob job) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> job.getLastBuild() != null);
    }
}
