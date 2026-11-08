/**
 * JUnit 4 test suite for {@link ch.qos.logback.core.model.processor.ModelInterpretationContext}.
 *
 * The tests cover:
 *  - Construction and basic field initialization
 *  - Stack operations for models and generic objects
 *  - Property substitution handling
 *  - Dependency definition handling
 *  - Import map handling
 *  - Edge‑case behaviours such as empty stacks and illegal arguments
 *
 * The test uses a minimal concrete implementation of {@link ch.qos.logback.core.Context}
 * (named {@code SimpleTestContext}) because the library does not expose a public
 * concrete Context implementation in the provided signatures.
 *
 * All tests are written with JUnit 4.
 */
package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.DefaultNestedComponentRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ModelInterpretationContextTest {

    /** A very small {@link Context} implementation sufficient for the tests. */
    private static class SimpleTestContext implements Context {
        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null; // not needed for the tests
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
            return "SimpleTestContext";
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
            return this;
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

        // default methods from PropertyContainer are fine – we do not need to override them
    }

    private SimpleTestContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleTestContext();
        mic = new ModelInterpretationContext(context);
    }

    // -------------------------------------------------------------------------
    //  Constructor and basic getters
    // -------------------------------------------------------------------------

    @Test
    public void constructorInitialisesStacksAndMaps() {
        assertNotNull("objectStack must be created", mic.getObjectStack());
        assertTrue("objectStack must be empty", mic.isObjectStackEmpty());

        assertNotNull("modelStack must be created", mic.peekModel()); // peekModel would throw if empty, so we test emptiness instead
        assertTrue("modelStack must be empty", mic.isModelStackEmpty());

        assertNotNull("objectMap must be created", mic.getObjectMap());
        assertTrue("objectMap should be empty after plain construction",
                mic.getObjectMap().isEmpty());
    }

    @Test
    public void createAppenderBagsPopulatesObjectMap() {
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
    public void topModelSetAndGet() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("Top model should be returned unchanged", model, mic.getTopModel());
    }

    // -------------------------------------------------------------------------
    //  Model stack operations
    // -------------------------------------------------------------------------

    @Test
    public void modelStackPushPeekPop() {
        Model m = new Model();
        mic.pushModel(m);
        assertFalse("Stack must not be empty after push", mic.isModelStackEmpty());
        assertSame("peekModel should return the pushed model", m, mic.peekModel());
        assertSame("popModel should return the same model", m, mic.popModel());
        assertTrue("Stack must be empty after pop", mic.isModelStackEmpty());
    }

    @Test(expected = EmptyStackException.class)
    public void peekModelOnEmptyStackThrows() {
        mic.peekModel(); // should throw EmptyStackException
    }

    @Test(expected = EmptyStackException.class)
    public void popModelOnEmptyStackThrows() {
        mic.popModel();
    }

    // -------------------------------------------------------------------------
    //  Object stack operations
    // -------------------------------------------------------------------------

    @Test
    public void objectStackPushPeekPopAndIndexAccess() {
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        mic.pushObject(second);

        assertFalse("Object stack must not be empty", mic.isObjectStackEmpty());

        assertEquals("peekObject must return top element", second, mic.peekObject());

        assertEquals("getObject(0) must return first pushed element", first, mic.getObject(0));
        assertEquals("popObject must return top element", second, mic.popObject());

        assertEquals("peekObject after pop must be first element", first, mic.peekObject());
        assertEquals("popObject again must return first element", first, mic.popObject());

        assertTrue("Object stack must be empty after popping all elements", mic.isObjectStackEmpty());
    }

    @Test(expected = EmptyStackException.class)
    public void peekObjectOnEmptyStackThrows() {
        mic.peekObject();
    }

    @Test(expected = EmptyStackException.class)
    public void popObjectOnEmptyStackThrows() {
        mic.popObject();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getObjectWithInvalidIndexThrows() {
        mic.pushObject("only");
        mic.getObject(5); // out of bounds
    }

    // -------------------------------------------------------------------------
    //  Configurator hint handling
    // -------------------------------------------------------------------------

    @Test
    public void configuratorHintSetAndGet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint should be returned unchanged", hint, mic.getConfiguratorHint());
    }

    // -------------------------------------------------------------------------
    //  Property substitution (VariableSubstitutionsHelper delegation)
    // -------------------------------------------------------------------------

    @Test
    public void addAndResolveSubstitutionProperty() {
        mic.addSubstitutionProperty("name", "Logback");
        assertEquals("Variable substitution should resolve correctly",
                "Logback", mic.subst("${name}"));
    }

    @Test
    public void getPropertyDelegatesToVariableSubstitutionsHelper() {
        mic.addSubstitutionProperty("key", "value");
        assertEquals("getProperty must return the same value as VariableSubstitutionsHelper",
                "value", mic.getProperty("key"));
    }

    @Test
    public void copyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("a", "1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("copy should contain the added property", "1", copy.get("a"));
        // modify original and ensure copy does not change
        mic.addSubstitutionProperty("b", "2");
        assertNull("copy must not see later changes", copy.get("b"));
    }

    // -------------------------------------------------------------------------
    //  Dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void addAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals("One dependency definition should be present", 1, list.size());
        assertSame("Returned definition must be the same instance", dd, list.get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void dependencyDefinitionsListIsUnmodifiable() {
        Model depender = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(depender, "dep"));
        mic.getDependencyDefinitions().add(new DependencyDefinition(depender, "another"));
    }

    @Test
    public void getDependeeNamesForModel() {
        Model modelA = new Model();
        Model modelB = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "foo"));
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "bar"));
        mic.addDependencyDefinition(new DependencyDefinition(modelB, "baz"));

        List<String> depsForA = mic.getDependeeNamesForModel(modelA);
        assertEquals("Two dependees expected for modelA", 2, depsForA.size());
        assertTrue(depsForA.contains("foo"));
        assertTrue(depsForA.contains("bar"));

        List<String> depsForB = mic.getDependeeNamesForModel(modelB);
        assertEquals("One dependee expected for modelB", 1, depsForB.size());
        assertEquals("baz", depsForB.get(0));
    }

    @Test
    public void hasDependersNormalAndEdgeCases() {
        Model model = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(model, "myDep"));

        assertTrue("Existing dependee name must be reported", mic.hasDependers("myDep"));
        assertFalse("Non‑existing dependee name must be reported as absent", mic.hasDependers("unknown"));
        // According to the implementation, null or empty strings do NOT throw
        // but simply return false.
        assertFalse("null name must return false", mic.hasDependers(null));
        assertFalse("empty name must return false", mic.hasDependers(""));
        assertFalse("blank name must return false", mic.hasDependers("   "));
    }

    // -------------------------------------------------------------------------
    //  Named dependee start tracking
    // -------------------------------------------------------------------------

    @Test
    public void markAndCheckNamedDependeeStarted() {
        String name = "myDependee";
        assertFalse("Initially not started", mic.isNamedDependeeStarted(name));
        mic.markStartOfNamedDependee(name);
        assertTrue("After marking, it must be reported as started", mic.isNamedDependeeStarted(name));
    }

    // -------------------------------------------------------------------------
    //  Import handling
    // -------------------------------------------------------------------------

    @Test
    public void addImportAndGetImport() {
        mic.addImport("List", "java.util.List");
        assertEquals("Fully qualified name must be returned for known stem",
                "java.util.List", mic.getImport("List"));
        assertEquals("Unknown stem returns the stem itself",
                "UnknownStem", mic.getImport("UnknownStem"));
        assertNull("null stem returns null", mic.getImport(null));
    }

    @Test
    public void getImportMapCopyIsIndependent() {
        mic.addImport("Map", "java.util.Map");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("Map should contain the added import", "java.util.Map", copy.get("Map"));
        // Mutating the copy must not affect the original
        copy.put("Set", "java.util.Set");
        assertNull("Original import map must not contain 'Set'", mic.getImport("Set"));
    }

    // -------------------------------------------------------------------------
    //  Bean description cache and nested component registry exposure
    // -------------------------------------------------------------------------

    @Test
    public void beanDescriptionCacheIsNonNull() {
        assertNotNull("beanDescriptionCache must be instantiated", mic.getBeanDescriptionCache());
    }

    @Test
    public void defaultNestedComponentRegistryIsAccessible() {
        DefaultNestedComponentRegistry reg = mic.getDefaultNestedComponentRegistry();
        assertNotNull("defaultNestedComponentRegistry must be non‑null", reg);
        // Simple sanity check – duplicate with a new registry should not throw
        DefaultNestedComponentRegistry other = new DefaultNestedComponentRegistry();
        reg.duplicate(other);
    }
}
