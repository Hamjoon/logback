package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.VariableSubstitutionsHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_addSubstitutionProperty_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // a dummy Context required by the constructor
    mockContext;

    @Mock
    private VariableSubstitutionsHelper // the collaborator we want to verify
    mockHelper;

    /**
     * Creates a fresh {@link ModelInterpretationContext} before each test and injects a
     * Mockito mock for {@code variableSubstitutionsHelper} using reflection.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The real class has a constructor that accepts a Context (the second argument
        // is not used in the current tests, we pass {@code null}).
        mic = new ModelInterpretationContext(mockContext, null);
        // Replace the real helper with our mock.
        Field helperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        helperField.setAccessible(true);
        helperField.set(mic, mockHelper);
    }

    @Test
    void addSubstitutionProperty_delegatesToVariableSubstitutionsHelper() {
        // GIVEN
        String key = "myKey";
        String value = "myValue";
        // WHEN
        mic.addSubstitutionProperty(key, value);
        // THEN
        verify(mockHelper, times(1)).addSubstitutionProperty(key, value);
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_helperThrowsRuntimeException_propagates() {
        // GIVEN
        String key = "key";
        String value = "value";
        doThrow(new IllegalStateException("boom")).when(mockHelper).addSubstitutionProperty(key, value);
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mic.addSubstitutionProperty(key, value));
        assertEquals("boom", ex.getMessage());
        // Verify that the helper was indeed invoked once before the exception bubbled up.
        verify(mockHelper, times(1)).addSubstitutionProperty(key, value);
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_normalAddition_propertyIsStored() {
        // Use a fresh instance that contains a real VariableSubstitutionsHelper.
        ModelInterpretationContext realMic = new ModelInterpretationContext(mockContext, null);
        // WHEN
        String key = "myKey";
        String value = "myValue";
        realMic.addSubstitutionProperty(key, value);
        // THEN – the property can be retrieved via the public API
        assertEquals(value, realMic.getProperty(key), "getProperty should return the value that was added");
        Map<String, String> copy = realMic.getCopyOfPropertyMap();
        assertNotNull(copy, "Copy of property map must not be null");
        assertTrue(copy.containsKey(key), "Property map must contain the added key");
        assertEquals(value, copy.get(key), "Stored value must match the one that was added");
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_nullHelper_throwsNullPointerException() throws Exception {
        // Replace the mock with a literal null via reflection.
        Field helperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        helperField.setAccessible(true);
        helperField.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty("someKey", "someValue"));
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_nullKey_propagatesException() {
        // GIVEN
        String value = "validValue";
        doThrow(new NullPointerException("key is null")).when(mockHelper).addSubstitutionProperty(null, value);
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty(null, value));
        assertEquals("key is null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_nullValue_propagatesException() {
        // GIVEN
        String key = "validKey";
        doThrow(new NullPointerException("value is null")).when(mockHelper).addSubstitutionProperty(key, null);
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, () -> mic.addSubstitutionProperty(key, null));
        assertEquals("value is null", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    @Test
    void addSubstitutionProperty_emptyKey_propagatesIllegalArgumentException() {
        // GIVEN
        String emptyKey = "";
        String value = "someValue";
        doThrow(new IllegalArgumentException("empty key")).when(mockHelper).addSubstitutionProperty(emptyKey, value);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mic.addSubstitutionProperty(emptyKey, value));
        assertEquals("empty key", ex.getMessage());
    }
}
