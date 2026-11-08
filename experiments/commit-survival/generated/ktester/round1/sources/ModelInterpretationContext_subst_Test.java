package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.ScanException;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.util.OptionHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_subst_Test {

    /**
     * Mocked Logback context – the real implementation is not needed for the tests.
     */
    @Mock
    Context mockContext;

    /**
     * Instance under test – created fresh for each test method.
     */
    ModelInterpretationContext mic;

    /**
     * A spy wrapper used when we need to verify that an error is logged.
     */
    ModelInterpretationContext micSpy;

    /**
     * Subclass that throws {@link IllegalArgumentException} when a specific
     * property is requested. This forces {@link OptionHelper#substVars} to
     * propagate the exception, which {@code subst()} must catch.
     */
    private static class IllegalArgumentModelInterpretationContext extends ModelInterpretationContext {

        IllegalArgumentModelInterpretationContext(Context ctx) {
            super(ctx);
        }

        @Override
        public String getProperty(String key) {
            if ("null".equals(key)) {
                throw new IllegalArgumentException("property value is null");
            }
            return super.getProperty(key);
        }
    }

    /**
     * Subclass that throws a {@link RuntimeException} from {@code getProperty}.
     * The test ensures that such an exception is *not* caught by {@code subst()}.
     */
    private static class RuntimeExceptionModelInterpretationContext extends ModelInterpretationContext {

        RuntimeExceptionModelInterpretationContext(Context ctx) {
            super(ctx);
        }

        @Override
        public String getProperty(String key) {
            if ("trigger".equals(key)) {
                throw new RuntimeException("unexpected failure");
            }
            return super.getProperty(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a plain instance for the normal test cases.
        mic = new ModelInterpretationContext(mockContext);
        // Create a spy for the error‑handling test case.
        micSpy = Mockito.spy(new ModelInterpretationContext(mockContext));
        mic = new ModelInterpretationContext(mockContext);
        micSpy = Mockito.spy(new ModelInterpretationContext(mockContext));
        // These properties are needed by many parameterised test cases.
        mic.addSubstitutionProperty("user", "John");
        mic.addSubstitutionProperty("greeting", "Hello");
        mic.addSubstitutionProperty("site", "Example.com");
        // empty value
        mic.addSubstitutionProperty("empty", "");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; just help the garbage collector.
        mic = null;
        micSpy = null;
    }

    /**
     * Supplies test data for successful substitution scenarios.
     */
    private static Stream<Arguments> successfulSubstitutionProvider() {
        return Stream.of(// single variable
        Arguments.of("Hello ${user}", "Hello John", "single variable substitution"), // empty string (no change)
        Arguments.of("", "", "empty input string"), // multiple variables
        Arguments.of("${greeting}, ${user}! Welcome to ${site}.", "Hello, John! Welcome to Example.com.", "multiple distinct variables"), // undefined variable – left untouched
        Arguments.of("Value is ${undefinedVar}", "Value is ${undefinedVar}", "undefined variable stays as‑is"), // variable resolves to empty string
        Arguments.of("Prefix${empty}Suffix", "PrefixSuffix", "empty property value"), // escaped variable syntax
        Arguments.of("Literal $${user}", "Literal ${user}", "escaped placeholder"), // very long input with many repetitions
        Arguments.of("Start ${greeting} ${user} ${greeting} ${user} ${greeting} ${user} ${greeting} ${user} End", "Start Hello John Hello John Hello John Hello John Hello John Hello John End", "long string with repeated placeholders"));
    }

    @Test
    void substReturnsNullWhenInputIsNull() {
        assertNull(mic.subst(null), "subst(null) should return null");
    }

    @Test
    void substReplacesVariableUsingSubstitutionProperties() {
        // Arrange – define a substitution property that OptionHelper can resolve.
        mic.addSubstitutionProperty("myVar", "myValue");
        // Act
        String result = mic.subst("prefix=${myVar}-suffix");
        // Assert
        assertEquals("prefix=myValue-suffix", result, "Variable ${myVar} should be replaced with the value defined via addSubstitutionProperty");
    }

    @Test
    void substReturnsOriginalStringAndLogsErrorWhenVariableIsMalformed() {
        // Arrange – malformed variable (missing closing '}')
        String malformed = "value=${myVar";
        // We do not want the real addError implementation to interfere with the test output,
        // so we stub it out on the spy.
        doNothing().when(micSpy).addError(anyString(), any(Throwable.class));
        // Act
        String result = micSpy.subst(malformed);
        // Assert – the method should return the original string.
        assertEquals(malformed, result, "When OptionHelper throws a ScanException, subst should return the original input string");
        // Verify that an error was logged via ContextAwareBase#addError.
        verify(micSpy, times(1)).addError(startsWith("Problem while parsing [value=${myVar"), any(ScanException.class));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulSubstitutionProvider")
    void substSuccessfulScenarios(String input, String expected, String _description) {
        // common properties used by the above cases
        mic.addSubstitutionProperty("user", "John");
        mic.addSubstitutionProperty("greeting", "Hello");
        mic.addSubstitutionProperty("site", "Example.com");
        // empty value
        mic.addSubstitutionProperty("empty", "");
        assertEquals(expected, mic.subst(input));
    }

    @Test
    void substCatchesIllegalArgumentExceptionAndLogsError() {
        // Prepare an instance that will throw IllegalArgumentException for ${null}
        IllegalArgumentModelInterpretationContext ctx = Mockito.spy(new IllegalArgumentModelInterpretationContext(mockContext));
        doNothing().when(ctx).addError(anyString(), any(Throwable.class));
        String input = "illegal ${null}";
        String result = ctx.subst(input);
        // The original string must be returned unchanged
        assertEquals(input, result);
        // Verify that an error was logged (the exact message contains the input)
        verify(ctx, times(1)).addError(startsWith("Problem while parsing [illegal ${null]"), any(IllegalArgumentException.class));
    }

    @Test
    void substPropagatesUncheckedException() {
        RuntimeExceptionModelInterpretationContext ctx = new RuntimeExceptionModelInterpretationContext(mockContext);
        String input = "value ${trigger}";
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ctx.subst(input));
        assertEquals("unexpected failure", thrown.getMessage());
    }

    @Test
    void substWorksWhenContextIsNull() {
        // Create an instance without passing a Context (null)
        ModelInterpretationContext ctx = new ModelInterpretationContext(null);
        ctx.addSubstitutionProperty("user", "John");
        String result = ctx.subst("User: ${user}");
        assertEquals("User: John", result);
    }
}
