package ch.qos.logback.core.model.processor;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.AppenderAttachable;

/**
 * Unit tests for {@link ModelInterpretationContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (including constructors)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑cases such as null arguments, empty strings and out‑of‑bounds accesses</li>
 *   <li>Exception handling (or the lack thereof) for methods that create objects but do not throw</li>
 * </ul>
 *
 * A very small {@link Context} implementation is provided locally so that
 * the tests do not depend on the full Logback runtime.
 */
public class ModelInterpretationContextTest {

    /** Simple {@link Context} implementation used only for the tests. */
    private static class SimpleContext implements Context {
        private final Map<String, Object> objectMap = new HashMap<>();
        private final Map<String, String> propertyMap = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objectMap.get(key); }
        @Override public void putObject(String key, Object value) { objectMap.put(key, value); }
        @Override public String getProperty(String key) { return propertyMap.get(key); }
        @Override public void putProperty(String key, String value) { propertyMap.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(propertyMap); }
        @Override public String getName() { return "testContext"; }
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
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.event.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.event.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    private Context context;
    private ModelInterpretationContext mic;

    @Before
    public void setUp() {
        context = new SimpleContext();
        mic = new ModelInterpretationContext(context);
    }

    // -------------------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------------------

    @Test
    public void constructor_withContext_only_initialisesFields() {
        ModelInterpretationContext local = new ModelInterpretationContext(context);
        assertNotNull(local.getObjectMap());
        assertNotNull(local.getObjectStack());
        assertNotNull(local.getTopModel()); // initially null
        assertNotNull(local.getBeanDescriptionCache());
        assertTrue(local.isObjectStackEmpty());
        assertTrue(local.isModelStackEmpty());
    }

    @Test
    public void constructor_withContextAndHint_storesHint() {
        Object hint = new Object();
        ModelInterpretationContext local = new ModelInterpretationContext(context, hint);
        assertSame(hint, local.getConfiguratorHint());
    }

    @Test
    public void copyConstructor_copiesImportMap_andVariableSubstitutions() {
        mic.addImport("List", "java.util.List");
        mic.addSubstitutionProperty("key", "value");

        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // import map must be a deep copy
        assertEquals("java.util.List", copy.getImport("List"));
        // variable substitution must be copied
        assertEquals("value", copy.subst("${key}"));
        // ensure that modifying the original does not affect the copy
        mic.addImport("Map", "java.util.Map");
        assertNull(copy.getImport("Map"));
    }

    // -------------------------------------------------------------------------
    // Object‑stack related methods
    // -------------------------------------------------------------------------

