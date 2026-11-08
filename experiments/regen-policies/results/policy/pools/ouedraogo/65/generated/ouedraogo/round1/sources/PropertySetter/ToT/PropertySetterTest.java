package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests exercise:
 * <ul>
 *   <li>basic property setting with type conversion (String, int, boolean)</li>
 *   <li>behaviour when a setter is missing</li>
 *   <li>behaviour when conversion fails</li>
 *   <li>adding complex properties via adder methods</li>
 *   <li>adding basic (String) properties via adder methods</li>
 *   <li>setting complex properties via setter methods</li>
 *   <li>aggregation type computation</li>
 *   <li>implicit‑rule class name resolution</li>
 *   <li>simple getters (obj, objClass)</li>
 * </ul>
 *
 * No mocking frameworks are used – only concrete classes that are part of the
 * public API listed in the prompt.  A minimal {@link Context} implementation
 * is provided to satisfy the constructor of {@link BeanDescriptionCache}.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- *
     *  Helper classes used as test fixtures
     * --------------------------------------------------------------------- */

    /** Minimal {@link Context} implementation – all methods are no‑ops or return
     *  simple defaults.  It is sufficient for the constructors of the classes
     *  under test. */
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
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    /** Simple POJO used as the target of {@link PropertySetter}. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> tags = new ArrayList<>();
        private final List<Child> children = new ArrayList<>();
        private Child child; // for setComplexProperty test

        // ----- basic setters -------------------------------------------------
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }

        // ----- adder for basic (String) property ----------------------------
        public void addTag(String tag) { this.tags.add(tag); }
        public List<String> getTags() { return tags; }

        // ----- adder for complex property ------------------------------------
        public void addChild(Child child) { this.children.add(child); }
        public List<Child> getChildren() { return children; }

        // ----- setter for complex property -----------------------------------
        public void setChild(Child child) { this.child = child; }
        public Child getChild() { return child; }

        // ----- getters for verification --------------------------------------
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
    }

    /** Simple complex type used in collection/complex‑property tests. */
    public static class Child {
        private final String id;
        public Child(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /* --------------------------------------------------------------------- *
     *  Test fixture fields
     * --------------------------------------------------------------------- */

    private SimpleContext context;
    private BeanDescriptionCache beanCache;
    private SimpleBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        // initialise a minimal context and the cache that the PropertySetter needs
        context = new SimpleContext();
        beanCache = new BeanDescriptionCache(context);
        bean = new SimpleBean();
        setter = new PropertySetter(beanCache, bean);
    }

    /* --------------------------------------------------------------------- *
     *  1. setProperty – basic type conversion
     * --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_StringConversion() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetProperty_IntConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_BooleanConversion() {
        setter.setProperty("active", "true");
        assertTrue(bean.isActive());

        // also test false
        setter.setProperty("active", "false");
        assertFalse(bean.isActive());
    }

    /* --------------------------------------------------------------------- *
     *  2. setProperty – missing setter (should not throw)
     * --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_MissingSetterIsIgnored() {
        // there is no setFoo(String) method on SimpleBean
        setter.setProperty("foo", "bar");
        // no exception, object state unchanged
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertFalse(bean.isActive());
    }

    /* --------------------------------------------------------------------- *
     *  3. setProperty – conversion failure (non‑numeric int)
     * --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_InvalidIntConversionDoesNotChangeState() {
        // age expects an int; providing a non‑numeric string triggers conversion failure
        setter.setProperty("age", "not-a-number");
        // original value (default 0) must stay unchanged
        assertEquals(0, bean.getAge());
    }

    /* --------------------------------------------------------------------- *
     *  4. addComplexProperty – uses adder method that accepts a complex type
     * --------------------------------------------------------------------- */

    @Test
    public void testAddComplexProperty_UsesAdder() {
        Child child = new Child("c1");
        setter.addComplexProperty("Child", child); // method name is capitalized as per API
        assertEquals(1, bean.getChildren().size());
        assertSame(child, bean.getChildren().get(0));
    }

    /* --------------------------------------------------------------------- *
     *  5. addBasicProperty – adds a basic (String) property via adder
     * --------------------------------------------------------------------- */

    @Test
    public void testAddBasicProperty_UsesAdder() {
        setter.addBasicProperty("tag", "firstTag");
        assertEquals(1, bean.getTags().size());
        assertEquals("firstTag", bean.getTags().get(0));
    }

    @Test
    public void testAddBasicProperty_NullValueIsIgnored() {
        setter.addBasicProperty("tag", null);
        // No tag should be added
        assertTrue(bean.getTags().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  6. setComplexProperty – uses regular setter that accepts a complex type
     * --------------------------------------------------------------------- */

    @Test
    public void testSetComplexProperty_UsesSetter() {
        Child child = new Child("single");
        setter.setComplexProperty("child", child);
        assertSame(child, bean.getChild());
    }

    @Test
    public void testSetComplexProperty_WrongTypeIsRejected() {
        // Trying to set a String where a Child is expected – should log an error but not throw
        setter.setComplexProperty("child", "I am not a Child");
        // The bean's child field must remain null
        assertNull(bean.getChild());
    }

    /* --------------------------------------------------------------------- *
     *  7. computeAggregationType – basic sanity check (non‑null result)
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationType_ReturnsNonNull() {
        AggregationType type = setter.computeAggregationType("name");
        assertNotNull(type);
    }

    /* --------------------------------------------------------------------- *
     *  8. getClassNameViaImplicitRules – verifies delegation to AggregationAssessor
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameViaImplicitRules_DelegatesCorrectly() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Register a default component type for the property "child"
        registry.add(SimpleBean.class, "child", Child.class);

        AggregationType aggType = AggregationType.AS_COMPLEX_PROPERTY; // any valid enum value
        Class<?> resolved = setter.getClassNameViaImplicitRules("child", aggType, registry);
        assertEquals(Child.class, resolved);
    }

    /* --------------------------------------------------------------------- *
     *  9. Simple getters – obj and objClass
     * --------------------------------------------------------------------- */

    @Test
    public void testGettersReturnCorrectReferences() {
        assertSame(bean, setter.getObj());
        assertEquals(SimpleBean.class, setter.getObjClass());
    }
}
