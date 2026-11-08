package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_createAppenderBags_Test {

    private Context mockContext;

    private ModelInterpretationContext mic;

    class StringOnlyMap extends AbstractMap<String, Object> {

        private final Map<String, Object> delegate = new HashMap<>();

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Object put(String key, Object value) {
            if (!(value instanceof String)) {
                throw new ClassCastException("Only String values are allowed");
            }
            return delegate.put(key, value);
        }
    }

    class StringOnlyMap extends AbstractMap<String, Object> {

        private final Map<String, Object> delegate = new HashMap<>();

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Object put(String key, Object value) {
            if (!(value instanceof String)) {
                throw new ClassCastException("Only String values are allowed");
            }
            return delegate.put(key, value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A simple mock of Context is sufficient – the constructor only stores the reference.
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        mic = null;
        mockContext = null;
    }

    /**
     * Helper that injects a custom {@code objectMap} into the instance via reflection.
     */
    private void setObjectMap(Map<String, Object> map) throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
        f.setAccessible(true);
        f.set(mic, map);
    }

    @Test
    void createAppenderBags_populatesObjectMapWithExpectedEntries() {
        // GIVEN – a freshly constructed ModelInterpretationContext (handled in @BeforeEach)
        // WHEN
        mic.createAppenderBags();
        // THEN
        Map<String, Object> objectMap = mic.getObjectMap();
        // Verify that the two expected keys are present.
        assertTrue(objectMap.containsKey(JoranConstants.APPENDER_BAG), "Object map should contain key for APPENDER_BAG");
        assertTrue(objectMap.containsKey(JoranConstants.APPENDER_REF_BAG), "Object map should contain key for APPENDER_REF_BAG");
        // Verify that the associated values are maps (the method creates HashMap instances).
        Object appenderBag = objectMap.get(JoranConstants.APPENDER_BAG);
        Object appenderRefBag = objectMap.get(JoranConstants.APPENDER_REF_BAG);
        assertInstanceOf(Map.class, appenderBag, "APPENDER_BAG should be a Map instance");
        assertInstanceOf(Map.class, appenderRefBag, "APPENDER_REF_BAG should be a Map instance");
        // The maps should be initially empty.
        assertTrue(((Map<?, ?>) appenderBag).isEmpty(), "APPENDER_BAG map should be empty after creation");
        assertTrue(((Map<?, ?>) appenderRefBag).isEmpty(), "APPENDER_REF_BAG map should be empty after creation");
    }

    /**
     * Helper that extracts the current {@code objectMap} via reflection.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getObjectMap() throws Exception {
        Field f = ModelInterpretationContext.class.getDeclaredField("objectMap");
        f.setAccessible(true);
        return (Map<String, Object>) f.get(mic);
    }

    @Test
    void createAppenderBags_preservesUnrelatedEntries() throws Exception {
        // GIVEN – objectMap already contains an unrelated entry.
        Map<String, Object> preMap = new HashMap<>();
        preMap.put("someKey", "someValue");
        setObjectMap(preMap);
        // WHEN
        mic.createAppenderBags();
        // THEN
        Map<String, Object> objectMap = getObjectMap();
        // Unrelated entry must still be present.
        assertEquals("someValue", objectMap.get("someKey"), "Unrelated entries must be preserved");
        // The two bag entries must exist and be maps.
        assertTrue(objectMap.get(JoranConstants.APPENDER_BAG) instanceof Map, "APPENDER_BAG must be a Map");
        assertTrue(objectMap.get(JoranConstants.APPENDER_REF_BAG) instanceof Map, "APPENDER_REF_BAG must be a Map");
    }

    @Test
    void createAppenderBags_nullObjectMap_throwsNullPointerException() throws Exception {
        // GIVEN – internal objectMap is null.
        setObjectMap(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> mic.createAppenderBags(), "Calling createAppenderBags() with a null objectMap should throw NPE");
    }

    @Test
    void createAppenderBags_unmodifiableObjectMap_throwsUnsupportedOperationException() throws Exception {
        // GIVEN – an unmodifiable map.
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        setObjectMap(unmodifiable);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> mic.createAppenderBags(), "Calling createAppenderBags() on an unmodifiable map should propagate UOE");
    }

    @Test
    void createAppenderBags_idempotentReplacesPreviousBags() throws Exception {
        // GIVEN – first call creates the bags.
        mic.createAppenderBags();
        Map<String, Object> firstMap = getObjectMap();
        // Add a dummy entry to the APPENDER_BAG map.
        @SuppressWarnings("unchecked")
        Map<String, Object> appenderBag = (Map<String, Object>) firstMap.get(JoranConstants.APPENDER_BAG);
        appenderBag.put("dummy", "value");
        // WHEN – call again.
        mic.createAppenderBags();
        Map<String, Object> secondMap = getObjectMap();
        // THEN – new maps are fresh instances.
        assertNotSame(firstMap.get(JoranConstants.APPENDER_BAG), secondMap.get(JoranConstants.APPENDER_BAG), "APPENDER_BAG map should be replaced with a new instance");
        assertNotSame(firstMap.get(JoranConstants.APPENDER_REF_BAG), secondMap.get(JoranConstants.APPENDER_REF_BAG), "APPENDER_REF_BAG map should be replaced with a new instance");
        // Both new maps must be empty.
        assertTrue(((Map<?, ?>) secondMap.get(JoranConstants.APPENDER_BAG)).isEmpty(), "New APPENDER_BAG map must be empty");
        assertTrue(((Map<?, ?>) secondMap.get(JoranConstants.APPENDER_REF_BAG)).isEmpty(), "New APPENDER_REF_BAG map must be empty");
    }

    @Test
    void createAppenderBags_valuesAreConcreteHashMapInstances() throws Exception {
        // GIVEN – fresh context.
        mic.createAppenderBags();
        // WHEN
        Map<String, Object> objectMap = getObjectMap();
        // THEN – both values must be instances of java.util.HashMap (not just any Map).
        assertTrue(objectMap.get(JoranConstants.APPENDER_BAG) instanceof HashMap, "APPENDER_BAG should be a concrete HashMap");
        assertTrue(objectMap.get(JoranConstants.APPENDER_REF_BAG) instanceof HashMap, "APPENDER_REF_BAG should be a concrete HashMap");
    }

    @Test
    void createAppenderBags_noSideEffectsOnOtherFields() throws Exception {
        // Capture snapshots of the fields that must stay untouched.
        Field importMapField = ModelInterpretationContext.class.getDeclaredField("importMap");
        importMapField.setAccessible(true);
        Map<String, String> importMapBefore = (Map<String, String>) importMapField.get(mic);
        Field varHelperField = ModelInterpretationContext.class.getDeclaredField("variableSubstitutionsHelper");
        varHelperField.setAccessible(true);
        Object varHelperBefore = varHelperField.get(mic);
        Field registryField = ModelInterpretationContext.class.getDeclaredField("defaultNestedComponentRegistry");
        registryField.setAccessible(true);
        Object registryBefore = registryField.get(mic);
        Field depDefListField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        depDefListField.setAccessible(true);
        List<?> depDefListBefore = (List<?>) depDefListField.get(mic);
        Field startedDependeesField = ModelInterpretationContext.class.getDeclaredField("startedDependees");
        startedDependeesField.setAccessible(true);
        List<?> startedDependeesBefore = (List<?>) startedDependeesField.get(mic);
        Field configuratorHintField = ModelInterpretationContext.class.getDeclaredField("configuratorHint");
        configuratorHintField.setAccessible(true);
        Object configuratorHintBefore = configuratorHintField.get(mic);
        Field topModelField = ModelInterpretationContext.class.getDeclaredField("topModel");
        topModelField.setAccessible(true);
        Object topModelBefore = topModelField.get(mic);
        // WHEN
        mic.createAppenderBags();
        // THEN – all captured references must be unchanged (same instance and unchanged content).
        assertSame(importMapBefore, importMapField.get(mic), "importMap must remain unchanged");
        assertSame(varHelperBefore, varHelperField.get(mic), "variableSubstitutionsHelper must remain unchanged");
        assertSame(registryBefore, registryField.get(mic), "defaultNestedComponentRegistry must remain unchanged");
        assertSame(depDefListBefore, depDefListField.get(mic), "dependencyDefinitionList must remain unchanged");
        assertSame(startedDependeesBefore, startedDependeesField.get(mic), "startedDependees must remain unchanged");
        assertSame(configuratorHintBefore, configuratorHintField.get(mic), "configuratorHint must remain unchanged");
        assertSame(topModelBefore, topModelField.get(mic), "topModel must remain unchanged");
    }

    @Test
    void createAppenderBags_incompatibleValueType_throwsClassCastException() throws Exception {
        // GIVEN – a custom map that only accepts String values.
        class StringOnlyMap extends AbstractMap<String, Object> {

            private final Map<String, Object> delegate = new HashMap<>();

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public Object put(String key, Object value) {
                if (!(value instanceof String)) {
                    throw new ClassCastException("Only String values are allowed");
                }
                return delegate.put(key, value);
            }
        }
        Map<String, Object> badMap = new StringOnlyMap();
        setObjectMap(badMap);
        // WHEN / THEN – the method tries to put a HashMap, which is not a String.
        assertThrows(ClassCastException.class, () -> mic.createAppenderBags(), "Putting a HashMap into a map that only accepts Strings should cause CCE");
    }
}
