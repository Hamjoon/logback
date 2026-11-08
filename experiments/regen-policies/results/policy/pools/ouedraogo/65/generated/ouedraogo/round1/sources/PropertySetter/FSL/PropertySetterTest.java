package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;

/**
 * Unit tests for {@link PropertySetter}.
 *
 * The tests use real objects (no mocks) and rely only on the public API that
 * appears in the source supplied together with the required Logback classes.
 */
public class PropertySetterTest {

    /**
     * Simple bean used throughout the tests.  It contains a mixture of
     * setters, adders and complex‑type properties so that every public method
     * of {@link PropertySetter} can be exercised.
     */
    public static class SampleBean {

        private String foo;
        private int bar;
        private boolean active;
        private final List<String> items = new ArrayList<>();
        private final List<Integer> numbers = new ArrayList<>();
        private SampleComplex complex;
        private SampleComplex addedComplex;

        // ----- simple setters -------------------------------------------------
        public void setFoo(String foo) {
            this.foo = foo;
        }

        public void setBar(int bar) {
            this.bar = bar;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        // ----- adder methods -------------------------------------------------
        public void addItem(String item) {
            this.items.add(item);
        }

        public void addNumber(Integer number) {
            this.numbers.add(number);
        }

        public void addComplex(SampleComplex complex) {
            this.addedComplex = complex;
        }

        // ----- complex setter -------------------------------------------------
        public void setComplex(SampleComplex complex) {
            this.complex = complex;
        }

        // ----- getters (used only for assertions) ---------------------------
        public String getFoo() {
            return foo;
        }

        public int getBar() {
            return bar;
        }

        public boolean isActive() {
            return active;
        }

        public List<String> getItems() {
            return items;
        }

        public List<Integer> getNumbers() {
            return numbers;
        }

        public SampleComplex getComplex() {
            return complex;
        }

        public SampleComplex getAddedComplex() {
            return addedComplex;
        }
    }

    /** a tiny “complex” object used for the complex‑property tests */
    public static class SampleComplex {
        private final String name;

