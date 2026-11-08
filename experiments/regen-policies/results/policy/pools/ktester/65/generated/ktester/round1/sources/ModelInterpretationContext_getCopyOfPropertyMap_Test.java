package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getCopyOfPropertyMap_Test {

    private ModelInterpretationContext mic;

    private Context context;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation to avoid unexpected NullPointerExceptions
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
        // Use a real Context to avoid NPEs inside the class under test.
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    /**
     * Utility that injects a mock {@link VariableSubstitutionsHelper} into the
     * {@code ModelInterpretationContext} instance using reflection.
     */
    private void injectHelper(VariableSubstitutionsHelper helper) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        f.set(mic, helper);
    }

    @Test
    @DisplayName("getCopyOfPropertyMap returns a map containing all added substitution properties")
    void testGetCopyOfPropertyMapContainsAddedProperties() {
        // given
        mic.addSubstitutionProperty("host", "localhost");
        mic.addSubstitutionProperty("port", "8080");
        // when
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // then
        assertNotNull(copy, "The returned map should not be null");
        assertEquals(2, copy.size(), "The map should contain exactly the two added entries");
        assertEquals("localhost", copy.get("host"));
        assertEquals("8080", copy.get("port"));
    }

    @Test
    @DisplayName("getCopyOfPropertyMap returns an independent copy (modifying the copy does not affect the internal map)")
    void testGetCopyOfPropertyMapIsIndependentCopy() {
        // given
        mic.addSubstitutionProperty("env", "prod");
        // when
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        // modify the copy
        firstCopy.put("newKey", "newValue");
        // retrieve a fresh copy from the context
        Map<String, String> secondCopy = mic.getCopyOfPropertyMap();
        // then
        assertFalse(secondCopy.containsKey("newKey"), "Modifying the previously returned map must not affect the internal property map");
        assertEquals(1, secondCopy.size(), "The internal map should still contain only the originally added entry");
        assertEquals("prod", secondCopy.get("env"));
    }

    @Test
    @DisplayName("Normal behaviour – map with entries is copied")
    void normalBehaviourMapWithEntries() throws Exception {
        // given
        Map<String, String> original = new HashMap<>();
        original.put("key1", "value1");
        original.put("key2", "value2");
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(original);
        injectHelper(helper);
        // when
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // then
        assertNotNull(copy);
        assertEquals(original, copy, "Returned map must contain the same entries");
        assertNotSame(original, copy, "Returned map must be a different instance (a copy)");
    }

    @Test
    @DisplayName("Normal behaviour – empty map is copied")
    void normalBehaviourEmptyMap() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(Collections.emptyMap());
        injectHelper(helper);
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertNotNull(copy);
        assertTrue(copy.isEmpty(), "Returned map should be empty");
        assertNotSame(Collections.emptyMap(), copy, "Returned map must not be the same instance");
    }

    @Test
    @DisplayName("Returned map is independent from original (modifying copy does not affect helper)")
    void copyIsIndependent() throws Exception {
        Map<String, String> original = new HashMap<>();
        original.put("env", "prod");
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(original);
        injectHelper(helper);
        Map<String, String> firstCopy = mic.getCopyOfPropertyMap();
        // mutate the copy
        firstCopy.put("newKey", "newValue");
        // obtain a fresh copy – it must reflect the unchanged original
        Map<String, String> secondCopy = mic.getCopyOfPropertyMap();
        assertFalse(secondCopy.containsKey("newKey"), "Modifying the previously returned map must not affect the internal map");
        assertEquals(1, secondCopy.size());
        assertEquals("prod", secondCopy.get("env"));
    }

    @Test
    @DisplayName("Null helper handling – NPE is thrown")
    void nullHelperThrowsNPE() throws Exception {
        // explicitly set the helper to null
        Field f = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        f.setAccessible(true);
        f.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.getCopyOfPropertyMap(), "When helper is null a NullPointerException should be thrown");
    }

    @Test
    @DisplayName("Helper returns null map – null is propagated")
    void helperReturnsNullMap() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(null);
        injectHelper(helper);
        assertNull(mic.getCopyOfPropertyMap(), "If the helper returns null, the method should propagate null");
    }

    @Test
    @DisplayName("Map containing null key and null values is copied correctly")
    void mapWithNullKeyAndValue() throws Exception {
        Map<String, String> original = new HashMap<>();
        // null key
        original.put(null, "someValue");
        // null value
        original.put("someKey", null);
        // another null value
        original.put("nullKey2", null);
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(original);
        injectHelper(helper);
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertTrue(copy.containsKey(null));
        assertNull(copy.get("someKey"));
    }

    @Test
    @DisplayName("Unmodifiable map from helper yields a mutable copy")
    void unmodifiableHelperMap() throws Exception {
        Map<String, String> unmod = Collections.unmodifiableMap(Map.of("a", "1", "b", "2"));
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(unmod);
        injectHelper(helper);
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // verify content
        assertEquals(unmod, copy);
        // verify mutability
        assertDoesNotThrow(() -> copy.put("c", "3"), "Returned map must be mutable even if the source map is unmodifiable");
        assertEquals("3", copy.get("c"));
    }

    @Test
    @DisplayName("Each invocation returns a fresh copy")
    void freshCopyOnEachInvocation() throws Exception {
        Map<String, String> original = new HashMap<>();
        original.put("x", "old");
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenReturn(original);
        injectHelper(helper);
        Map<String, String> first = mic.getCopyOfPropertyMap();
        Map<String, String> second = mic.getCopyOfPropertyMap();
        assertNotSame(first, second, "Two calls must return distinct map instances");
        assertEquals(first, second, "Both maps must contain the same entries");
    }

    @Test
    @DisplayName("Returned map is isolated from later changes in helper's map")
    void copyIsolationFromHelperChanges() throws Exception {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("x", "old");
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenAnswer(invocation -> new HashMap<>(mutable));
        injectHelper(helper);
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        // mutate the underlying map after the copy was taken
        mutable.put("x", "new");
        // the previously obtained copy must stay unchanged
        assertEquals("old", copy.get("x"));
    }

    @Test
    @DisplayName("Unchecked exception from helper is propagated")
    void helperThrowsUncheckedException() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenThrow(new IllegalStateException("boom"));
        injectHelper(helper);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mic.getCopyOfPropertyMap(), "Exception from helper must be propagated unchanged");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("Helper method throws NullPointerException – it propagates")
    void helperThrowsNPE() throws Exception {
        VariableSubstitutionsHelper helper = mock(VariableSubstitutionsHelper.class);
        when(helper.getCopyOfPropertyMap()).thenThrow(new NullPointerException("inner NPE"));
        injectHelper(helper);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> mic.getCopyOfPropertyMap(), "NullPointerException from helper must bubble up");
        assertEquals("inner NPE", ex.getMessage());
    }
}
