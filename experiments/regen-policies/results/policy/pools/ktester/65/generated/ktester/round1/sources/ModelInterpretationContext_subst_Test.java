package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_subst_Test {

    /**
     * The context used by the {@link ModelInterpretationContext} under test.
     */
    private Context context;

    /**
     * Instance of the class whose {@code subst} method we are testing.
     */
    private ModelInterpretationContext mic;

    /**
     * Holds a single test case definition.
     */
    private static class TestCase {

        final String ref;

        // either the expected string or the fully‑qualified exception class name
        final String expected;

        final String description;

        TestCase(String ref, String expected, String description) {
            this.ref = ref;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use Logback's simple Context implementation.
        context = new ContextBase();
        // Create the ModelInterpretationContext that we will exercise.
        mic = new ModelInterpretationContext(context);
        // Register a few substitution properties that are used by many test cases.
        mic.addSubstitutionProperty("myVar", "myValue");
        mic.addSubstitutionProperty("empty", "");
        // Common properties for the JSON driven tests.
        mic.addSubstitutionProperty("name", "World");
        mic.addSubstitutionProperty("greeting", "Hi");
        mic.addSubstitutionProperty("dir", "/usr/local");
        mic.addSubstitutionProperty("file", "config.xml");
        mic.addSubstitutionProperty("user", "john");
        mic.addSubstitutionProperty("inner", "Key");
        mic.addSubstitutionProperty("outerKey", "finalValue");
        mic.addSubstitutionProperty("outer", "${inner}");
        mic.addSubstitutionProperty("fallback", "fallbackValue");
        mic.addSubstitutionProperty("a", "1");
        mic.addSubstitutionProperty("b", "2");
        mic.addSubstitutionProperty("c", "3");
        mic.addSubstitutionProperty("d", "4");
        mic.addSubstitutionProperty("e", "5");
        mic.addSubstitutionProperty("f", "6");
        mic.addSubstitutionProperty("g", "7");
        mic.addSubstitutionProperty("h", "8");
        mic.addSubstitutionProperty("i", "9");
        mic.addSubstitutionProperty("j", "10");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and avoid accidental reuse between tests.
        mic = null;
        context = null;
    }

    /**
     * Provides all test cases grouped by logical feature.
     */
    private static Stream<Arguments> provideTestCases() {
        List<Arguments> args = new ArrayList<>();
        // Simple variable substitution
        args.addAll(casesFromGroup("Simple variable substitution", List.of(new TestCase("Hello ${name}", "Hello World", "The helper replaces a single variable placeholder ${name} with the value \"World\"."), new TestCase("${greeting}", "Hi", "When the whole string is a variable, it is replaced entirely with its value."))));
        // Multiple variable substitution
        args.addAll(casesFromGroup("Multiple variable substitution", List.of(new TestCase("${greeting}, ${name}!", "Hi, World!", "Two placeholders are resolved in a single pass."), new TestCase("Path: ${dir}/${file}", "Path: /usr/local/config.xml", "Placeholders can appear adjacent to each other and with surrounding text."))));
        // Missing or undefined variables
        args.addAll(casesFromGroup("Missing or undefined variables", List.of(new TestCase("Value is ${unknown}", "Value is ${unknown}", "If a variable is not defined, the placeholder is left unchanged."), new TestCase("${missing}", "${missing}", "A whole‑string placeholder that cannot be resolved is returned as‑is."))));
        // Default value syntax
        args.addAll(casesFromGroup("Default value syntax", List.of(new TestCase("${env:-dev}", "dev", "When a variable is undefined, the default value after \" :- \" is used."), new TestCase("${user:-anonymous}", "john", "If the variable is defined, its value overrides the default."))));
        // Empty and null inputs
        args.addAll(casesFromGroup("Empty and null inputs", List.of(new TestCase("", "", "An empty string is returned unchanged."), new TestCase(null, "java.lang.NullPointerException", "Passing null should result in a NullPointerException (the helper does not accept null)."))));
        // Nested variable substitution
        args.addAll(casesFromGroup("Nested variable substitution", List.of(new TestCase("${outer${inner}}", "finalValue", "The helper resolves the inner placeholder first (${inner}= \"Key\"), then resolves the outer placeholder (${outerKey}= \"finalValue\")."))));
        // No substitution needed
        args.addAll(casesFromGroup("No substitution needed", List.of(new TestCase("Just a plain string", "Just a plain string", "String without any placeholders is returned unchanged."))));
        // Variable with empty default
        args.addAll(casesFromGroup("Variable with empty default", List.of(new TestCase("${undefinedVar:-}", "", "When the variable is undefined and the default after :- is empty, an empty string is returned."))));
        // Colon syntax without dash (unsupported)
        args.addAll(casesFromGroup("Colon syntax without dash (unsupported)", List.of(new TestCase("${var:default}", "${var:default}", "Placeholder using ':' without '-' is not recognized and is left as‑is."))));
        // Placeholder with spaces
        args.addAll(casesFromGroup("Placeholder with spaces", List.of(new TestCase("${ name }", "${ name }", "Variable names containing spaces are invalid; the placeholder is left unchanged."))));
        // Variable value contains another placeholder
        args.addAll(casesFromGroup("Variable value contains another placeholder", List.of(new TestCase("${outer}", "innerValue", "The value of 'outer' is another placeholder (${inner}); the helper resolves it recursively to 'innerValue'."))));
        // Default value contains a placeholder
        args.addAll(casesFromGroup("Default value contains a placeholder", List.of(new TestCase("${missing:-${fallback}}", "fallbackValue", "Default part '${fallback}' is resolved after the missing variable is detected, yielding 'fallbackValue'."))));
        // Helper is null (null VariableSubstitutionsHelper)
        args.addAll(casesFromGroup("Helper is null (null VariableSubstitutionsHelper)", List.of(new TestCase("any", "java.lang.NullPointerException", "If variableSubstitutionsHelper is null, calling subst triggers a NullPointerException."))));
        // Large input with many placeholders
        args.addAll(casesFromGroup("Large input with many placeholders", List.of(new TestCase("${a}-${b}-${c}-${d}-${e}-${f}-${g}-${h}-${i}-${j}", "1-2-3-4-5-6-7-8-9-10", "Stress test with ten distinct placeholders resolved in a single string."))));
        // Malformed placeholder syntax
        args.addAll(casesFromGroup("Malformed placeholder syntax", List.of(new TestCase("${name", "java.lang.IllegalArgumentException", "Missing closing brace should cause VariableSubstitutionsHelper to throw IllegalArgumentException."), new TestCase("Hello ${name}}", "java.lang.IllegalArgumentException", "Extra closing brace makes the placeholder malformed and should trigger IllegalArgumentException."))));
        // Empty placeholder
        args.addAll(casesFromGroup("Empty placeholder", List.of(new TestCase("${}", "java.lang.IllegalArgumentException", "An empty variable name inside ${} is invalid and must raise IllegalArgumentException."))));
        // Invalid characters in variable name
        args.addAll(casesFromGroup("Invalid characters in variable name", List.of(new TestCase("${na!me}", "java.lang.IllegalArgumentException", "Variable names containing characters other than alphanumerics and '_' are illegal and should cause IllegalArgumentException."))));
        // Circular variable reference
        args.addAll(casesFromGroup("Circular variable reference", List.of(new TestCase("${a}", "java.lang.IllegalStateException", "Variables a -> ${b}, b -> ${a} create a circular reference; the helper should detect it and throw IllegalStateException."))));
        // Helper throws unexpected runtime exception
        args.addAll(casesFromGroup("Helper throws unexpected runtime exception", List.of(new TestCase("any", "java.lang.RuntimeException: boom", "When VariableSubstitutionsHelper is replaced by a stub that throws RuntimeException, ModelInterpretationContext.subst must propagate that exception."))));
        return args.stream();
    }

    /**
     * Helper to convert a group of {@link TestCase}s into {@link Arguments}.
     */
    private static List<Arguments> casesFromGroup(String groupName, List<TestCase> cases) {
        List<Arguments> list = new ArrayList<>();
        for (TestCase tc : cases) {
            list.add(Arguments.of(groupName, tc.ref, tc.expected, tc.description));
        }
        return list;
    }

    /**
     * Utility method that uses reflection to replace the {@code variableSubstitutionsHelper}
     * field inside {@link ModelInterpretationContext}.
     *
     * @param helper the helper instance to set (may be {@code null})
     * @throws Exception if reflection fails
     */
    private void setHelperViaReflection(VariableSubstitutionsHelper helper) throws Exception {
        Field field = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        field.setAccessible(true);
        field.set(mic, helper);
    }

    @Test
    void subst_replacesSimpleVariable() {
        // GIVEN
        String input = "${myVar}";
        // WHEN
        String result = mic.subst(input);
        // THEN
        assertEquals("myValue", result, "Variable substitution should replace ${myVar} with its value");
    }

    @Test
    void subst_returnsUnchangedStringWhenNoVariable() {
        // GIVEN
        String input = "plainText";
        // WHEN
        String result = mic.subst(input);
        // THEN
        assertEquals("plainText", result, "String without variables should be returned unchanged");
    }

    @Test
    void subst_handlesMultipleVariablesInOneString() {
        // GIVEN
        mic.addSubstitutionProperty("second", "2nd");
        String input = "first=${myVar}, second=${second}";
        // WHEN
        String result = mic.subst(input);
        // THEN
        assertEquals("first=myValue, second=2nd", result);
    }

    @Test
    void subst_returnsEmptyStringForEmptyVariableValue() {
        // GIVEN
        String input = "${empty}";
        // WHEN
        String result = mic.subst(input);
        // THEN
        assertEquals("", result, "Variable with empty value should result in empty string");
    }

    @Test
    void subst_leavesUnknownVariableUnchanged() {
        // GIVEN
        String input = "${unknown}";
        // WHEN
        String result = mic.subst(input);
        // THEN
        // Logback's VariableSubstitutionsHelper leaves unknown variables untouched.
        assertEquals("${unknown}", result);
    }

    /**
     * Parameterised test that drives all scenarios from the JSON definition.
     *
     * <p>Depending on the {@code expected} value the test either asserts the
     * returned string or asserts that a particular exception (and optionally its
     * message) is thrown.</p>
     */
    @ParameterizedTest(name = "[{index}] {0}: {3}")
    @MethodSource("provideTestCases")
    void subst_parameterised(String group, String ref, String expected, String description) throws Exception {
        // -----------------------------------------------------------------
        // Special preparation for a few edge‑cases
        // -----------------------------------------------------------------
        if ("Helper is null (null VariableSubstitutionsHelper)".equals(group)) {
            // Force the helper to null via reflection.
            setHelperViaReflection(null);
        } else if ("Helper throws unexpected runtime exception".equals(group)) {
            // Create a mock helper that throws RuntimeException("boom").
            VariableSubstitutionsHelper mockHelper = mock(VariableSubstitutionsHelper.class);
            when(mockHelper.subst(anyString())).thenThrow(new RuntimeException("boom"));
            setHelperViaReflection(mockHelper);
        } else if ("Circular variable reference".equals(group)) {
            // a -> ${b}, b -> ${a}
            mic.addSubstitutionProperty("a", "${b}");
            mic.addSubstitutionProperty("b", "${a}");
        } else if ("Variable value contains another placeholder".equals(group)) {
            // outer already points to ${inner} (set in @BeforeEach), inner -> innerValue
            mic.addSubstitutionProperty("inner", "innerValue");
        }
        // -----------------------------------------------------------------
        // Execute and verify
        // -----------------------------------------------------------------
        if (expected.startsWith("java.lang.") && !expected.contains(":")) {
            // Expected an exception class without a specific message.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) Class.forName(expected);
            assertThrows(exClass, () -> mic.subst(ref), description);
        } else if (expected.startsWith("java.lang.") && expected.contains(":")) {
            // Expected exception with a message, e.g. "java.lang.RuntimeException: boom"
            String[] parts = expected.split(":", 2);
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) Class.forName(parts[0].trim());
            String expectedMessage = parts[1].trim();
            Throwable thrown = assertThrows(exClass, () -> mic.subst(ref), description);
            assertEquals(expectedMessage, thrown.getMessage(), "Exception message should match");
        } else {
            // Expected a normal substitution result.
            String actual = mic.subst(ref);
            assertEquals(expected, actual, description);
        }
    }
}
