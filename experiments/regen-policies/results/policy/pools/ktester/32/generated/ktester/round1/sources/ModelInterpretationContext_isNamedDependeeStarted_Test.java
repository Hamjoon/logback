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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isNamedDependeeStarted_Test {

    @Mock
    Context // mocked dependency required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for every test method
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference to help GC; not strictly required but keeps the pattern consistent
        mic = null;
    }

    static Stream<Arguments> startedDependeePresenceProvider() {
        return Stream.of(Arguments.of("myDep", true, "After calling markStartOfNamedDependee(\"myDep\"), isNamedDependeeStarted(\"myDep\") should return true."), Arguments.of("  dep  ", true, "Whitespace is part of the string. After marking a name that contains leading/trailing spaces, the same exact string should be recognised as started."), Arguments.of("Dep", true, "Case‑sensitive match: after marking \"Dep\", querying with the same case returns true."));
    }

    static Stream<Arguments> startedDependeeAbsenceProvider() {
        return Stream.of(Arguments.of("otherDep", false, "Even though \"myDep\" was marked as started, a different name \"otherDep\" has not been added, so isNamedDependeeStarted must return false."), Arguments.of("dep", false, "Case‑sensitivity test: after marking \"Dep\", querying with lower‑case \"dep\" should return false."), Arguments.of("nonexistent", false, "When the startedDependees list is empty (no calls to markStartOfNamedDependee), any name queried must return false."));
    }

    static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of(null, false, "Calling isNamedDependeeStarted(null) should safely return false (the underlying List.contains handles null) and must not throw a NullPointerException."), Arguments.of("", false, "An empty string that has never been marked as started should result in false."));
    }

    static Stream<Arguments> multipleDependeeHandlingProvider() {
        return Stream.of(Arguments.of("alpha", true, "After calling markStartOfNamedDependee(\"alpha\") and markStartOfNamedDependee(\"beta\"), querying isNamedDependeeStarted(\"alpha\") should return true."), Arguments.of("beta", true, "After the same two marks, querying isNamedDependeeStarted(\"beta\") should also return true."), Arguments.of("gamma", false, "Only \"alpha\" and \"beta\" were marked as started; querying an unmarked name \"gamma\" must return false."));
    }

    @Test
    void isNamedDependeeStarted_ReturnsFalse_WhenNoDependeeWasMarked() {
        // GIVEN – a fresh ModelInterpretationContext (set up in @BeforeEach)
        // WHEN
        boolean result = mic.isNamedDependeeStarted("myDependee");
        // THEN
        assertFalse(result, "Expected false because no dependee has been marked as started");
    }

    @Test
    void isNamedDependeeStarted_ReturnsTrue_AfterMarkStartOfNamedDependee() {
        // GIVEN
        String name = "serviceA";
        mic.markStartOfNamedDependee(name);
        // WHEN
        boolean result = mic.isNamedDependeeStarted(name);
        // THEN
        assertTrue(result, "Expected true because the dependee was marked as started");
    }

    @Test
    void isNamedDependeeStarted_IsIndependentForDifferentNames() {
        // GIVEN
        String started = "startedDep";
        String notStarted = "notStartedDep";
        mic.markStartOfNamedDependee(started);
        // WHEN
        boolean startedResult = mic.isNamedDependeeStarted(started);
        boolean notStartedResult = mic.isNamedDependeeStarted(notStarted);
        // THEN
        assertTrue(startedResult, "Started dependee should be reported as started");
        assertFalse(notStartedResult, "Dependee that was never started should be reported as not started");
    }

    @Test
    void isNamedDependeeStarted_HandlesMultipleMarkCalls() {
        // GIVEN
        mic.markStartOfNamedDependee("first");
        mic.markStartOfNamedDependee("second");
        mic.markStartOfNamedDependee("third");
        // WHEN / THEN
        assertTrue(mic.isNamedDependeeStarted("first"));
        assertTrue(mic.isNamedDependeeStarted("second"));
        assertTrue(mic.isNamedDependeeStarted("third"));
    }

    @Test
    void isNamedDependeeStarted_IsCaseSensitive() {
        // GIVEN
        mic.markStartOfNamedDependee("MyDep");
        // WHEN
        boolean exactCase = mic.isNamedDependeeStarted("MyDep");
        boolean differentCase = mic.isNamedDependeeStarted("mydep");
        // THEN
        assertTrue(exactCase, "Exact case match should be true");
        assertFalse(differentCase, "Different case should be false (list uses case‑sensitive contains)");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{2}")
    @MethodSource("startedDependeePresenceProvider")
    void testStartedDependeePresence(String name, boolean expected, String description) {
        mic.markStartOfNamedDependee(name);
        assertEquals(expected, mic.isNamedDependeeStarted(name), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{2}")
    @MethodSource("startedDependeeAbsenceProvider")
    void testStartedDependeeAbsence(String queryName, boolean expected, String description) {
        // Setup according to the description:
        // - For "otherDep" we first mark "myDep".
        // - For "dep" we first mark "Dep".
        // - For "nonexistent" we do not mark anything.
        if ("otherDep".equals(queryName)) {
            mic.markStartOfNamedDependee("myDep");
        } else if ("dep".equals(queryName)) {
            mic.markStartOfNamedDependee("Dep");
        }
        assertEquals(expected, mic.isNamedDependeeStarted(queryName), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{2}")
    @MethodSource("edgeCasesProvider")
    void testEdgeCases(String name, boolean expected, String description) {
        // For the null case we first mark null to reflect the JSON expectation.
        if (name == null) {
            mic.markStartOfNamedDependee(null);
        }
        assertEquals(expected, mic.isNamedDependeeStarted(name), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{2}")
    @MethodSource("multipleDependeeHandlingProvider")
    void testMultipleDependeeHandling(String queryName, boolean expected, String description) {
        // Mark both "alpha" and "beta" before each query.
        mic.markStartOfNamedDependee("alpha");
        mic.markStartOfNamedDependee("beta");
        assertEquals(expected, mic.isNamedDependeeStarted(queryName), description);
    }

    // -----------------------------------------------------------------
    @Test
    void testDuplicateMarking() {
        // Mark the same name twice.
        mic.markStartOfNamedDependee("alpha");
        mic.markStartOfNamedDependee("alpha");
        assertTrue(mic.isNamedDependeeStarted("alpha"), "Calling markStartOfNamedDependee(\"alpha\") twice should not affect the result; isNamedDependeeStarted(\"alpha\") must still return true.");
    }

    // -----------------------------------------------------------------
    @Test
    void testNullMarking() {
        // Mark a null value.
        mic.markStartOfNamedDependee(null);
        assertTrue(mic.isNamedDependeeStarted(null), "If markStartOfNamedDependee(null) is invoked, the internal list will contain a null entry; consequently isNamedDependeeStarted(null) should return true and not throw an exception.");
    }

    // -----------------------------------------------------------------
    @Test
    void testWhitespaceSensitivity() {
        mic.markStartOfNamedDependee("  spaced  ");
        assertFalse(mic.isNamedDependeeStarted("spaced"), "After marking a name with surrounding spaces (\"  spaced  \"), querying the trimmed version (\"spaced\") must return false, confirming exact‑match behavior.");
    }

    // -----------------------------------------------------------------
    @Test
    void testUnicodeSensitivity() {
        mic.markStartOfNamedDependee("café");
        assertTrue(mic.isNamedDependeeStarted("café"), "Marking a name containing a Unicode character (\"café\") should be recognised exactly; isNamedDependeeStarted(\"café\") returns true.");
        assertFalse(mic.isNamedDependeeStarted("cafe"), "A similar name without the accent (\"cafe\") has not been marked, so isNamedDependeeStarted(\"cafe\") must return false, proving case‑ and accent‑sensitivity.");
    }

    // -----------------------------------------------------------------
    @Test
    void testSpecialCharacters() {
        mic.markStartOfNamedDependee("name-with-dash");
        assertTrue(mic.isNamedDependeeStarted("name-with-dash"), "Names containing hyphens are valid identifiers; after markStartOfNamedDependee(\"name-with-dash\"), the method should return true for the same string.");
    }
}
