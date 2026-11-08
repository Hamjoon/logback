package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DependencyDefinition;

/**
 * Unit tests for {@link ModelInterpretationContext}. All public behaviours are
 * exercised without using any external mocking framework – only concrete
 * implementations that are part of the public API are instantiated.
 */
public class ModelInterpretationContextTest {

    /** Simple {@link Context} implementation used for the tests. */
    private static class SimpleContext implements Context {
        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null; // not needed for the tests
        }

        @Override
        public Object getObject(String key) {
            return objectMap.get(key);
        }

        @Override
        public void putObject(String key, Object value) {
            objectMap.put(key, value);
        }

        @Override
        public String getProperty(String key) {
            return propertyMap.get(key);
        }

        @Override
        public void putProperty(String key, String value) {
            propertyMap.put(key, value);
        }

        @Override
        public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(propertyMap);
        }

        @Override
        public String getName() {
            return "SimpleContext";
        }

        @Override
        public void setName(String name) {
            // no‑op
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
        public ScheduledExecutorService getScheduledExecutorService() {
            return null;
        }

        @Override
        public ExecutorService getExecutorService() {
            return null;
        }

        @Override
        public void register(ch.qos.logback.core.spi.LifeCycle component) {
            // no‑op
        }

        @Override
        public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            // no‑op
        }

        @Override
        public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() {
            return null;
        }

        @Override
        public void setSequenceNumberGenerator(
                ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {
            // no‑op
        }

        @Override
        public void addConfigurationEventListener(
                ch.qos.logback.core.spi.ConfigurationEventListener listener) {
            // no‑op
        }

        @Override
        public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {
            // no‑op
        }

        @Override
        public void addSubstitutionProperties(Properties props) {
            for (String name : props.stringPropertyNames()) {
                addSubstitutionProperty(name, props.getProperty(name));
            }
        }

        // PropertyContainer methods (delegated to the same map)
        @Override
        public void addSubstitutionProperty(String key, String value) {
            propertyMap.put(key, value);
        }

        @Override
        public String getProperty(String key) {
            return propertyMap.get(key);
        }

        @Override
        public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(propertyMap);
        }
    }

    /** Minimal concrete {@link GenericXMLConfigurator} for supplier tests. */
    private static class DummyConfigurator extends GenericXMLConfigurator {
        @Override
        protected void addElementSelectorAndActionAssociations(
                ch.qos.logback.core.joran.spi.RuleStore rs) {
            // no‑op
        }

        @Override
        protected void setImplicitRuleSupplier(
                ch.qos.logback.core.joran.spi.SaxEventInterpreter interpreter) {
            // no‑op
        }
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // ------------------------------------------------------------------------
    // Basic construction and appender‑bag handling
    // ------------------------------------------------------------------------
    @Test
    public void testCreateAppenderBags_initializesMaps() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG));

        assertTrue(map.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    // ------------------------------------------------------------------------
    // Object stack handling
    // ------------------------------------------------------------------------
    @Test
    public void testObjectStack_operations() {
        assertTrue(mic.isObjectStackEmpty());

        String first = "first";
        Integer second = 2;

        mic.pushObject(first);
        mic.pushObject(second);

        assertFalse(mic.isObjectStackEmpty());
        assertEquals(second, mic.peekObject());
        assertEquals(first, mic.getObject(0));
        assertEquals(second, mic.popObject());
        assertEquals(first, mic.peekObject());
        assertEquals(first, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    // ------------------------------------------------------------------------
    // Model stack handling
    // ------------------------------------------------------------------------
    @Test
    public void testModelStack_operations() {
        assertTrue(mic.isModelStackEmpty());

        Model m1 = new Model();
        Model m2 = new Model();

        mic.pushModel(m1);
        mic.pushModel(m2);

        assertFalse(mic.isModelStackEmpty());
        assertSame(m2, mic.peekModel());
        assertSame(m2, mic.popModel());
        assertSame(m1, mic.peekModel());
        assertSame(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    // ------------------------------------------------------------------------
    // Configurator hint
    // ------------------------------------------------------------------------
    @Test
    public void testConfiguratorHint_setAndGet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());

        mic.setConfiguratorHint(null);
        assertNull(mic.getConfiguratorHint());
    }

    // ------------------------------------------------------------------------
    // Variable substitution handling (delegated to VariableSubstitutionsHelper)
    // ------------------------------------------------------------------------
    @Test
    public void testVariableSubstitution_addGetAndSubst() {
        mic.addSubstitutionProperty("key1", "value1");
        assertEquals("value1", mic.getProperty("key1"));
        assertEquals("value1", mic.subst("${key1}"));
    }

    // ------------------------------------------------------------------------
    // Dependency handling
    // ------------------------------------------------------------------------
    @Test
    public void testDependencyDefinitions_addAndQuery() {
        Model depender = new Model();
        DependencyDefinition dd1 = new DependencyDefinition(depender, "dependeeA");
        DependencyDefinition dd2 = new DependencyDefinition(depender, "dependeeB");

        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);

        // getDependencyDefinitions returns an unmodifiable view
        assertEquals(2, mic.getDependencyDefinitions().size());
        assertTrue(mic.getDependencyDefinitions().contains(dd1));
        assertTrue(mic.getDependencyDefinitions().contains(dd2));
        try {
            mic.getDependencyDefinitions().add(dd1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // getDependeeNamesForModel
        assertEquals(
                // order is preserved
                java.util.Arrays.asList("dependeeA", "dependeeB"),
                mic.getDependeeNamesForModel(depender));

        // hasDependers
        assertTrue(mic.hasDependers("dependeeA"));
        assertTrue(mic.hasDependers("dependeeB"));
        assertFalse(mic.hasDependers("unknown"));

        // empty or null name should simply return false (no exception is thrown)
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    // ------------------------------------------------------------------------
    // Named dependee start tracking
    // ------------------------------------------------------------------------
    @Test
    public void testNamedDependeeTracking() {
        assertFalse(mic.isNamedDependeeStarted("myDep"));
        mic.markStartOfNamedDependee("myDep");
        assertTrue(mic.isNamedDependeeStarted("myDep"));
        assertFalse(mic.isNamedDependeeStarted("other"));
    }

    // ------------------------------------------------------------------------
    // Import map handling
    // ------------------------------------------------------------------------
    @Test
    public void testImportMap_addGetCopy() {
        // initially empty copy
        Map<String, String> emptyCopy = mic.getImportMapCopy();
        assertTrue(emptyCopy.isEmpty());

        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // modifications to the returned copy must not affect the internal map
        copy.put("Set", "java.util.Set");
        assertNull(mic.getImport("Set"));
        assertEquals("java.util.List", mic.getImport("List"));
        assertEquals("java.util.Map", mic.getImport("Map"));

        // getImport behaviour
        assertNull(mic.getImport(null));
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
        assertEquals("java.util.List", mic.getImport("List"));
    }

    // ------------------------------------------------------------------------
    // Top model getter / setter
    // ------------------------------------------------------------------------
    @Test
    public void testTopModel_setAndGet() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }

    // ------------------------------------------------------------------------
    // Configurator supplier handling
    // ------------------------------------------------------------------------
    @Test
    public void testConfiguratorSupplier_setAndGet() {
        Supplier<? extends GenericXMLConfigurator> supplier = DummyConfigurator::new;
        mic.setConfiguratorSupplier(supplier);
        assertSame(supplier, mic.getConfiguratorSupplier());

        GenericXMLConfigurator cfg = mic.getConfiguratorSupplier().get();
        assertNotNull(cfg);
        assertTrue(cfg instanceof DummyConfigurator);
    }

    // ------------------------------------------------------------------------
    // Copy constructor – deep copy of mutable fields
    // ------------------------------------------------------------------------
    @Test
    public void testCopyConstructor_copiesStateButKeepsIndependence() {
        // prepare original MIC with some state
        mic.addImport("Thread", "java.lang.Thread");
        mic.addSubstitutionProperty("p", "v");
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "dep"));
        mic.pushObject("object");
        mic.createAppenderBags();

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);

        // import map is copied (equal content but different instance)
        assertEquals(mic.getImportMapCopy(), copy.getImportMapCopy());
        assertNotSame(mic.getImportMapCopy(), copy.getImportMapCopy());

        // variable substitution map is copied
        assertEquals(mic.getCopyOfPropertyMap(), copy.getCopyOfPropertyMap());

        // object map contains its own appender bags (the copy constructor calls createAppenderBags)
        assertTrue(copy.getObjectMap().containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG));
        assertTrue(copy.getObjectMap().containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG));

        // dependency list is NOT shared – the copy constructor does not copy it
        assertTrue(copy.getDependencyDefinitions().isEmpty());
        assertEquals(1, mic.getDependencyDefinitions().size());

        // object stack is independent
        assertEquals(1, mic.getObjectStack().size());
        assertTrue(copy.getObjectStack().isEmpty());

        // configurator hint is copied (same reference)
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        ModelInterpretationContext copy2 = new ModelInterpretationContext(mic);
        assertSame(hint, copy2.getConfiguratorHint());
    }

    // ------------------------------------------------------------------------
    // BeanDescriptionCache existence (simple sanity check)
    // ------------------------------------------------------------------------
    @Test
    public void testBeanDescriptionCache_isCreated() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    // ------------------------------------------------------------------------
    // DefaultNestedComponentRegistry existence
    // ------------------------------------------------------------------------
    @Test
    public void testDefaultNestedComponentRegistry_isAvailable() {
        assertNotNull(mic.getDefaultNestedComponentRegistry());
    }
}
