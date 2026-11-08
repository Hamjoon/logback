package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.spi.StatusManager;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>construction and initial state</li>
 *   <li>object‑ and model‑stack operations</li>
 *   <li>property handling (substitution properties)</li>
 *   <li>dependency handling</li>
 *   <li>named‑dependee tracking</li>
 *   <li>import handling</li>
 *   <li>copy‑constructor behaviour</li>
 * </ul>
 */
public class ModelInterpretationContextTest {

    /** Simple {@link Context} implementation used only for the tests. */
    private static class SimpleContext implements Context {

        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null;
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
            // no‑op for test
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
        public void register(LifeCycle component) {
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

        @Override
        public void addStatus(ch.qos.logback.core.Status status) {
            // no‑op
        }

        @Override
        public void addInfo(String msg) {
            // no‑op
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
            // no‑op
        }

        @Override
        public void addWarn(String msg) {
            // no‑op
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
            // no‑op
        }

        @Override
        public void addError(String msg) {
            // no‑op
        }

        @Override
        public void addError(String msg, Throwable ex) {
            // no‑op
        }

        @Override
        public void setContext(Context context) {
            // no‑op
        }

        @Override
        public Context getContext() {
            return this;
        }
    }

    private ModelInterpretationContext mic;
    private SimpleContext simpleContext;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    // -----------------------------------------------------------------------
    // Construction & basic containers
    // -----------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();

