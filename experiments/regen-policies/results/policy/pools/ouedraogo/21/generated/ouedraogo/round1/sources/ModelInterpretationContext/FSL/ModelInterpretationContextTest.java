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
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Comprehensive unit tests for {@link ModelInterpretationContext}.
 */
public class ModelInterpretationContextTest {

    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override
        public StatusManager getStatusManager() {
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

    // -----------------------------------------------------------------------
    // Constructor and basic getters / setters
    // -----------------------------------------------------------------------

    @Test
    public void testConstructorAndTopModel() {
        assertNotNull(mic.getObjectMap());
        assertNotNull(mic.getObjectStack());
        assertNotNull(mic.getCopyOfPropertyMap());

        // topModel defaults to null
        assertNull(mic.getTopModel());

        Model m = new Model();
        mic.setTopModel(m);
        assertSame(m, mic.getTopModel());
    }

    @Test
    public void testConfiguratorHint() {
        assertNull(mic.getConfiguratorHint());

        String hint = "myHint";
        mic.setConfiguratorHint(hint);
        assertEquals(hint, mic.getConfiguratorHint());

        Integer anotherHint = 42;
        mic.setConfiguratorHint(anotherHint);
        assertEquals(anotherHint, mic.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    // Object map – createAppenderBags
    // -----------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        // clear any existing entries
        mic.getObjectMap().clear();

        mic.createAppenderBags();

        assertTrue(mic.getObjectMap().containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(mic.getObjectMap().containsKey(JoranConstants.APPENDER_REF_BAG));

        Object appenderBag = mic.getObjectMap().get(JoranConstants.APPENDER_BAG);
        Object appenderRefBag = mic.getObjectMap().get(JoranConstants.APPENDER_REF_BAG);

        assertTrue(appenderBag instanceof Map);
        assertTrue(appenderRefBag instanceof Map);
    }

    // -----------------------------------------------------------------------
    // Model stack operations
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
    // Object stack operations
    // -----------------------------------------------------------------------

    @Test
    public void testObjectStackOperations() {
        String obj1 = "first";
        Integer obj2 = 123;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(obj1);
        mic.pushObject(obj2);

        assertFalse(mic.isObjectStackEmpty());
        assertEquals(obj2, mic.peekObject());

        assertEquals(obj2, mic.popObject());
        assertEquals(obj1, mic.peekObject());

        assertEquals(obj1, mic.getObject(0));
        assertEquals(obj1, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    // -----------------------------------------------------------------------
    // Property handling (addSubstitutionProperty, addSubstitutionProperties,
    // getProperty, getCopyOfPropertyMap)
    // -----------------------------------------------------------------------

    @Test
    public void testAddSubstitutionPropertyAndTrim() {
        mic.addSubstitutionProperty("key1", "  value1  ");
        assertEquals("value1", mic.getProperty("key1"));

        // null key or value should be ignored
        mic.addSubstitutionProperty(null, "ignored");
        mic.addSubstitutionProperty("ignored", null);
        assertNull(mic.getProperty(null));
        assertNull(mic.getProperty("ignored"));
    }

    @Test
    public void testAddSubstitutionPropertiesFromPropertiesObject() {
        Properties props = new Properties();
        props.setProperty("p1", " v1 ");
        props.setProperty("p2", "v2");

        mic.addSubstitutionProperties(props);

        assertEquals("v1", mic.getProperty("p1"));
        assertEquals("v2", mic.getProperty("p2"));
    }

    @Test
    public void testGetPropertyPrefersLocalMapOverContext() {
        // property only in context
        context.putProperty("cKey", "cValue");
        assertEquals("cValue", mic.getProperty("cKey"));

        // property in mic overrides context
        mic.addSubstitutionProperty("cKey", "micValue");
        assertEquals("micValue", mic.getProperty("cKey"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("k", "v");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals(1, copy.size());
        assertEquals("v", copy.get("k"));

        // modify original map – copy should stay unchanged
        mic.addSubstitutionProperty("k2", "v2");
        assertNull(copy.get("k2"));
    }

    // -----------------------------------------------------------------------
    // Variable substitution (subst)
    // -----------------------------------------------------------------------

    @Test
    public void testSubstHandlesNullAndNoSubstitution() {
        assertNull(mic.subst(null));
        assertEquals("plain", mic.subst("plain"));
    }

    @Test
    public void testSubstPerformsVariableReplacement() {
        mic.addSubstitutionProperty("name", "Logback");
        String result = mic.subst("Hello ${name}!");
        assertEquals("Hello Logback!", result);
    }

    @Test
    public void testSubstHandlesOptionHelperExceptionGracefully() {
        // Force a ScanException by using an invalid variable syntax
        String malformed = "${unclosed";
        String result = mic.subst(malformed);
        // When substitution fails, the original string is returned
        assertEquals(malformed, result);
    }

    // -----------------------------------------------------------------------
    // Dependency handling
    // -----------------------------------------------------------------------

    @Test
    public void testDependencyDefinitionManagement() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");

        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(1, defs.size());
        assertSame(dd, defs.get(0));

        // verify unmodifiable list
        try {
            defs.add(dd);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }

        // getDependeeNamesForModel
        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals(1, dependees.size());
        assertEquals("myDependee", dependees.get(0));

        // hasDependers
        assertTrue(mic.hasDependers("myDependee"));
        assertFalse(mic.hasDependers("unknown"));
    }

    @Test
    public void testHasDependersWithNullOrEmptyArgument() {
        // According to implementation, it just creates an exception and returns false
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testNamedDependeeTracking() {
        assertFalse(mic.isNamedDependeeStarted("foo"));
        mic.markStartOfNamedDependee("foo");
        assertTrue(mic.isNamedDependeeStarted("foo"));
        assertFalse(mic.isNamedDependeeStarted("bar"));
    }

    // -----------------------------------------------------------------------
    // Import handling
    // -----------------------------------------------------------------------

    @Test
    public void testImportMapOperations() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // getImport returns fully qualified name if present
        assertEquals("java.util.List", mic.getImport("List"));
        // unknown stem returns the stem itself
        assertEquals("Set", mic.getImport("Set"));
        // null stem returns null
        assertNull(mic.getImport(null));
    }

    // -----------------------------------------------------------------------
    // BeanDescriptionCache exposure
    // -----------------------------------------------------------------------

    @Test
    public void testBeanDescriptionCacheIsCreated() {
        assertNotNull(mic.getBeanDescriptionCache());
        // The cache should be associated with the same context
        assertSame(context, mic.getBeanDescriptionCache().getContext());
    }

    // -----------------------------------------------------------------------
    // DefaultNestedComponentRegistry duplication via copy‑constructor
    // -----------------------------------------------------------------------

    @Test
    public void testCopyConstructorDuplicatesRegistryAndImports() {
        mic.addImport("Foo", "com.example.Foo");
        mic.addSubstitutionProperty("k", "v");
        mic.createAppenderBags();

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // imports should be duplicated
        assertEquals("com.example.Foo", copy.getImport("Foo"));
        // properties should be duplicated
        assertEquals("v", copy.getProperty("k"));
        // object map should contain the appender bags (they are created in the copy ctor)
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_REF_BAG));
    }

}
