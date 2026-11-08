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
    Context // Mockito will provide a mock Context instance
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The class under test expects a Context in its constructor (as used in the original framework)
        mic = new ModelInterpretationContext(mockContext);
    }

    static Stream<Arguments> emptyListProvider() {
        return Stream.of(Arguments.of("someDependee", false), Arguments.of(null, false));
    }

    static Stream<Arguments> singleMarkProvider() {
        return Stream.of(Arguments.of("myDependee", true), Arguments.of("anotherDependee", false));
    }

    static Stream<Arguments> multipleDependeesProvider() {
        return Stream.of(Arguments.of("firstDep", true), Arguments.of("secondDep", true), Arguments.of("unmarkedDep", false));
    }

    static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(// exact match
        Arguments.of("CaseDep", true), // different case
        Arguments.of("casedep", false));
    }

    static Stream<Arguments> emptyAndWhitespaceProvider() {
        return Stream.of(// empty string was marked
        Arguments.of("", true), // three spaces were marked
        Arguments.of("   ", true), // single space not marked
        Arguments.of(" ", false));
    }

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(// first query
        Arguments.of("dep!@#$$%^&*()_+", true), // repeated query – should stay true
        Arguments.of("dep!@#$$%^&*()_+", true));
    }

    @Test
    void whenDependeeWasMarkedStarted_isNamedDependeeStartedReturnsTrue() {
        // given
        String dependeeName = "myDependee";
        // registers the dependee as started
        mic.markStartOfNamedDependee(dependeeName);
        // when
        boolean result = mic.isNamedDependeeStarted(dependeeName);
        // then
        assertTrue(result, "Expected isNamedDependeeStarted to return true for a started dependee");
    }

    @Test
    void whenDependeeWasNotMarkedStarted_isNamedDependeeStartedReturnsFalse() {
        // given
        String startedDependee = "startedDep";
        String notStartedDependee = "notStartedDep";
        // only this one is started
        mic.markStartOfNamedDependee(startedDependee);
        // when
        boolean result = mic.isNamedDependeeStarted(notStartedDependee);
        // then
        assertFalse(result, "Expected isNamedDependeeStarted to return false for a dependee that was not started");
    }

    @Test
    void whenNullIsPassed_isNamedDependeeStartedReturnsFalse() {
        // given
        // ensure list is non‑empty
        mic.markStartOfNamedDependee("someDep");
        // when
        boolean result = mic.isNamedDependeeStarted(null);
        // then
        assertFalse(result, "Expected isNamedDependeeStarted to return false when null is passed");
    }

    @Test
    void whenMultipleDependeesAreStarted_isNamedDependeeStartedDetectsEachCorrectly() {
        // given
        String first = "firstDep";
        String second = "secondDep";
        String third = "thirdDep";
        mic.markStartOfNamedDependee(first);
        mic.markStartOfNamedDependee(second);
        // third is intentionally not started
        // when / then
        assertTrue(mic.isNamedDependeeStarted(first), "First dependee should be reported as started");
        assertTrue(mic.isNamedDependeeStarted(second), "Second dependee should be reported as started");
        assertFalse(mic.isNamedDependeeStarted(third), "Third dependee should be reported as not started");
    }

    /**
     * 1. Empty {@code startedDependees} list – any name should yield {@code false}.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("emptyListProvider")
    void isNamedDependeeStarted_EmptyList(String name, boolean expected) {
        // No call to markStartOfNamedDependee – the list stays empty
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "With an empty startedDependees list, query for '" + name + "' should return " + expected);
    }

    /**
     * 2. After marking a single dependee – only that exact name returns {@code true}.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("singleMarkProvider")
    void isNamedDependeeStarted_SingleMark(String name, boolean expected) {
        // Arrange: mark exactly one dependee
        mic.markStartOfNamedDependee("myDependee");
        // Act & Assert
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "After marking 'myDependee', query for '" + name + "' should be " + expected);
    }

    /**
     * 3. Multiple distinct dependees – each marked name returns {@code true}, others {@code false}.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("multipleDependeesProvider")
    void isNamedDependeeStarted_MultipleDistinct(String name, boolean expected) {
        // Arrange: mark two distinct dependees
        mic.markStartOfNamedDependee("firstDep");
        mic.markStartOfNamedDependee("secondDep");
        // Act & Assert
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "Query for '" + name + "' should be " + expected);
    }

    /**
     * 4. Duplicate marking – marking the same name twice does not affect the result.
     */
    @Test
    void isNamedDependeeStarted_DuplicateMarking() {
        // Arrange
        mic.markStartOfNamedDependee("dupDep");
        // duplicate
        mic.markStartOfNamedDependee("dupDep");
        // Act & Assert
        assertTrue(mic.isNamedDependeeStarted("dupDep"), "Marking the same dependee twice should still result in true");
    }

    /**
     * 5. Case sensitivity – exact case matches succeed, different case fails.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("caseSensitivityProvider")
    void isNamedDependeeStarted_CaseSensitivity(String name, boolean expected) {
        // Arrange: mark with specific case
        mic.markStartOfNamedDependee("CaseDep");
        // Act & Assert
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "Case-sensitive check for '" + name + "' should be " + expected);
    }

    /**
     * 6. Empty and whitespace strings – they are treated as ordinary keys.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("emptyAndWhitespaceProvider")
    void isNamedDependeeStarted_EmptyAndWhitespace(String name, boolean expected) {
        // Arrange: mark the specific strings that are expected to be true
        // empty string
        mic.markStartOfNamedDependee("");
        // three spaces
        mic.markStartOfNamedDependee("   ");
        // Act & Assert
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "Query for whitespace key '" + name + "' should be " + expected);
    }

    /**
     * 7. Special characters – any string (including special characters) is a valid key.
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected={1}")
    @MethodSource("specialCharactersProvider")
    void isNamedDependeeStarted_SpecialCharacters(String name, boolean expected) {
        // Arrange: mark the special‑character name once
        mic.markStartOfNamedDependee("dep!@#$$%^&*()_+");
        // Act & Assert
        assertEquals(expected, mic.isNamedDependeeStarted(name), () -> "Special‑character name query for '" + name + "' should be " + expected);
    }
}
