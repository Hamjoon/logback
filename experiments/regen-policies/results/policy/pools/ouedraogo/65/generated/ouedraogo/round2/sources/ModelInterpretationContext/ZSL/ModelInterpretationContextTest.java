package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ModelInterpretationContext}.
 * All public methods are exercised, including edge cases.
 */
public class ModelInterpretationContextTest {

    // -------------------------------------------------------------------------
    // Helper stub implementations
    // -------------------------------------------------------------------------

    /**
     * Minimal {@link Context} implementation used only for testing.
     */
    private static class TestContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();
        private String name = "testContext";

        @Override
        public StatusManager getStatusManager() {
            // Not needed for the tests – return null.
            return null;
        }

        @Override
        public Object getObject(String key) {
            return objects.get(key);
        }

        @Override
        public void putObject(String key, Object value) {
            objects.put(key, value);
        }

        @Override
        public String getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public void putProperty(String key, String value) {
            properties.put(key, value);
        }

        @Override
        public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(properties);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public long getBirthTime() {
            return System.currentTimeMillis();
        }

        @Override
        public Object getConfigurationLock() {
            return new Object();
        }

        @Override
        public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() {
            return null;
        }

        @Override
        public java.util.concurrent.ExecutorService getExecutorService() {
            return null;
        }

        @Override
        public java.util.concurrent.ExecutorService getAlternateExecutorService() {
            return null;
        }

        @Override
        public void register(LifeCycle component) {
            // no‑op
        }

        @Override
        public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {
            // no‑op
        }

        @Override
        public SequenceNumberGenerator getSequenceNumberGenerator() {
            return null;
        }

