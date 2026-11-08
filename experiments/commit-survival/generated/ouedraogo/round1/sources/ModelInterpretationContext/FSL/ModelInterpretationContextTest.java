package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.spi.StatusManager;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 * <p>
 * The tests use a very small in‑package implementation of {@link Context}
 * that fulfills only the methods required by the class under test.
 */
public class ModelInterpretationContextTest {

    /** Minimal {@link Context} implementation for testing purposes. */
    private static class SimpleContext implements Context {
        private final Map<String, String> props = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return props.get(key); }
        @Override public void putProperty(String key, String value) { props.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(props); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // -----------------------------------------------------------------------
    // Constructor & basic fields
    // -----------------------------------------------------------------------
    @Test
    public void testCreateAppenderBags_initializesObjectMap() {
        // the constructor already called createAppenderBags via copy‑ctor,
        // but we explicitly call it to verify the behaviour.
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));
        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof HashMap);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof HashMap);
    }

    @Test
    public void testCopyConstructor_copiesMapsAndRegistry() {
        mic.addImport("List", "java.util.List");
        mic.addSubstitutionProperty("key", "value");
        mic.markStartOfNamedDependee("myDep");
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        assertEquals("java.util.List", copy.getImport("List"));
        assertEquals("value", copy.getProperty("key"));
        assertTrue(copy.isNamedDependeeStarted("myDep"));
        // ensure that the appender bags are present in the copy
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_BAG));
    }

    // -----------------------------------------------------------------------
    // Object stack tests
    // -----------------------------------------------------------------------
    @Test
    public void testObjectStack_pushPeekPop_isEmpty() {
        assertTrue(mic.isObjectStackEmpty());
        String obj = "testObj";
        mic.pushObject(obj);
        assertFalse(mic.isObjectStackEmpty());
        assertEquals(obj, mic.peekObject());
        assertEquals(obj, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test
    public void testObjectStack_getByIndex() {
        mic.pushObject("first");
        mic.pushObject("second");
        assertEquals("first", mic.getObject(0));
        assertEquals("second", mic.getObject(1));
    }

    // -----------------------------------------------------------------------
    // Model stack tests
    // -----------------------------------------------------------------------
    @Test
    public void testModelStack_pushPeekPop_isEmpty() {
        assertTrue(mic.isModelStackEmpty());
        Model m = new Model();
        mic.pushModel(m);
        assertFalse(mic.isModelStackEmpty());
        assertSame(m, mic.peekModel());
        assertSame(m, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    // -----------------------------------------------------------------------
    // Configurator hint
    // -----------------------------------------------------------------------
    @Test
    public void testConfiguratorHint_getSet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    // Property substitution helpers
    // -----------------------------------------------------------------------
    @Test
    public void testSubst_nullInputReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubst_withoutVariablesReturnsSameString() {
        String input = "plain text";
        assertEquals(input, mic.subst(input));
    }

    @Test
    public void testAddSubstitutionProperty_andRetrieval() {
        mic.addSubstitutionProperty("k1", "  value  ");
        // leading/trailing spaces must be trimmed
        assertEquals("value", mic.getProperty("k1"));
        // overwrite existing key
        mic.addSubstitutionProperty("k1", "new");
        assertEquals("new", mic.getProperty("k1"));
        // null key/value are ignored
        mic.addSubstitutionProperty(null, "ignore");
        mic.addSubstitutionProperty("ignore", null);
        assertNull(mic.getProperty(null));
    }

    @Test
    public void testAddSubstitutionProperties_bulk() {
        Properties props = new Properties();
        props.setProperty("a", "  A  ");
        props.setProperty("b", "B");
        mic.addSubstitutionProperties(props);
        assertEquals("A", mic.getProperty("a"));
        assertEquals("B", mic.getProperty("b"));
    }

    @Test
    public void testGetProperty_delegatesToContextWhenNotInMap() {
        context.putProperty("ctxKey", "ctxVal");
        assertEquals("ctxVal", mic.getProperty("ctxKey"));
    }

    @Test
    public void testGetCopyOfPropertyMap_isIndependentCopy() {
        mic.addSubstitutionProperty("c", "C");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("C", copy.get("c"));
        // modify copy – original must stay unchanged
        copy.put("c", "changed");
        assertEquals("C", mic.getProperty("c"));
    }

    // -----------------------------------------------------------------------
    // Import handling
    // -----------------------------------------------------------------------
    @Test
    public void testAddImport_andGetImport() {
        mic.addImport("Map", "java.util.Map");
        assertEquals("java.util.Map", mic.getImport("Map"));
        // unknown stem returns the stem itself
        assertEquals("Unknown", mic.getImport("Unknown"));
        // null stem returns null
        assertNull(mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopy_isIndependent() {
        mic.addImport("Set", "java.util.Set");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Set", copy.get("Set"));
        copy.put("Set", "changed");
        assertEquals("java.util.Set", mic.getImport("Set"));
    }

    // -----------------------------------------------------------------------
    // Dependency handling
    // -----------------------------------------------------------------------
    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDep");
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(1, defs.size());
        assertSame(dd, defs.get(0));
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model m1 = new Model();
        Model m2 = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m1, "A"));
        mic.addDependencyDefinition(new DependencyDefinition(m1, "B"));
        mic.addDependencyDefinition(new DependencyDefinition(m2, "C"));
        List<String> deps = mic.getDependeeNamesForModel(m1);
        assertEquals(2, deps.size());
        assertTrue(deps.containsAll(Arrays.asList("A", "B")));
    }

    @Test
    public void testHasDependers_trueAndFalse() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "X"));
        assertTrue(mic.hasDependers("X"));
        assertFalse(mic.hasDependers("Y"));
        // null or empty name – method creates an exception object but returns false
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
    }

    @Test
    public void testMarkAndCheckNamedDependeeStarted() {
        mic.markStartOfNamedDependee("first");
        assertTrue(mic.isNamedDependeeStarted("first"));
        assertFalse(mic.isNamedDependeeStarted("second"));
    }

    // -----------------------------------------------------------------------
    // BeanDescriptionCache accessor
    // -----------------------------------------------------------------------
    @Test
    public void testGetBeanDescriptionCache_isNotNull() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    // -----------------------------------------------------------------------
    // Top model getter / setter
    // -----------------------------------------------------------------------
    @Test
    public void testTopModelGetterSetter() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }
}