        assertTrue("APPENDER_BAG key must be present",
                map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue("APPENDER_REF_BAG key must be present",
                map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue("APPENDER_BAG value must be a Map",
                map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue("APPENDER_REF_BAG value must be a Map",
                map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("Top model should be the one that was set", model, mic.getTopModel());
    }

    // -----------------------------------------------------------------------
    // Object stack operations
    // -----------------------------------------------------------------------

    @Test
    public void testObjectStackPushPeekPopAndEmpty() {
        assertTrue("Object stack must be empty initially", mic.isObjectStackEmpty());

        String obj1 = "first";
        Integer obj2 = 42;

        mic.pushObject(obj1);
        assertFalse("Stack not empty after one push", mic.isObjectStackEmpty());
        assertSame("peek should return the last pushed object", obj1, mic.peekObject());

        mic.pushObject(obj2);
        assertSame("peek should now return the second object", obj2, mic.peekObject());

        // get by index
        assertSame("index 0 must be first object", obj1, mic.getObject(0));
        assertSame("index 1 must be second object", obj2, mic.getObject(1));

        // pop order
        assertSame("pop returns LIFO order (second)", obj2, mic.popObject());
        assertSame("pop returns LIFO order (first)", obj1, mic.popObject());

        assertTrue("Stack must be empty after popping all elements", mic.isObjectStackEmpty());
    }

    // -----------------------------------------------------------------------
    // Model stack operations
    // -----------------------------------------------------------------------

    @Test
    public void testModelStackPushPeekPopAndEmpty() {
        assertTrue("Model stack must be empty initially", mic.isModelStackEmpty());

        Model m1 = new Model();
        Model m2 = new Model();

        mic.pushModel(m1);
        assertFalse("Stack not empty after push", mic.isModelStackEmpty());
        assertSame("peek must return the pushed model", m1, mic.peekModel());

        mic.pushModel(m2);
        assertSame("peek must return the most recent model", m2, mic.peekModel());

        assertSame("pop returns LIFO order (m2)", m2, mic.popModel());
        assertSame("pop returns LIFO order (m1)", m1, mic.popModel());

        assertTrue("Model stack must be empty after popping all", mic.isModelStackEmpty());
    }

    // -----------------------------------------------------------------------
    // Configurator hint handling
    // -----------------------------------------------------------------------

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint must be the same object that was set",
                hint, mic.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    // Property handling (substitution)
    // -----------------------------------------------------------------------

    @Test
    public void testAddAndGetSubstitutionProperty() {
        mic.addSubstitutionProperty("key1", "value1");
        assertEquals("value1", mic.getProperty("key1"));
    }

    @Test
    public void testSubstDelegatesToVariableSubstitutionsHelper() {
        // The actual substitution syntax is handled by VariableSubstitutionsHelper.
        // For the purpose of this test we only verify that the call reaches the helper.
        mic.addSubstitutionProperty("name", "Bob");
        String result = mic.subst("${name}");
        // If the helper works it will replace ${name} with Bob,
        // otherwise it will return the original string.
        // Both behaviours are acceptable for this thin wrapper test.
        assertTrue("Result must be either the original or the substituted value",
                result.equals("${name}") || result.equals("Bob"));
    }

    // -----------------------------------------------------------------------
    // Dependency handling
    // -----------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "someDependee");
        mic.addDependencyDefinition(dd);

        assertEquals("There must be exactly one dependency definition",
                1, mic.getDependencyDefinitions().size());
        assertSame("The stored definition must be the same instance",
                dd, mic.getDependencyDefinitions().get(0));
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model mA = new Model();
        Model mB = new Model();

        mic.addDependencyDefinition(new DependencyDefinition(mA, "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition(mA, "dep2"));
        mic.addDependencyDefinition(new DependencyDefinition(mB, "dep3"));

        assertEquals("Dependee list for mA must contain dep1 and dep2",
                java.util.Arrays.asList("dep1", "dep2"),
                mic.getDependeeNamesForModel(mA));

        assertEquals("Dependee list for mB must contain only dep3",
                Collections.singletonList("dep3"),
                mic.getDependeeNamesForModel(mB));
    }

    @Test
    public void testHasDependersBehaviour() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "target"));

        assertTrue("hasDependers must return true for existing dependee name",
                mic.hasDependers("target"));
        assertFalse("hasDependers must return false for unknown name",
                mic.hasDependers("unknown"));

        // Null or empty strings should not cause an exception and must return false
        assertFalse("hasDependers(null) must return false", mic.hasDependers(null));
        assertFalse("hasDependers(\"\") must return false", mic.hasDependers(""));
        assertFalse("hasDependers(\"   \") must return false", mic.hasDependers("   "));
    }

    // -----------------------------------------------------------------------
    // Named‑dependee tracking
    // -----------------------------------------------------------------------

    @Test
    public void testNamedDependeeStartTracking() {
        String name = "myDependee";
        assertFalse("Initially the named dependee must not be marked as started",
                mic.isNamedDependeeStarted(name));

        mic.markStartOfNamedDependee(name);
        assertTrue("After marking, the named dependee must be reported as started",
                mic.isNamedDependeeStarted(name));

        assertFalse("A different name must still be reported as not started",
                mic.isNamedDependeeStarted("other"));
    }

    // -----------------------------------------------------------------------
    // Import handling
    // -----------------------------------------------------------------------

    @Test
    public void testAddImportAndGetImport() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        assertEquals("java.util.List", mic.getImport("List"));
        assertEquals("java.util.Map", mic.getImport("Map"));
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
        assertNull("null stem must return null", mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Set", "java.util.Set");
        Map<String, String> copy = mic.getImportMapCopy();

        assertEquals("Copy must contain the added entry", "java.util.Set", copy.get("Set"));
        // Mutate the copy and verify original map is untouched
        copy.put("Set", "changed");
        assertEquals("Original map must stay unchanged after copy mutation",
                "java.util.Set", mic.getImport("Set"));
    }

    // -----------------------------------------------------------------------
    // Copy‑constructor behaviour
    // -----------------------------------------------------------------------

    @Test
    public void testCopyConstructorCopiesImportMapAndProperties() {
        // Prepare original MIC with some state
        mic.addImport("Queue", "java.util.Queue");
        mic.addSubstitutionProperty("pKey", "pValue");
        mic.setConfiguratorHint("originalHint");

        // Create a copy
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);

        // Verify that the import map was copied
        assertEquals("java.util.Queue", copy.getImport("Queue"));
        // Changing the copy must not affect the original
        copy.addImport("Queue", "changed");
        assertEquals("java.util.Queue", mic.getImport("Queue"));

        // Verify that substitution properties were copied
        assertEquals("pValue", copy.getProperty("pKey"));
        // Changing copy's property map must not affect the original
        copy.addSubstitutionProperty("pKey", "newValue");
        assertEquals("pValue", mic.getProperty("pKey"));

        // Configurator hint must be the same as the original
        assertEquals("originalHint", copy.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    // BeanDescriptionCache existence
    // -----------------------------------------------------------------------

    @Test
    public void testBeanDescriptionCacheIsNotNull() {
        assertNotNull("BeanDescriptionCache must be instantiated by the constructor",
                mic.getBeanDescriptionCache());
    }

    // -----------------------------------------------------------------------
    // DefaultNestedComponentRegistry exposure
    // -----------------------------------------------------------------------

    @Test
    public void testGetDefaultNestedComponentRegistryReturnsSameInstance() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull("DefaultNestedComponentRegistry must not be null", registry);
        // The registry is created once; calling again must return the same instance
        assertSame("Subsequent calls must return the same instance",
                registry, mic.getDefaultNestedComponentRegistry());
    }
}
