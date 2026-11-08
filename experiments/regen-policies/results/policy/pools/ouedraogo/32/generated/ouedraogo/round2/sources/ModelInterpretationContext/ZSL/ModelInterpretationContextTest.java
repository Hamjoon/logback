package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.spi.FilterAttachable;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.joran.JoranConstants;

/**
 * Comprehensive JUnit 4 test suite for {@link ModelInterpretationContext}.
 */
public class ModelInterpretationContextTest {

    /** Simple in‑memory {@link Context} implementation used for the tests. */
    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();
        private String name = "simpleContext";

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }
        @Override public String getName() { return name; }
        @Override public void setName(String name) { this.name = name; }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public void register(LifeCycle component) {}
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {}
        // ContextAware methods are inherited from ContextAwareBase via default implementations,
        // but they are not needed for this test suite.
    }

    private Context simpleContext;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    // -------------------------------------------------------------------------
    // Constructor and basic fields
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorInitialisesStacksAndMaps() {
        assertNotNull(mic.getObjectStack());
        assertTrue(mic.isObjectStackEmpty());

        assertNotNull(mic.peekModel()); // peekModel will throw if stack empty – we test emptiness instead
        assertTrue(mic.isModelStackEmpty());

        assertNotNull(mic.getObjectMap());
        assertTrue(mic.getObjectMap().isEmpty());

        assertNotNull(mic.getImportMapCopy());
        assertTrue(mic.getImportMapCopy().isEmpty());

        assertNotNull(mic.getBeanDescriptionCache());
        assertNotNull(mic.getDefaultNestedComponentRegistry());
    }

    // -------------------------------------------------------------------------
    // Appender bags creation
    // -------------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();

        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    // -------------------------------------------------------------------------
    // Model stack operations
    // -------------------------------------------------------------------------

    @Test
    public void testModelStackPushPeekPop() {
        Model m1 = new Model();
        Model m2 = new Model();

        assertTrue(mic.isModelStackEmpty());

        mic.pushModel(m1);
        assertFalse(mic.isModelStackEmpty());
        assertSame(m1, mic.peekModel());

        mic.pushModel(m2);
        assertSame(m2, mic.peekModel());

        assertSame(m2, mic.popModel());
        assertSame(m1, mic.peekModel());

        assertSame(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    // -------------------------------------------------------------------------
    // Object stack operations
    // -------------------------------------------------------------------------

    @Test
    public void testObjectStackPushPeekPopAndGet() {
        String o1 = "first";
        Integer o2 = 42;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(o1);
        mic.pushObject(o2);

        assertFalse(mic.isObjectStackEmpty());

        assertSame(o2, mic.peekObject());
        assertSame(o2, mic.popObject());
        assertSame(o1, mic.peekObject());

        assertSame(o1, mic.getObject(0));
    }

    // -------------------------------------------------------------------------
    // Configurator hint handling
    // -------------------------------------------------------------------------

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    // -------------------------------------------------------------------------
    // Variable substitution delegation
    // -------------------------------------------------------------------------

    @Test
    public void testVariableSubstitutionDelegatesToHelper() {
        mic.addSubstitutionProperty("key", "value");
        assertEquals("value", mic.getProperty("key"));
        assertEquals("value", mic.subst("${key}"));
    }

    // -------------------------------------------------------------------------
    // Import handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveImport() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // Known stem
        assertEquals("java.util.List", mic.getImport("List"));
        // Unknown stem – should return the stem itself
        assertEquals("Unknown", mic.getImport("Unknown"));
        // Null stem – should return null
        assertNull(mic.getImport(null));
    }

    // -------------------------------------------------------------------------
    // Dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndQueryDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd1 = new DependencyDefinition(depender, "dependeeA");
        DependencyDefinition dd2 = new DependencyDefinition(depender, "dependeeB");

        mic.addDependencyDefinition(dd1);
        mic.addDependencyDefinition(dd2);

        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(2, defs.size());
        assertTrue(defs.contains(dd1));
        assertTrue(defs.contains(dd2));

        // Ensure the returned list is unmodifiable
        try {
            defs.add(new DependencyDefinition(new Model(), "another"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // Get dependee names for a specific model
        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals(2, dependees.size());
        assertTrue(dependees.contains("dependeeA"));
        assertTrue(dependees.contains("dependeeB"));

        // hasDependers
        assertTrue(mic.hasDependers("dependeeA"));
        assertFalse(mic.hasDependers("nonexistent"));
    }

    @Test
    public void testHasDependersWithInvalidInputDoesNotThrowButDoesNothing() {
        // According to the implementation, an IllegalArgumentException is created but not thrown.
        // We verify that the method simply returns false for null/empty input.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testMarkAndCheckNamedDependeeStarted() {
        String name = "myDependee";
        assertFalse(mic.isNamedDependeeStarted(name));

        mic.markStartOfNamedDependee(name);
        assertTrue(mic.isNamedDependeeStarted(name));
    }

    // -------------------------------------------------------------------------
    // Bean description cache accessor
    // -------------------------------------------------------------------------

    @Test
    public void testGetBeanDescriptionCacheReturnsNonNull() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    // -------------------------------------------------------------------------
    // Default nested component registry accessor
    // -------------------------------------------------------------------------

    @Test
    public void testGetDefaultNestedComponentRegistryReturnsSameInstance() {
        DefaultNestedComponentRegistry registry1 = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry registry2 = mic.getDefaultNestedComponentRegistry();
        assertSame(registry1, registry2);
    }

    // -------------------------------------------------------------------------
    // Top model getter / setter
    // -------------------------------------------------------------------------

    @Test
    public void testTopModelGetterSetter() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }
}
