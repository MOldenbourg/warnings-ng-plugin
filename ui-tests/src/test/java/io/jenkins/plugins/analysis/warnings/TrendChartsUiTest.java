package io.jenkins.plugins.analysis.warnings;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;

import static io.jenkins.plugins.analysis.warnings.Assertions.*;

/**
 * Ui test for the Trend Chart Page.
 *
 * @author Mitja Oldenbourg
 */
@WithPlugins("warnings-ng")
public class TrendChartsUiTest extends AbstractJUnitTest {
    private static final String WARNINGS_PLUGIN_PREFIX = "/";
    private static final String SOURCE_VIEW_FOLDER = WARNINGS_PLUGIN_PREFIX + "trend_charts_tests/";

    /**
     * Shows all three Trend Charts for the build.
     */
    @Test
    public void ShouldDisplayDifferentTrendChartsOnClick() {
        FreeStyleJob job = createFreeStyleJob("build_01");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setToolWithPattern("Java", "**/*.txt"));
        job.save();

        shouldBuildJobSuccessfully(job);
        reconfigureJobWithResource(job, "build_02");

        Build build = shouldBuildJobSuccessfully(job);

        AnalysisResult analysisResultPage = new AnalysisResult(build, "java");
        analysisResultPage.open();
        WebElement trendChart = analysisResultPage.getTrendChart();
        WebElement nextButton = trendChart.findElement(By.className("carousel-control-next-icon"));

        boolean severitiesDisplayed = trendChart.findElement(By.id("severities-trend-chart")).isDisplayed();

        nextButton.click();
        boolean toolsDisplayed = trendChart.findElement(By.id("tools-trend-chart")).isDisplayed();

        nextButton.click();
        boolean newVsFixedDisplayed = trendChart.findElement(By.id("new-versus-fixed-trend-chart")).isDisplayed();

        assertThat(severitiesDisplayed).isTrue();
        assertThat(toolsDisplayed).isTrue();
        assertThat(newVsFixedDisplayed).isTrue();
    }

    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.jobs.create(FreeStyleJob.class);
        ScrollerUtil.hideScrollerTabBar(driver);
        for (String resource : resourcesToCopy) {
            job.copyResource(SOURCE_VIEW_FOLDER + resource);
        }
        return job;
    }

    private Build shouldBuildJobSuccessfully(final Job job) {
        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();
        return build;
    }

    private void reconfigureJobWithResource(final FreeStyleJob job, final String resource) {
        job.configure(() -> job.copyResource(SOURCE_VIEW_FOLDER + resource));
    }
}
