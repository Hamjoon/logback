package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.JoranConstants;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_createAppenderBags_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Context – the real Context implementation is not needed for this test
        mockContext = mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the instance references to help GC; not strictly required
        mic = null;
        mockContext = null;
    }

    @Test
    void createAppenderBags_populatesObjectMapWithEmptyBags() {
        // GIVEN – a freshly created ModelInterpretationContext
        Map<String, Object> objectMap = mic.getObjectMap();
        // Ensure the map does not already contain the expected keys
        assertFalse(objectMap.containsKey(JoranConstants.APPENDER_BAG), "Object map should not contain APPENDER_BAG before createAppenderBags()");
        assertFalse(objectMap.containsKey(JoranConstants.APPENDER_REF_BAG), "Object map should not contain APPENDER_REF_BAG before createAppenderBags()");
        // WHEN – the method under test is invoked
        mic.createAppenderBags();
        // THEN – the map contains the two expected entries, each holding an empty HashMap
        assertTrue(objectMap.containsKey(JoranConstants.APPENDER_BAG), "Object map should contain APPENDER_BAG after createAppenderBags()");
        assertTrue(objectMap.containsKey(JoranConstants.APPENDER_REF_BAG), "Object map should contain APPENDER_REF_BAG after createAppenderBags()");
        Object appenderBagObj = objectMap.get(JoranConstants.APPENDER_BAG);
        Object appenderRefBagObj = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        assertNotNull(appenderBagObj, "APPENDER_BAG value must not be null");
        assertNotNull(appenderRefBagObj, "APPENDER_REF_BAG value must not be null");
        assertTrue(appenderBagObj instanceof HashMap, "APPENDER_BAG should be an instance of HashMap");
        assertTrue(appenderRefBagObj instanceof HashMap, "APPENDER_REF_BAG should be an instance of HashMap");
        @SuppressWarnings("unchecked")
        HashMap<String, Appender<?>> appenderBag = (HashMap<String, Appender<?>>) appenderBagObj;
        @SuppressWarnings("unchecked")
        HashMap<String, AppenderAttachable<?>> appenderRefBag = (HashMap<String, AppenderAttachable<?>>) appenderRefBagObj;
        assertTrue(appenderBag.isEmpty(), "APPENDER_BAG map should be empty after creation");
        assertTrue(appenderRefBag.isEmpty(), "APPENDER_REF_BAG map should be empty after creation");
    }

    /* -------------------------------------------------------------
     *  Overwrite existing bags – method is idempotent
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_isIdempotentAndReplacesOldBags() {
        // GIVEN – objectMap already contains some entries including old bags
        Map<String, Object> objectMap = mic.getObjectMap();
        HashMap<String, Appender<?>> oldAppenderBag = new HashMap<>();
        oldAppenderBag.put("old", mock(Appender.class));
        HashMap<String, AppenderAttachable<?>> oldAppenderRefBag = new HashMap<>();
        oldAppenderRefBag.put("oldRef", mock(AppenderAttachable.class));
        objectMap.put(JoranConstants.APPENDER_BAG, oldAppenderBag);
        objectMap.put(JoranConstants.APPENDER_REF_BAG, oldAppenderRefBag);
        objectMap.put("customKey", "customValue");
        // WHEN – createAppenderBags is called a second time
        mic.createAppenderBags();
        // THEN – new empty HashMaps are stored, old instances are not referenced any more
        Object newAppenderBagObj = objectMap.get(JoranConstants.APPENDER_BAG);
        Object newAppenderRefBagObj = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        assertNotSame(oldAppenderBag, newAppenderBagObj, "APPENDER_BAG should be replaced with a new HashMap instance");
        assertNotSame(oldAppenderRefBag, newAppenderRefBagObj, "APPENDER_REF_BAG should be replaced with a new HashMap instance");
        @SuppressWarnings("unchecked")
        HashMap<String, Appender<?>> newAppenderBag = (HashMap<String, Appender<?>>) newAppenderBagObj;
        @SuppressWarnings("unchecked")
        HashMap<String, AppenderAttachable<?>> newAppenderRefBag = (HashMap<String, AppenderAttachable<?>>) newAppenderRefBagObj;
        assertTrue(newAppenderBag.isEmpty(), "New APPENDER_BAG must be empty");
        assertTrue(newAppenderRefBag.isEmpty(), "New APPENDER_REF_BAG must be empty");
        // Custom entry must stay untouched
        assertEquals("customValue", objectMap.get("customKey"));
    }

    /* -------------------------------------------------------------
     *  Null objectMap – defensive check
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_throwsNPEWhenObjectMapIsNull() throws Exception {
        // GIVEN – force the private field objectMap to be null via reflection
        Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
        f.setAccessible(true);
        f.set(mic, null);
        // WHEN / THEN – invoking the method should throw NullPointerException
        assertThrows(NullPointerException.class, mic::createAppenderBags, "Expected NPE when objectMap is null");
    }

    /* -------------------------------------------------------------
     *  Replace non‑HashMap values with new HashMaps
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_replacesNonHashMapValuesWithEmptyMaps() {
        // GIVEN – map contains non‑HashMap objects for the two keys
        Map<String, Object> objectMap = mic.getObjectMap();
        objectMap.put(JoranConstants.APPENDER_BAG, "not a map");
        objectMap.put(JoranConstants.APPENDER_REF_BAG, 12345);
        objectMap.put("otherKey", "otherValue");
        // WHEN
        mic.createAppenderBags();
        // THEN – values are now empty HashMaps, other entries unchanged
        Object appenderBagObj = objectMap.get(JoranConstants.APPENDER_BAG);
        Object appenderRefBagObj = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        assertTrue(appenderBagObj instanceof HashMap, "APPENDER_BAG should be replaced by a HashMap");
        assertTrue(appenderRefBagObj instanceof HashMap, "APPENDER_REF_BAG should be replaced by a HashMap");
        @SuppressWarnings("unchecked")
        HashMap<String, ?> appenderBag = (HashMap<String, ?>) appenderBagObj;
        @SuppressWarnings("unchecked")
        HashMap<String, ?> appenderRefBag = (HashMap<String, ?>) appenderRefBagObj;
        assertTrue(appenderBag.isEmpty(), "Replaced APPENDER_BAG must be empty");
        assertTrue(appenderRefBag.isEmpty(), "Replaced APPENDER_REF_BAG must be empty");
        assertEquals("otherValue", objectMap.get("otherKey"));
    }

    /* -------------------------------------------------------------
     *  Null values for existing bag keys are turned into empty HashMaps
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_turnsNullValuesIntoEmptyHashMaps() {
        // GIVEN – keys exist but map to null
        Map<String, Object> objectMap = mic.getObjectMap();
        objectMap.put(JoranConstants.APPENDER_BAG, null);
        objectMap.put(JoranConstants.APPENDER_REF_BAG, null);
        // WHEN
        mic.createAppenderBags();
        // THEN – both entries are non‑null empty HashMaps
        Object appenderBagObj = objectMap.get(JoranConstants.APPENDER_BAG);
        Object appenderRefBagObj = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        assertNotNull(appenderBagObj, "APPENDER_BAG should be non‑null after creation");
        assertNotNull(appenderRefBagObj, "APPENDER_REF_BAG should be non‑null after creation");
        assertTrue(appenderBagObj instanceof HashMap, "APPENDER_BAG should be a HashMap");
        assertTrue(appenderRefBagObj instanceof HashMap, "APPENDER_REF_BAG should be a HashMap");
        @SuppressWarnings("unchecked")
        HashMap<String, ?> appenderBag = (HashMap<String, ?>) appenderBagObj;
        @SuppressWarnings("unchecked")
        HashMap<String, ?> appenderRefBag = (HashMap<String, ?>) appenderRefBagObj;
        assertTrue(appenderBag.isEmpty(), "APPENDER_BAG must be empty");
        assertTrue(appenderRefBag.isEmpty(), "APPENDER_REF_BAG must be empty");
    }

    /* -------------------------------------------------------------
     *  Unmodifiable objectMap throws UnsupportedOperationException
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_throwsUOEWhenObjectMapIsUnmodifiable() throws Exception {
        // GIVEN – replace objectMap with an unmodifiable view
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("someKey", "someValue");
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(mutable);
        Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
        f.setAccessible(true);
        f.set(mic, unmodifiable);
        // WHEN / THEN – put operation inside createAppenderBags should fail
        assertThrows(UnsupportedOperationException.class, mic::createAppenderBags, "Expected UnsupportedOperationException when objectMap is unmodifiable");
    }

    /* -------------------------------------------------------------
     *  Method does not affect other internal state
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_doesNotAlterOtherInternalFields() {
        // GIVEN – populate other mutable fields via public API
        mic.pushObject("obj1");
        mic.pushObject(42);
        mic.pushModel(mock(Model.class));
        mic.addSubstitutionProperty("key1", "value1");
        mic.addImport("stem", "fqcn");
        // Capture snapshots before the call
        List<Object> objectStackBefore = new ArrayList<>(mic.getObjectStack());
        // modelStack is not directly exposed; we can peek/pop to infer size
        // we only pushed one model
        int modelStackSizeBefore = mic.isModelStackEmpty() ? 0 : 1;
        Map<String, String> propertiesBefore = new HashMap<>(mic.getCopyOfPropertyMap());
        Map<String, String> importBefore = new HashMap<>(mic.getImportMapCopy());
        // WHEN
        mic.createAppenderBags();
        // THEN – verify that the captured snapshots are unchanged
        assertEquals(objectStackBefore, new ArrayList<>(mic.getObjectStack()), "objectStack must remain unchanged");
        // modelStack: we only pushed one mock; ensure it is still present
        assertFalse(mic.isModelStackEmpty(), "modelStack should still contain the previously pushed model");
        // properties and import maps
        assertEquals(propertiesBefore, mic.getCopyOfPropertyMap(), "propertiesMap must remain unchanged");
        assertEquals(importBefore, mic.getImportMapCopy(), "importMap must remain unchanged");
    }

    /* -------------------------------------------------------------
     *  Created HashMaps are distinct objects
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_hashMapsAreDistinctInstances() {
        // GIVEN – fresh context
        mic.createAppenderBags();
        // WHEN – retrieve both bag objects
        Map<String, Object> objectMap = mic.getObjectMap();
        Object bag1 = objectMap.get(JoranConstants.APPENDER_BAG);
        Object bag2 = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        // THEN – they must not be the same reference
        assertNotSame(bag1, bag2, "APPENDER_BAG and APPENDER_REF_BAG must be distinct objects");
    }

    /* -------------------------------------------------------------
     *  Multiple sequential calls keep other entries intact
     * ------------------------------------------------------------- */
    @Test
    void createAppenderBags_multipleCallsPreserveCustomEntriesAndRefreshBags() {
        // GIVEN – add a custom entry
        Map<String, Object> objectMap = mic.getObjectMap();
        objectMap.put("customKey", "customValue");
        // Capture the custom entry reference (String is immutable, but we compare value)
        String customValueBefore = (String) objectMap.get("customKey");
        // Capture bag references across three calls
        List<Object> bagRefs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mic.createAppenderBags();
            bagRefs.add(objectMap.get(JoranConstants.APPENDER_BAG));
            bagRefs.add(objectMap.get(JoranConstants.APPENDER_REF_BAG));
        }
        // THEN – custom entry is still present and unchanged
        assertEquals("customValue", objectMap.get("customKey"), "Custom entry must survive multiple calls");
        // Verify that each successive bag reference is a new instance
        for (int i = 2; i < bagRefs.size(); i++) {
            assertNotSame(bagRefs.get(i - 2), bagRefs.get(i), "Bag instance should be refreshed on each call");
        }
        // Also ensure all bags are empty HashMaps
        for (Object bagObj : bagRefs) {
            assertTrue(bagObj instanceof HashMap, "Bag should be a HashMap");
            @SuppressWarnings("unchecked")
            HashMap<String, ?> bag = (HashMap<String, ?>) bagObj;
            assertTrue(bag.isEmpty(), "Bag should be empty after creation");
        }
    }
}
