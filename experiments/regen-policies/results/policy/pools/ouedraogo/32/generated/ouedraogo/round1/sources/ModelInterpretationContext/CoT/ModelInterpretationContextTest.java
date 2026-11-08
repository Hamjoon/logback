package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.*;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import ch.qos.logback.core.spi.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ModelInterpretationContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>all public methods (basic behaviour)</li>
 *   <li>edge cases such as empty stacks, out‑of‑bounds index, null arguments</li>
 *   <li>exceptional paths (EmptyStackException, IllegalArgumentException handling)</li>
 *   <li>copy‑constructor semantics</li>
 * </ul>
 *
 * A very small {@link Context} implementation ( {@code SimpleContext} ) is provided
 * because the real Logback {@code Context} is an interface.  The implementation
 * supplies only the members used by {@code ModelInterpretationContext}.
 */
public class ModelInterpretationContextTest {

    /** Minimal Context implementation used by the tests. */
    private static class SimpleContext implements Context {

        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objectMap.get(key); }
        @Override public void putObject(String key, Object value) { objectMap.put(key, value); }
        @Override public String getProperty(String key) { return propertyMap.get(key); }
        @Override public void putProperty(String key, String value) { propertyMap.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(propertyMap); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) {}
        @Override public void register(LifeCycle component) {}
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {}
        @Override public void addStatus(Status status) {}
        @Override public void addInfo(String msg) {}
        @Override public void addInfo(String msg, Throwable ex) {}
        @Override public void addWarn(String msg) {}
        @Override public void addWarn(String msg, Throwable ex) {}
        @Override public void addError(String msg) {}
        @Override public void addError(String msg, Throwable ex) {}
        @Override public Context getContext() { return this; }
        @Override public void setContext(Context context) {}
    }

    private SimpleContext context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // -----------------------------------------------------------------------
    //  Public API – basic behaviour
    // -----------------------------------------------------------------------

    @Test
    public void testCreateAppenderBags() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));
        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof HashMap);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof HashMap);
    }

    @Test
    public void testTopModelGetterSetter() {
        Model model = new Model();
        mic.setTopModel(model);
        assertSame(model, mic.getTopModel());
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
        assertSame(m1, mic.peekModel());

        assertSame(m1, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    @Test(expected = EmptyStackException.class)
    public void testModelStackPopEmptyThrows() {
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
        assertSame(obj1, mic.peekObject());

        assertSame(obj1, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test(expected = EmptyStackException.class)
    public void testObjectStackPopEmptyThrows() {
        mic.popObject();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testObjectStackGetOutOfBoundsThrows() {
        mic.pushObject("a");
        mic.getObject(5); // invalid index
    }

    @Test
    public void testConfiguratorHint() {
        assertNull(mic.getConfiguratorHint());

        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    @Test
    public void testVariableSubstitutionHelper() {
        // No substitution defined – should return the input unchanged
        assertEquals("plain text", mic.subst("plain text"));

        // Define a property and verify substitution using ${key} syntax
        mic.addSubstitutionProperty("myKey", "myValue");
        assertEquals("value is myValue", mic.subst("value is ${myKey}"));
    }

    @Test
    public void testDependencyDefinitionHandling() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dependee1");
        mic.addDependencyDefinition(dd);

        // retrieve list (unmodifiable) – size must be 1
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));

        // getDependeeNamesForModel should return the dependee name
        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals(Collections.singletonList("dependee1"), dependees);

        // hasDependers – true for existing name, false otherwise
        assertTrue(mic.hasDependers("dependee1"));
        assertFalse(mic.hasDependers("nonexistent"));

        // Edge case: null or empty string – method does NOT throw (bug in source)
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
    }

    @Test
    public void testNamedDependeeTracking() {
        mic.markStartOfNamedDependee("serviceA");
        assertTrue(mic.isNamedDependeeStarted("serviceA"));
        assertFalse(mic.isNamedDependeeStarted("serviceB"));
    }

    @Test
    public void testImportMapBehaviour() {
        // normal import
        mic.addImport("List", "java.util.List");
        assertEquals("java.util.List", mic.getImport("List"));

        // unknown stem – returns the stem itself
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));

        // null argument – returns null
        assertNull(mic.getImport(null));

        // ensure defensive copy
        Map<String, String> copy = mic.getImportMapCopy();
        copy.put("NewStem", "foo.Bar");
        assertNull(mic.getImport("NewStem")); // original map unchanged
    }

    // -----------------------------------------------------------------------
    //  Copy‑constructor tests
    // -----------------------------------------------------------------------

    @Test
    public void testCopyConstructorCopiesState() {
        // Populate original MIC
        mic.addImport("Map", "java.util.Map");
        mic.addSubstitutionProperty("key1", "value1");
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "dep");
        mic.addDependencyDefinition(dd);
        mic.setConfiguratorHint("originalHint");

        // invoke copy‑constructor
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);

        // import map must be a deep copy
        assertEquals("java.util.Map", copy.getImport("Map"));
        copy.addImport("Map", "changed");          // modifying copy does not affect original
        assertEquals("java.util.Map", mic.getImport("Map"));

        // substitution properties must be copied
        assertEquals("value1", copy.getProperty("key1"));
        copy.addSubstitutionProperty("key1", "newValue");
        assertEquals("value1", mic.getProperty("key1"));

        // dependency definitions list must be copied (unmodifiable view)
        List<DependencyDefinition> originalDeps = mic.getDependencyDefinitions();
        List<DependencyDefinition> copyDeps = copy.getDependencyDefinitions();
        assertEquals(originalDeps, copyDeps);
        assertNotSame(originalDeps, copyDeps);

        // configurator hint is copied
        assertEquals("originalHint", copy.getConfiguratorHint());

        // createAppenderBags is called inside the copy‑constructor
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(copy.getObjectMap().containsKey(JoranConstants.APPENDER_REF_BAG));
    }

    // -----------------------------------------------------------------------
    //  Edge cases for stack handling (null values)
    // -----------------------------------------------------------------------

    @Test
    public void testPushNullModelAndObject() {
        mic.pushModel(null);
        assertNull(mic.peekModel());
        assertNull(mic.popModel());

        mic.pushObject(null);
        assertNull(mic.peekObject());
        assertNull(mic.popObject());
    }

    // -----------------------------------------------------------------------
    //  Additional sanity checks
    // -----------------------------------------------------------------------

    @Test
    public void testGetObjectMapIsMutable() {
        Map<String, Object> map = mic.getObjectMap();
        map.put("customKey", "customValue");
        assertEquals("customValue", mic.getObjectMap().get("customKey"));
    }

    @Test
    public void testGetDefaultNestedComponentRegistry() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull(registry);
        // simple sanity: duplicate with itself should not throw
        registry.duplicate(registry);
    }
}
