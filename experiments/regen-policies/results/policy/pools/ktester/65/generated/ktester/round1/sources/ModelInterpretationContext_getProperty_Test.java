package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getProperty_Test {

    /**
     * The instance under test.
     */
    private ModelInterpretationContext mic;

    /**
     * Mocked collaborators.
     */
    @Mock
    private Context mockContext;

    @Mock
    private VariableSubstitutionsHelper mockHelper;

    /**
     * Create a fresh {@link ModelInterpretationContext} for each test and inject a
     * mocked {@link VariableSubstitutionsHelper}. The helper is {@code protected}
     * in the target class, therefore we can replace it via reflection without
     * touching any private members.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The constructor we use is the one that only needs a Context.
        mic = new ModelInterpretationContext(mockContext);
        // Replace the real VariableSubstitutionsHelper with our mock.
        Field helperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        helperField.setAccessible(true);
        helperField.set(mic, mockHelper);
    }

    /**
     * Clean up after each test – make the instance eligible for garbage collection.
     */
    @AfterEach
    void teardownAfterEach() {
        mic = null;
    }

    private static Stream<Arguments> typicalCases() {
        // normal key
        Arguments a1 = arguments("existingKey", "valueForExistingKey", "valueForExistingKey");
        // missing key → null
        Arguments a2 = arguments("missingKey", null, null);
        // null key → helper returns null
        Arguments a3 = arguments(null, null, null);
        // special characters
        Arguments a4 = arguments("key with spaces", "value with spaces", "value with spaces");
        Arguments a5 = arguments("key!@#$%^&*()", "specialValue", "specialValue");
        // empty string key
        Arguments a6 = arguments("", "emptyKeyValue", "emptyKeyValue");
        // empty value (empty string)
        Arguments a7 = arguments("keyWithEmptyValue", "", "");
        // whitespace only key (no mapping)
        Arguments a8 = arguments("   ", null, null);
        // very long key (1000+ chars)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1200; i++) sb.append('a');
        String longKey = sb.toString();
        Arguments a9 = arguments(longKey, "longValue", "longValue");
        return Stream.of(a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// helper throws IllegalStateException
        arguments("anyKey", IllegalStateException.class), // helper throws NullPointerException when key is null
        arguments(null, NullPointerException.class), // helper throws IllegalArgumentException for a key containing a null char
        arguments("invalid\u0000key", IllegalArgumentException.class), // helper throws generic RuntimeException
        arguments("triggerRuntime", RuntimeException.class));
    }

    /**
     * Verify that the {@code getProperty} method delegates to the
     * {@link VariableSubstitutionsHelper#getProperty(String)} method and returns
     * the value supplied by the helper.
     */
    @Test
    void getProperty_delegatesToHelper_andReturnsValue() {
        // GIVEN
        String key = "my.key";
        String expectedValue = "myValue";
        when(mockHelper.getProperty(key)).thenReturn(expectedValue);
        // WHEN
        String actual = mic.getProperty(key);
        // THEN
        assertEquals(expectedValue, actual, "The value returned by getProperty should match the helper's value");
        verify(mockHelper, times(1)).getProperty(key);
    }

    /**
     * Verify the behaviour when the helper returns {@code null}. The method
     * should simply propagate the {@code null} result.
     */
    @Test
    void getProperty_whenHelperReturnsNull_returnsNull() {
        // GIVEN
        String key = "unknown.key";
        when(mockHelper.getProperty(key)).thenReturn(null);
        // WHEN
        String actual = mic.getProperty(key);
        // THEN
        assertNull(actual, "When the helper returns null, getProperty should also return null");
        verify(mockHelper, times(1)).getProperty(key);
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'' → expected=''{1}''")
    @MethodSource("typicalCases")
    void getProperty_typicalScenarios(String key, String helperResult, String expected) {
        // GIVEN
        when(mockHelper.getProperty(key)).thenReturn(helperResult);
        // WHEN
        String actual = mic.getProperty(key);
        // THEN
        assertEquals(expected, actual, () -> "getProperty should return the value supplied by the helper");
        verify(mockHelper, times(1)).getProperty(key);
    }

    @Test
    void getProperty_helperIsNull_throwsNullPointerException() throws Exception {
        // GIVEN: replace the helper with null via reflection
        Field helperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        helperField.setAccessible(true);
        helperField.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.getProperty("anyKey"), "When variableSubstitutionsHelper is null, getProperty must throw NPE");
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'' → throws {1}")
    @MethodSource("exceptionCases")
    void getProperty_helperThrowsException_propagates(String key, Class<? extends Throwable> exceptionClass) {
        // GIVEN
        when(mockHelper.getProperty(key)).thenThrow(exceptionClass);
        // WHEN / THEN
        assertThrows(exceptionClass, () -> mic.getProperty(key), () -> "getProperty must let the helper's exception propagate");
        verify(mockHelper, times(1)).getProperty(key);
    }

    @Test
    void getProperty_nullKey_helperReturnsNull_returnsNull() {
        // GIVEN
        when(mockHelper.getProperty(null)).thenReturn(null);
        // WHEN
        String actual = mic.getProperty(null);
        // THEN
        assertNull(actual, "When the helper returns null for a null key, getProperty should return null");
        verify(mockHelper, times(1)).getProperty(null);
    }
}
