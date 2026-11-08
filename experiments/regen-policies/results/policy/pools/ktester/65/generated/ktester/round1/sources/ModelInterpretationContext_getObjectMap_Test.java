package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectMap_Test {

    /**
     * Mocked Logback context – required by the MIC constructor.
     */
    @Mock
    Context mockContext;

    /**
     * Instance under test, created before each test method.
     */
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test does not require any special state
        // from the Context, therefore a simple Mockito mock is sufficient.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        mic = null;
    }

    /**
     * Sets the private {@code objectMap} field of the {@code mic} instance via reflection.
     *
     * @param map the map to inject (may be {@code null})
     */
    private void injectObjectMap(Map<String, Object> map) {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
            f.setAccessible(true);
            f.set(mic, map);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject objectMap via reflection", e);
        }
    }

    @Test
    @DisplayName("getObjectMap() returns a non‑null, initially empty map")
    void shouldReturnNonNullEmptyMapOnNewInstance() {
        // When
        Map<String, Object> map = mic.getObjectMap();
        // Then
        assertNotNull(map, "The object map must never be null");
        assertTrue(map.isEmpty(), "A newly created ModelInterpretationContext should have an empty object map");
    }

    @Test
    @DisplayName("Modifying the returned map reflects inside the MIC")
    void shouldReflectModificationsThroughReturnedMap() {
        // Given
        Map<String, Object> map = mic.getObjectMap();
        String key = "sampleKey";
        Object value = new Object();
        // When
        map.put(key, value);
        // Then
        assertEquals(1, map.size(), "Map size should be 1 after insertion");
        assertSame(value, mic.getObjectMap().get(key), "The value retrieved via getObjectMap() must be the same instance that was put into the map");
    }

    @Test
    @DisplayName("Two separate MIC instances have independent object maps")
    void shouldProvideIndependentMapsForDifferentInstances() {
        // Given
        ModelInterpretationContext anotherMic = new ModelInterpretationContext(mockContext);
        Map<String, Object> firstMap = mic.getObjectMap();
        Map<String, Object> secondMap = anotherMic.getObjectMap();
        // When
        firstMap.put("first", "value1");
        secondMap.put("second", "value2");
        // Then
        assertEquals(1, firstMap.size(), "First MIC map should contain exactly one entry");
        assertEquals(1, secondMap.size(), "Second MIC map should contain exactly one entry");
        assertFalse(firstMap.containsKey("second"), "First map must not contain keys from the second map");
        assertFalse(secondMap.containsKey("first"), "Second map must not contain keys from the first map");
    }

    @Test
    @DisplayName("Copy‑constructor copies the object map reference, not its contents")
    void copyConstructorShouldShareObjectMapReference() {
        // Given: populate original MIC
        mic.getObjectMap().put("original", "value");
        ModelInterpretationContext copyMic = new ModelInterpretationContext(mic);
        // When: modify the map via the copy
        copyMic.getObjectMap().put("copy", "otherValue");
        // Then: both instances see the same entries because the map reference is shared
        assertEquals(2, mic.getObjectMap().size(), "Original MIC should see entries added through the copy's map");
        assertTrue(mic.getObjectMap().containsKey("copy"), "Original MIC must contain the key added via the copy");
    }

    @Test
    @DisplayName("When never initialised, getObjectMap() returns null")
    void shouldReturnNullWhenNeverInitialised() {
        // No injection – the field stays at its default value (null)
        assertNull(mic.getObjectMap(), "A freshly constructed ModelInterpretationContext should return null for getObjectMap()");
    }

    @Test
    @DisplayName("After injecting an empty HashMap, getObjectMap() returns that empty map")
    void shouldReturnInjectedEmptyMap() {
        Map<String, Object> empty = new HashMap<>();
        injectObjectMap(empty);
        Map<String, Object> result = mic.getObjectMap();
        assertSame(empty, result, "The returned map must be the exact instance injected via reflection");
        assertTrue(result.isEmpty(), "Injected empty map must be reported as empty");
    }

    @Test
    @DisplayName("Map with several entries is returned unchanged")
    void shouldReturnMapWithMultipleEntries() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);
        injectObjectMap(map);
        Map<String, Object> result = mic.getObjectMap();
        assertSame(map, result, "Returned map must be the same instance");
        assertEquals(2, result.size(), "Map size must match number of inserted entries");
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }

    @Test
    @DisplayName("Returned map is the live internal reference – modifications are reflected")
    void modificationShouldBeReflectedInInternalState() {
        Map<String, Object> map = new HashMap<>();
        injectObjectMap(map);
        Map<String, Object> returned = mic.getObjectMap();
        returned.put("newKey", "newValue");
        assertEquals(1, mic.getObjectMap().size(), "Internal map size must reflect the addition");
        assertEquals("newValue", mic.getObjectMap().get("newKey"));
    }

    @Test
    @DisplayName("Repeated calls return the identical map instance")
    void repeatedCallsReturnSameInstance() {
        Map<String, Object> map = new HashMap<>();
        injectObjectMap(map);
        Map<String, Object> first = mic.getObjectMap();
        Map<String, Object> second = mic.getObjectMap();
        assertSame(first, second, "Multiple invocations must return the same map instance");
    }

    @Test
    @DisplayName("Map containing a null key and null value is returned correctly")
    void mapWithNullKeyAndValue() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, null);
        injectObjectMap(map);
        Map<String, Object> result = mic.getObjectMap();
        assertEquals(1, result.size());
        assertTrue(result.containsKey(null));
        assertNull(result.get(null));
    }

    @Test
    @DisplayName("Immutable map is returned unchanged and throws on modification")
    void immutableMapBehaviour() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("a", "b");
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(mutable);
        injectObjectMap(unmodifiable);
        Map<String, Object> result = mic.getObjectMap();
        assertSame(unmodifiable, result, "Returned map must be the same immutable instance");
        assertThrows(UnsupportedOperationException.class, () -> result.put("c", "d"), "Attempting to modify the immutable map must throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("LinkedHashMap preserves insertion order")
    void linkedHashMapPreservesOrder() {
        LinkedHashMap<String, Object> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);
        injectObjectMap(linked);
        Map<String, Object> result = mic.getObjectMap();
        assertSame(linked, result, "Returned map must be the same LinkedHashMap instance");
        // Verify iteration order
        Iterator<Map.Entry<String, Object>> it = result.entrySet().iterator();
        assertEquals("first", it.next().getKey());
        assertEquals("second", it.next().getKey());
        assertEquals("third", it.next().getKey());
        assertFalse(it.hasNext(), "No more entries expected");
    }

    @Test
    @DisplayName("Duplicate key – later value overwrites earlier")
    void duplicateKeyKeepsLastValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "oldValue");
        // overwrite
        map.put("key", "newValue");
        injectObjectMap(map);
        Map<String, Object> result = mic.getObjectMap();
        assertEquals(1, result.size());
        assertEquals("newValue", result.get("key"));
    }

    @Test
    @DisplayName("Large map (1000 entries) size is reported correctly")
    void largeMapSize() {
        Map<String, Object> large = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            large.put("k" + i, i);
        }
        injectObjectMap(large);
        Map<String, Object> result = mic.getObjectMap();
        assertEquals(1000, result.size(), "Map should contain exactly 1000 entries");
        // Spot‑check a few entries
        assertEquals(0, result.get("k0"));
        assertEquals(999, result.get("k999"));
    }

    @Test
    @DisplayName("Calling clear() on the returned map empties the internal state")
    void clearShouldEmptyInternalMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("x", "y");
        injectObjectMap(map);
        Map<String, Object> returned = mic.getObjectMap();
        returned.clear();
        assertTrue(mic.getObjectMap().isEmpty(), "After clear(), internal map must be empty");
    }
}
