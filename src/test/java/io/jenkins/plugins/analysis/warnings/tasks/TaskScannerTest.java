package io.jenkins.plugins.analysis.warnings.tasks;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import static edu.hm.hafner.analysis.assertj.Assertions.*;
import edu.hm.hafner.util.ResourceTest;
import io.jenkins.plugins.analysis.warnings.tasks.TaskScanner.CaseMode;
import io.jenkins.plugins.analysis.warnings.tasks.TaskScanner.MatcherMode;

/**
 * Tests the class {@link TaskScanner}.
 *
 * @author Ullrich Hafner
 */
class TaskScannerTest extends ResourceTest {
    private static final String FIXME = "FIXME";
    private static final String CASE_TEST_FILE = "tasks-case-test.txt";
    private static final String PRIORITY_HIGH_MESSAGE = "here another task with priority HIGH";
    private static final String PRIORITY_NORMAL_MESSAGE = "here we have a task with priority NORMAL";
    private static final String FILE_WITH_TASKS = "file-with-tasks.txt";

    @Test
    void shouldReportErrorIfPatternIsInvalid() {
        TaskScanner scanner = new TaskScannerBuilder().setHigh("\\").setMatcherMode(MatcherMode.REGEXP_MATCH).build();

        Report report = scanner.scan(read(FILE_WITH_TASKS));

        assertThat(report).hasSize(0);
        String errorMessage = "Specified pattern is an invalid regular expression: '\\': "
                + "'Unexpected internal error near index 1";
        assertThat(report.getErrorMessages()).hasSize(1);
        assertThat(report.getErrorMessages().get(0)).startsWith(errorMessage);
        
        assertThat(scanner.isInvalidPattern()).isTrue();
        assertThat(scanner.getErrors()).startsWith(errorMessage);
    }

    /**
     * Parses tasks using a regular expression.
     *
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-17225">Issue 17225</a>
     */
    @Test
    void shouldParseRegularExpressionsIssue17225() {
        Report tasks = new TaskScannerBuilder()
                .setHigh("^.*(TODO(?:[0-9]*))(.*)$")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.REGEXP_MATCH)
                .build()
                .scan(read("regexp.txt"));

        assertThat(tasks).hasSize(5);
        assertThat(tasks.get(0)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("TODO1")
                .hasLineStart(1)
                .hasMessage("erstes");
        assertThat(tasks.get(1)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("TODO2")
                .hasLineStart(2)
                .hasMessage("zweites");
        assertThat(tasks.get(2)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("TODO3")
                .hasLineStart(3)
                .hasMessage("drittes");
        assertThat(tasks.get(3)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("TODO4")
                .hasLineStart(4)
                .hasMessage("viertes");
        assertThat(tasks.get(4)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("TODO20")
                .hasLineStart(5)
                .hasMessage("zwanzigstes");
    }

    /**
     * Parses a warning log with characters in different locale.
     *
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-22744">Issue 22744</a>
     */
    @Test
    void issue22744() {
        Report tasks = new TaskScannerBuilder()
                .setHigh("FIXME")
                .setNormal("TODO")
                .setLow("")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read("issue22744.java", "windows-1251"));

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0)).hasSeverity(Severity.WARNING_HIGH)
                .hasType("FIXME")
                .hasLineStart(4)
                .hasMessage("\u0442\u0435\u0441\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 Jenkins");
        assertThat(tasks.get(1)).hasSeverity(Severity.WARNING_NORMAL)
                .hasType("TODO")
                .hasLineStart(5)
                .hasMessage("\u043f\u0440\u0438\u043c\u0435\u0440 \u043a\u043e\u043c\u043c\u0435\u043d\u0442\u0430\u0440\u0438\u044f \u043d\u0430 \u0440\u0443\u0441\u0441\u043a\u043e\u043c");
    }

    /**
     * Parses a warning log with !!! and !!!! warnings.
     *
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-12782">Issue 12782</a>
     */
    @Test
    void issue12782() {
        Report tasks = new TaskScannerBuilder()
                .setHigh("!!!!!")
                .setNormal("!!!")
                .setLow("")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read("issue12782.txt"));

        assertThat(tasks).hasSize(3);
    }

    /**
     * Checks whether we find tasks at word boundaries.
     */
    @Test
    void shouldScanFileWithWords() {
        Report tasks = new TaskScannerBuilder()
                .setHigh("WARNING")
                .setNormal("TODO")
                .setLow("@todo")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read("tasks-words-test.txt"));

