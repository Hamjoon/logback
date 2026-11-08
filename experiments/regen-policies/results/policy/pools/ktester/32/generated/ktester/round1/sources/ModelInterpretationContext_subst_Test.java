package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import ch.qos.logback.core.model.util.VariableSubstitutionsException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.anyString;

class ModelInterpretationContext_subst_Test {

    private ModelInterpretationContext mic;

    private ContextBase context;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation so that VariableSubstitutionsHelper can read properties
        context = new ContextBase();
        // Core properties used by many test cases
        context.putProperty("myKey", "myValue");
        context.putProperty("emptyKey", "");
        context.putProperty("user.name", "John Doe");
        context.putProperty("first", "A");
        context.putProperty("second", "B");
        context.putProperty("user.age", "30");
        // 'code' is intentionally left undefined to test unknown placeholders
        // Create the object under test
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        mic = null;
        context = null;
    }

    private static Stream<Arguments> standardSubstitutionProvider() {
        return Stream.of(Arguments.of("Substitutes a single known variable placeholder with its value", "${user.name}", "John Doe"), Arguments.of("Substitutes a variable embedded in a larger string", "Hello ${user.name}, welcome!", "Hello John Doe, welcome!"), Arguments.of("Substitutes multiple adjacent variables", "${first}${second}", "AB"));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("Empty string should be returned unchanged", "", ""), Arguments.of("Null input should be handled gracefully (returns null)", null, null), Arguments.of("String without any placeholders should be returned unchanged", "No placeholders", "No placeholders"), Arguments.of("Unknown variable should remain unchanged", "${unknown}", "${unknown}"), Arguments.of("Malformed placeholder (missing closing brace) should be returned unchanged", "${user.name", "${user.name"), Arguments.of("Empty placeholder should be returned unchanged", "${}", "${}"), Arguments.of("Repeated variable placeholders should each be substituted", "${user.name}${user.name}", "John DoeJohn Doe"));
    }

    private static Stream<Arguments> mixedVariablesProvider() {
        return Stream.of(Arguments.of("A string containing a known variable, an unknown variable, and another known variable", "${user.name}${unknown}${second}", "John Doe${unknown}B"), Arguments.of("Mixes several known placeholders with an unknown one", "User: ${user.name}, Age: ${user.age}, Code: ${code}", "User: John Doe, Age: 30, Code: ${code}"));
    }

    /**
     * Uses reflection to replace the private {@code variableSubstitutionsHelper}
     * field of the {@link ModelInterpretationContext} under test.
     *
     * @param helper the helper instance to inject (may be {@code null})
     * @throws Exception if reflection fails
     */
    private void setHelperViaReflection(Object helper) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        f.set(mic, helper);
    }

    @Test
    void substReplacesExistingProperty() {
        // given
        String input = "${myKey}";
        // when
        String result = mic.subst(input);
        // then
        assertEquals("myValue", result, "Variable substitution should replace ${myKey} with its value");
    }

    @Test
    void substLeavesStringUnchangedWhenNoPlaceholder() {
        // given
        String input = "plainString";
        // when
        String result = mic.subst(input);
        // then
        assertEquals("plainString", result, "String without placeholders should be returned unchanged");
    }

    @Test
    void substLeavesUndefinedPropertyUnchanged() {
        // given
        String input = "${unknownKey}";
        // when
        String result = mic.subst(input);
        // then
        // VariableSubstitutionsHelper leaves unknown placeholders as‑is
        assertEquals("${unknownKey}", result, "Undefined property placeholders should remain unchanged");
    }

    @Test
    void substHandlesEmptyPropertyValue() {
        // given
        String input = "${emptyKey}";
        // when
        String result = mic.subst(input);
        // then
        // Empty property value should be substituted with an empty string
        assertEquals("", result, "Empty property values should be substituted with an empty string");
    }

    @Test
    void substReplacesMultiplePlaceholdersInOneString() {
        // given
        context.putProperty("first", "1st");
        context.putProperty("second", "2nd");
        String input = "Values: ${first} and ${second}";
        // when
        String result = mic.subst(input);
        // then
        assertEquals("Values: 1st and 2nd", result, "All placeholders in the string should be replaced with their corresponding values");
    }

    // ---------- Standard Substitution ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardSubstitutionProvider")
    void standardSubstitution(String description, String ref, String expected) {
        assertEquals(expected, mic.subst(ref), description);
    }

    // ---------- Edge Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesProvider")
    void edgeCases(String description, String ref, Object expected) {
        if (expected == null) {
            // Expected a null return value
            assertNull(mic.subst(ref), description);
        } else {
            assertEquals(expected, mic.subst(ref), description);
        }
    }

    // ---------- Mixed Known and Unknown Variables ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mixedVariablesProvider")
    void mixedKnownAndUnknown(String description, String ref, String expected) {
        assertEquals(expected, mic.subst(ref), description);
    }

    // ---------- Exception Handling ----------
    @Test
    void substPropagatesVariableSubstitutionsException() throws Exception {
        // Arrange: mock helper to throw the checked exception
        VariableSubstitutionsHelper mockHelper = Mockito.mock(VariableSubstitutionsHelper.class);
        Mockito.when(mockHelper.subst(anyString())).thenThrow(new VariableSubstitutionsException("forced failure"));
        setHelperViaReflection(mockHelper);
        // Act & Assert
        VariableSubstitutionsException ex = assertThrows(VariableSubstitutionsException.class, () -> mic.subst("${error}"), "VariableSubstitutionsException should be propagated unchanged");
        assertEquals("forced failure", ex.getMessage());
    }

    // ---------- Null Helper ----------
    @Test
    void substThrowsNullPointerExceptionWhenHelperIsNull() throws Exception {
        // Arrange: force the helper field to null
        setHelperViaReflection(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> mic.subst("anyString"), "Calling subst with a null VariableSubstitutionsHelper should throw NPE");
    }

    // ---------- Unchecked Exception Propagation ----------
    @Test
    void substPropagatesIllegalArgumentExceptionFromHelper() throws Exception {
        VariableSubstitutionsHelper mockHelper = Mockito.mock(VariableSubstitutionsHelper.class);
        Mockito.when(mockHelper.subst(anyString())).thenThrow(new IllegalArgumentException("illegal placeholder"));
        setHelperViaReflection(mockHelper);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mic.subst("${trigger}"), "IllegalArgumentException should be propagated unchanged");
        assertEquals("illegal placeholder", ex.getMessage());
    }

    @Test
    void substPropagatesIllegalStateExceptionFromHelper() throws Exception {
        VariableSubstitutionsHelper mockHelper = Mockito.mock(VariableSubstitutionsHelper.class);
        Mockito.when(mockHelper.subst(anyString())).thenThrow(new IllegalStateException("runtime problem"));
        setHelperViaReflection(mockHelper);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mic.subst("${runtime}"), "IllegalStateException should be propagated unchanged");
        assertEquals("runtime problem", ex.getMessage());
    }
}
