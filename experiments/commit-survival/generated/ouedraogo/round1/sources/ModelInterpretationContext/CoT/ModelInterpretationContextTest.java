package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.StatusManager;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.joran.JoranConstants;

/**
 * JUnit4 test suite for {@link ModelInterpretationContext}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic behaviour)</li>
 *   <li>Edge cases such as empty stacks, null arguments, and unmodifiable collections</li>
 *   <li>Exception‑safe paths (e.g. substitution errors are handled internally)</li>
 * </ul>
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
        public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) {
            // no‑op
        }

        @Override
        public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) {
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
    // Constructor & basic getters
    // -----------------------------------------------------------------------

    @Test
    public void testConstructor_initialState() {
        assertNotNull("objectStack must be created", mic.getObjectStack());
        assertTrue("objectStack must be empty", mic.isObjectStackEmpty());

        assertNotNull("modelStack must be created", mic.peekModel()); // will throw if empty, so we test isModelStackEmpty instead
        assertTrue("modelStack must be empty", mic.isModelStackEmpty());

        assertNotNull("objectMap must be created", mic.getObjectMap());
        assertTrue("objectMap must be empty initially", mic.getObjectMap().isEmpty());

        assertNotNull("beanDescriptionCache must be created", mic.getBeanDescriptionCache());

        assertNull("topModel is null initially", mic.getTopModel());

        assertNull("configuratorHint is null initially", mic.getConfiguratorHint());
    }

    @Test
    public void testConstructor_withConfiguratorHint() {
        Object hint = new Object();
        ModelInterpretationContext mic2 = new ModelInterpretationContext(context, hint);
        assertSame("configuratorHint must be stored", hint, mic2.getConfiguratorHint());
    }

    @Test
    public void testCopyConstructor_copiesMapsAndRegistry() {
        mic.addImport("List", "java.util.List");
        mic.addSubstitutionProperty("key", "value");
        mic.setTopModel(new Model());

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        assertEquals("import map must be copied", mic.getImportMapCopy(), copy.getImportMapCopy());
        assertEquals("properties map must be copied", mic.getCopyOfPropertyMap(), copy.getCopyOfPropertyMap());
        assertNotSame("registry must be duplicated, not the same instance", mic.getDefaultNestedComponentRegistry(),
                copy.getDefaultNestedComponentRegistry());

        // ensure appender bags are created in copy
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_REF_BAG));
    }

    // -----------------------------------------------------------------------
    // Top model handling
    // -----------------------------------------------------------------------

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("setter/getter must return same instance", model, mic.getTopModel());
    }

    // -----------------------------------------------------------------------
    // Model stack operations
    // -----------------------------------------------------------------------

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

    @Test(expected = java.util.EmptyStackException.class)
    public void testModelStackPeekOnEmptyThrows() {
        mic.peekModel();
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testModelStackPopOnEmptyThrows() {
        mic.popModel();
    }

    // -----------------------------------------------------------------------
    // Object stack operations
    // -----------------------------------------------------------------------

    @Test
    public void testObjectStackPushPeekPopAndGet() {
        String o1 = "first";
        Integer o2 = 42;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(o1);
        assertFalse(mic.isObjectStackEmpty());
        assertSame(o1, mic.peekObject());

        mic.pushObject(o2);
        assertSame(o2, mic.peekObject());

        assertSame(o2, mic.popObject());
        assertSame(o1, mic.peekObject());

        assertSame(o1, mic.getObject(0));
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testObjectStackPeekOnEmptyThrows() {
        mic.peekObject();
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void testObjectStackPopOnEmptyThrows() {
        mic.popObject();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testObjectStackGetInvalidIndexThrows() {
        mic.pushObject("a");
        mic.getObject(5);
    }

    // -----------------------------------------------------------------------
    // Configurator hint
    // -----------------------------------------------------------------------

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    // -----------------------------------------------------------------------
    // Appender bags creation
    // -----------------------------------------------------------------------

    @Test
    public void testCreateAppenderBagsAddsExpectedKeys() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));
        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    // -----------------------------------------------------------------------
    // Property substitution
    // -----------------------------------------------------------------------

    @Test
    public void testSubstNullReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubstVariableResolution() {
        // place a property in the MIC's own property map
        mic.addSubstitutionProperty("myKey", "myValue");
        // variable syntax used by OptionHelper
        String result = mic.subst("${myKey}");
        assertEquals("myValue", result);
    }

    // Note: the internal error handling path (ScanException / IllegalArgumentException)
    // cannot be triggered without a custom OptionHelper implementation, therefore it is
    // exercised indirectly by ensuring no exception is thrown for a valid substitution.

    // -----------------------------------------------------------------------
    // Substitution properties handling
    // -----------------------------------------------------------------------

    @Test
    public void testAddSubstitutionPropertyTrimsAndOverwrite() {
        mic.addSubstitutionProperty(" key ", "  value  ");
        assertEquals("value", mic.getProperty(" key ")); // key is stored as‑is, value trimmed

        // overwrite
        mic.addSubstitutionProperty(" key ", "new");
        assertEquals("new", mic.getProperty(" key "));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNulls() {
        mic.addSubstitutionProperty(null, "val");
        mic.addSubstitutionProperty("k", null);
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    @Test
    public void testAddSubstitutionPropertiesFromPropertiesObject() {
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.setProperty("b", " 2 ");
        mic.addSubstitutionProperties(props);
        assertEquals("1", mic.getProperty("a"));
        assertEquals("2", mic.getProperty("b"));
    }

    @Test
    public void testAddSubstitutionPropertiesIgnoresNull() {
        mic.addSubstitutionProperties(null); // should not throw
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Dependency handling
    // -----------------------------------------------------------------------

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dependee");
        mic.addDependencyDefinition(dd);

        // the returned list must be unmodifiable
        try {
            mic.getDependencyDefinitions().add(new DependencyDefinition(new Model(), "another"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertEquals(1, mic.getDependencyDefinitions().size());
        assertSame(dd, mic.getDependencyDefinitions().get(0));
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "first"));
        mic.addDependencyDefinition(new DependencyDefinition(m, "second"));
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "other"));

        java.util.List<String> names = mic.getDependeeNamesForModel(m);
        assertEquals(2, names.size());
        assertTrue(names.contains("first"));
        assertTrue(names.contains("second"));
    }

    @Test
    public void testHasDependersTrueAndFalse() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "target"));
        assertTrue(mic.hasDependers("target"));
        assertFalse(mic.hasDependers("nonexistent"));
    }

    @Test
    public void testHasDependersWithNullOrEmptyNameDoesNotThrow() {
        // According to implementation, it just creates an exception object and returns false
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testMarkStartOfNamedDependeeAndCheck() {
        mic.markStartOfNamedDependee("alpha");
        assertTrue(mic.isNamedDependeeStarted("alpha"));
        assertFalse(mic.isNamedDependeeStarted("beta"));
    }

    // -----------------------------------------------------------------------
    // Property handling (overridden from PropertyContainer)
    // -----------------------------------------------------------------------

    @Test
    public void testGetPropertyPrefersLocalMapOverContext() {
        // put a property in the context
        context.putProperty("p", "contextVal");
        // put a different value in the MIC's map
        mic.addSubstitutionProperty("p", "micVal");

        assertEquals("micVal", mic.getProperty("p"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("k", "v");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v", copy.get("k"));
        copy.put("k", "changed");
        // original map must stay unchanged
        assertEquals("v", mic.getProperty("k"));
    }

    // -----------------------------------------------------------------------
    // Import handling
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
    public void testAddImportWithNullStem() {
        mic.addImport(null, "java.lang.String");
        // Map permits null keys; getImport(null) returns null per contract
        assertNull(mic.getImport(null));
        // The internal map should contain the null key
        assertTrue(mic.getImportMapCopy().containsKey(null));
    }

    @Test
    public void testGetImportMapCopyIsIndependent() {
        mic.addImport("Map", "java.util.Map");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Map", copy.get("Map"));
        copy.put("Map", "changed");
        assertEquals("java.util.Map", mic.getImport("Map"));
    }

    // -----------------------------------------------------------------------
    // DefaultNestedComponentRegistry accessor
    // -----------------------------------------------------------------------

    @Test
    public void testGetDefaultNestedComponentRegistryIsNotNull() {
        assertNotNull(mic.getDefaultNestedComponentRegistry());
    }
}
