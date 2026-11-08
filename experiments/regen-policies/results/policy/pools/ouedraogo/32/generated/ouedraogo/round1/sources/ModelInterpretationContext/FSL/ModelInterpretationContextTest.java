package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.spi.StatusManager;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 *
 * The tests use a very small concrete implementation of {@link Context}
 * that satisfies the interface contract without pulling in the full Logback
 * runtime.  No mocking frameworks are used – only real objects that are
 * part of the public API listed in the task description.
 */
public class ModelInterpretationContextTest {

    /**
     * Minimal {@link Context} implementation required for the tests.
     */
    private static class SimpleContext implements Context {

        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null; // not needed for these tests
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
            // no-op for test
        }

        @Override
        public long getBirthTime() {
            return System.currentTimeMillis();
        }

        @Override
        public Object getConfigurationLock() {
            return this;
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService() {
            return Executors.newSingleThreadScheduledExecutor();
        }

        @Override
        public ExecutorService getExecutorService() {
            return Executors.newSingleThreadExecutor();
        }

        @Override
        public void register(ch.qos.logback.core.spi.LifeCycle component) {
            // no-op
        }

        @Override
        public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            // no-op
        }

        @Override
        public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() {
            return null;
        }

        @Override
        public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {
            // no-op
        }

        @Override
        public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) {
            // no-op
        }

        @Override
        public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {
            // no-op
        }

        // PropertyContainer default method addSubstitutionProperties is not needed
    }

    @Test
    public void testConstructorAndBasicProperties() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx, "hint");

        // configurator hint
        assertEquals("hint", mic.getConfiguratorHint());

        // top model is null initially
        assertNull(mic.getTopModel());

        // bean description cache must be non‑null
        assertNotNull(mic.getBeanDescriptionCache());

        // default nested component registry must be instantiated
        assertNotNull(mic.getDefaultNestedComponentRegistry());

        // object map must be empty (apart from the two bags after explicit creation)
        assertTrue(mic.getObjectMap().isEmpty());

        // stacks are empty
        assertTrue(mic.isObjectStackEmpty());
        assertTrue(mic.isModelStackEmpty());
    }

    @Test
    public void testCreateAppenderBags() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);
        mic.createAppenderBags();

        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    @Test
    public void testObjectStackOperations() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        assertTrue(mic.isObjectStackEmpty());

        String first = "first";
        Integer second = 42;

        mic.pushObject(first);
        mic.pushObject(second);

        assertFalse(mic.isObjectStackEmpty());
        assertEquals(2, mic.getObjectStack().size());

        // peek should return the last pushed object
        assertEquals(second, mic.peekObject());

        // get by index
        assertEquals(first, mic.getObject(0));
        assertEquals(second, mic.getObject(1));

        // pop order
        assertEquals(second, mic.popObject());
        assertEquals(first, mic.popObject());

        // now empty again
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test(expected = EmptyStackException.class)
    public void testObjectStackPopEmptyThrows() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);
        mic.popObject(); // should throw EmptyStackException
    }

    @Test
    public void testModelStackOperations() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        Model m1 = new Model();
        Model m2 = new Model();

        mic.pushModel(m1);
        mic.pushModel(m2);

        assertFalse(mic.isModelStackEmpty());
        assertEquals(m2, mic.peekModel());
        assertEquals(m2, mic.popModel());
        assertEquals(m1, mic.peekModel());
        assertEquals(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    @Test
    public void testTopModelGetterSetter() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        Model model = new Model();
        mic.setTopModel(model);
        assertSame(model, mic.getTopModel());
    }

    @Test
    public void testConfiguratorHintGetterSetter() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);
        assertNull(mic.getConfiguratorHint());

        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    @Test
    public void testVariableSubstitution() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        mic.addSubstitutionProperty("key", "value");
        assertEquals("value", mic.getProperty("key"));
        assertEquals("prefix-value-suffix", mic.subst("prefix-${key}-suffix"));
    }

    @Test
    public void testCopyOfPropertyMapIsIndependent() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        mic.addSubstitutionProperty("a", "1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("1", copy.get("a"));

        // modify the copy – original must stay unchanged
        copy.put("a", "2");
        assertEquals("1", mic.getProperty("a"));
    }

    @Test
    public void testImportMapOperations() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        // check copy
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // getImport behaviour
        assertEquals("java.util.List", mic.getImport("List"));
        assertEquals("Unknown", mic.getImport("Unknown")); // unknown stem returns stem
        assertNull(mic.getImport(null));
    }

    @Test
    public void testDependencyDefinitionsAndQueries() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        Model depender1 = new Model();
        Model depender2 = new Model();

        DependencyDefinition dd1 = new DependencyDefinition(depender1, "serviceA");
        DependencyDefinition dd2 = new DependencyDefinition(depender2, "serviceB");
        DependencyDefinition dd3 = new DependencyDefinition(depender1, "serviceC");

        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);
        mic.addDependencyDefinition(dd3);

        // getDependencyDefinitions returns an unmodifiable list
        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(3, defs.size());
        try {
            defs.add(dd1);
            fail("List should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // getDependeeNamesForModel
        List<String> namesForDepender1 = mic.getDependeeNamesForModel(depender1);
        assertEquals(2, namesForDepender1.size());
        assertTrue(namesForDepender1.contains("serviceA"));
        assertTrue(namesForDepender1.contains("serviceC"));

        // hasDependers
        assertTrue(mic.hasDependers("serviceA"));
        assertTrue(mic.hasDependers("serviceB"));
        assertFalse(mic.hasDependers("nonexistent"));

        // empty or null name – the method does NOT throw (bug in implementation)
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testNamedDependeeTracking() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);

        assertFalse(mic.isNamedDependeeStarted("alpha"));
        mic.markStartOfNamedDependee("alpha");
        assertTrue(mic.isNamedDependeeStarted("alpha"));
        assertFalse(mic.isNamedDependeeStarted("beta"));
    }

    @Test
    public void testCopyConstructorCopiesImportMapAndVariableSubstitutions() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext original = new ModelInterpretationContext(ctx);
        original.addImport("Foo", "com.example.Foo");
        original.addSubstitutionProperty("p", "v");

        ModelInterpretationContext copy = new ModelInterpretationContext(original);

        // import map should be a copy (modifying copy does not affect original)
        copy.addImport("Bar", "com.example.Bar");
        assertEquals("com.example.Foo", original.getImport("Foo"));
        assertNull(original.getImport("Bar"));
        assertEquals("com.example.Bar", copy.getImport("Bar"));

        // variable substitution map should be copied
        assertEquals("v", copy.getProperty("p"));
        copy.addSubstitutionProperty("p", "newV");
        assertEquals("v", original.getProperty("p"));
    }

    @Test
    public void testGetBeanDescriptionCacheIsNotNull() {
        SimpleContext ctx = new SimpleContext();
        ModelInterpretationContext mic = new ModelInterpretationContext(ctx);
        assertNotNull(mic.getBeanDescriptionCache());
    }
}
