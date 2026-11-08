package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ModelInterpretationContext}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Construction and initial state</li>
 *   <li>Model stack operations</li>
 *   <li>Object stack operations (including error scenarios)</li>
 *   <li>Configurator hint handling</li>
 *   <li>Variable substitution facilities</li>
 *   <li>Dependency handling (including immutability and edge cases)</li>
 *   <li>Import map handling</li>
 *   <li>Property container behaviour</li>
 * </ul>
 * <p>
 * A minimal {@link Context} implementation is provided inside the test class
 * because the real Logback {@code ContextBase} is not part of the allowed API.
 */
public class ModelInterpretationContextTest {

    /** Simple concrete {@link Context} implementation used only for testing. */
    private static class SimpleContext implements Context {
        private final Map<String, String> propertyMap = new HashMap<>();
        private final Map<String, Object> objectMap = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objectMap.get(key); }
        @Override public void putObject(String key, Object value) { objectMap.put(key, value); }
        @Override public String getProperty(String key) { return propertyMap.get(key); }
        @Override public void putProperty(String key, String value) { propertyMap.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(propertyMap); }
        @Override public String getName() { return "SimpleContext"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) {}
        @Override public void setContext(Context context) {}
        @Override public Context getContext() { return this; }
        @Override public void addStatus(Status status) {}
        @Override public void addInfo(String msg) {}
        @Override public void addInfo(String msg, Throwable ex) {}
        @Override public void addWarn(String msg) {}
        @Override public void addWarn(String msg, Throwable ex) {}
        @Override public void addError(String msg) {}
        @Override public void addError(String msg, Throwable ex) {}
        @Override public void addSubstitutionProperty(String key, String value) { propertyMap.put(key, value); }
        @Override public String getProperty(String key, String defaultValue) { return propertyMap.getOrDefault(key, defaultValue); }
        @Override public void addSubstitutionProperties(Properties props) {
            for (String name : props.stringPropertyNames()) {
                propertyMap.put(name, props.getProperty(name));
            }
        }
    }

    private ModelInterpretationContext mic;
    private SimpleContext context;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // -------------------------------------------------------------------------
    // Construction & basic getters
    // -------------------------------------------------------------------------

    @Test
    public void constructorInitialisesStacksAndMaps() {
        assertNotNull("objectStack should be created", mic.getObjectStack());
        assertNotNull("modelStack should be created", mic.getObjectStack()); // same reference check as above
        assertTrue("objectStack must be empty initially", mic.isObjectStackEmpty());
        assertTrue("modelStack must be empty initially", mic.isModelStackEmpty());

        // objectMap is empty until createAppenderBags() is called
        assertTrue("objectMap should be empty before createAppenderBags()", mic.getObjectMap().isEmpty());

        // topModel is null by default
        assertNull("topModel should be null initially", mic.getTopModel());
    }

    @Test
    public void createAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue("APPENDER_BAG must be present", map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue("APPENDER_REF_BAG must be present", map.containsKey(JoranConstants.APPENDER_REF_BAG));
        assertTrue("APPENDER_BAG should be a Map", map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue("APPENDER_REF_BAG should be a Map", map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    @Test
    public void topModelGetterSetterWork() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("setTopModel / getTopModel must return the same instance", model, mic.getTopModel());
    }

    // -------------------------------------------------------------------------
    // Model stack operations
    // -------------------------------------------------------------------------

    @Test
    public void modelStackPushPeekPopAndEmptyChecks() {
        Model m1 = new Model();
        Model m2 = new Model();

        assertTrue("model stack should be empty initially", mic.isModelStackEmpty());

        mic.pushModel(m1);
        assertFalse("model stack should not be empty after push", mic.isModelStackEmpty());
        assertSame("peekModel must return the last pushed model", m1, mic.peekModel());

        mic.pushModel(m2);
        assertSame("peekModel must return the most recent model", m2, mic.peekModel());

        assertSame("popModel must return m2", m2, mic.popModel());
        assertSame("peekModel now should be m1", m1, mic.peekModel());

        assertSame("popModel must return m1", m1, mic.popModel());
        assertTrue("model stack should be empty after popping all", mic.isModelStackEmpty());
    }

    // -------------------------------------------------------------------------
    // Object stack operations
    // -------------------------------------------------------------------------

    @Test
    public void objectStackBasicOperations() {
        String obj1 = "first";
        Integer obj2 = 42;

        assertTrue("object stack should be empty initially", mic.isObjectStackEmpty());

        mic.pushObject(obj1);
        assertFalse("object stack not empty after push", mic.isObjectStackEmpty());
        assertSame("peekObject returns the pushed object", obj1, mic.peekObject());

        mic.pushObject(obj2);
        assertSame("peekObject returns the most recent object", obj2, mic.peekObject());

        assertSame("getObject(0) returns the first object", obj1, mic.getObject(0));
        assertSame("getObject(1) returns the second object", obj2, mic.getObject(1));

        assertSame("popObject returns the last pushed object", obj2, mic.popObject());
        assertSame("peekObject now returns the first object again", obj1, mic.peekObject());

        assertSame("popObject returns the remaining object", obj1, mic.popObject());
        assertTrue("object stack empty after popping all", mic.isObjectStackEmpty());
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void peekObjectOnEmptyStackThrowsException() {
        mic.peekObject(); // should throw EmptyStackException
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void popObjectOnEmptyStackThrowsException() {
        mic.popObject(); // should throw EmptyStackException
    }

    // -------------------------------------------------------------------------
    // Configurator hint handling
    // -------------------------------------------------------------------------

    @Test
    public void configuratorHintGetterSetterWork() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint must be returned unchanged", hint, mic.getConfiguratorHint());
    }

    // -------------------------------------------------------------------------
    // BeanDescriptionCache accessor
    // -------------------------------------------------------------------------

    @Test
    public void beanDescriptionCacheIsCreated() {
        assertNotNull("beanDescriptionCache should be non‑null", mic.getBeanDescriptionCache());
    }

    // -------------------------------------------------------------------------
    // Variable substitution facilities
    // -------------------------------------------------------------------------

    @Test
    public void substitutionWorksThroughVariableSubstitutionsHelper() {
        mic.addSubstitutionProperty("key", "value");
        assertEquals("Variable substitution should replace ${key} with value",
                "value", mic.subst("${key}"));
    }

    @Test
    public void getCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("a", "1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("Copy should contain the added property", "1", copy.get("a"));
        // modifying the copy must not affect the original
        copy.put("b", "2");
        assertNull("Original map must not contain newly added entry", mic.getProperty("b"));
    }

    // -------------------------------------------------------------------------
    // DefaultNestedComponentRegistry accessor
    // -------------------------------------------------------------------------

    @Test
    public void defaultNestedComponentRegistryIsAvailable() {
        assertNotNull("defaultNestedComponentRegistry must be non‑null", mic.getDefaultNestedComponentRegistry());
    }

    // -------------------------------------------------------------------------
    // Dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void addAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dependee1");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals("Exactly one dependency definition should be present", 1, list.size());
        assertSame("The stored definition must be the same instance", dd, list.get(0));

        // list must be unmodifiable
        try {
            list.add(dd);
            fail("Expected UnsupportedOperationException when modifying the returned dependency list");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void getDependeeNamesForModelReturnsCorrectNames() {
        Model modelA = new Model();
        Model modelB = new Model();

        mic.addDependencyDefinition(new DependencyDefinition(modelA, "X"));
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "Y"));
        mic.addDependencyDefinition(new DependencyDefinition(modelB, "Z"));

        List<String> dependees = mic.getDependeeNamesForModel(modelA);
        assertEquals("Two dependees expected for modelA", 2, dependees.size());
        assertTrue(dependees.contains("X"));
        assertTrue(dependees.contains("Y"));
        assertFalse(dependees.contains("Z"));
    }

    @Test
    public void hasDependersHandlesNullOrEmptyGracefully() {
        // No dependencies added – should return false
        assertFalse("Empty name should yield false", mic.hasDependers(null));
        assertFalse("Empty string should yield false", mic.hasDependers("   "));

        // Add a dependency and verify detection
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "myDependee"));
        assertTrue("Existing dependee name must be reported", mic.hasDependers("myDependee"));
        assertFalse("Non‑existing name must be reported as false", mic.hasDependers("other"));
    }

    @Test
    public void markAndCheckNamedDependeeStarted() {
        String name = "serviceA";
        assertFalse("Initially not started", mic.isNamedDependeeStarted(name));
        mic.markStartOfNamedDependee(name);
        assertTrue("After marking, should be started", mic.isNamedDependeeStarted(name));
    }

    // -------------------------------------------------------------------------
    // Import map handling
    // -------------------------------------------------------------------------

    @Test
    public void importMapAddAndRetrieveWorks() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // getImport should resolve known stems and fallback to the stem itself
        assertEquals("java.util.List", mic.getImport("List"));
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
        assertNull("null stem must return null", mic.getImport(null));
    }

    // -------------------------------------------------------------------------
    // Property container behaviour (delegated to VariableSubstitutionsHelper)
    // -------------------------------------------------------------------------

    @Test
    public void propertyContainerAddAndRetrieve() {
        mic.addSubstitutionProperty("prop1", "val1");
        assertEquals("val1", mic.getProperty("prop1"));
    }

    // -------------------------------------------------------------------------
    // Additional edge case: ensure getObject(int) respects stack order
    // -------------------------------------------------------------------------

    @Test
    public void getObjectByIndexReflectsStackOrder() {
        String first = "first";
        String second = "second";
        String third = "third";

        mic.pushObject(first);
        mic.pushObject(second);
        mic.pushObject(third);

        // Stack order: bottom[0]=first, [1]=second, top[2]=third
        assertSame(first, mic.getObject(0));
        assertSame(second, mic.getObject(1));
        assertSame(third, mic.getObject(2));
    }
}
