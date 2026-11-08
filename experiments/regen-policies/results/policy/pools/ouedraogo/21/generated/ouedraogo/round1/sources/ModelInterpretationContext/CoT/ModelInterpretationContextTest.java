package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 *
 * The tests use a minimal {@link Context} implementation that lives only inside
 * this test class – no external mocking frameworks are used.
 */
public class ModelInterpretationContextTest {

    /** Simple {@link Context} implementation used for the tests. */
    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null; // not needed for these tests
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
        public void register(ch.qos.logback.core.spi.LifeCycle component) {
            // no‑op
        }

        @Override
        public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {
            // no‑op
        }

        @Override
        public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() {
            return null;
        }

        @Override
        public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {
            // no‑op
        }

        @Override
        public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) {
            // no‑op
        }

        @Override
        public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {
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

    /* ----------------------------------------------------------------------
     *  Public method signatures (extracted for reference)
     * ----------------------------------------------------------------------
     *
     * ModelInterpretationContext(Context context)
     * ModelInterpretationContext(Context context, Object configuratorHint)
     * ModelInterpretationContext(ModelInterpretationContext otherMic)
     *
     * Map<String,Object> getObjectMap()
     * void createAppenderBags()
     * Model getTopModel()
     * void setTopModel(Model topModel)
     *
     * void pushModel(Model m)
     * Model peekModel()
     * boolean isModelStackEmpty()
     * Model popModel()
     *
     * Stack<Object> getObjectStack()
     * boolean isObjectStackEmpty()
     * Object peekObject()
     * void pushObject(Object o)
     * Object popObject()
     * Object getObject(int i)
     *
     * Object getConfiguratorHint()
     * void setConfiguratorHint(Object configuratorHint)
     *
     * BeanDescriptionCache getBeanDescriptionCache()
     *
     * String subst(String ref)
     *
     * void addSubstitutionProperty(String key, String value)
     * void addSubstitutionProperties(Properties props)
     *
     * DefaultNestedComponentRegistry getDefaultNestedComponentRegistry()
     *
     * void addDependencyDefinition(DependencyDefinition dd)
     * List<DependencyDefinition> getDependencyDefinitions()
     * List<String> getDependeeNamesForModel(Model model)
     * boolean hasDependers(String dependeeName)
     *
     * void markStartOfNamedDependee(String name)
     * boolean isNamedDependeeStarted(String name)
     *
     * String getProperty(String key)
     * Map<String,String> getCopyOfPropertyMap()
     *
     * void addImport(String stem, String fqcn)
     * Map<String,String> getImportMapCopy()
     * String getImport(String stem)
     */

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorAndContextAssignment() {
        assertSame(context, mic.getContext());
    }

    @Test
    public void testCreateAppenderBags() {
        mic.createAppenderBags();
        Map<String, Object> om = mic.getObjectMap();
        assertTrue(om.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(om.containsKey(JoranConstants.APPENDER_REF_BAG));
        assertTrue(om.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(om.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    @Test
    public void testTopModelGetterSetter() {
        Model m = new Model();
        mic.setTopModel(m);
        assertSame(m, mic.getTopModel());
    }

    @Test
    public void testModelStackOperations() {
        Model m1 = new Model();
        Model m2 = new Model();

        assertTrue(mic.isModelStackEmpty());

        mic.pushModel(m1);
        assertFalse(mic.isModelStackEmpty());
        assertSame(m1, mic.peekModel());

        mic.pushModel(m2);
        assertSame(m2, mic.peekModel());

        assertSame(m2, mic.popModel());
        assertSame(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testModelStackPeekWhenEmptyThrows() {
        mic.peekModel(); // should throw EmptyStackException
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testModelStackPopWhenEmptyThrows() {
        mic.popModel(); // should throw EmptyStackException
    }

    @Test
    public void testObjectStackOperations() {
        String obj1 = "first";
        Integer obj2 = 42;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(obj1);
        assertFalse(mic.isObjectStackEmpty());
        assertSame(obj1, mic.peekObject());

        mic.pushObject(obj2);
        assertSame(obj2, mic.peekObject());

        assertSame(obj2, mic.popObject());
        assertSame(obj1, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testObjectStackPeekWhenEmptyThrows() {
        mic.peekObject();
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testObjectStackPopWhenEmptyThrows() {
        mic.popObject();
    }

    @Test
    public void testObjectStackGetByIndex() {
        String a = "a";
        String b = "b";
        mic.pushObject(a);
        mic.pushObject(b);
        assertSame(a, mic.getObject(0));
        assertSame(b, mic.getObject(1));
    }

    @Test
    public void testConfiguratorHint() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    @Test
    public void testBeanDescriptionCacheIsCreated() {
        BeanDescriptionCache cache = mic.getBeanDescriptionCache();
        assertNotNull(cache);
        // BeanDescriptionCache needs a Context – verify it is the same one we passed.
        assertSame(context, cache.getContext());
    }

    @Test
    public void testSubstSimpleVariable() {
        context.putProperty("myVar", "value");
        String result = mic.subst("${myVar}");
        assertEquals("value", result);
    }

    @Test
    public void testSubstNullReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubstWithInvalidExpressionReturnsOriginal() {
        // An unclosed variable expression triggers ScanException inside OptionHelper
        String bad = "${unclosed";
        String result = mic.subst(bad);
        // The implementation catches the exception and returns the original string
        assertEquals(bad, result);
    }

    @Test
    public void testAddSubstitutionPropertyAndTrim() {
        mic.addSubstitutionProperty(" key ", "  value  ");
        assertEquals("value", mic.getProperty(" key "));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNulls() {
        mic.addSubstitutionProperty(null, "value");
        mic.addSubstitutionProperty("key", null);
        // neither entry should be present
        assertNull(mic.getProperty(null));
        assertNull(mic.getProperty("key"));
    }

    @Test
    public void testAddSubstitutionProperties() {
        Properties props = new Properties();
        props.setProperty("p1", "v1");
        props.setProperty("p2", " v2 "); // with spaces to test trimming
        mic.addSubstitutionProperties(props);
        assertEquals("v1", mic.getProperty("p1"));
        assertEquals("v2", mic.getProperty("p2"));
    }

    @Test
    public void testAddSubstitutionPropertiesWithNullDoesNothing() {
        mic.addSubstitutionProperties(null); // should not throw
        // internal map should stay empty
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    @Test
    public void testDefaultNestedComponentRegistryAccess() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull(registry);
    }

    @Test
    public void testDependencyDefinitionHandling() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));

        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals(1, dependees.size());
        assertEquals("myDependee", dependees.get(0));
    }

    @Test
    public void testHasDependersTrueAndFalse() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "A"));
        assertTrue(mic.hasDependers("A"));
        assertFalse(mic.hasDependers("B"));
    }

    @Test
    public void testHasDependersWithNullOrEmpty() {
        // According to implementation, it creates an IllegalArgumentException but does not throw it.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testNamedDependeeTracking() {
        mic.markStartOfNamedDependee("first");
        assertTrue(mic.isNamedDependeeStarted("first"));
        assertFalse(mic.isNamedDependeeStarted("second"));
    }

    @Test
    public void testGetPropertyPrefersLocalMapOverContext() {
        // put a value in the context
        context.putProperty("shared", "ctxValue");
        // put a different value in the mic's own map
        mic.addSubstitutionProperty("shared", "micValue");
        assertEquals("micValue", mic.getProperty("shared"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("k", "v");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v", copy.get("k"));
        // modify copy should not affect original
        copy.put("k", "changed");
        assertEquals("v", mic.getProperty("k"));
    }

    @Test
    public void testImportHandling() {
        mic.addImport("List", "java.util.List");
        assertEquals("java.util.List", mic.getImport("List"));
        // unknown stem returns the stem itself
        assertEquals("Unknown", mic.getImport("Unknown"));
        // null returns null
        assertNull(mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Map", "java.util.Map");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Map", copy.get("Map"));
        copy.put("Map", "changed");
        assertEquals("java.util.Map", mic.getImport("Map"));
    }

    /* ----------------------------------------------------------------------
     *  Edge case & exception handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testSubstWithIllegalArgumentExceptionFromOptionHelper() {
        // OptionHelper.substVars throws IllegalArgumentException when the property
        // name contains a colon (":") – this behaviour is part of Logback.
        // We use a string that triggers this condition.
        String bad = "${bad:property}";
        String result = mic.subst(bad);
        // The method should catch the exception and return the original string.
        assertEquals(bad, result);
    }

    @Test
    public void testPushNullObjectOntoObjectStack() {
        mic.pushObject(null);
        assertTrue(mic.isObjectStackEmpty() == false);
        assertNull(mic.peekObject());
        assertNull(mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test
    public void testPushNullModelOntoModelStack() {
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty());
        assertNull(mic.peekModel());
        assertNull(mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    @Test
    public void testConstructorCopyingOtherMic() {
        // set up an original MIC with some state
        Model top = new Model();
        mic.setTopModel(top);
        mic.addSubstitutionProperty("a", "b");
        mic.addImport("S", "FQCN");
        mic.markStartOfNamedDependee("start");

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // verify that mutable maps are copied, not shared
        assertEquals("b", copy.getProperty("a"));
        assertEquals("FQCN", copy.getImport("S"));
        assertTrue(copy.isNamedDependeeStarted("start"));
        // top model reference should be the same (shallow copy)
        assertSame(top, copy.getTopModel());
        // ensure that changing the copy does not affect the original
        copy.addSubstitutionProperty("a", "c");
        assertEquals("b", mic.getProperty("a"));
    }

    @Test
    public void testCreateAppenderBagsIdempotent() {
        mic.createAppenderBags(); // first call
        Map<String, Object> first = mic.getObjectMap();

        mic.createAppenderBags(); // second call – should replace the same keys
        Map<String, Object> second = mic.getObjectMap();

        assertSame(first.get(JoranConstants.APPENDER_BAG), second.get(JoranConstants.APPENDER_BAG));
        assertSame(first.get(JoranConstants.APPENDER_REF_BAG), second.get(JoranConstants.APPENDER_REF_BAG));
    }
}
