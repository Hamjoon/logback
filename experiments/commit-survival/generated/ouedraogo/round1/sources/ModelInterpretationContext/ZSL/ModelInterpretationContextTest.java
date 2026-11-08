package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ModelInterpretationContext}.
 * <p>
 * The tests use only the public API that is visible in the supplied source
 * code and the minimal stub implementations required for the {@link Context}
 * interface. No external mocking frameworks are used.
 */
public class ModelInterpretationContextTest {

    /**
     * Very small {@link Context} implementation used only for the tests.
     */
    private static class SimpleContext implements Context {

        private final Map<String, String> props = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
            return null; // not needed for the current tests
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
            return props.get(key);
        }

        @Override
        public void putProperty(String key, String value) {
            props.put(key, value);
        }

        @Override
        public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(props);
        }

        @Override
        public String getName() {
            return "simple";
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
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // -----------------------------------------------------------------------
    //  Construction and basic fields
    // -----------------------------------------------------------------------
    @Test
    public void testConstructor_initialState() {
        assertNotNull(mic.getObjectStack());
        assertTrue(mic.isObjectStackEmpty());

        assertNotNull(mic.getObjectMap());
        assertTrue(mic.getObjectMap().isEmpty());

        assertNotNull(mic.getObjectStack());

        assertNull(mic.getTopModel());
        assertNull(mic.getConfiguratorHint());
    }

    @Test
    public void testCreateAppenderBags_populatesObjectMap() {
        mic.createAppenderBags();

        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    // -----------------------------------------------------------------------
    //  Model stack handling
    // -----------------------------------------------------------------------
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
        assertSame(m1, mic.peekModel());

        assertSame(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    // -----------------------------------------------------------------------
    //  Object stack handling
    // -----------------------------------------------------------------------
    @Test
    public void testObjectStackOperations() {
        String o1 = "first";
        Integer o2 = 42;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(o1);
        assertFalse(mic.isObjectStackEmpty());
        assertEquals(o1, mic.peekObject());

        mic.pushObject(o2);
        assertEquals(o2, mic.peekObject());

        assertEquals(o2, mic.popObject());
        assertEquals(o1, mic.peekObject());

        assertEquals(o1, mic.getObject(0));

        assertEquals(o1, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    // -----------------------------------------------------------------------
    //  Configurator hint
    // -----------------------------------------------------------------------
    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    //  Property substitution (subst)
    // -----------------------------------------------------------------------
    @Test
    public void testSubstVariableResolution() {
        // put a property into the Context
        context.putProperty("myKey", "myValue");

        String input = "value=${myKey}";
        String expected = "value=myValue";

        assertEquals(expected, mic.subst(input));
    }

    @Test
    public void testSubstNullReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubstWhenOptionHelperThrows_returnsOriginal() {
        // An unclosed variable reference triggers ScanException inside OptionHelper
        String bad = "value=${unclosed";
        // The method should catch the exception and return the original string
        assertEquals(bad, mic.subst(bad));
    }

    // -----------------------------------------------------------------------
    //  Substitution properties handling
    // -----------------------------------------------------------------------
    @Test
    public void testAddSubstitutionPropertyTrimsAndStores() {
        mic.addSubstitutionProperty(" key ", "  value  ");
        assertEquals("value", mic.getProperty(" key "));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNulls() {
        mic.addSubstitutionProperty(null, "value");
        mic.addSubstitutionProperty("key", null);
        assertNull(mic.getProperty(null));
        assertNull(mic.getProperty("key"));
    }

    @Test
    public void testAddSubstitutionPropertiesFromPropertiesObject() {
        Properties props = new Properties();
        props.setProperty("a", "  1 ");
        props.setProperty("b", "2");
        mic.addSubstitutionProperties(props);

        assertEquals("1", mic.getProperty("a"));
        assertEquals("2", mic.getProperty("b"));
    }

    // -----------------------------------------------------------------------
    //  getProperty delegation
    // -----------------------------------------------------------------------
    @Test
    public void testGetPropertyPrefersLocalMapOverContext() {
        // local map
        mic.addSubstitutionProperty("foo", "local");
        // context map
        context.putProperty("foo", "context");
        context.putProperty("bar", "contextOnly");

        assertEquals("local", mic.getProperty("foo"));
        assertEquals("contextOnly", mic.getProperty("bar"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("x", "1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("1", copy.get("x"));

        // modify original map and ensure copy is unchanged
        mic.addSubstitutionProperty("x", "2");
        assertEquals("1", copy.get("x"));
    }

    // -----------------------------------------------------------------------
    //  Import handling
    // -----------------------------------------------------------------------
    @Test
    public void testAddAndGetImport() {
        mic.addImport("List", "java.util.List");
        assertEquals("java.util.List", mic.getImport("List"));
    }

    @Test
    public void testGetImportUnknownStemReturnsStemItself() {
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
    }

    @Test
    public void testGetImportNullReturnsNull() {
        assertNull(mic.getImport(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Map", "java.util.Map");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Map", copy.get("Map"));

        // mutate the original map
        mic.addImport("Map", "changed");
        assertEquals("java.util.Map", copy.get("Map"));
    }

    // -----------------------------------------------------------------------
    //  Dependency handling
    // -----------------------------------------------------------------------
    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));
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

        List<String> deps2 = mic.getDependeeNamesForModel(m2);
        assertEquals(1, deps2.size());
        assertEquals("C", deps2.get(0));
    }

    @Test
    public void testHasDependersDetectsPresence() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "target"));

        assertTrue(mic.hasDependers("target"));
        assertFalse(mic.hasDependers("nonexistent"));
    }

    @Test
    public void testHasDependersWithNullOrEmptyDoesNotThrow() {
        // According to implementation the method creates an IllegalArgumentException
        // but does not throw it, so we just verify it returns false.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testMarkStartAndCheckNamedDependee() {
        mic.markStartOfNamedDependee("first");
        assertTrue(mic.isNamedDependeeStarted("first"));
        assertFalse(mic.isNamedDependeeStarted("second"));
    }

    // -----------------------------------------------------------------------
    //  DefaultNestedComponentRegistry duplication
    // -----------------------------------------------------------------------
    @Test
    public void testDefaultNestedComponentRegistryDuplicateDoesNotFail() {
        // The default registry is always present; duplication should simply copy
        // its internal state without throwing.
        DefaultNestedComponentRegistry other = new DefaultNestedComponentRegistry();
        other.add(String.class, "prop", Integer.class);
        mic.getDefaultNestedComponentRegistry().duplicate(other);
        // No assertions – the test passes if no exception is thrown.
    }

    // -----------------------------------------------------------------------
    //  BeanDescriptionCache existence
    // -----------------------------------------------------------------------
    @Test
    public void testBeanDescriptionCacheIsCreated() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    // -----------------------------------------------------------------------
    //  Top model getter/setter
    // -----------------------------------------------------------------------
    @Test
    public void testTopModelGetterSetter() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }
}
