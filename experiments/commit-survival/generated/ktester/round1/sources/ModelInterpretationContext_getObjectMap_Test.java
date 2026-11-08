package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectMap_Test {

    @Mock
    Context // mocked dependency required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for every test to avoid cross‑test contamination
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Helper that uses reflection to replace the private {@code objectMap} field with the
     * supplied map instance.
     */
    private void setInternalObjectMap(Map<String, Object> map) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
        f.setAccessible(true);
        f.set(mic, map);
    }

    /**
     * Verify that a newly created ModelInterpretationContext returns a non‑null,
     * empty map from {@code getObjectMap()}.
     */
    @Test
    void getObjectMap_initiallyEmpty() {
        Map<String, Object> map = mic.getObjectMap();
        assertNotNull(map, "Object map should never be null");
        assertTrue(map.isEmpty(), "Object map should be empty immediately after construction");
    }

    /**
     * Verify that the map returned by {@code getObjectMap()} is the actual internal
     * map – modifications performed on the returned map are reflected on subsequent
     * calls.
     */
    @Test
    void getObjectMap_isLiveReference() {
        Map<String, Object> map1 = mic.getObjectMap();
        map1.put("myKey", "myValue");
        // Retrieve the map again and ensure the entry is present
        Map<String, Object> map2 = mic.getObjectMap();
        assertEquals(1, map2.size(), "Map should contain exactly one entry after put");
        assertEquals("myValue", map2.get("myKey"), "Stored value should be retrievable");
        assertSame(map1, map2, "Both calls should return the same map instance");
    }

    /**
     * After invoking {@code createAppenderBags()}, the internal object map must contain
     * a dedicated entry for the appender bag (key defined in {@link JoranConstants#APPENDER_BAG}).
     */
    @Test
    void getObjectMap_containsAppenderBagAfterCreateAppenderBags() {
        // Pre‑condition: the map does not yet contain the appender bag
        assertNull(mic.getObjectMap().get(JoranConstants.APPENDER_BAG), "Appender bag should not be present before createAppenderBags()");
        // Trigger the creation of the appender bag
        mic.createAppenderBags();
        Object bagObject = mic.getObjectMap().get(JoranConstants.APPENDER_BAG);
        assertNotNull(bagObject, "Appender bag should be present after createAppenderBags()");
        assertTrue(bagObject instanceof Map, "Appender bag entry should be a Map instance");
    }

    /**
     * Case 1 – “null”:
     * When a newly constructed {@code ModelInterpretationContext} has never had its
     * {@code objectMap} field initialized, {@code getObjectMap()} should return {@code null}.
     */
    @Test
    void getObjectMap_returnsNullWhenNeverInitialized() {
        assertNull(mic.getObjectMap(), "A fresh ModelInterpretationContext should return null because the internal map has not been created yet.");
    }

    /**
     * Case 2 – “empty HashMap instance”:
     * After the private field {@code objectMap} is set (via reflection) to a new empty
     * {@link HashMap}, {@code getObjectMap()} must return that exact map instance (not null).
     */
    @Test
    void getObjectMap_returnsExactMapInstanceWhenSetViaReflection() throws Exception {
        Map<String, Object> emptyMap = new HashMap<>();
        setInternalObjectMap(emptyMap);
        Map<String, Object> returned = mic.getObjectMap();
        assertNotNull(returned, "The getter should not return null after the field has been set.");
        assertSame(emptyMap, returned, "The returned map must be the same instance that was injected via reflection.");
        assertTrue(returned.isEmpty(), "The injected map is empty, so the returned map should also be empty.");
    }

    /**
     * Case 3 – “HashMap containing {\"key\":\"value\"}”:
     * Populate the internal {@code objectMap} with a single entry (using reflection) and
     * verify that {@code getObjectMap()} returns a map that contains the inserted key/value pair.
     */
    @Test
    void getObjectMap_reflectsPrepopulatedEntries() throws Exception {
        Map<String, Object> prepopulated = new HashMap<>();
        prepopulated.put("key", "value");
        setInternalObjectMap(prepopulated);
        Map<String, Object> returned = mic.getObjectMap();
        assertNotNull(returned, "Returned map must not be null after reflection injection.");
        assertEquals(1, returned.size(), "Returned map should contain exactly one entry.");
        assertEquals("value", returned.get("key"), "Returned map should contain the entry that was pre‑populated.");
    }

    /**
     * Case 4 – “HashMap containing {\"newKey\":\"newValue\"} after mutation”:
     * Obtain the map via {@code getObjectMap()}, add a new entry to the returned map,
     * and assert that the internal {@code objectMap} of the context reflects this change.
     * This confirms that the getter returns the actual mutable map reference.
     */
    @Test
    void getObjectMap_isLiveReferenceWhenMutated() throws Exception {
        // Initialise the internal map to an empty HashMap via reflection
        Map<String, Object> mutableMap = new HashMap<>();
        setInternalObjectMap(mutableMap);
        // Retrieve the map through the public getter and mutate it
        Map<String, Object> mapFromGetter = mic.getObjectMap();
        mapFromGetter.put("newKey", "newValue");
        // The internal field should now contain the new entry as well
        Map<String, Object> internalAfterMutation = mic.getObjectMap();
        assertEquals(1, internalAfterMutation.size(), "After mutating the map obtained from the getter, the internal map should reflect the change.");
        assertEquals("newValue", internalAfterMutation.get("newKey"), "The newly added entry must be visible via subsequent calls to getObjectMap().");
        assertSame(mapFromGetter, internalAfterMutation, "Both references should point to the same underlying map instance.");
    }
}