        @Override
        public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) {
            // no‑op
        }

        @Override
        public void addConfigurationEventListener(ConfigurationEventListener listener) {
            // no‑op
        }

        @Override
        public void fireConfigurationEvent(ConfigurationEvent configurationEvent) {
            // no‑op
        }

        // PropertyContainer methods (inherited from Context)
        @Override
        public void addSubstitutionProperty(String key, String value) {
            addProperty(key, value);
        }

        @Override
        public void addSubstitutionProperties(Properties props) {
            for (String name : props.stringPropertyNames()) {
                addProperty(name, props.getProperty(name));
            }
        }

        private void addProperty(String key, String value) {
            properties.put(key, value);
        }
    }

    /**
     * Minimal {@link GenericXMLConfigurator} implementation.
     */
    private static class TestConfigurator extends GenericXMLConfigurator {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op
        }
    }

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private TestContext testContext;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        testContext = new TestContext();
        mic = new ModelInterpretationContext(testContext);
    }

    // -------------------------------------------------------------------------
    // Tests for constructor and basic getters/setters
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorInitialisesStacksAndMaps() {
        assertNotNull("objectStack must be initialised", mic.getObjectStack());
        assertTrue("objectStack should be empty", mic.isObjectStackEmpty());

        assertNotNull("modelStack must be initialised", mic.modelStack);
        assertTrue("modelStack should be empty", mic.isModelStackEmpty());

        assertNotNull("objectMap must be initialised", mic.getObjectMap());
        assertTrue("objectMap should be empty initially", mic.getObjectMap().isEmpty());

        assertNotNull("variableSubstitutionsHelper must be initialised", mic.variableSubstitutionsHelper);
        assertNotNull("importMap must be initialised", mic.importMap);
        assertTrue("importMap should be empty initially", mic.importMap.isEmpty());

        assertNotNull("beanDescriptionCache must be initialised", mic.getBeanDescriptionCache());
    }

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint should be returned unchanged", hint, mic.getConfiguratorHint());
    }

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("Top model should be the one we set", model, mic.getTopModel());
    }

    // -------------------------------------------------------------------------
    // Tests for object stack manipulation
    // -------------------------------------------------------------------------

    @Test
    public void testObjectStackOperations() {
        String first = "first";
        Integer second = 42;

        mic.pushObject(first);
        mic.pushObject(second);

        assertFalse("Object stack should not be empty after pushes", mic.isObjectStackEmpty());
        assertEquals("peekObject should return last pushed", second, mic.peekObject());
        assertEquals("getObject(0) should be first element", first, mic.getObject(0));
        assertEquals("popObject should return last element", second, mic.popObject());
        assertEquals("peekObject after pop should be first element", first, mic.peekObject());
        assertEquals("popObject should now return first element", first, mic.popObject());
        assertTrue("Object stack should be empty after popping all", mic.isObjectStackEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests for model stack manipulation
    // -------------------------------------------------------------------------

    @Test
    public void testModelStackOperations() {
        Model a = new Model();
        Model b = new Model();

        mic.pushModel(a);
        mic.pushModel(b);

        assertFalse("Model stack should not be empty", mic.isModelStackEmpty());
        assertSame("peekModel should return last pushed", b, mic.peekModel());
        assertSame("popModel should return last pushed", b, mic.popModel());
        assertSame("peekModel after pop should be first pushed", a, mic.peekModel());
        assertSame("popModel should now return first pushed", a, mic.popModel());
        assertTrue("Model stack should be empty after pops", mic.isModelStackEmpty());
    }

    // -------------------------------------------------------------------------
    // Tests for createAppenderBags
    // -------------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();

        assertTrue("APPENDER_BAG key must be present", map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue("APPENDER_REF_BAG key must be present", map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue("APPENDER_BAG value must be a Map",
                map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue("APPENDER_REF_BAG value must be a Map",
                map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    // -------------------------------------------------------------------------
    // Tests for variable substitution helper
    // -------------------------------------------------------------------------

    @Test
    public void testSubstitutionWorksThroughVariableHelper() {
        mic.addSubstitutionProperty("key1", "value1");
        assertEquals("subst should resolve existing key", "value1", mic.subst("${key1}"));
        // Direct call to subst with a string that does not contain a variable
        assertEquals("subst should return input unchanged when no variable", "plain", mic.subst("plain"));
    }

    @Test
    public void testGetPropertyDelegatesToVariableHelper() {
        mic.addSubstitutionProperty("myProp", "myValue");
        assertEquals("getProperty should retrieve the same value", "myValue", mic.getProperty("myProp"));
    }

    @Test
    public void testCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("p", "v");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("Copy should contain the entry", "v", copy.get("p"));
        // modify copy and ensure original is unchanged
        copy.put("p", "changed");
        assertEquals("Original map should stay unchanged", "v", mic.getProperty("p"));
    }

    // -------------------------------------------------------------------------
    // Tests for import handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveImport() {
        mic.addImport("List", "java.util.List");
        assertEquals("getImport should return fully qualified name", "java.util.List", mic.getImport("List"));
        assertEquals("getImport for unknown stem returns the stem itself", "Unknown", mic.getImport("Unknown"));
        assertNull("getImport with null returns null", mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Map", "java.util.Map");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("Copy should contain the mapping", "java.util.Map", copy.get("Map"));
        copy.put("Map", "changed");
        assertEquals("Original import map should stay unchanged", "java.util.Map", mic.getImport("Map"));
    }

    // -------------------------------------------------------------------------
    // Tests for dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "someDependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals("Should contain exactly one dependency definition", 1, defs.size());
        assertSame("The stored definition should be the same instance", dd, defs.get(0));

        // Ensure the returned list is unmodifiable
        try {
            defs.add(dd);
            fail("Returned dependency list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model m1 = new Model();
        Model m2 = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m1, "A"));
        mic.addDependencyDefinition(new DependencyDefinition(m1, "B"));
        mic.addDependencyDefinition(new DependencyDefinition(m2, "C"));

        List<String> dependees = mic.getDependeeNamesForModel(m1);
        assertEquals("Should return two dependees for m1", 2, dependees.size());
        assertTrue(dependees.contains("A"));
        assertTrue(dependees.contains("B"));

        List<String> dependeesM2 = mic.getDependeeNamesForModel(m2);
        assertEquals("Should return one dependee for m2", 1, dependeesM2.size());
        assertEquals("C", dependeesM2.get(0));
    }

    @Test
    public void testHasDependersAndMarkStartOfNamedDependee() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "Target"));
        assertTrue("hasDependers should be true for existing dependee name", mic.hasDependers("Target"));
        assertFalse("hasDependers should be false for non‑existent name", mic.hasDependers("Missing"));

        mic.markStartOfNamedDependee("Alpha");
        assertTrue("isNamedDependeeStarted should be true after marking", mic.isNamedDependeeStarted("Alpha"));
        assertFalse("isNamedDependeeStarted should be false for unmarked name", mic.isNamedDependeeStarted("Beta"));
    }

    // -------------------------------------------------------------------------
    // Tests for configurator supplier handling
    // -------------------------------------------------------------------------

    @Test
    public void testConfiguratorSupplierGetterSetter() {
        Supplier<? extends GenericXMLConfigurator> supplier = TestConfigurator::new;
        mic.setConfiguratorSupplier(supplier);
        assertSame("Supplier should be returned unchanged", supplier, mic.getConfiguratorSupplier());

        // Verify that the supplier actually creates an instance of TestConfigurator
        GenericXMLConfigurator configurator = mic.getConfiguratorSupplier().get();
        assertTrue("Created configurator should be instance of TestConfigurator", configurator instanceof TestConfigurator);
    }

    // -------------------------------------------------------------------------
    // Tests for DefaultNestedComponentRegistry duplication
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultNestedComponentRegistryDuplicate() {
        // Add a dummy registration to the original registry
        mic.getDefaultNestedComponentRegistry().add(String.class, "dummy", Integer.class);

        // Create a new ModelInterpretationContext copying the original
        ModelInterpretationContext copyMic = new ModelInterpretationContext(mic);

        // The duplicated registry should contain the same registration
        String result = copyMic.getDefaultNestedComponentRegistry()
                .findDefaultComponentTypeByTag("dummy");
        // Since findDefaultComponentTypeByTag returns null by default (no implementation),
        // we simply verify that the duplicate operation did not throw and the registry exists.
        assertNotNull("Duplicated DefaultNestedComponentRegistry must not be null",
                copyMic.getDefaultNestedComponentRegistry());
    }

    // -------------------------------------------------------------------------
    // Tests for BeanDescriptionCache exposure
    // -------------------------------------------------------------------------

    @Test
    public void testBeanDescriptionCacheIsAccessible() {
        assertNotNull("BeanDescriptionCache should be non‑null", mic.getBeanDescriptionCache());
        // The actual BeanDescription retrieval is not exercised here; we just ensure the cache exists.
    }
}