    @Test
    public void objectStack_pushPeekPop_behaveAsLIFO() {
        String a = "first";
        Integer b = 42;
        mic.pushObject(a);
        mic.pushObject(b);

        assertFalse(mic.isObjectStackEmpty());
        assertSame(b, mic.peekObject());

        assertSame(b, mic.popObject());
        assertSame(a, mic.peekObject());
        assertEquals(a, mic.getObject(0));
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void objectStack_peekWhenEmpty_throwsException() {
        mic.peekObject();
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void objectStack_popWhenEmpty_throwsException() {
        mic.popObject();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void objectStack_getInvalidIndex_throwsException() {
        mic.pushObject("onlyOne");
        mic.getObject(5);
    }

    // -------------------------------------------------------------------------
    // Model‑stack related methods
    // -------------------------------------------------------------------------

    @Test
    public void modelStack_pushPeekPop_behaveAsLIFO() {
        Model m1 = new Model();
        Model m2 = new Model();

        mic.pushModel(m1);
        mic.pushModel(m2);

        assertFalse(mic.isModelStackEmpty());
        assertSame(m2, mic.peekModel());

        assertSame(m2, mic.popModel());
        assertSame(m1, mic.peekModel());
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void modelStack_peekWhenEmpty_throwsException() {
        mic.peekModel();
    }

    @Test(expected = java.util.EmptyStackException.class)
    public void modelStack_popWhenEmpty_throwsException() {
        mic.popModel();
    }

    // -------------------------------------------------------------------------
    // Top‑model getter / setter
    // -------------------------------------------------------------------------

    @Test
    public void topModel_setAndGet_workCorrectly() {
        Model top = new Model();
        mic.setTopModel(top);
        assertSame(top, mic.getTopModel());
    }

    // -------------------------------------------------------------------------
    // Appender‑bag creation
    // -------------------------------------------------------------------------

    @Test
    public void createAppenderBags_populatesObjectMapWithTwoEntries() {
        mic.createAppenderBags();
        Map<String, Object> map = mic.getObjectMap();
        assertTrue(map.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG));
        assertTrue(map.containsKey(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG));

        Object appenderBag = map.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_BAG);
        Object appenderRefBag = map.get(ch.qos.logback.core.joran.JoranConstants.APPENDER_REF_BAG);
        assertTrue(appenderBag instanceof Map);
        assertTrue(appenderRefBag instanceof Map);
    }

    // -------------------------------------------------------------------------
    // Configurator hint getter / setter
    // -------------------------------------------------------------------------

    @Test
    public void configuratorHint_setAndGet() {
        Object hint = new Object();
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
    }

    // -------------------------------------------------------------------------
    // BeanDescriptionCache getter
    // -------------------------------------------------------------------------

    @Test
    public void getBeanDescriptionCache_returnsNonNullInstance() {
        assertNotNull(mic.getBeanDescriptionCache());
    }

    // -------------------------------------------------------------------------
    // Variable substitution (delegated to VariableSubstitutionsHelper)
    // -------------------------------------------------------------------------

    @Test
    public void subst_delegatesToVariableSubstitutionsHelper() {
        mic.addSubstitutionProperty("myKey", "myValue");
        assertEquals("myValue", mic.subst("${myKey}"));
    }

    // -------------------------------------------------------------------------
    // DefaultNestedComponentRegistry getter
    // -------------------------------------------------------------------------

    @Test
    public void getDefaultNestedComponentRegistry_returnsSameInstance() {
        DefaultNestedComponentRegistry reg1 = mic.getDefaultNestedComponentRegistry();
        DefaultNestedComponentRegistry reg2 = mic.getDefaultNestedComponentRegistry();
        assertSame(reg1, reg2);
    }

    // -------------------------------------------------------------------------
    // Dependency handling
    // -------------------------------------------------------------------------

    @Test
    public void addAndRetrieveDependencyDefinitions() {
        Model depender = new Model();
        DependencyDefinition dd = new DependencyDefinition(depender, "myDependee");
        mic.addDependencyDefinition(dd);

        List<DependencyDefinition> list = mic.getDependencyDefinitions();
        assertEquals(1, list.size());
        assertSame(dd, list.get(0));
    }

    @Test
    public void getDependeeNamesForModel_returnsCorrectNames() {
        Model modelA = new Model();
        Model modelB = new Model();

        mic.addDependencyDefinition(new DependencyDefinition(modelA, "first"));
        mic.addDependencyDefinition(new DependencyDefinition(modelA, "second"));
        mic.addDependencyDefinition(new DependencyDefinition(modelB, "third"));

        List<String> names = mic.getDependeeNamesForModel(modelA);
        assertEquals(2, names.size());
        assertTrue(names.contains("first"));
        assertTrue(names.contains("second"));
        assertFalse(names.contains("third"));
    }

    @Test
    public void hasDependers_nullOrEmptyString_returnsFalseWithoutThrowing() {
        // According to the source code an IllegalArgumentException is *created* but not thrown.
        // The method should simply return false.
        assertFalse(mic.hasDependers(null));
        assertFalse(mic.hasDependers(""));
        assertFalse(mic.hasDependers("   "));
    }

    @Test
    public void hasDependers_existingDependee_returnsTrue() {
        Model model = new Model();
        mic.addDependencyDefinition(new DependencyDefinition(model, "target"));
        assertTrue(mic.hasDependers("target"));
    }

    // -------------------------------------------------------------------------
    // Named dependee tracking
    // -------------------------------------------------------------------------

    @Test
    public void markStartOfNamedDependee_and_isNamedDependeeStarted() {
        String name = "myDependee";
        assertFalse(mic.isNamedDependeeStarted(name));
        mic.markStartOfNamedDependee(name);
        assertTrue(mic.isNamedDependeeStarted(name));
    }

    // -------------------------------------------------------------------------
    // Property handling (substitution properties)
    // -------------------------------------------------------------------------

    @Test
    public void addSubstitutionProperty_and_getProperty_workTogether() {
        mic.addSubstitutionProperty("p1", "v1");
        assertEquals("v1", mic.getProperty("p1"));
    }

    @Test
    public void getCopyOfPropertyMap_returnsIndependentCopy() {
        mic.addSubstitutionProperty("k", "v");
        Map<String, String> copy = mic.getCopyOfPropertyMap();
        assertEquals("v", copy.get("k"));
        // modify the copy – original must stay unchanged
        copy.put("k", "changed");
        assertEquals("v", mic.getProperty("k"));
    }

    // -------------------------------------------------------------------------
    // Import handling
    // -------------------------------------------------------------------------

    @Test
    public void addImport_and_getImport_behaveCorrectly() {
        mic.addImport("List", "java.util.List");
        assertEquals("java.util.List", mic.getImport("List"));
    }

    @Test
    public void getImport_unknownStem_returnsStemItself() {
        assertEquals("UnknownStem", mic.getImport("UnknownStem"));
    }

    @Test
    public void getImport_nullStem_returnsNull() {
        assertNull(mic.getImport(null));
    }

    @Test
    public void getImportMapCopy_isIndependent() {
        mic.addImport("Set", "java.util.Set");
        Map<String, String> copy = mic.getImportMapCopy();
        assertEquals("java.util.Set", copy.get("Set"));
        // Mutate the copy – original map must not change
        copy.put("Set", "changed");
        assertEquals("java.util.Set", mic.getImport("Set"));
    }

    // -------------------------------------------------------------------------
    // Configurator supplier handling
    // -------------------------------------------------------------------------

    @Test
    public void configuratorSupplier_setAndGet() {
        Supplier<GenericXMLConfigurator> supplier = () -> null;
        mic.setConfiguratorSupplier(supplier);
        assertSame(supplier, mic.getConfiguratorSupplier());
    }

    // -------------------------------------------------------------------------
    // Additional sanity checks for methods that return unmodifiable collections
    // -------------------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void getDependencyDefinitions_returnsUnmodifiableList() {
        mic.addDependencyDefinition(new DependencyDefinition(new Model(), "x"));
        List<DependencyDefinition> defs = mic.getDependencyDefinitions();
        defs.add(new DependencyDefinition(new Model(), "y")); // should fail
    }

    @Test
    public void getObjectMap_isMutable() {
        Map<String, Object> map = mic.getObjectMap();
        map.put("customKey", "customValue");
        assertEquals("customValue", mic.getObjectMap().get("customKey"));
    }
}
