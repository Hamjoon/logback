package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_hasDependers_Test {

    /**
     * The object under test.
     */
    private ModelInterpretationContext mic;

    /**
     * A mock {@link Context} required by the constructor of {@link ModelInterpretationContext}.
     */
    @Mock
    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for every test to avoid cross‑test contamination.
        mic = new ModelInterpretationContext(mockContext);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up at the moment.
        mic = null;
        mic = null;
    }

    /**
     * Sets a private or package‑private field via reflection.
     *
     * @param target    the object whose field should be modified
     * @param name      the field name
     * @param newValue  the new value to assign
     */
    private static void setField(Object target, String name, Object newValue) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, newValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------
    static Stream<Arguments> simpleScenarios() {
        return Stream.of(// (dependeeName, listSetup, expected)
        Arguments.of("myDep", List.of(new DependencyDefinition("myDep", "x")), true), Arguments.of("myDep", List.of(new DependencyDefinition("other", "x")), false), Arguments.of("myDep", List.of(new DependencyDefinition("first", "x"), new DependencyDefinition("myDep", "y")), true));
    }

    @Test
    void returnsFalseWhenDependeeNameIsNull() {
        // given – a fresh context with no dependency definitions
        // when
        boolean result = mic.hasDependers(null);
        // then
        assertFalse(result, "hasDependers(null) should return false when no definitions are present");
    }

    @Test
    void returnsFalseWhenDependeeNameIsEmptyOrBlank() {
        // given – a fresh context with no dependency definitions
        // when
        boolean resultEmpty = mic.hasDependers("");
        boolean resultBlank = mic.hasDependers("   ");
        // then
        assertFalse(resultEmpty, "Empty string should not be considered a valid dependee name");
        assertFalse(resultBlank, "Blank string should not be considered a valid dependee name");
    }

    @Test
    void returnsTrueWhenDependeeIsPresentInDependencyDefinitionList() {
        // given – a dependency definition whose dependee matches the queried name
        DependencyDefinition dd = new DependencyDefinition("myAppender", "someDependent");
        mic.addDependencyDefinition(dd);
        // when
        boolean result = mic.hasDependers("myAppender");
        // then
        assertTrue(result, "hasDependers should return true when a matching dependee is present");
    }

    @Test
    void returnsFalseWhenDependeeIsNotPresentInDependencyDefinitionList() {
        // given – a dependency definition with a different dependee name
        DependencyDefinition dd = new DependencyDefinition("otherAppender", "someDependent");
        mic.addDependencyDefinition(dd);
        // when
        boolean result = mic.hasDependers("myAppender");
        // then
        assertFalse(result, "hasDependers should return false when no matching dependee is found");
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nullOrEmptyInput_returnsFalse() {
        assertFalse(mic.hasDependers(null), "null input must yield false");
        assertFalse(mic.hasDependers(""), "empty string must yield false");
        assertFalse(mic.hasDependers("   "), "blank string must yield false");
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_matchingDependency_returnsTrue() {
        DependencyDefinition dd = new DependencyDefinition("myDep", "someDependent");
        mic.addDependencyDefinition(dd);
        assertTrue(mic.hasDependers("myDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nonMatchingDependency_returnsFalse() {
        DependencyDefinition dd = new DependencyDefinition("otherDep", "someDependent");
        mic.addDependencyDefinition(dd);
        assertFalse(mic.hasDependers("myDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_multipleDefinitionsLaterMatch_returnsTrue() {
        mic.addDependencyDefinition(new DependencyDefinition("firstDep", "x"));
        mic.addDependencyDefinition(new DependencyDefinition("secondDep", "y"));
        assertTrue(mic.hasDependers("secondDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_whitespaceAroundName_returnsFalse() {
        mic.addDependencyDefinition(new DependencyDefinition("myDep", "x"));
        // The method does NOT trim before comparison, so surrounding spaces break the match.
        assertFalse(mic.hasDependers("  myDep  "));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nullDependeeInDefinition_returnsFalse() {
        // Create a definition with a null dependee field.
        DependencyDefinition dd = new DependencyDefinition("placeholder", "x");
        // force null value
        setField(dd, "dependee", null);
        mic.addDependencyDefinition(dd);
        assertFalse(mic.hasDependers("anyDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_duplicateMatchingDefinitions_returnsTrue() {
        mic.addDependencyDefinition(new DependencyDefinition("dupDep", "a"));
        mic.addDependencyDefinition(new DependencyDefinition("dupDep", "b"));
        assertTrue(mic.hasDependers("dupDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_caseSensitivity_returnsFalse() {
        mic.addDependencyDefinition(new DependencyDefinition("mydep", "x"));
        // different case
        assertFalse(mic.hasDependers("MyDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_emptyListValidName_returnsFalse() {
        assertFalse(mic.hasDependers("someDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_manyDefinitionsThenMatch_returnsTrue() {
        for (int i = 0; i < 100; i++) {
            mic.addDependencyDefinition(new DependencyDefinition("dep" + i, "x"));
        }
        mic.addDependencyDefinition(new DependencyDefinition("targetDep", "y"));
        assertTrue(mic.hasDependers("targetDep"));
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nullDependeeField_throwsNPE() {
        DependencyDefinition dd = new DependencyDefinition("placeholder", "x");
        // make the field null
        setField(dd, "dependee", null);
        mic.addDependencyDefinition(dd);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyDep"), "Expected NPE because dd.dependee is null");
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nullElementInList_throwsNPE() {
        // Directly manipulate the internal list to insert a null entry.
        List<DependencyDefinition> list = new ArrayList<>();
        list.add(null);
        setField(mic, "dependencyDefinitionList", list);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyDep"), "Expected NPE because list contains a null element");
    }

    // -----------------------------------------------------------------
    @Test
    void hasDependers_nullDefinitionList_throwsNPE() {
        setField(mic, "dependencyDefinitionList", null);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyDep"), "Expected NPE because dependencyDefinitionList itself is null");
    }

    @ParameterizedTest
    @MethodSource("simpleScenarios")
    void hasDependers_parameterisedScenarios(String name, List<DependencyDefinition> definitions, boolean expected) {
        definitions.forEach(mic::addDependencyDefinition);
        assertEquals(expected, mic.hasDependers(name));
    }
}
