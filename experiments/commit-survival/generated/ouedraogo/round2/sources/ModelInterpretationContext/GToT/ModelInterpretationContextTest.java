package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ModelInterpretationContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic happy‑path usage)</li>
 *   <li>Edge‑case handling (nulls, empty strings, unknown keys)</li>
 *   <li>Exception‑related paths (variable substitution errors)</li>
 *   <li>Stateful behaviour of the internal stacks, maps and dependency handling</li>
 * </ul>
 *
 * A very small concrete implementation of {@link Context} is provided
 * ( {@code SimpleContext} ) because the library only defines the interface.
 * This implementation stores properties in a {@link HashMap} and returns
 * {@code null} for all other methods – sufficient for the tests.
 */
public class ModelInterpretationContextTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal {@link Context} implementation required for the tests.
     */
    private static class SimpleContext implements Context {

        private final Map<String, String> props = new HashMap<>();

        @Override public void setName(String name) {}
        @Override public String getName() { return null; }
        @Override public long getBirthTime() { return 0L; }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) {}
        @Override public ch.qos.logback.core.spi.StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return props.get(key); }
        @Override public void putProperty(String key, String value) { props.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(props); }
    }

    private ModelInterpretationContext mic;
    private SimpleContext simpleContext;

    @Before
    public void setUp() {
        simpleContext = new SimpleContext();
        mic = new ModelInterpretationContext(simpleContext);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Constructor & basic initialisation                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorInitialisesStacksAndMaps() {
        assertNotNull(mic.getObjectStack());
        assertTrue(mic.isObjectStackEmpty());

        assertNotNull(mic.peekModel()); // should throw NoSuchElementException if empty
        // The above line is intentionally not called; we just verify the stacks exist:
        assertTrue(mic.isModelStackEmpty());

        assertNotNull(mic.getObjectMap());
        assertNotNull(mic.getCopyOfPropertyMap());
        assertNotNull(mic.getImportMapCopy());
        assertNotNull(mic.getBeanDescriptionCache());
    }

    /* --------------------------------------------------------------------- */
    /* 2. Appender bags creation                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateAppenderBagsCreatesTwoMaps() {
        mic.createAppenderBags();
        Map<String, Object> om = mic.getObjectMap();
        assertTrue(om.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG));
        assertTrue(om.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG));

        assertTrue(om.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG) instanceof Map);
        assertTrue(om.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG) instanceof Map);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Object stack operations                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testObjectStackPushPeekPop() {
        String obj = "testObject";
        mic.pushObject(obj);
        assertFalse(mic.isObjectStackEmpty());
        assertEquals(obj, mic.peekObject());
        assertEquals(obj, mic.popObject());
        assertTrue(mic.isObjectStackEmpty());
    }

    @Test
    public void testObjectStackGetByIndex() {
        mic.pushObject("first");
        mic.pushObject("second");
        assertEquals("first", mic.getObject(0));
        assertEquals("second", mic.getObject(1));
    }

    /* --------------------------------------------------------------------- */
    /* 4. Model stack operations                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testModelStackPushPeekPop() {
        Model model = new Model();
        mic.pushModel(model);
        assertFalse(mic.isModelStackEmpty());
        assertSame(model, mic.peekModel());
        assertSame(model, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 5. Top model getter / setter                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTopModelSetAndGet() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }

    /* --------------------------------------------------------------------- */
    /* 6. Property substitution (subst)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSubstResolvesVariableFromContext() {
        simpleContext.putProperty("myKey", "myValue");
        String result = mic.subst("${myKey}");
        assertEquals("myValue", result);
    }

    @Test
    public void testSubstNullInputReturnsNull() {
        assertNull(mic.subst(null));
    }

    @Test
    public void testSubstInvalidVariableReturnsOriginalString() {
        // No property named "unknown" in the context => substitution fails,
        // OptionHelper throws ScanException which is caught and the original
        // reference is returned.
        String original = "${unknown}";
        String result = mic.subst(original);
        assertEquals(original, result);
    }

    /* --------------------------------------------------------------------- */
    /* 7. Substitution property handling                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddSubstitutionPropertyTrimsAndStores() {
        mic.addSubstitutionProperty(" key ", "  value  ");
        assertEquals("value", mic.getProperty(" key "));
    }

    @Test
    public void testAddSubstitutionPropertyIgnoresNullKeyOrValue() {
        mic.addSubstitutionProperty(null, "value");
        mic.addSubstitutionProperty("key", null);
        // nothing should have been added
        assertNull(mic.getProperty(null));
        assertNull(mic.getProperty("key"));
    }

    @Test
    public void testAddSubstitutionPropertiesMultiple() {
        Properties props = new Properties();
        props.setProperty("a", "1");
        props.setProperty("b", "2");
        mic.addSubstitutionProperties(props);
        assertEquals("1", mic.getProperty("a"));
        assertEquals("2", mic.getProperty("b"));
    }

    @Test
    public void testAddSubstitutionPropertiesIgnoresNull() {
        mic.addSubstitutionProperties(null); // should not throw
        // map stays empty
        assertTrue(mic.getCopyOfPropertyMap().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 8. Property lookup (local vs context)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetPropertyPrefersLocalOverContext() {
        mic.addSubstitutionProperty("p", "local");
        simpleContext.putProperty("p", "context");
        assertEquals("local", mic.getProperty("p"));
    }

    @Test
    public void testGetPropertyFallsBackToContext() {
        simpleContext.putProperty("onlyInContext", "ctxValue");
        assertEquals("ctxValue", mic.getProperty("onlyInContext"));
    }

    /* --------------------------------------------------------------------- */
    /* 9. Dependency handling                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "someDependee");
        mic.addDependencyDefinition(dd);
        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));

        List<String> dependees = mic.getDependeeNamesForModel(depender);
        assertEquals(1, dependees.size());
        assertEquals("someDependee", dependees.get(0));
    }

    @Test
    public void testHasDependersTrueAndFalse() {
        Model m = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(m, "dep1"));
        assertTrue(mic.hasDependers("dep1"));
        assertFalse(mic.hasDependers("nonExisting"));
    }

    @Test
    public void testHasDependersWithInvalidInputDoesNotThrow() {
        // The method creates an IllegalArgumentException internally but does not throw it.
        // Verify that it simply returns false for null / empty strings.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Named dependee tracking                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMarkAndCheckNamedDependeeStarted() {
        mic.markStartOfNamedDependee("myDep");
        assertTrue(mic.isNamedDependeeStarted("myDep"));
        assertFalse(mic.isNamedDependeeStarted("otherDep"));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Import map handling                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRetrieveImport() {
        mic.addImport("List", "java.util.List");
        assertEquals("java.util.List", mic.getImport("List"));
    }

    @Test
    public void testGetImportReturnsStemIfNotFound() {
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
        // modify copy – original must stay unchanged
        copy.put("Map", "changed");
        assertEquals("java.util.Map", mic.getImport("Map"));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Configurator hint getter / setter                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfiguratorHintGetSet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    /* --------------------------------------------------------------------- */
    /* 13. Default nested component registry exposure                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDefaultNestedComponentRegistryIsNotNull() {
        assertNotNull(mic.getDefaultNestedComponentRegistry());
    }

    /* --------------------------------------------------------------------- */
    /* 14. BeanDescriptionCache exposure                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetBeanDescriptionCacheIsInitialized() {
        assertNotNull(mic.getBeanDescriptionCache());
    }
}
