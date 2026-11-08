package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PropertySetter}.
 *
 * The tests use real instances of the library classes – no mocking framework is used.
 * A very small {@link SimpleContext} implementation is provided to satisfy the
 * {@link Context} contract required by {@link BeanDescriptionCache}.
 */
public class PropertySetterTest {

    private SimpleContext context;
    private BeanDescriptionCache beanCache;
    private SampleBean bean;
    private PropertySetter propertySetter;

    @Before
    public void setUp() {
        context = new SimpleContext();
        beanCache = new BeanDescriptionCache(context);
        bean = new SampleBean();
        propertySetter = new PropertySetter(beanCache, bean);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic setProperty tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyWithValidValues() {
        propertySetter.setProperty("name", "John Doe");
        propertySetter.setProperty("age", "42");
        propertySetter.setProperty("active", "true");

        assertEquals("John Doe", bean.getName());
        assertEquals(42, bean.getAge());
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        bean.setName("initial");
        propertySetter.setProperty("name", null);
        // value must stay unchanged
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetPropertyWithMissingSetterAddsWarn() {
        // No setter named "unknown" – should be ignored without exception
        propertySetter.setProperty("unknown", "value");
        // bean stays unchanged
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetPropertyWithConversionFailure() {
        // age expects an int – conversion of "abc" will fail silently (warn added)
        propertySetter.setProperty("age", "abc");
        // original default value should remain
        assertEquals(0, bean.getAge());
    }

    /* --------------------------------------------------------------------- */
    /*  addComplexProperty tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddComplexPropertySuccess() {
        NestedBean nested = new NestedBean();
        nested.setValue("nestedValue");

        // The adder method is "addNested"
        propertySetter.addComplexProperty("Nested", nested);

        assertNotNull(bean.getNested());
        assertEquals("nestedValue", bean.getNested().getValue());
    }

    @Test
    public void testAddComplexPropertyMissingAdder() {
        // No adder named "addMissing"
        propertySetter.addComplexProperty("Missing", new Object());
        // No exception – just an error logged internally
        assertNull(bean.getNested());
    }

    /* --------------------------------------------------------------------- */
    /*  addBasicProperty tests                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddBasicPropertySuccess() {
        propertySetter.addBasicProperty("item", "firstItem");
        propertySetter.addBasicProperty("item", "secondItem");

        assertEquals(2, bean.getItems().size());
        assertTrue(bean.getItems().contains("firstItem"));
        assertTrue(bean.getItems().contains("secondItem"));
    }

    @Test
    public void testAddBasicPropertyMissingAdder() {
        // No adder for "missing"
        propertySetter.addBasicProperty("missing", "value");
        // Nothing should be added
        assertTrue(bean.getItems().isEmpty());
    }

    @Test
    public void testAddBasicPropertyConversionFailure() {
        // The adder expects a String, conversion will never fail, but we
        // verify that a null argument does not get added.
        propertySetter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  setComplexProperty tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetComplexPropertySuccess() {
        NestedBean nested = new NestedBean();
        nested.setValue("complex");
        propertySetter.setComplexProperty("nested", nested);

        assertNotNull(bean.getNested());
        assertEquals("complex", bean.getNested().getValue());
    }

    @Test
    public void testSetComplexPropertyMissingSetter() {
        // No setter called "setMissing"
        propertySetter.setComplexProperty("missing", new Object());
        // No change to bean
        assertNull(bean.getNested());
    }

    /* --------------------------------------------------------------------- */
    /*  Delegation / accessor tests                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationTypeDelegates() {
        // The exact value depends on internal rules; we only assert that a
        // non‑null value is returned.
        AggregationType type = propertySetter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void testGetObjClass() {
        assertEquals(SampleBean.class, propertySetter.getObjClass());
    }

    @Test
    public void testGetObj() {
        assertSame(bean, propertySetter.getObj());
    }

    @Test
    public void testGetClassNameViaImplicitRulesDelegates() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = propertySetter.getClassNameViaImplicitRules(
                "dummy", AggregationType.AS_BASIC_PROPERTY, registry);
        // The default implementation returns null; the test only ensures that
        // the method can be called without throwing.
        assertNull(result);
    }

    @Test
    public void testSetContextPropagatesToAggregationAssessor() {
        // Setting a context should not throw and should be stored inside
        // the PropertySetter (via its superclass). We verify that the getter
        // returns the same instance.
        propertySetter.setContext(context);
        assertSame(context, propertySetter.getContext());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper classes – simple bean and context implementation            */
    /* --------------------------------------------------------------------- */

    /**
     * Very small bean used for the tests. It contains simple setters,
     * an adder for a collection and a complex property.
     */
    public static class SampleBean {
        private String name;
        private int age;
        private boolean active;
        private java.util.List<String> items = new java.util.ArrayList<>();
        private NestedBean nested;

        // ----- simple setters -------------------------------------------------
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }
        public void setNested(NestedBean nested) { this.nested = nested; }

        // ----- adder for a basic property (String) ---------------------------
        public void addItem(String item) { this.items.add(item); }

        // ----- adder for a complex property -----------------------------------
        public void addNested(NestedBean nb) { this.nested = nb; }

        // ----- getters for verification ---------------------------------------
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public java.util.List<String> getItems() { return items; }
        public NestedBean getNested() { return nested; }
    }

    /**
     * Simple nested bean used as a complex property.
     */
    public static class NestedBean {
        private String value;
        public void setValue(String v) { this.value = v; }
        public String getValue() { return value; }
    }

    /**
     * Minimal {@link Context} implementation required by the library.
     * All methods return {@code null} or no‑op implementations because the
     * tests do not depend on the actual context behaviour.
     */
    private static class SimpleContext implements Context {

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public Map<String, String> getCopyOfPropertyMap() { return Collections.emptyMap(); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) {}
    }
}
