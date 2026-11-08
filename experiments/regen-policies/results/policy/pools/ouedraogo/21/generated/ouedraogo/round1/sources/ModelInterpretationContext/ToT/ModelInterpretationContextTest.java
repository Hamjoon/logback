package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Comprehensive JUnit‑4 test suite for {@link ModelInterpretationContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction and initial state</li>
 *   <li>object‑stack and model‑stack operations</li>
 *   <li>property handling and substitution</li>
 *   <li>import handling</li>
 *   <li>dependency definition handling</li>
 *   <li>named dependee tracking</li>
 *   <li>accessors for configurator hint, bean cache and nested component registry</li>
 * </ul>
 *
 * A minimal {@link Context} implementation is provided inside the test file
 * (named {@code SimpleContext}) because the library does not expose a concrete
 * implementation that can be instantiated directly. No mocking frameworks are
 * used – all objects are real instances created via their public constructors.
 */
public class ModelInterpretationContextTest {

    /**
     * Very small concrete {@link Context} implementation sufficient for the
     * tests. Only the methods used by {@link ModelInterpretationContext}
     * are fully implemented; the rest return dummy values.
     */
    private static class SimpleContext implements Context {

        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();
        private String name = "simple-context";

        @Override public StatusManager getStatusManager() { return null; }

        @Override public Object getObject(String key) { return objectMap.get(key); }

        @Override public void putObject(String key, Object value) { objectMap.put(key, value); }

        @Override public String getProperty(String key) { return propertyMap.get(key); }

        @Override public void putProperty(String key, String value) { propertyMap.put(key, value); }

        @Override public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(propertyMap);
        }

        @Override public String getName() { return name; }

        @Override public void setName(String name) { this.name = name; }

        @Override public long getBirthTime() { return System.currentTimeMillis(); }

        @Override public Object getConfigurationLock() { return new Object(); }

        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }

        @Override public ExecutorService getExecutorService() { return null; }

        @Override public ExecutorService getAlternateExecutorService() { return null; }

        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}

        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}

        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }

        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {}

        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) {}

        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {}
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    /* --------------------------------------------------------------------- */
    /*  Construction / basic fields                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorInitializesStacksAndMaps() {
        assertNotNull("objectStack must be created", mic.getObjectStack());
        assertNotNull("modelStack must be created", mic.modelStack);
        assertTrue("objectStack should start empty", mic.isObjectStackEmpty());
        assertTrue("modelStack should start empty", mic.isModelStackEmpty());

        // objectMap is empty until createAppenderBags is called
        assertTrue("objectMap should be empty initially", mic.getObjectMap().isEmpty());
    }

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue("APPENDER_BAG key must be present", map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue("APPENDER_REF_BAG key must be present", map.containsKey(JoranConstants.APPENDER_REF_BAG));

        // The values must be maps
        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("setTopModel should store the model", model, mic.getTopModel());
    }

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("configurator hint should be returned unchanged", hint, mic.getConfiguratorHint());
    }

    @Test
    public void testGetBeanDescriptionCacheIsNotNull() {
        assertNotNull("beanDescriptionCache must be instantiated", mic.getBeanDescriptionCache());
    }

    @Test
    public void testGetDefaultNestedComponentRegistryIsSingleton() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull(registry);
        // calling again returns the same instance
        assertSame(registry, mic.getDefaultNestedComponentRegistry());
    }

    /* --------------------------------------------------------------------- */
    /*  Model stack operations                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testModelStackPushPeekPop() {
        Model m1 = new Model();
        Model m2 = new Model();

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

    /* --------------------------------------------------------------------- */
    /*  Object stack operations                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testObjectStackPushPeekPopAndIndexedAccess() {
        String obj1 = "first";
        Integer obj2 = 42;

        mic.pushObject(obj1);
        mic.pushObject(obj2);

        assertFalse(mic.isObjectStackEmpty());
        assertSame(obj2, mic.peekObject());

        assertSame(obj2, mic.popObject());
        assertSame(obj1, mic.peekObject());

        assertSame(obj1, mic.getObject(0));
        assertEquals("objectStack size must be 1 after one pop", 1, mic.getObjectStack().size());
    }

    /* --------------------------------------------------------------------- */
    /*  Property handling (substitution)                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddSubstitutionPropertyAndGetProperty() {
        mic.addSubstitutionProperty("key1", "  value1  "); // leading/trailing spaces trimmed
        assertEquals("value1", mic.getProperty("key1"));

        // property not in mic -> delegated to context
        context.putProperty("ctxKey", "ctxValue");
        assertEquals("ctxValue", mic.getProperty("ctxKey"));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNulls() {
        mic.addSubstitutionProperty(null, "val");
        mic.addSubstitutionProperty("k", null);
        assertNull("null key must not be added", mic.getProperty(null));
        assertNull("null value must not be added", mic.getProperty("k"));
    }

    @Test
    public void testAddSubstitutionPropertiesFromJavaUtilProperties() {
        Properties props = new Properties();
        props.setProperty("p1", "v1");
        props.setProperty("p2", "  v2  "); // should be trimmed
        mic.addSubstitutionProperties(props);

        assertEquals("v1", mic.getProperty("p1"));
        assertEquals("v2", mic.getProperty("p2"));
    }

    @Test
    public void testAddSubstitutionPropertiesIgnoresNullArgument() {
        mic.addSubstitutionProperties(null); // should be a no‑op, not NPE
        // verify map still empty
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    @Test
    public void testSubstVariableResolutionAndErrorHandling() {
        // set a property in the context and in mic's own map
        context.putProperty("ctxProp", "ctxValue");
        mic.addSubstitutionProperty("micProp", "micValue");

        // variable from mic's map
        assertEquals("micValue", mic.subst("${micProp}"));
        // variable from context
        assertEquals("ctxValue", mic.subst("${ctxProp}"));
        // undefined variable – OptionHelper returns the original string (no exception)
        assertEquals("${unknown}", mic.subst("${unknown}"));
        // null input returns null
        assertNull(mic.subst(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Dependency handling                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(1, defs.size());
        assertSame(dd, defs.get(0));

        // verify returned list is unmodifiable
        try {
            defs.add(dd);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) { }

        // getDependeeNamesForModel should return the dependee name
        List<String> depNames = mic.getDependeeNamesForModel(depender);
        assertEquals(Collections.singletonList("myDependee"), depNames);
    }

    @Test
    public void testHasDependersBehaviour() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition(m, "dep2"));

        assertTrue(mic.hasDependers("dep1"));
        assertTrue(mic.hasDependers("dep2"));
        assertFalse(mic.hasDependers("nonExisting"));

        // null or empty name should *not* throw but simply return false
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testNamedDependeeTracking() {
        String name = "someDependee";
        assertFalse(mic.isNamedDependeeStarted(name));
        mic.markStartOfNamedDependee(name);
        assertTrue(mic.isNamedDependeeStarted(name));
    }

    /* --------------------------------------------------------------------- */
    /*  Import handling                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddImportAndLookup() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        assertEquals("java.util.List", mic.getImport("List"));
        assertEquals("java.util.Map", mic.getImport("Map"));
        // unknown stem returns the stem itself
        assertEquals("String", mic.getImport("String"));
        // null stem returns null
        assertNull(mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Foo", "com.example.Foo");
        Map<String, String> copy = mic.getImportMapCopy();

        assertEquals("com.example.Foo", copy.get("Foo"));
        // modify the copy – original must stay unchanged
        copy.put("Bar", "com.example.Bar");
        assertNull(mic.getImport("Bar"));
    }

    /* --------------------------------------------------------------------- */
    /*  PropertyContainer contract                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCopyOfPropertyMapReturnsIndependentMap() {
        mic.addSubstitutionProperty("k1", "v1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v1", copy.get("k1"));

        // mutating copy must not affect the original
        copy.put("k2", "v2");
        assertNull(mic.getProperty("k2"));
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetObjectMapIsMutable() {
        Map<String, Object> map = mic.getObjectMap();
        map.put("testKey", "testValue");
        assertEquals("testValue", mic.getObjectMap().get("testKey"));
    }
}