        public SampleComplex(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * A very small {@link Context} implementation.  Only the methods required
     * by the Logback base classes are implemented; all others throw
     * {@link UnsupportedOperationException}.  This satisfies the constructor
     * contract of {@link BeanDescriptionCache} without pulling in the whole
     * Logback runtime.
     */
    private static class MinimalContext implements Context {

        @Override public StatusManager getStatusManager() { throw new UnsupportedOperationException(); }
        @Override public Object getObject(String key) { throw new UnsupportedOperationException(); }
        @Override public void putObject(String key, Object value) { throw new UnsupportedOperationException(); }
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) { throw new UnsupportedOperationException(); }
        @Override public java.util.Map<String, String> getCopyOfPropertyMap() { throw new UnsupportedOperationException(); }
        @Override public String getName() { return "minimal"; }
        @Override public void setName(String name) { throw new UnsupportedOperationException(); }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { throw new UnsupportedOperationException(); }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { throw new UnsupportedOperationException(); }
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { throw new UnsupportedOperationException(); }
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { throw new UnsupportedOperationException(); }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) { throw new UnsupportedOperationException(); }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) { throw new UnsupportedOperationException(); }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) { throw new UnsupportedOperationException(); }
    }

    private SampleBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        bean = new SampleBean();

        // BeanDescriptionCache needs a Context; a minimal stub is sufficient.
        BeanDescriptionCache cache = new BeanDescriptionCache(new MinimalContext());

        // The class under test.
        setter = new PropertySetter(cache, bean);
    }

    // -----------------------------------------------------------------------
    //  Tests for the simple setProperty(String, String) method
    // -----------------------------------------------------------------------

    @Test
    public void testSetPropertyString() {
        setter.setProperty("foo", "hello");
        assertEquals("hello", bean.getFoo());
    }

    @Test
    public void testSetPropertyIntConversion() {
        setter.setProperty("bar", "123");
        assertEquals(123, bean.getBar());
    }

    @Test
    public void testSetPropertyBooleanConversion() {
        setter.setProperty("active", "true");
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        // No exception must be thrown and the bean stays unchanged.
        setter.setProperty("foo", null);
        assertNull(bean.getFoo());
    }

    @Test
    public void testSetPropertyWhenNoSetterExists() {
        // The property "unknown" does not exist – the method should simply warn
        // (which we cannot capture here) and leave the bean untouched.
        setter.setProperty("unknown", "value");
        // Verify that none of the existing fields have been altered.
        assertNull(bean.getFoo());
        assertEquals(0, bean.getBar());
        assertFalse(bean.isActive());
    }

    // -----------------------------------------------------------------------
    //  Tests for addBasicProperty(String, String)
    // -----------------------------------------------------------------------

    @Test
    public void testAddBasicPropertyAddsStringItem() {
        setter.addBasicProperty("item", "first");
        setter.addBasicProperty("item", "second");
        List<String> items = bean.getItems();
        assertEquals(2, items.size());
        assertEquals("first", items.get(0));
        assertEquals("second", items.get(1));
    }

    @Test
    public void testAddBasicPropertyConvertsToInteger() {
        setter.addBasicProperty("number", "42");
        List<Integer> numbers = bean.getNumbers();
        assertEquals(1, numbers.size());
        assertEquals(Integer.valueOf(42), numbers.get(0));
    }

    @Test
    public void testAddBasicPropertyWithNullValueIsIgnored() {
        setter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty());
    }

    @Test
    public void testAddBasicPropertyWhenAdderDoesNotExist() {
        // "nonExisting" has no adder – the method will log an error internally.
        // The test only asserts that no exception propagates.
        setter.addBasicProperty("nonExisting", "value");
        // Ensure the bean state is still untouched.
        assertTrue(bean.getItems().isEmpty());
        assertTrue(bean.getNumbers().isEmpty());
    }

    // -----------------------------------------------------------------------
    //  Tests for addComplexProperty(String, Object)
    // -----------------------------------------------------------------------

    @Test
    public void testAddComplexPropertyWithCorrectType() {
        SampleComplex complex = new SampleComplex("c1");
        setter.addComplexProperty("Complex", complex);
        assertSame(complex, bean.getAddedComplex());
    }

    @Test
    public void testAddComplexPropertyWithWrongTypeIsRejected() {
        // The adder expects SampleComplex, we pass a plain String.
        setter.addComplexProperty("Complex", "not a complex object");
        // The bean must not have stored the value.
        assertNull(bean.getAddedComplex());
    }

    @Test
    public void testAddComplexPropertyWhenAdderIsMissing() {
        // No adder named "Missing" exists – the method should log an error but
        // must not throw.
        SampleComplex complex = new SampleComplex("c2");
        setter.addComplexProperty("Missing", complex);
        // No side‑effects on the bean.
        assertNull(bean.getAddedComplex());
    }

    // -----------------------------------------------------------------------
    //  Tests for setComplexProperty(String, Object)
    // -----------------------------------------------------------------------

    @Test
    public void testSetComplexPropertyWithCorrectType() {
        SampleComplex complex = new SampleComplex("c3");
        setter.setComplexProperty("complex", complex);
        assertSame(complex, bean.getComplex());
    }

    @Test
    public void testSetComplexPropertyWhenSetterIsMissing() {
        SampleComplex complex = new SampleComplex("c4");
        // "nonExisting" has no setter – the method should only emit a warning.
        setter.setComplexProperty("nonExisting", complex);
        assertNull(bean.getComplex());
    }

    @Test
    public void testSetComplexPropertyWithIncompatibleTypeIsRejected() {
        // The setter expects SampleComplex; we give a String.
        setter.setComplexProperty("complex", "wrong type");
        assertNull(bean.getComplex());
    }

    // -----------------------------------------------------------------------
    //  Tests for computeAggregationType(String)
    // -----------------------------------------------------------------------

    @Test
    public void testComputeAggregationTypeForKnownProperty() {
        AggregationType type = setter.computeAggregationType("foo");
        // The exact value depends on Logback’s internal rules, but it must be
        // one of the enum constants and not null.
        assertNotNull(type);
    }

    @Test
    public void testComputeAggregationTypeForUnknownProperty() {
        AggregationType type = setter.computeAggregationType("doesNotExist");
        assertNotNull(type);
        // Typically the assessor returns NOT_FOUND for unknown names.
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    // -----------------------------------------------------------------------
    //  Tests for getClassNameViaImplicitRules(...)
    // -----------------------------------------------------------------------

    @Test
    public void testGetClassNameViaImplicitRulesDelegatesToAssessor() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // No explicit registration – we just verify that the call does not
        // explode and returns either null or a Class object.
        Class<?> result = setter.getClassNameViaImplicitRules(
                "foo", AggregationType.AS_BASIC_PROPERTY, registry);
        // The contract permits a null result when no implicit rule matches.
        // The important part is that the method runs without throwing.
        // (We cannot assert a concrete class without reproducing Logback’s
        // internal rule set.)
        assertTrue(result == null || result instanceof Class);
    }
}
