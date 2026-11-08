package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic property setting (String, int, boolean)</li>
 *   <li>null value handling</li>
 *   <li>basic collection property addition via {@code addBasicProperty}</li>
 *   <li>complex property addition via {@code addComplexProperty}</li>
 *   <li>complex property setting via {@code setComplexProperty}</li>
 *   <li>aggregation type computation</li>
 *   <li>implicit‑rule class lookup</li>
 * </ul>
 *
 * No mocking frameworks are used – only real objects constructed from the
 * public API that appears in the supplied source code.
 */
public class PropertySetterTest {

    /** Simple {@link Context} implementation that satisfies the contract of
     *  {@link ContextAwareBase}.  It provides a no‑op {@link StatusManager}
     *  because the tests never inspect the status messages. */
    private static class SimpleContext implements Context {

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public Map<String, String> getCopyOfPropertyMap() { return Collections.emptyMap(); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return 0L; }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) {}
    }

    /** A tiny bean used throughout the test suite. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean male;
        private TestBean other;
        private final List<String> items = new ArrayList<>();
        private final List<TestBean> children = new ArrayList<>();

        // ----- simple setters -----
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }

        // ----- complex setter -----
        public void setOther(TestBean other) { this.other = other; }

        // ----- adder for basic collection -----
        public void addItem(String item) { this.items.add(item); }

        // ----- adder for complex collection -----
        public void addChild(TestBean child) { this.children.add(child); }

        // ----- getters for verification -----
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public TestBean getOther() { return other; }
        public List<String> getItems() { return items; }
        public List<TestBean> getChildren() { return children; }
    }

    private BeanDescriptionCache beanDescriptionCache;
    private TestBean testBean;
    private PropertySetter propertySetter;

    @Before
    public void setUp() {
        // initialise a BeanDescriptionCache with a dummy Context
        beanDescriptionCache = new BeanDescriptionCache(new SimpleContext());

        // the bean on which properties will be set
        testBean = new TestBean();

        // the class under test
        propertySetter = new PropertySetter(beanDescriptionCache, testBean);
    }

    /* --------------------------------------------------------------
     *  1.  Basic property setting (String, int, boolean)
     * -------------------------------------------------------------- */
    @Test
    public void testSetPropertyBasicTypes() {
        propertySetter.setProperty("name", "John Doe");
        propertySetter.setProperty("age", "42");
        propertySetter.setProperty("male", "true");

        assertEquals("John Doe", testBean.getName());
        assertEquals(42, testBean.getAge());
        assertTrue(testBean.isMale());
    }

    /* --------------------------------------------------------------
     *  2.  Null value handling – the method must simply return.
     * -------------------------------------------------------------- */
    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        // set an initial value first
        testBean.setName("initial");
        propertySetter.setProperty("name", null);   // should be ignored

        assertEquals("initial", testBean.getName());
    }

    /* --------------------------------------------------------------
     *  3.  Adding a basic (String) collection property via addBasicProperty
     * -------------------------------------------------------------- */
    @Test
    public void testAddBasicPropertyAddsToCollection() {
        // first element
        propertySetter.addBasicProperty("item", "alpha");
        // second element
        propertySetter.addBasicProperty("item", "beta");

        List<String> items = testBean.getItems();
        assertEquals(2, items.size());
        assertTrue(items.contains("alpha"));
        assertTrue(items.contains("beta"));
    }

    /* --------------------------------------------------------------
     *  4.  Adding a complex object via addComplexProperty
     * -------------------------------------------------------------- */
    @Test
    public void testAddComplexPropertyAddsComplexObject() {
        TestBean child = new TestBean();
        child.setName("child1");

        // The adder method is “addChild” – the property name passed is “Child”
        propertySetter.addComplexProperty("Child", child);

        List<TestBean> children = testBean.getChildren();
        assertEquals(1, children.size());
        assertSame(child, children.get(0));
    }

    /* --------------------------------------------------------------
     *  5.  Setting a complex property via setComplexProperty
     * -------------------------------------------------------------- */
    @Test
    public void testSetComplexPropertySetsObject() {
        TestBean other = new TestBean();
        other.setName("otherBean");

        propertySetter.setComplexProperty("other", other);

        assertSame(other, testBean.getOther());
    }

    /* --------------------------------------------------------------
     *  6.  computeAggregationType – check that a non‑null value is returned
     * -------------------------------------------------------------- */
    @Test
    public void testComputeAggregationTypeReturnsNonNull() {
        AggregationType type = propertySetter.computeAggregationType("name");
        assertNotNull(type);
        // The exact value depends on internal heuristics; the contract only
        // requires a non‑null result.
    }

    /* --------------------------------------------------------------
     *  7.  getClassNameViaImplicitRules – verify implicit rule lookup works
     * -------------------------------------------------------------- */
    @Test
    public void testGetClassNameViaImplicitRules() {
        // Register an implicit rule: property “child” of TestBean maps to TestBean.class
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(TestBean.class, "child", TestBean.class);

        Class<?> resolved = propertySetter.getClassNameViaImplicitRules(
                "child",
                AggregationType.AS_COMPLEX_PROPERTY,
                registry);

        assertEquals(TestBean.class, resolved);
    }

    /* --------------------------------------------------------------
     *  8.  Verify that unknown properties do not cause exceptions
     * -------------------------------------------------------------- */
    @Test
    public void testSetPropertyWithUnknownNameDoesNotThrow() {
        // No setter called “setUnknown” – should log a warning internally but
        // must not throw an exception.
        propertySetter.setProperty("unknown", "value");
        // nothing to assert – the absence of an exception is the success criterion.
    }

    /* --------------------------------------------------------------
     *  9.  Verify that addBasicProperty with a non‑convertible value
     *      (e.g., passing an integer string to a String adder) does not
     *      corrupt the bean.
     * -------------------------------------------------------------- */
    @Test
    public void testAddBasicPropertyWithInvalidConversionIsHandledGracefully() {
        // The adder expects a String; passing a value that can be converted
        // to String is fine, but we simulate a failure by passing null.
        propertySetter.addBasicProperty("item", null); // should be ignored

        assertTrue(testBean.getItems().isEmpty());
    }
}