        assertThat(tasks).hasSize(12)
                .hasSeverities(0, 0, 7, 5);
    }

    /**
     * Checks case sensitivity.
     */
    @Test
    void shouldIgnoreCase() {
        verifyOneTaskWhenCheckingCase("todo", 25);
        verifyOneTaskWhenCheckingCase("ToDo", 27);
    }

    private void verifyOneTaskWhenCheckingCase(final String tag, final int lineNumber) {
        Report tasks = new TaskScannerBuilder()
                .setNormal(tag)
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(CASE_TEST_FILE));

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0))
                .hasSeverity(Severity.WARNING_NORMAL)
                .hasType(tag)
                .hasLineStart(lineNumber)
                .hasMessage("");
    }

    @Test
    void shouldIgnoreCaseInSource() {
        Report tasks = new TaskScannerBuilder()
                .setNormal("todo")
                .setCaseMode(CaseMode.IGNORE_CASE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(CASE_TEST_FILE));

        assertThat(tasks).hasSize(9);
        for (Issue task : tasks) {
            assertThat(task).hasType("TODO");
        }
    }

    @Test
    void shouldIgnoreCaseInTag() {
        Report tasks = new TaskScannerBuilder()
                .setNormal("Todo, TodoS")
                .setCaseMode(CaseMode.IGNORE_CASE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(CASE_TEST_FILE));

        assertThat(tasks).hasSize(12);
        for (Issue task : tasks) {
            assertThat(task.getType()).startsWith("TODO");
        }
    }

    /**
     * Checks whether we find the two task in the test file.
     */
    @Test
    void shouldUseDefaults() {
        Report tasks = new TaskScannerBuilder().setHigh("FIXME")
                .setNormal("TODO")
                .setLow("@deprecated")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(FILE_WITH_TASKS));

        assertThat(tasks).hasSize(2)
                .hasSeverities(0, 1, 1, 0);
        assertThat(tasks.get(0)).hasMessage(PRIORITY_NORMAL_MESSAGE);
        assertThat(tasks.get(1)).hasMessage(PRIORITY_HIGH_MESSAGE);
    }

    /**
     * Checks whether we find one high priority task in the test file.
     */
    @Test
    void shouldFindHighPriority() {
        Report tasks = new TaskScannerBuilder().setHigh(FIXME)
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(FILE_WITH_TASKS));

        assertThat(tasks).hasSize(1)
                .hasSeverities(0, 1, 0, 0);
    }

    /**
     * Checks whether we correctly strip whitespace from the message.
     */
    @Test
    void shouldIgnoreSpaceInTags() {
        Report tasks = new TaskScannerBuilder().setHigh(" FIXME , TODO ")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(FILE_WITH_TASKS));

        assertThat(tasks).hasSize(2)
                .hasSeverities(0, 2, 0, 0);
    }

    /**
     * Checks whether we find two high priority tasks with different identifiers in the test file.
     */
    @Test
    void shouldHaveTwoItemsWithHighPriority() {
        Report tasks = new TaskScannerBuilder().setHigh("FIXME,TODO")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(FILE_WITH_TASKS));

        assertThat(tasks).hasSize(2)
                .hasSeverities(0, 2, 0, 0);
    }

    /**
     * Checks whether we set the type of the task to the actual tag.
     */
    @Test
    void shouldIdentifyTags() {
        String text = "FIXME: this is a fixme";
        Report high = new TaskScannerBuilder()
                .setHigh("FIXME,TODO")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(new StringReader(text));

        assertThat(high).hasSize(1);
        assertThat(high.get(0)).hasType(FIXME);

        Report normal = new TaskScannerBuilder()
                .setNormal("XXX, HELP, FIXME, TODO")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(new StringReader(text));

        assertThat(normal).hasSize(1);
        assertThat(normal.get(0)).hasType(FIXME);
    }

    /**
     * Checks whether we find all priority tasks in the test file.
     */
    @Test
    void shouldScanAllPriorities() {
        Report tasks = new TaskScannerBuilder().setHigh(FIXME)
                .setNormal("FIXME,TODO")
                .setLow("TODO")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(read(FILE_WITH_TASKS));

        assertThat(tasks).hasSize(4)
                .hasSeverities(0, 1, 2, 1);
    }

    /**
     * Checks whether we find no task in the test file.
     */
    @Test
    void shouldScanFileWithoutTasks() {
        InputStreamReader reader = read("file-without-tasks.txt");

        Report tasks = new TaskScannerBuilder().setHigh("FIXME")
                .setNormal("TODO")
                .setLow("@deprecated")
                .setCaseMode(CaseMode.CASE_SENSITIVE)
                .setMatcherMode(MatcherMode.STRING_MATCH)
                .build()
                .scan(reader);

        assertThat(tasks).hasSize(0);
    }

    private InputStreamReader read(final String fileName) {
        return read(fileName, "UTF-8");
    }

    private InputStreamReader read(final String fileName, final String charset) {
        return new InputStreamReader(asInputStream(fileName), Charset.forName(charset));
    }
}

