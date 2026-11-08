package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.processor.DependencyDefinition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_hasDependers_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // logback Context is an interface, a mock is sufficient
    mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of((String) null), Arguments.of(""), Arguments.of("   "));
    }

    private static Stream<Arguments> multipleDefinitionsProvider() {
        return Stream.of(Arguments.of("serviceC", true), Arguments.of("serviceB", true), Arguments.of("unknownService", false));
    }

    private static Stream<Arguments> whitespaceSensitivityProvider() {
        return Stream.of(// exact match including spaces
        Arguments.of("  serviceY  ", true), // same logical name without spaces
        Arguments.of("serviceY", false));
    }

    @Test
    void hasDependers_returnsTrueWhenDependeeIsPresent() {
        // given
        String dependeeName = "myAppender";
        DependencyDefinition dd = new DependencyDefinition(dependeeName, "someDependent");
        mic.addDependencyDefinition(dd);
        // when
        boolean result = mic.hasDependers(dependeeName);
        // then
        assertTrue(result, "hasDependers should return true when a matching dependee is present");
    }

    @Test
    void hasDependers_returnsFalseWhenDependeeIsAbsent() {
        // given
        // add a different dependency definition
        mic.addDependencyDefinition(new DependencyDefinition("otherAppender", "someDependent"));
        // when
        boolean result = mic.hasDependers("myAppender");
        // then
        assertFalse(result, "hasDependers should return false when no matching dependee exists");
    }

    @Test
    void hasDependers_returnsFalseForNullOrEmptyNames() {
        // given
        mic.addDependencyDefinition(new DependencyDefinition("anyAppender", "someDependent"));
        // when / then
        assertFalse(mic.hasDependers(null), "null dependee name should result in false");
        assertFalse(mic.hasDependers(""), "empty string dependee name should result in false");
        assertFalse(mic.hasDependers("   "), "blank string dependee name should result in false");
    }

    /* --------------------------------------------------------------
     *  Group: Invalid input handling
     *  -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => dependeeName=''{0}''")
    @MethodSource("invalidInputProvider")
    void hasDependers_invalidInput_returnsFalse(String dependeeName) {
        // No dependency definitions are added – the method should simply return false
        boolean result = mic.hasDependers(dependeeName);
        assertFalse(result, "Method should return false for invalid input: " + dependeeName);
    }

    /* --------------------------------------------------------------
     *  Group: Dependency matching – no definitions
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_noDefinitions_returnsFalse() {
        // dependencyDefinitionList is empty by default
        boolean result = mic.hasDependers("serviceA");
        assertFalse(result, "When no definitions exist the method must return false");
    }

    /* --------------------------------------------------------------
     *  Group: Dependency matching – positive match
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_positiveMatch_returnsTrue() {
        mic.addDependencyDefinition(new DependencyDefinition("serviceA", "dependentX"));
        assertTrue(mic.hasDependers("serviceA"), "Method should return true when a matching dependee is present");
    }

    /* --------------------------------------------------------------
     *  Group: Dependency matching – negative match
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_negativeMatch_returnsFalse() {
        mic.addDependencyDefinition(new DependencyDefinition("serviceA", "dependentX"));
        assertFalse(mic.hasDependers("serviceB"), "Method should return false when the name does not match any definition");
    }

    /* --------------------------------------------------------------
     *  Group: Multiple dependency definitions
     *  -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => query=''{0}'', expected={1}")
    @MethodSource("multipleDefinitionsProvider")
    void hasDependers_multipleDefinitions(String query, boolean expected) {
        // add three distinct definitions
        mic.addDependencyDefinition(new DependencyDefinition("serviceA", "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition("serviceB", "dep2"));
        mic.addDependencyDefinition(new DependencyDefinition("serviceC", "dep3"));
        boolean result = mic.hasDependers(query);
        assertEquals(expected, result, "Query '" + query + "' should return " + expected + " with three definitions present");
    }

    /* --------------------------------------------------------------
     *  Group: Duplicate dependee names
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_duplicateDependeeNames_returnsTrue() {
        mic.addDependencyDefinition(new DependencyDefinition("sharedService", "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition("sharedService", "dep2"));
        assertTrue(mic.hasDependers("sharedService"), "Method should return true when duplicate dependee names exist");
    }

    /* --------------------------------------------------------------
     *  Group: Case sensitivity
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_caseSensitivity_returnsFalse() {
        mic.addDependencyDefinition(new DependencyDefinition("servicex", "dep"));
        assertFalse(mic.hasDependers("ServiceX"), "Method uses String.equals, therefore case‑sensitive comparison must fail");
    }

    /* --------------------------------------------------------------
     *  Group: Whitespace sensitivity
     *  -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => query=''{0}'', expected={1}")
    @MethodSource("whitespaceSensitivityProvider")
    void hasDependers_whitespaceSensitivity(String query, boolean expected) {
        mic.addDependencyDefinition(new DependencyDefinition("  serviceY  ", "dep"));
        boolean result = mic.hasDependers(query);
        assertEquals(expected, result, "Whitespace handling test failed for query '" + query + "'");
    }

    /* --------------------------------------------------------------
     *  Group: Substring non‑match
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_substringNonMatch_returnsFalse() {
        mic.addDependencyDefinition(new DependencyDefinition("serviceA", "dep"));
        assertFalse(mic.hasDependers("service"), "A substring of the stored name must not match");
    }

    /* --------------------------------------------------------------
     *  Group: Null dependee field in definition
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_nullDependeeInDefinition_throwsNPE() {
        // DependencyDefinition with a null dependee (constructor allows null)
        mic.addDependencyDefinition(new DependencyDefinition(null, "dependent"));
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyName"), "Iterating over a definition with null dependee should throw NPE");
    }

    /* --------------------------------------------------------------
     *  Group: Large dependency list
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_largeList_returnsTrue() {
        // add 9,999 dummy definitions
        for (int i = 0; i < 9_999; i++) {
            mic.addDependencyDefinition(new DependencyDefinition("dummy" + i, "dep"));
        }
        // last definition is the target
        mic.addDependencyDefinition(new DependencyDefinition("targetService", "dep"));
        assertTrue(mic.hasDependers("targetService"), "Method must locate the matching entry even in a large list");
    }

    /* --------------------------------------------------------------
     *  Group: Null element in dependency list
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_nullElementInList_throwsNPE() throws Exception {
        // Use the public accessor to obtain the mutable list and insert a null entry
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        list.add(null);
        // Adding a normal entry after the null to ensure iteration reaches the null element
        mic.addDependencyDefinition(new DependencyDefinition("valid", "dep"));
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyName"), "A null element inside the list should cause NPE during iteration");
    }

    /* --------------------------------------------------------------
     *  Group: Null dependencyDefinitionList field
     *  -------------------------------------------------------------- */
    @Test
    void hasDependers_nullDependencyDefinitionListField_throwsNPE() throws Exception {
        // Set the internal list to null via reflection
        Field field = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        field.setAccessible(true);
        field.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.hasDependers("anyName"), "If the internal list reference is null, a NPE must be thrown");
    }
}
