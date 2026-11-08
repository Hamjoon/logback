package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Stack;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_createAppenderBags_Test {

    private Context mockContext;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple mock of Context – its behaviour is not required for this test
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        mic = null;
        mockContext = null;
    }

    @Test
    void createAppenderBags_populatesObjectMapWithEmptyAppenderBags() {
        // GIVEN – a freshly constructed ModelInterpretationContext
        Map<String, Object> objectMapBefore = mic.getObjectMap();
        assertFalse(objectMapBefore.containsKey(JoranConstants.APPENDER_BAG), "Object map should not contain APPENDER_BAG before creation");
        assertFalse(objectMapBefore.containsKey(JoranConstants.APPENDER_REF_BAG), "Object map should not contain APPENDER_REF_BAG before creation");
        // WHEN – invoking the method under test
        mic.createAppenderBags();
        // THEN – the object map contains the two expected entries, each an empty HashMap
        Map<String, Object> objectMapAfter = mic.getObjectMap();
        assertTrue(objectMapAfter.containsKey(JoranConstants.APPENDER_BAG), "Object map must contain APPENDER_BAG after creation");
        assertTrue(objectMapAfter.containsKey(JoranConstants.APPENDER_REF_BAG), "Object map must contain APPENDER_REF_BAG after creation");
        Object appenderBag = objectMapAfter.get(JoranConstants.APPENDER_BAG);
        Object appenderRefBag = objectMapAfter.get(JoranConstants.APPENDER_REF_BAG);
        assertNotNull(appenderBag, "APPENDER_BAG value should not be null");
        assertNotNull(appenderRefBag, "APPENDER_REF_BAG value should not be null");
        // Both values should be instances of HashMap (or at least Map) and initially empty
        assertTrue(appenderBag instanceof Map, "APPENDER_BAG should be a Map");
        assertTrue(appenderRefBag instanceof Map, "APPENDER_REF_BAG should be a Map");
        @SuppressWarnings("unchecked")
        Map<String, ?> appenderBagMap = (Map<String, ?>) appenderBag;
        @SuppressWarnings("unchecked")
        Map<String, ?> appenderRefBagMap = (Map<String, ?>) appenderRefBag;
        assertTrue(appenderBagMap.isEmpty(), "APPENDER_BAG map should be empty");
        assertTrue(appenderRefBagMap.isEmpty(), "APPENDER_REF_BAG map should be empty");
    }

    /* --------------------------------------------------------------
     *  Overwrite existing keys
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_overwritesExistingNonMapValues() {
        // GIVEN – objectMap already contains non‑Map values for the two keys
        Map<String, Object> map = mic.getObjectMap();
        map.put(JoranConstants.APPENDER_BAG, "oldString");
        map.put(JoranConstants.APPENDER_REF_BAG, Integer.valueOf(123));
        // WHEN
        mic.createAppenderBags();
        // THEN – the old values are replaced by fresh HashMap instances
        Object newBag = map.get(JoranConstants.APPENDER_BAG);
        Object newRefBag = map.get(JoranConstants.APPENDER_REF_BAG);
        assertNotNull(newBag, "APPENDER_BAG should be replaced with a non‑null value");
        assertNotNull(newRefBag, "APPENDER_REF_BAG should be replaced with a non‑null value");
        assertTrue(newBag instanceof HashMap, "APPENDER_BAG should be a HashMap after replacement");
        assertTrue(newRefBag instanceof HashMap, "APPENDER_REF_BAG should be a HashMap after replacement");
    }

    /* --------------------------------------------------------------
     *  Preserve unrelated entries
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_preservesUnrelatedEntries() {
        // GIVEN – an unrelated entry in objectMap
        Map<String, Object> map = mic.getObjectMap();
        map.put("foo", Integer.valueOf(42));
        // WHEN
        mic.createAppenderBags();
        // THEN – the unrelated entry is still present and unchanged
        assertTrue(map.containsKey("foo"), "Unrelated key 'foo' must still be present");
        assertEquals(Integer.valueOf(42), map.get("foo"), "Value for key 'foo' must remain unchanged");
    }

    /* --------------------------------------------------------------
     *  Null objectMap handling
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_nullObjectMap_throwsNPE() throws Exception {
        // GIVEN – force objectMap to null via reflection
        Field objectMapField = ModelInterpretationContext.class.getDeclaredField("objectMap");
        objectMapField.setAccessible(true);
        objectMapField.set(mic, null);
        // WHEN / THEN – NPE is expected
        assertThrows(NullPointerException.class, () -> mic.createAppenderBags(), "Calling createAppenderBags() with a null objectMap should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Idempotent behavior – multiple invocations
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_multipleInvocationsReplaceMaps() {
        // FIRST CALL
        mic.createAppenderBags();
        Map<String, Object> afterFirst = mic.getObjectMap();
        Object firstBag = afterFirst.get(JoranConstants.APPENDER_BAG);
        Object firstRefBag = afterFirst.get(JoranConstants.APPENDER_REF_BAG);
        // SECOND CALL
        mic.createAppenderBags();
        Map<String, Object> afterSecond = mic.getObjectMap();
        Object secondBag = afterSecond.get(JoranConstants.APPENDER_BAG);
        Object secondRefBag = afterSecond.get(JoranConstants.APPENDER_REF_BAG);
        // THEN – new instances, both empty
        assertNotSame(firstBag, secondBag, "APPENDER_BAG instance must be different after second call");
        assertNotSame(firstRefBag, secondRefBag, "APPENDER_REF_BAG instance must be different after second call");
        @SuppressWarnings("unchecked")
        Map<String, ?> secondBagMap = (Map<String, ?>) secondBag;
        @SuppressWarnings("unchecked")
        Map<String, ?> secondRefBagMap = (Map<String, ?>) secondRefBag;
        assertTrue(secondBagMap.isEmpty(), "APPENDER_BAG after second call must be empty");
        assertTrue(secondRefBagMap.isEmpty(), "APPENDER_REF_BAG after second call must be empty");
    }

    /* --------------------------------------------------------------
     *  Map mutability after creation
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_mapsAreMutable() {
        // GIVEN – create the bags
        mic.createAppenderBags();
        // Retrieve the APPENDER_BAG map
        @SuppressWarnings("unchecked")
        Map<String, Appender<?>> appenderBag = (Map<String, Appender<?>>) mic.getObjectMap().get(JoranConstants.APPENDER_BAG);
        // Mock an Appender
        @SuppressWarnings("unchecked")
        Appender<Object> mockAppender = Mockito.mock(Appender.class);
        String appenderName = "myAppender";
        // WHEN – put the mock into the bag and retrieve it
        appenderBag.put(appenderName, mockAppender);
        Appender<?> retrieved = appenderBag.get(appenderName);
        // THEN – the map behaved as a normal mutable map
        assertNotNull(retrieved, "Retrieved appender should not be null");
        assertSame(mockAppender, retrieved, "Retrieved appender must be the same instance that was put");
    }

    /* --------------------------------------------------------------
     *  No side‑effects on other stacks
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_doesNotTouchObjectOrModelStacks() throws Exception {
        // GIVEN – capture sizes before the call
        Stack<Object> objectStack = mic.getObjectStack();
        int objectStackSizeBefore = objectStack.size();
        // modelStack is package‑private; access via reflection
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Stack<Model> modelStack = (Stack<Model>) modelStackField.get(mic);
        int modelStackSizeBefore = modelStack.size();
        // WHEN
        mic.createAppenderBags();
        // THEN – sizes unchanged
        assertEquals(objectStackSizeBefore, objectStack.size(), "objectStack size must remain unchanged after createAppenderBags()");
        assertEquals(modelStackSizeBefore, modelStack.size(), "modelStack size must remain unchanged after createAppenderBags()");
    }

    /* --------------------------------------------------------------
     *  Unmodifiable objectMap handling
     * -------------------------------------------------------------- */
    @Test
    void createAppenderBags_unmodifiableObjectMap_throwsUnsupportedOperationException() throws Exception {
        // GIVEN – replace objectMap with an unmodifiable wrapper
        Field objectMapField = ModelInterpretationContext.class.getDeclaredField("objectMap");
        objectMapField.setAccessible(true);
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        objectMapField.set(mic, unmodifiable);
        // WHEN / THEN – UnsupportedOperationException expected
        assertThrows(UnsupportedOperationException.class, () -> mic.createAppenderBags(), "Calling createAppenderBags() on an unmodifiable objectMap should throw UnsupportedOperationException");
    }
}
