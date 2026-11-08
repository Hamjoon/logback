package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getCopyOfPropertyMap_Test {

    @Mock
    Context // mocked Context required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    /**
     * Simple wrapper used by {@link #setHelper(FieldHelper)} to hide reflection details.
     */
    private interface FieldHelper {

        VariableSubstitutionsHelper get();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
        // By default the real helper is created inside the constructor – we keep it for the
        // "normal" tests. For the tests that need a mock we will replace it via reflection.
    }

    /**
     * Utility to inject a mock or null {@link VariableSubstitutionsHelper} via reflection.
     */
    private void setHelper(FieldHelper helper) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        f.set(mic, helper.get());
    }

    @Test
    void getCopyOfPropertyMap_returnsEmptyMapInitially() {
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertNotNull(copy, "The returned map should never be null");
        assertTrue(copy.isEmpty(), "The map should be empty when no substitution properties have been added");
    }

    @Test
    void getCopyOfPropertyMap_reflectsAddedProperties() {
        mic.addSubstitutionProperty("myKey", "myValue");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals(1, copy.size(), "Map should contain exactly one entry");
        assertEquals("myValue", copy.get("myKey"), "The value associated with the key should match the added property");
    }

    @Test
    void getCopyOfPropertyMap_returnsACopyNotTheInternalMap() {
        mic.addSubstitutionProperty("origKey", "origValue");
        // Obtain a copy and mutate it
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        firstCopy.put("tamperedKey", "tamperedValue");
        // Obtain a second copy and verify it does not contain the tampered entry
        Map<String, String> secondCopy = mic.getCopyOfPropertyMap();
        assertFalse(secondCopy.containsKey("tamperedKey"), "Subsequent calls must return a fresh copy that is not affected by external modifications");
        // Also verify the original entry is still present
        assertEquals("origValue", secondCopy.get("origKey"));
    }

    @Test
    void normalBehavior_twoPropertiesReturned() {
        // Populate the real helper with two entries
        mic.addSubstitutionProperty("key1", "value1");
        mic.addSubstitutionProperty("key2", "value2");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals(2, copy.size(), "Map should contain exactly two entries");
        assertEquals("value1", copy.get("key1"));
        assertEquals("value2", copy.get("key2"));
    }

    @Test
    void isolationOfInternalMap_modifyingReturnedMapDoesNotAffectHelper() throws Exception {
        mic.addSubstitutionProperty("a", "A");
        mic.addSubstitutionProperty("b", "B");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // Mutate the copy
        copy.remove("a");
        copy.put("c", "C");
        // Get a fresh copy from the context – it must still contain the original entries
        Map<String, String> fresh = mic.getCopyOfPropertyMap();
        assertTrue(fresh.containsKey("a"));
        assertTrue(fresh.containsKey("b"));
        assertFalse(fresh.containsKey("c"));
    }

    @Test
    void nullHelperHandling_throwsNullPointerException() throws Exception {
        // Set the helper to null via reflection
        setHelper(() -> null);
        assertThrows(NullPointerException.class, () -> mic.getCopyOfPropertyMap(), "Calling getCopyOfPropertyMap() with a null helper must throw NPE");
    }

    @Test
    void copyRemainsUnchangedAfterInternalMapMutation() throws Exception {
        // Use a real helper, add two properties
        mic.addSubstitutionProperty("alpha", "A");
        mic.addSubstitutionProperty("beta", "B");
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        // Directly mutate the internal helper via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        VariableSubstitutionsHelper helper = (VariableSubstitutionsHelper) f.get(mic);
        // The helper does not expose a mutator, but we can add via the public API of the context
        mic.addSubstitutionProperty("gamma", "G");
        // The first copy must still contain only the original two entries
        assertEquals(2, firstCopy.size());
        assertEquals("A", firstCopy.get("alpha"));
        assertEquals("B", firstCopy.get("beta"));
        assertNull(firstCopy.get("gamma"));
    }

    @Test
    void helperReturnsNullMap_propagatesNull() throws Exception {
        VariableSubstitutionsHelper mockHelper = mock(VariableSubstitutionsHelper.class);
        when(mockHelper.getCopyOfPropertyMap()).thenReturn(null);
        setHelper(() -> mockHelper);
        Map<String, String> result = mic.getCopyOfPropertyMap();
        assertNull(result, "When the helper returns null, the method should propagate null");
    }

    @ParameterizedTest(name = "Exception propagation: {0}")
    @EnumSource(value = ExceptionType.class)
    void exceptionPropagationFromHelper(ExceptionType type) throws Exception {
        VariableSubstitutionsHelper mockHelper = mock(VariableSubstitutionsHelper.class);
        RuntimeException toThrow;
        switch(type) {
            case ILLEGAL_STATE:
                toThrow = new IllegalStateException("illegal state");
                break;
            case NULL_POINTER:
                toThrow = new NullPointerException("null pointer");
                break;
            case ILLEGAL_ARGUMENT:
                toThrow = new IllegalArgumentException("illegal argument");
                break;
            case RUNTIME:
                toThrow = new RuntimeException("runtime");
                break;
            case ERROR:
                toThrow = new AssertionError("assertion error");
                break;
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
        when(mockHelper.getCopyOfPropertyMap()).thenThrow(toThrow);
        setHelper(() -> mockHelper);
        // Verify that the exact same exception bubbles up unchanged
        RuntimeException thrown = assertThrows(toThrow.getClass(), () -> mic.getCopyOfPropertyMap(), "Exception should be propagated without alteration");
        assertSame(toThrow, thrown, "The thrown exception must be the same instance");
    }

    @Test
    void largePropertySetHandling() {
        // Populate with ten distinct properties
        for (int i = 0; i < 10; i++) {
            mic.addSubstitutionProperty("key" + i, "value" + i);
        }
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals(10, copy.size(), "Map should contain ten entries");
        for (int i = 0; i < 10; i++) {
            assertEquals("value" + i, copy.get("key" + i));
        }
    }

    @Test
    void copyIsolationAfterExternalMapClear() throws Exception {
        mic.addSubstitutionProperty("x", "1");
        mic.addSubstitutionProperty("y", "2");
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        // Clear the internal map via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        VariableSubstitutionsHelper helper = (VariableSubstitutionsHelper) f.get(mic);
        // The helper does not expose a clear method; we simulate by creating a new empty helper
        VariableSubstitutionsHelper emptyHelper = mock(VariableSubstitutionsHelper.class);
        when(emptyHelper.getCopyOfPropertyMap()).thenReturn(new HashMap<>());
        setHelper(() -> emptyHelper);
        // The previously obtained copy must still contain the original entries
        assertEquals(2, firstCopy.size());
        assertEquals("1", firstCopy.get("x"));
        assertEquals("2", firstCopy.get("y"));
    }

    @Test
    void helperThrowsError_propagatesError() throws Exception {
        VariableSubstitutionsHelper mockHelper = mock(VariableSubstitutionsHelper.class);
        AssertionError error = new AssertionError("boom");
        when(mockHelper.getCopyOfPropertyMap()).thenThrow(error);
        setHelper(() -> mockHelper);
        AssertionError thrown = assertThrows(AssertionError.class, () -> mic.getCopyOfPropertyMap(), "Error should be propagated unchanged");
        assertSame(error, thrown);
    }
}
