package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Comprehensive JUnit‑4 test suite for {@link ModelInterpretationContext}.
 * The tests cover normal behaviour, edge cases and error handling for every
 * public method of the class.
 *
 * No external mocking frameworks are used – only concrete implementations
 * that are defined in this test file (a minimal {@link SimpleContext}
 * implementation) and the classes that appear in the supplied source.
 */
public class ModelInterpretationContextTest {

    /** Simple in‑memory {@link Context} implementation used for testing. */
    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }

        @Override public Object getObject(String key) { return objects.get(key); }

        @Override public void putObject(String key, Object value) { objects.put(key, value); }

        @Override public String getProperty(String key) { return properties.get(key); }

        @Override public void putProperty(String key, String value) { properties.put(key, value); }

        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }

        @Override public String getName() { return "SimpleContext"; }

        @Override public void setName(String name) { /* no‑op */ }

        @Override public long getBirthTime() { return System.currentTimeMillis(); }

        @Override public Object getConfigurationLock() { return new Object(); }

        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }

        @Override public ExecutorService getExecutorService() { return null; }

        @Override public ExecutorService getAlternateExecutorService() { return null; }

        @Override public void register(LifeCycle component) { /* no‑op */ }

        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) { /* no‑op */ }

        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }

        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }

        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) { /* no‑op */ }

        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor & basic initialisation                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorInitialisesStacksAndMaps() {
        assertNotNull("objectStack must be created", mic.getObjectStack());
        assertTrue("objectStack must be empty initially", mic.isObjectStackEmpty());

        assertNotNull("modelStack must be created", mic.modelStack);
        assertTrue("modelStack must be empty initially", mic.isModelStackEmpty());

        assertNotNull("objectMap must be created", mic.getObjectMap());
        assertTrue("objectMap must be empty initially", mic.getObjectMap().isEmpty());

        assertNotNull("propertiesMap must be created", mic.propertiesMap);
        assertTrue("propertiesMap must be empty initially", mic.propertiesMap.isEmpty());

        assertNotNull("importMap must be created", mic.importMap);
        assertTrue("importMap must be empty initially", mic.importMap.isEmpty());

        assertNotNull("beanDescriptionCache must be created", mic.getBeanDescriptionCache());
    }

    @Test
    public void testCreateAppenderBagsAddsExpectedEntries() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();

        assertTrue("APPENDER_BAG must be present", map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue("APPENDER_REF_BAG must be present", map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue("APPENDER_BAG value must be a Map", map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue("APPENDER_REF_BAG value must be a Map", map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    /* ---------------------------------------------------------------------- */
    /* Top model getter / setter                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTopModelSetAndGet() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame("Returned top model must be the one set", model, mic.getTopModel());
    }

    /* ---------------------------------------------------------------------- */
    /* Model stack operations                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testModelStackPushPeekPopAndEmpty() {
        Model m1 = new Model();
        Model m2 = new Model();

        assertTrue("Stack must be empty initially", mic.isModelStackEmpty());

        mic.pushModel(m1);
        assertFalse("Stack must not be empty after push", mic.isModelStackEmpty());
        assertSame("peekModel must return the last pushed model", m1, mic.peekModel());

        mic.pushModel(m2);
        assertSame("peekModel must return the most recent model", m2, mic.peekModel());

        assertSame("popModel must return m2", m2, mic.popModel());
        assertSame("peekModel now must return m1", m1, mic.peekModel());

        assertSame("popModel must return m1", m1, mic.popModel());
        assertTrue("Stack must be empty after popping all", mic.isModelStackEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Object stack operations                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testObjectStackPushPeekPopAndGet() {
        String obj1 = "first";
        Integer obj2 = 42;

        assertTrue("Object stack must be empty initially", mic.isObjectStackEmpty());

        mic.pushObject(obj1);
        assertFalse("Object stack must not be empty after push", mic.isObjectStackEmpty());
        assertSame("peekObject must return obj1", obj1, mic.peekObject());

        mic.pushObject(obj2);
        assertSame("peekObject must now return obj2", obj2, mic.peekObject());

        assertSame("getObject(0) must return first element", obj1, mic.getObject(0));
        assertSame("getObject(1) must return second element", obj2, mic.getObject(1));

        assertSame("popObject must return obj2", obj2, mic.popObject());
        assertSame("peekObject after pop must return obj1", obj1, mic.peekObject());

        assertSame("popObject must return obj1", obj1, mic.popObject());
        assertTrue("Object stack must be empty after popping all", mic.isObjectStackEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Configurator hint getter / setter                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfiguratorHintGetSet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame("Configurator hint must be the one set", hint, mic.getConfiguratorHint());
    }

    /* ---------------------------------------------------------------------- */
    /* Variable substitution (subst)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSubstReturnsNullForNullInput() {
        assertNull("subst(null) must return null", mic.subst(null));
    }

    @Test
    public void testSubstPerformsVariableReplacement() {
        context.putProperty("myKey", "myValue");
        String result = mic.subst("${myKey}");
        assertEquals("Variable must be substituted with context property", "myValue", result);
    }

    @Test
    public void testSubstHandlesExceptionGracefully() {
        // A PropertyContainer that throws IllegalArgumentException on getProperty
        PropertyContainer badContainer = new PropertyContainer() {
            @Override public String getProperty(String key) { throw new IllegalArgumentException("bad"); }
            @Override public Map<String, String> getCopyOfPropertyMap() { return Collections.emptyMap(); }
        };
        // Use a dummy context that delegates to the bad container via OptionHelper
        Context badContext = new SimpleContext() {
            @Override public String getProperty(String key) { return badContainer.getProperty(key); }
        };
        ModelInterpretationContext badMic = new ModelInterpretationContext(badContext);
        String input = "${any}";
        String result = badMic.subst(input);
        // In case of error the original string should be returned
        assertEquals("On error subst must return original string", input, result);
    }

    /* ---------------------------------------------------------------------- */
    /* Substitution property handling                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddSubstitutionPropertyTrimsValue() {
        mic.addSubstitutionProperty(" key ", "  value  ");
        assertEquals("Key must be stored without trimming", " value  ", mic.propertiesMap.get(" key "));
        // The value should be trimmed
        assertEquals("Stored value must be trimmed", "value", mic.getProperty(" key "));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNulls() {
        mic.addSubstitutionProperty(null, "value");
        mic.addSubstitutionProperty("key", null);
        assertTrue("No entry should be added when key is null", mic.propertiesMap.isEmpty());
    }

    @Test
    public void testAddSubstitutionPropertiesCopiesAllEntries() {
        Properties props = new Properties();
        props.setProperty("p1", "v1");
        props.setProperty("p2", "  v2  ");

        mic.addSubstitutionProperties(props);
        assertEquals("First property must be stored trimmed", "v1", mic.getProperty("p1"));
        assertEquals("Second property must be stored trimmed", "v2", mic.getProperty("p2"));
    }

    /* ---------------------------------------------------------------------- */
    /* Dependency handling                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDependencyDefinitionLifecycle() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dependeeA");

        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals("One dependency definition must be present", 1, list.size());
        assertSame("Stored definition must be the one added", dd, list.get(0));

        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals("Dependee list for the model must contain one entry", 1, dependees.size());
        assertEquals("Dependee name must match", "dependeeA", dependees.get(0));

        assertTrue("hasDependers must return true for existing dependee", mic.hasDependers("dependeeA"));
        assertFalse("hasDependers must return false for unknown dependee", mic.hasDependers("unknown"));
    }

    @Test
    public void testDependerStartTracking() {
        mic.markStartOfNamedDependee("serviceX");
        assertTrue("isNamedDependeeStarted must be true after marking", mic.isNamedDependeeStarted("serviceX"));
        assertFalse("isNamedDependeeStarted must be false for unmarked name", mic.isNamedDependeeStarted("serviceY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasDependersRejectsEmptyName() {
        mic.hasDependers("   ");
    }

    /* ---------------------------------------------------------------------- */
    /* Property lookup – local overrides context                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetPropertyPrefersLocalOverContext() {
        context.putProperty("common", "contextValue");
        mic.addSubstitutionProperty("common", "localValue");
        assertEquals("Local property must win over context property", "localValue", mic.getProperty("common"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsDefensive() {
        mic.addSubstitutionProperty("a", "1");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("Copy must contain the same entry", "1", copy.get("a"));
        copy.put("a", "2");
        assertEquals("Original map must not be affected by changes to the copy", "1", mic.getProperty("a"));
    }

    /* ---------------------------------------------------------------------- */
    /* Import handling                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddImportAndRetrieval() {
        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("Import map copy must contain added entries", "java.util.List", copy.get("List"));
        assertEquals("Import map copy must contain added entries", "java.util.Map", copy.get("Map"));
    }

    @Test
    public void testGetImportReturnsFullyQualifiedOrStem() {
        mic.addImport("Set", "java.util.Set");
        assertEquals("Known stem must resolve to fully qualified name", "java.util.Set", mic.getImport("Set"));
        assertEquals("Unknown stem must be returned unchanged", "UnknownStem", mic.getImport("UnknownStem"));
        assertNull("Null stem must return null", mic.getImport(null));
    }

    /* ---------------------------------------------------------------------- */
    /* DefaultNestedComponentRegistry exposure                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDefaultNestedComponentRegistryIsNotNull() {
        assertNotNull("defaultNestedComponentRegistry must be instantiated", mic.getDefaultNestedComponentRegistry());
    }

    /* ---------------------------------------------------------------------- */
    /* BeanDescriptionCache exposure                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetBeanDescriptionCacheIsAvailable() {
        assertNotNull("BeanDescriptionCache must be available", mic.getBeanDescriptionCache());
    }
}
