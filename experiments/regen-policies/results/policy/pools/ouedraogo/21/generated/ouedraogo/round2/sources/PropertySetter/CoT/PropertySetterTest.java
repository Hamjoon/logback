package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic happy‑path usage)</li>
 *   <li>Edge cases such as null values, missing setters/adders, conversion failures,
 *       and type‑mismatch errors.</li>
 * </ul>
 *
 * A tiny {@link TestContext} implementation is provided so that the
 * {@link ContextAwareBase} functionality (addInfo / addWarn / addError) can be
 * exercised without pulling in the full Logback infrastructure.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used in the tests                                      */
    /* --------------------------------------------------------------------- */

    /** Very small {@link Context} implementation – only the methods required
     *  by the base classes are implemented. All other methods return {@code null}
     *  or sensible defaults. */
    private static class TestContext implements Context {
        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public java.util.Map<String, String> getCopyOfPropertyMap() { return null; }
        @Override public String getName() { return "test-context"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
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
    }

    /** Simple bean used for the various setter / adder tests. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean male;
        private Config config;
        private final List<Child> children = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();

        /* ----- setters ----- */
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }
        public void setConfig(Config config) { this.config = config; }

        /* ----- adders ----- */
        public void addChild(Child child) { this.children.add(child); }
        public void addTag(String tag) { this.tags.add(tag); }

        /* ----- getters for verification ----- */
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public Config getConfig() { return config; }
        public List<Child> getChildren() { return children; }
        public List<String> getTags() { return tags; }
    }

    /** Dummy complex property class used with {@code addComplexProperty} /
     *  {@code setComplexProperty}. */
    public static class Config { }

    /** Another dummy class – used to provoke “type not assignable” errors. */
    public static class WrongType { }

    /** Simple child class for collection‑adder tests. */
    public static class Child { }

    /* --------------------------------------------------------------------- */
    /* Test fixtures                                                          */
    /* --------------------------------------------------------------------- */

    private BeanDescriptionCache beanCache;
    private SimpleBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        // a minimal context – sufficient for the base classes
        Context ctx = new TestContext();

        // BeanDescriptionCache needs a Context; the implementation will lazily
        // create BeanDescription objects via reflection.
        beanCache = new BeanDescriptionCache(ctx);

        bean = new SimpleBean();
        setter = new PropertySetter(beanCache, bean);
        // make sure the PropertySetter has a context (otherwise addWarn/addError
        // may silently drop messages)
        setter.setContext(ctx);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Basic happy‑path tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyString() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetPropertyIntConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetPropertyBooleanConversion() {
        setter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    @Test
    public void testAddBasicPropertySuccess() {
        setter.addBasicProperty("tag", "alpha");
        assertEquals(1, bean.getTags().size());
        assertEquals("alpha", bean.getTags().get(0));
    }

    @Test
    public void testAddComplexPropertySuccess() {
        Child child = new Child();
        setter.addComplexProperty("child", child);
        assertEquals(1, bean.getChildren().size());
        assertSame(child, bean.getChildren().get(0));
    }

    @Test
    public void testSetComplexPropertySuccess() {
        Config cfg = new Config();
        setter.setComplexProperty("config", cfg);
        assertSame(cfg, bean.getConfig());
    }

    @Test
    public void testGetObjClass() {
        assertEquals(SimpleBean.class, setter.getObjClass());
    }

    @Test
    public void testComputeAggregationTypeNotFound() {
        // “unknown” does not correspond to any setter/adder in SimpleBean
        AggregationType type = setter.computeAggregationType("unknown");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testGetClassNameViaImplicitRulesReturnsNullWhenNoRule() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> clazz = setter.getClassNameViaImplicitRules("nonexistent",
                AggregationType.NOT_FOUND, registry);
        assertNull(clazz);
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Edge‑case / error‑handling tests                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        // set a known property first, then try to set it with null – it must stay unchanged
        bean.setName("initial");
        setter.setProperty("name", null);
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetPropertyWhenNoSetterExists() {
        // No setter for “unknown” – the method should swallow the situation
        // and not throw any exception.
        setter.setProperty("unknown", "value");
        // No state change to verify – the test passes if no exception is thrown.
    }

    @Test
    public void testSetPropertyConversionFailure() {
        // setAge expects an int; passing a non‑numeric string should trigger a
        // PropertySetterException which is caught inside setProperty().
        bean.setAge(99); // initial value
        setter.setProperty("age", "notANumber");
        // value must stay unchanged
        assertEquals(99, bean.getAge());
    }

    @Test
    public void testAddBasicPropertyWithNullValueDoesNothing() {
        setter.addBasicProperty("tag", null);
        assertTrue(bean.getTags().isEmpty());
    }

    @Test
    public void testAddBasicPropertyWhenAdderMissing() {
        // “foo” has no corresponding addFoo(String) method.
        setter.addBasicProperty("foo", "bar");
        // No exception – just a logged error. Verify that nothing was added.
        assertTrue(bean.getTags().isEmpty());
    }

    @Test
    public void testAddComplexPropertyWhenAdderMissing() {
        // No addFoo(…) method for property “foo”.
        setter.addComplexProperty("foo", new Object());
        // Nothing should be added and no exception should escape.
    }

    @Test
    public void testAddComplexPropertyWithWrongType() {
        // addChild expects a Child, we give a WrongType.
        setter.addComplexProperty("child", new WrongType());
        // The list must stay empty because the type check fails.
        assertTrue(bean.getChildren().isEmpty());
    }

    @Test
    public void testSetComplexPropertyWhenSetterMissing() {
        // No setFoo(…) method exists.
        setter.setComplexProperty("foo", new Object());
        // No exception – just a warning internally.
    }

    @Test
    public void testSetComplexPropertyWithWrongType() {
        // setConfig expects Config, we give WrongType.
        setter.setComplexProperty("config", new WrongType());
        // Config must remain null.
        assertNull(bean.getConfig());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Public method signatures (extracted for documentation)          */
    /* --------------------------------------------------------------------- */
    // The signatures are listed here for completeness; they are not part of
    // the executable test code but serve as a quick reference.

    /*
        public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
        public void setContext(Context context)
        public void setProperty(String name, String value)
        public AggregationType computeAggregationType(String name)
        public Class<?> getObjClass()
        public void addComplexProperty(String name, Object complexProperty)
        public void addBasicProperty(String name, String strValue)
        public void setComplexProperty(String name, Object complexProperty)
        public Object getObj()
        public Class<?> getClassNameViaImplicitRules(String name,
                                                     AggregationType aggregationType,
                                                     DefaultNestedComponentRegistry registry)
    */

}
