package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isNamedDependeeStarted_Test {

    // -----------------------------------------------------------------------
    @Mock
    Context // a dummy Context – its behaviour is not required
    mockContext;

    // the class under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor we want to test needs a Context instance.
        // Using the Mockito‑provided mock is sufficient because the constructor
        // only stores the reference and does not call any methods on it.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this simple test
        mic = null;
    }

    private static Stream<Arguments> positiveScenarios() {
        return Stream.of(// description, name, addTwice
        Arguments.of("After marking start of named dependee \"dep1\", should return true", "dep1", false), Arguments.of("Marking the same dependee name twice should still return true", "duplicate", true));
    }

    private static Stream<Arguments> negativeScenarios() {
        return Stream.of(Arguments.of("Querying a name that was never marked as started should return false", "unknown", false), Arguments.of("When the list contains other names but not \"dep2\", should return false", "dep2", true), Arguments.of("Passing null should safely return false (list does not contain null)", null, false), Arguments.of("Case sensitivity test: after adding \"DepA\", querying \"depa\" should return false", "depa", true), Arguments.of("When startedDependees list is empty, any name query should return false", "any", false));
    }

    private static Stream<Arguments> multipleDependeesScenarios() {
        return Stream.of(Arguments.of("After marking \"depA\" and \"depB\", querying \"depA\" should return true", "depA", List.of("depA", "depB")), Arguments.of("After marking \"depA\" and \"depB\", querying \"depB\" should return true", "depB", List.of("depA", "depB")), Arguments.of("Only \"depA\" and \"depB\" were marked; querying \"depC\" should return false", "depC", List.of("depA", "depB")));
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("returns true when the named dependee has been marked as started")
    void shouldReturnTrueWhenDependeeWasStarted() {
        // given
        String dependeeName = "myDependee";
        // adds the name to startedDependees
        mic.markStartOfNamedDependee(dependeeName);
        // when
        boolean result = mic.isNamedDependeeStarted(dependeeName);
        // then
        assertTrue(result, "Expected isNamedDependeeStarted to return true for a started dependee");
    }

    @Test
    @DisplayName("returns false when the named dependee has not been started")
    void shouldReturnFalseWhenDependeeWasNotStarted() {
        // given
        String startedName = "startedDependee";
        String notStartedName = "notStartedDependee";
        // only this name is recorded
        mic.markStartOfNamedDependee(startedName);
        // when
        boolean result = mic.isNamedDependeeStarted(notStartedName);
        // then
        assertFalse(result, "Expected isNamedDependeeStarted to return false for a non‑started dependee");
    }

    @Test
    @DisplayName("is case‑sensitive – different case yields false")
    void shouldBeCaseSensitive() {
        // given
        String original = "CaseSensitive";
        String differentCase = "casesensitive";
        mic.markStartOfNamedDependee(original);
        // when
        boolean result = mic.isNamedDependeeStarted(differentCase);
        // then
        assertFalse(result, "Expected case‑sensitivity: different case should not be considered started");
    }

    @Test
    @DisplayName("does not modify the internal startedDependees list")
    void shouldNotModifyStateWhenChecking() {
        // given
        String name = "immutableCheck";
        mic.markStartOfNamedDependee(name);
        // direct field access is allowed within the same package
        int sizeBefore = mic.startedDependees.size();
        // when
        mic.isNamedDependeeStarted(name);
        // then
        int sizeAfter = mic.startedDependees.size();
        assertEquals(sizeBefore, sizeAfter, "Calling isNamedDependeeStarted must not alter the internal list");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("positiveScenarios")
    @DisplayName("isNamedDependeeStarted returns true for started dependees")
    void shouldReturnTrueForStartedDependees(String description, String name, boolean addTwice) {
        // given
        mic.markStartOfNamedDependee(name);
        if (addTwice) {
            // add the same name a second time – the list may contain duplicates but
            // the semantics of contains() remain unchanged
            mic.markStartOfNamedDependee(name);
        }
        // when
        boolean result = mic.isNamedDependeeStarted(name);
        // then
        assertTrue(result, description);
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("negativeScenarios")
    @DisplayName("isNamedDependeeStarted returns false for non‑started dependees")
    void shouldReturnFalseForNonStartedDependees(String description, String name, boolean prepopulate) {
        // given
        if (prepopulate) {
            // add some other names so the list is not empty
            mic.markStartOfNamedDependee("other1");
            mic.markStartOfNamedDependee("other2");
        }
        // do NOT add the name under test
        // when
        boolean result = mic.isNamedDependeeStarted(name);
        // then
        assertFalse(result, description);
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("multipleDependeesScenarios")
    @DisplayName("isNamedDependeeStarted works correctly with several started dependees")
    void shouldHandleMultipleStartedDependees(String description, String queryName, List<String> startedNames) {
        // given
        startedNames.forEach(mic::markStartOfNamedDependee);
        // when
        boolean result = mic.isNamedDependeeStarted(queryName);
        // then
        boolean expected = startedNames.contains(queryName);
        assertEquals(expected, result, description);
    }
}
