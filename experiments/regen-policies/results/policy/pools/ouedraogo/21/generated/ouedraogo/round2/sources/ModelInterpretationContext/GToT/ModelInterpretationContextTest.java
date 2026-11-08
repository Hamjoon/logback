package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.OptionHelper;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Comprehensive JUnit‑4 test suite for {@link ModelInterpretationContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods (basic behaviour)</li>
 *   <li>Edge‑case handling (null arguments, empty strings, duplicate entries, etc.)</li>
 *   <li>Exception‑related paths (e.g. substitution errors)</li>
 * </ul>
 *
 * No external libraries are used – a tiny {@link SimpleContext} implementation
 * provides the required {@link Context} contract.
 */
public class ModelInterpretationContextTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal {@link Context} implementation sufficient for the tests.
     * Only the methods actually used by {@code ModelInterpretationContext}
     * are fully implemented; the rest throw {@link UnsupportedOperationException}.
     */
    private static class SimpleContext implements Context {
        private final Map<String, String> props = new java.util.HashMap<>();
        private final Map<String, Object> objects = new java.util.HashMap<>();

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
            return new java.util.HashMap<>(props);
        }

        @Override
        public Object getObject(String key) {
            return objects.get(key);
        }

        @Override
        public void putObject(String key, Object value) {
            objects.put(key, value);
        }

        // -----------------------------------------------------------------
        // The remaining Context methods are not needed for the current tests.
        // -----------------------------------------------------------------
        @Override public ch.qos.logback.core.spi.StatusManager getStatusManager() { return null; }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    private SimpleContext simpleContext;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    /* --------------------------------------------------------------------- */
    /* Constructor tests                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorWithContextOnly() {
        ModelInterpretationContext ctx = new ModelInterpretationContext(simpleContext);
        assertNotNull(ctx);
        assertSame(simpleContext, ctx.getContext());
        assertTrue(ctx.isObjectStackEmpty());
        assertTrue(ctx.isModelStackEmpty());
    }

    @Test
    public void testConstructorWithContextAndHint() {
        Object hint = new Object();
        ModelInterpretationContext ctx = new ModelInterpretationContext(simpleContext, hint);
        assertSame(hint, ctx.getConfiguratorHint());
    }

    @Test
    public void testCopyConstructorCopiesMapsAndRegistry() {
        // Populate original MIC
        mic.addImport("List", "java.util.List");
        mic.addSubstitutionProperty("key1", "value1");
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "dep"));
        mic.markStartOfNamedDependee("myDep");

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        assertEquals("value1", copy.getProperty("key1"));
        assertEquals("java.util.List", copy.getImport("List"));
        assertTrue(copy.isNamedDependeeStarted("myDep"));
        assertEquals(1, copy.getDependencyDefinitions().size());

        // Ensure that modifying the copy does not affect the original
        copy.addImport("Set", "java.util.Set");
        assertNull(mic.getImport("Set"));
    }

    /* --------------------------------------------------------------------- */
    /* Stack handling (model & object)                                        */
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
        assertEquals(o1, mic.getObject(0));
        assertEquals(o1, mic.popObject());

        assertTrue(mic.isObjectStackEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* Property handling                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddSubstitutionPropertyNormal() {
        mic.addSubstitutionProperty("pKey", "  someValue  ");
        assertEquals("someValue", mic.getProperty("pKey"));
    }

    @Test
    public void testAddSubstitutionPropertyNullKeyOrValue() {
        mic.addSubstitutionProperty(null, "value");
        mic.addSubstitutionProperty("key", null);
        assertNull(mic.getProperty(null));
        assertNull(mic.getProperty("key"));
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
    public void testAddSubstitutionPropertiesNull() {
        mic.addSubstitutionProperties(null); // should be a no‑op, no exception
        // Ensure map is still empty
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    @Test
    public void testGetPropertyPrefersLocalMapOverContext() {
        simpleContext.putProperty("ctxKey", "ctxValue");
        mic.addSubstitutionProperty("ctxKey", "localValue");
        assertEquals("localValue", mic.getProperty("ctxKey"));
    }

    @Test
    public void testGetCopyOfPropertyMapIsIndependent() {
        mic.addSubstitutionProperty("k", "v");
        Map<String,String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v", copy.get("k"));
        copy.put("k", "changed");
        // original map must stay unchanged
        assertEquals("v", mic.getProperty("k"));
    }

    /* --------------------------------------------------------------------- */
    /* Substitution (OptionHelper)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSubstNullReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubstSimpleVariableReplacement() {
        // Property defined in MIC's property map
        mic.addSubstitutionProperty("myVar", "replaced");
        String result = mic.subst("${myVar}");
        assertEquals("replaced", result);
    }

    @Test
    public void testSubstVariableNotFoundReturnsOriginal() {
        String input = "${unknown}";
        String result = mic.subst(input);
        // OptionHelper leaves the token unchanged when it cannot resolve it
        assertEquals(input, result);
    }

    @Test
    public void testSubstWhenOptionHelperThrowsException() {
        // Force an illegal argument by using a malformed token that causes
        // OptionHelper.substVars to throw IllegalArgumentException.
        // The implementation catches the exception, logs an error and returns
        // the original string.
        String malformed = "${unclosed";
        String result = mic.subst(malformed);
        assertEquals(malformed, result);
    }

    /* --------------------------------------------------------------------- */
    /* Appender bag creation                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateAppenderBagsPopulatesObjectMap() {
        mic.createAppenderBags();
        Map<String, Object> objMap = mic.getObjectMap();
        assertTrue(objMap.containsKey(JoranConstants.APPENDER_BAG));
        assertTrue(objMap.containsKey(JoranConstants.APPENDER_REF_BAG));

        assertTrue(objMap.get(JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(objMap.get(JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    /* --------------------------------------------------------------------- */
    /* Dependency handling                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDep");
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));
    }

    @Test
    public void testGetDependeeNamesForModel() {
        Model modelA = new Model();
        Model modelB = new Model();

        mic.addDependencyDefinition(new DependencyDefinition(modelA, "dep1"));
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "dep2"));
        mic.addDependencyDefinition(new DependencyDefinition(modelB, "otherDep"));

        List<String> depsForA = mic.getDependeeNamesForModel(modelA);
        assertEquals(2, depsForA.size());
        assertTrue(depsForA.contains("dep1"));
        assertTrue(depsForA.contains("dep2"));

        List<String> depsForB = mic.getDependeeNamesForModel(modelB);
        assertEquals(1, depsForB.size());
        assertEquals("otherDep", depsForB.get(0));
    }

    @Test
    public void testHasDependersNormalAndEdgeCases() {
        Model model = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(model, "target"));

        // normal case
        assertTrue(mic.hasDependers("target"));

        // non‑existing dependee
        assertFalse(mic.hasDependers("nonexistent"));

        // null or empty strings – the method creates an IllegalArgumentException
        // but does not throw it; it simply returns false.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void testMarkStartOfNamedDependeeAndQuery() {
        mic.markStartOfNamedDependee("first");
        assertTrue(mic.isNamedDependeeStarted("first"));
        assertFalse(mic.isNamedDependeeStarted("second"));
    }

    /* --------------------------------------------------------------------- */
    /* Import handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveImport() {
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
        Map<String,String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Map", copy.get("Map"));
        copy.put("Map", "changed");
        // Original map must stay unchanged
        assertEquals("java.util.Map", mic.getImport("Map"));
    }

    /* --------------------------------------------------------------------- */
    /* Miscellaneous getters / setters                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTopModelGetterSetter() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }

    @Test
    public void testConfiguratorHintGetterSetter() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    @Test
    public void testBeanDescriptionCacheIsNotNull() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    @Test
    public void testDefaultNestedComponentRegistryIsAccessible() {
        DefaultNestedComponentRegistry registry = mic.getDefaultNestedComponentRegistry();
        assertNotNull(registry);
        // Simple sanity check – adding a dummy component should not throw
        registry.add(ModelInterpretationContextTest.class, "dummy", ModelInterpretationContextTest.class);
    }

    @Test
    public void testObjectMapReferenceIsMutable() {
        Map<String, Object> map = mic.getObjectMap();
        map.put("key", "value");
        assertEquals("value", mic.getObjectMap().get("key"));
    }
}
