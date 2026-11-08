package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectMap_Test {

    private Context mockContext;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple mock Context – the concrete behavior of Context is not required
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Helper that forces the private {@code objectMap} field to a given value using reflection.
     */
    private static void setInternalObjectMap(ModelInterpretationContext mic, Map<String, Object> map) {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
            f.setAccessible(true);
            f.set(mic, map);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while setting objectMap: " + e.getMessage());
        }
    }

    @Test
    void getObjectMap_ShouldReturnNonNullMapInitially() {
        // This test reflects the default construction behaviour of the class.
        Map<String, Object> map = mic.getObjectMap();
        assertNotNull(map, "The object map must not be null after construction");
        // The default may be empty; we only assert non‑null here.
    }

    @Test
    void getObjectMap_ShouldReturnSameModifiableInstance() {
        Map<String, Object> map1 = mic.getObjectMap();
        map1.put("key", "value");
        // Retrieve the map again and verify that the entry is present,
        // proving that the returned map is the internal mutable map.
        Map<String, Object> map2 = mic.getObjectMap();
        assertSame(map1, map2, "Subsequent calls must return the same map instance");
        assertEquals("value", map2.get("key"), "The map should contain the value that was put earlier");
    }

    @Test
    void copyConstructor_ShouldCreateIndependentObjectMap() {
        // Populate the original MIC's map
        mic.getObjectMap().put("originalKey", "originalValue");
        // Create a copy using the copy‑constructor
        ModelInterpretationContext micCopy = new ModelInterpretationContext(mic);
        // The copy's map must be a new instance and must not contain the original entry
        Map<String, Object> originalMap = mic.getObjectMap();
        Map<String, Object> copyMap = micCopy.getObjectMap();
        assertNotSame(originalMap, copyMap, "Copy should have its own map instance");
        assertTrue(copyMap.isEmpty(), "Copy's map should start empty, independent of the original");
        assertEquals("originalValue", originalMap.get("originalKey"), "Original map must still contain its entry");
    }

    @Test
    void getObjectMap_ShouldReturnNullWhenNeverInitialized() {
        // Simulate a never‑initialized internal map
        setInternalObjectMap(mic, null);
        Map<String, Object> map = mic.getObjectMap();
        assertNull(map, "When the internal objectMap is null, getObjectMap() must return null");
    }

    @Test
    void getObjectMap_ShouldReturnPopulatedMapInstance() {
        // Prepare a map with known entries
        Map<String, Object> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", 123);
        // Inject the map via reflection
        setInternalObjectMap(mic, expected);
        Map<String, Object> actual = mic.getObjectMap();
        assertSame(expected, actual, "The returned map must be the exact same instance stored internally");
        assertEquals("value1", actual.get("key1"));
        assertEquals(123, actual.get("key2"));
    }

    @Test
    void getObjectMap_ShouldReturnExplicitEmptyMap() {
        // Explicitly set an empty HashMap
        setInternalObjectMap(mic, new HashMap<>());
        Map<String, Object> map = mic.getObjectMap();
        assertNotNull(map, "The map should not be null when explicitly set to an empty HashMap");
        assertTrue(map.isEmpty(), "The map must be empty");
    }

    @Test
    void getObjectMap_ShouldReturnUnmodifiableMapInstance() {
        // Create a regular mutable map and wrap it with Collections.unmodifiableMap
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("alpha", "beta");
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(mutable);
        // Inject the unmodifiable map
        setInternalObjectMap(mic, unmodifiable);
        Map<String, Object> returned = mic.getObjectMap();
        assertSame(unmodifiable, returned, "When the internal map is unmodifiable, getObjectMap() must return that exact instance");
        assertEquals("beta", returned.get("alpha"));
    }

    @Test
    void getObjectMap_UnmodifiableMapModificationThrowsException() {
        // Prepare an unmodifiable map
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("alpha", "beta");
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(mutable);
        setInternalObjectMap(mic, unmodifiable);
        Map<String, Object> map = mic.getObjectMap();
        // Any mutating operation must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", "newValue"), "Attempting to modify an unmodifiable map should throw UnsupportedOperationException");
    }
}
