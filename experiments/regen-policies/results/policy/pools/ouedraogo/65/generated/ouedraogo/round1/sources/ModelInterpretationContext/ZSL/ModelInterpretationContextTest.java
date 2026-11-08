package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import java.util.function.Supplier;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 *
 * The tests use simple stub implementations for the {@link Context} and other
 * interfaces required by {@link ModelInterpretationContext}. No external
 * libraries or mocking frameworks are used, as required.
 */
public class ModelInterpretationContextTest {

    /** Simple stub implementation of {@link Context}. */
    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }

        @Override public Object getObject(String key) { return objects.get(key); }

        @Override public void putObject(String key, Object value) { objects.put(key, value); }

        @Override public String getProperty(String key) { return properties.get(key); }

        @Override public void putProperty(String key, String value) { properties.put(key, value); }

        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }

        @Override public String getName() { return "SimpleContext"; }

        @Override public void setName(String name) { /* no‑op */ }

        @Override public long getBirthTime() { return System.currentTimeMillis(); }

        @Override public Object getConfigurationLock() { return this; }

        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }

        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }

        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }

        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }

        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { /* no‑op */ }

        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }

        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }

        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) { /* no‑op */ }

        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    private ModelInterpretationContext mic;
    private SimpleContext simpleContext;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    // -------------------------------------------------------------------------
    // Constructor & basic fields
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorInitialisesStacksAndMaps() {
        assertNotNull("objectStack must be initialised", mic.getObjectStack());
        assertTrue("objectStack should be empty initially", mic.isObjectStackEmpty());

        assertNotNull("modelStack must be initialised", mic.peekModel()); // will throw if null; but stack is empty initially, so we check emptiness instead
        assertTrue("modelStack should be empty initially", mic.isModelStackEmpty());

        assertNotNull("objectMap must be non‑null", mic.getObjectMap());
        assertTrue("objectMap should be empty before createAppenderBags", mic.getObjectMap().isEmpty());

        // after explicit call to createAppenderBags the map contains two entries
        mic.createAppenderBags();
        assertTrue("objectMap must contain APPENDER_BAG after createAppenderBags",
                mic.getObjectMap().containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG));
        assertTrue("objectMap must contain APPENDER_REF_BAG after createAppenderBags",
                mic.getObjectMap().containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG));
    }

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("setTopModel / getTopModel must return the same instance", model, mic.getTopModel());
    }

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint must be stored and returned", hint, mic.getConfiguratorHint());
    }

    @Test
    public void testBeanDescriptionCacheIsCreated() {
        assertNotNull("BeanDescriptionCache must be instantiated", mic.getBeanDescriptionCache());
    }

    @Test
    public void testDefaultNestedComponentRegistryIsAccessible() {
        assertNotNull("DefaultNestedComponentRegistry must be non‑null", mic.getDefaultNestedComponentRegistry());
    }

    // -------------------------------------------------------------------------
    // Object stack operations
    // -------------------------------------------------------------------------

    @Test
    public void testObjectStackPushPeekPop() {
        String obj1 = "first";
        Integer obj2 = 42;

        mic.pushObject(obj1);
        mic.pushObject(obj2);

        assertFalse("Stack should not be empty after pushes", mic.isObjectStackEmpty());

        assertEquals("peekObject must return the last pushed element", obj2, mic.peekObject());

        assertEquals("popObject must return last element", obj2, mic.popObject());
        assertEquals("peek after one pop must return the previous element", obj1, mic.peekObject());

        assertEquals("popObject must return the remaining element", obj1, mic.popObject());
        assertTrue("Stack must be empty after popping all elements", mic.isObjectStackEmpty());
    }

    @Test
    public void testObjectStackGetByIndex() {
        String a = "a";
        String b = "b";
        String c = "c";

        mic.pushObject(a);
        mic.pushObject(b);
        mic.pushObject(c);

        assertEquals("Index 0 must be first pushed element", a, mic.getObject(0));
        assertEquals("Index 1 must be second pushed element", b, mic.getObject(1));
        assertEquals("Index 2 must be third pushed element", c, mic.getObject(2));
    }

    // -------------------------------------------------------------------------
    // Model stack operations
    // -------------------------------------------------------------------------

    @Test
    public void testModelStackPushPeekPop() {
        Model m1 = new Model();
        Model m2 = new Model();

        mic.pushModel(m1);
        mic.pushModel(m2);

        assertFalse("Model stack should not be empty after pushes", mic.isModelStackEmpty());
        assertSame("peekModel must return the last pushed model", m2, mic.peekModel());

        assertSame("popModel must return last model", m2, mic.popModel());
        assertSame("peekModel after pop must return previous model", m1, mic.peekModel());

        assertSame("popModel must return remaining model", m1, mic.popModel());
        assertTrue("Model stack must be empty after popping all", mic.isModelStackEmpty());
    }

    // -------------------------------------------------------------------------
    // Dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");

        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> retrieved = mic.getDependencyDefinitions();

        assertEquals("Exactly one dependency definition should be stored", 1, retrieved.size());
        assertSame("Stored definition must be the one added", dd, retrieved.get(0));

        // Verify unmodifiable view
        try {
            retrieved.add(dd);
            fail("Dependency definitions list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model modelA = new Model();
        Model modelB = new Model();

        mic.addDependencyDefinition(new DependencyDefinition(modelA, "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "dep2"));
        mic.addDependencyDefinition(new DependencyDefinition(modelB, "other"));

        List<String> depsForA = mic.getDependeeNamesForModel(modelA);
        assertEquals("Model A should have two dependees", 2, depsForA.size());
        assertTrue("Dependee list must contain dep1", depsForA.contains("dep1"));
        assertTrue("Dependee list must contain dep2", depsForA.contains("dep2"));

        List<String> depsForB = mic.getDependeeNamesForModel(modelB);
        assertEquals("Model B should have one dependee", 1, depsForB.size());
        assertEquals("The only dependee for B must be 'other'", "other", depsForB.get(0));
    }

    @Test
    public void testHasDependers() {
        Model someModel = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(someModel, "target"));

        assertTrue("hasDependers should return true for existing dependee name", mic.hasDependers("target"));
        assertFalse("hasDependers should return false for unknown name", mic.hasDependers("unknown"));
    }

    @Test
    public void testMarkAndCheckNamedDependeeStarted() {
        String name = "myDependee";

        assertFalse("Initially the named dependee must not be marked as started", mic.isNamedDependeeStarted(name));

        mic.markStartOfNamedDependee(name);
        assertTrue("After marking, isNamedDependeeStarted must return true", mic.isNamedDependeeStarted(name));
    }

    // -------------------------------------------------------------------------
    // Property substitution handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddSubstitutionPropertyAndGetProperty() {
        mic.addSubstitutionProperty("myKey", "myValue");

        assertEquals("Property must be retrievable via getProperty", "myValue", mic.getProperty("myKey"));
    }

    @Test
    public void testSubstReplacesVariable() {
        mic.addSubstitutionProperty("user", "john");
        String result = mic.subst("Hello ${user}!");

        assertEquals("subst should replace ${user} with the stored value", "Hello john!", result);
    }

    // -------------------------------------------------------------------------
    // Import handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddImportAndLookup() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        assertEquals("Fully qualified name must be returned for known stem",
                "java.util.List", mic.getImport("List"));
        assertEquals("Fully qualified name must be returned for known stem",
                "java.util.Map", mic.getImport("Map"));

        // unknown stem returns the stem itself
        assertEquals("Unknown stem must be returned unchanged", "Set", mic.getImport("Set"));

        // null stem returns null
        assertNull("null stem must return null", mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("String", "java.lang.String");
        Map<String, String> copy = mic.getImportMapCopy();

        assertEquals("Copy must contain the imported entry", "java.lang.String", copy.get("String"));
        // modify copy and ensure original map is unaffected
        copy.put("Integer", "java.lang.Integer");
        assertNull("Original import map must not be affected by modifications to the copy",
                mic.getImport("Integer"));
    }

    // -------------------------------------------------------------------------
    // Configurator supplier handling
    // -------------------------------------------------------------------------

    @Test
    public void testConfiguratorSupplierGetterSetter() {
        Supplier<GenericXMLConfigurator> supplier = () -> null; // simple supplier returning null
        mic.setConfiguratorSupplier(supplier);
        assertSame("Configurator supplier must be stored and returned", supplier, mic.getConfiguratorSupplier());
    }

    // -------------------------------------------------------------------------
    // Additional sanity checks
    // -------------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsCreatesTwoDistinctMaps() {
        mic.createAppenderBags();

        Object appenderBag = mic.getObjectMap().get(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG);
        Object appenderRefBag = mic.getObjectMap().get(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG);

        assertNotNull("APPENDER_BAG entry must be present", appenderBag);
        assertNotNull("APPENDER_REF_BAG entry must be present", appenderRefBag);
        assertTrue("APPENDER_BAG must be a Map", appenderBag instanceof Map);
        assertTrue("APPENDER_REF_BAG must be a Map", appenderRefBag instanceof Map);
        assertNotSame("Both bags must be distinct objects", appenderBag, appenderRefBag);
    }
}
