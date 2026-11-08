package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import ch.qos.logback.core.*;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.model.util.VariableSubstitutionsHelper;
import ch.qos.logback.core.spi.*;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 *
 * The tests use a very small in‑memory {@link Context} implementation
 * (see {@link SimpleContext}) because the real Logback {@code Context}
 * implementation is not part of the allowed API for this exercise.
 *
 * All public methods of {@code ModelInterpretationContext} are exercised,
 * including edge cases such as null imports and empty dependency names.
 */
public class ModelInterpretationContextTest {

    private SimpleContext simpleContext;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    /* --------------------------------------------------------------------- */
    /* Helper Context implementation (minimal, only what is needed)        */
    /* --------------------------------------------------------------------- */
    private static class SimpleContext implements Context {

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, Object> objects = new HashMap<>();

        @Override public void setContext(Context context) { /* no‑op */ }
        @Override public Context getContext() { return this; }

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }

        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }

        @Override public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(properties);
        }

        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public void register(LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) { /* no‑op */ }

        // default method from PropertyContainer
        @Override public void addSubstitutionProperties(Properties props) {
            for (String name : props.stringPropertyNames()) {
                addSubstitutionProperty(name, props.getProperty(name));
            }
        }

        @Override public void addSubstitutionProperty(String key, String value) {
            properties.put(key, value);
        }

        @Override public String getProperty(String key) {
            return properties.get(key);
        }

        @Override public Map<String, String> getCopyOfPropertyMap() {
            return new HashMap<>(properties);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. Object map & appender bags                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        // initially the map is empty
        assertTrue(mic.getObjectMap().isEmpty());

        mic.createAppenderBags();

        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue(map.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(map.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Top model getter / setter                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTopModelAccessors() {
        Model m = new Model();
        assertNull(mic.getTopModel());

        mic.setTopModel(m);
        assertSame(m, mic.getTopModel());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Model stack operations                                             */
    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
    /* 4. Object stack operations                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testObjectStackOperations() {
        String o1 = "first";
        Integer o2 = 42;

        assertTrue(mic.isObjectStackEmpty());

        mic.pushObject(o1);
        assertFalse(mic.isObjectStackEmpty());
        assertSame(o1, mic.peekObject());
        assertSame(o1, mic.getObject(0));

        mic.pushObject(o2);
        assertSame(o2, mic.peekObject());
        assertSame(o2, mic.getObject(1));

        assertSame(o2, mic.popObject());
        assertSame(o1, mic.peekObject());

        assertSame(o1, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 5. Configurator hint getter / setter                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testConfiguratorHint() {
        assertNull(mic.getConfiguratorHint());

        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    /* --------------------------------------------------------------------- */
    /* 6. BeanDescriptionCache accessor                                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testBeanDescriptionCacheNotNull() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    /* --------------------------------------------------------------------- */
    /* 7. Variable substitution helper (subst)                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testVariableSubstitution() {
        mic.addSubstitutionProperty("key", "value");
        assertEquals("value", mic.subst("${key}")); // VariableSubstitutionsHelper does substitution of ${}
        // Direct getProperty should also return the value
        assertEquals("value", mic.getProperty("key"));
    }

    /* --------------------------------------------------------------------- */
    /* 8. DefaultNestedComponentRegistry accessor                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDefaultNestedComponentRegistry() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull(registry);
        // add a dummy component and ensure no exception is thrown
        registry.add(String.class, "dummyProp", Integer.class);
        assertEquals(Integer.class,
                registry.findDefaultComponentType(String.class, "dummyProp"));
    }

    /* --------------------------------------------------------------------- */
    /* 9. Dependency handling                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDependencyDefinitionLifecycle() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");

        mic.addDependencyDefinition(dd);

        // getDependencyDefinitions returns an unmodifiable list
        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        assertEquals(1, defs.size());
        assertSame(dd, defs.get(0));

        // attempting to modify the returned list should throw
        try {
            defs.add(dd);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) { }

        // getDependeeNamesForModel returns the name of the dependee
        List<String> depNames = mic.getDependeeNamesForModel(depender);
        assertEquals(Collections.singletonList("myDependee"), depNames);

        // hasDependers should be true for the name we added
        assertTrue(mic.hasDependers("myDependee"));

        // false for unrelated name
        assertFalse(mic.hasDependers("other"));

        // Null or empty string should not throw (the method creates an exception but does not throw)
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testDependeeStartMarking() {
        assertFalse(mic.isNamedDependeeStarted("alpha"));
        mic.markStartOfNamedDependee("alpha");
        assertTrue(mic.isNamedDependeeStarted("alpha"));
        // another name is still false
        assertFalse(mic.isNamedDependeeStarted("beta"));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Property container behaviour (add/substitution)                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddAndGetProperty() {
        mic.addSubstitutionProperty("p1", "v1");
        assertEquals("v1", mic.getProperty("p1"));
        // getCopyOfPropertyMap returns a copy – modifications to it should not affect MIC
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v1", copy.get("p1"));
        copy.put("p1", "changed");
        assertEquals("v1", mic.getProperty("p1"));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Import handling                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testImportMapOperations() {
        // initially empty copy
        assertTrue(mic.getImportMapCopy().isEmpty());

        mic.addImport("List", "java.util.List");
        mic.addImport("Map", "java.util.Map");

        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals(2, copy.size());
        assertEquals("java.util.List", copy.get("List"));
        assertEquals("java.util.Map", copy.get("Map"));

        // getImport returns fully qualified name when known
        assertEquals("java.util.List", mic.getImport("List"));
        // unknown stem returns the stem itself
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
        // null stem returns null
        assertNull(mic.getImport(null));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Copy constructor (clone‑like behavior)                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCopyConstructorCopiesImportMapAndVariableSubstitutions() {
        // set up original MIC
        mic.addImport("Str", "java.lang.String");
        mic.addSubstitutionProperty("foo", "bar");
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "dep"));

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);

        // import map should be a deep copy
        assertEquals("java.lang.String", copy.getImport("Str"));
        copy.addImport("Int", "java.lang.Integer");
        assertNull(mic.getImport("Int")); // original unchanged

        // substitution properties should be copied
        assertEquals("bar", copy.subst("${foo}"));
        copy.addSubstitutionProperty("foo", "baz");
        assertEquals("baz", copy.subst("${foo}"));
        // original still has original value
        assertEquals("bar", mic.subst("${foo}"));

        // dependency list should be independent (copy constructor does not copy the list)
        assertTrue(copy.getDependencyDefinitions().isEmpty());
    }
}
