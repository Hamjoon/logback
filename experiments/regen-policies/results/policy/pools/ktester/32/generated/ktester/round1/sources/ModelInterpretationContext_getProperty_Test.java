package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getProperty_Test {

    private Context context;

    private ModelInterpretationContext mic;

    /* --------------------------------------------------------------
     * Normal behaviour tests
     * -------------------------------------------------------------- */
    @Nested
    class NormalBehavior {

        @Test
        void getProperty_returnsValueWhenKeyExists() throws Exception {
            // GIVEN a helper that maps "app.name" -> "MyApp"
            VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
            when(helper.getProperty("app.name")).thenReturn("MyApp");
            injectHelper(helper);
            // WHEN
            String value = mic.getProperty("app.name");
            // THEN
            assertEquals("MyApp", value, "getProperty should return the value associated with the existing key");
            verify(helper).getProperty("app.name");
        }

        @Test
        void getProperty_returnsNullWhenKeyDoesNotExist() throws Exception {
            VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
            when(helper.getProperty("non.existent")).thenReturn(null);
            injectHelper(helper);
            String value = mic.getProperty("non.existent");
            assertNull(value, "When the key is not present the helper returns null, which should be propagated");
            verify(helper).getProperty("non.existent");
        }

        @Test
        void getProperty_handlesNullKeyGracefully() throws Exception {
            VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
            when(helper.getProperty(null)).thenReturn(null);
            injectHelper(helper);
            assertDoesNotThrow(() -> {
                String value = mic.getProperty(null);
                assertNull(value, "A null key should result in a null return value, not an exception");
            });
            verify(helper).getProperty(null);
        }
    }

    /* --------------------------------------------------------------
     * Edge‑case tests (empty, whitespace, special characters, long key)
     * -------------------------------------------------------------- */
    @Nested
    class EdgeCases {

        private static Stream<Arguments> edgeCaseProvider() {
            return Stream.of(// empty string
            Arguments.of("", ""), // whitespace string
            Arguments.of("   ", "   "), // special character (dollar sign)
            Arguments.of("key.with.dollar$", "dollarValue"), // very long key (>200 chars)
            Arguments.of("very.long.key." + "x".repeat(200), "longValue"));
        }

        @ParameterizedTest(name = "key=\"{0}\" → expected=\"{1}\"")
        @MethodSource("edgeCaseProvider")
        void getProperty_edgeCases(String key, String expected) throws Exception {
            VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
            when(helper.getProperty(key)).thenReturn(expected);
            injectHelper(helper);
            String actual = mic.getProperty(key);
            assertEquals(expected, actual, () -> "Edge case key [" + key + "] should return the mapped value");
            verify(helper).getProperty(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the concrete ContextBase implementation provided by Logback.
        context = new ContextBase();
        // Populate the context with a known property.
        context.putProperty("existingKey", "existingValue");
        // Create the object under test.
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy.
        mic = null;
        context = null;
    }

    /* --------------------------------------------------------------
     * Helper to inject a mock VariableSubstitutionsHelper into the
     * ModelInterpretationContext instance via reflection (the field is
     * protected, but we keep the injection explicit for readability).
     * -------------------------------------------------------------- */
    private void injectHelper(VariableSubstitutionsHelper helper) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        f.set(mic, helper);
    }

    @Test
    void getProperty_returnsValueWhenKeyExists() {
        // GIVEN – the context already contains the key "existingKey"
        // WHEN
        String value = mic.getProperty("existingKey");
        // THEN
        assertEquals("existingValue", value, "getProperty should return the value associated with the existing key");
    }

    @Test
    void getProperty_returnsNullWhenKeyDoesNotExist() {
        // GIVEN – a key that has not been added to the context
        // WHEN
        String value = mic.getProperty("nonExistingKey");
        // THEN
        assertNull(value, "getProperty should return null for a key that is not present in the context");
    }

    @Test
    void getProperty_handlesNullKeyGracefully() {
        // GIVEN – a null key
        // WHEN / THEN
        assertDoesNotThrow(() -> {
            String value = mic.getProperty(null);
            assertNull(value, "When the key is null, getProperty should return null");
        });
    }

    /* --------------------------------------------------------------
     * Null helper – the delegate field itself is null
     * -------------------------------------------------------------- */
    @Test
    void getProperty_throwsNPEWhenHelperIsNull() throws Exception {
        // Explicitly set the helper to null
        injectHelper(null);
        assertThrows(NullPointerException.class, () -> mic.getProperty("anyKey"), "When variableSubstitutionsHelper is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------
     * Helper throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void getProperty_propagatesIllegalArgumentException() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getProperty("badKey")).thenThrow(new IllegalArgumentException("illegal key"));
        injectHelper(helper);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mic.getProperty("badKey"), "The IllegalArgumentException from the helper must be propagated");
        assertEquals("illegal key", ex.getMessage());
        verify(helper).getProperty("badKey");
    }

    /* --------------------------------------------------------------
     * Helper throws NullPointerException for null key
     * -------------------------------------------------------------- */
    @Test
    void getProperty_propagatesNPEFromHelperOnNullKey() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getProperty(null)).thenThrow(new NullPointerException("null key"));
        injectHelper(helper);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> mic.getProperty(null), "If the helper throws NPE for a null key, it must bubble up");
        assertEquals("null key", ex.getMessage());
        verify(helper).getProperty(null);
    }

    /* --------------------------------------------------------------
     * Helper throws unexpected RuntimeException
     * -------------------------------------------------------------- */
    @Test
    void getProperty_propagatesRuntimeException() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getProperty("runtimeErrorKey")).thenThrow(new RuntimeException("runtime error"));
        injectHelper(helper);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> mic.getProperty("runtimeErrorKey"), "Any RuntimeException from the helper should be propagated unchanged");
        assertEquals("runtime error", ex.getMessage());
        verify(helper).getProperty("runtimeErrorKey");
    }
}
