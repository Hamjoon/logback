package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 * <p>
 * The tests use real concrete classes from the Logback library only
 * (no mocks, no subclassing of library interfaces).  A minimal
 * {@code SampleObject} together with a simple {@code Child} class
 * provides the setters/adder methods required for exercising the
 * different code paths inside {@code PropertySetter}.
 */
public class PropertySetterTest {

    // -----------------------------------------------------------------
    // Helper classes used by the tests
    // -----------------------------------------------------------------
    public static class Child {
        private final String name;
        public Child(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static class SampleObject {
        // simple properties
        private String name;
        private int age;
        private boolean active;

        // complex property (single)
        private Child child;

        // collection properties
        private final List<Child> children = new ArrayList<>();
        private final List<String> strings = new ArrayList<>();

        // ----- setters / getters -------------------------------------------------
        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setActive(boolean active) { this.active = active; }
        public boolean isActive() { return active; }

        public void setChild(Child child) { this.child = child; }
        public Child getChild() { return child; }

        // ----- adder methods ----------------------------------------------------
        public void addChild(Child child) { this.children.add(child); }
        public List<Child> getChildren() { return children; }

        public void addString(String s) { this.strings.add(s); }
        public List<String> getStrings() { return strings; }
    }

    private PropertySetter setter;
    private SampleObject sample;

    @Before
    public void setUp() {
        // BeanDescriptionCache can be instantiated with a null Context – the
        // implementation only uses the Context for status handling.
        BeanDescriptionCache cache = new BeanDescriptionCache(null);
        sample = new SampleObject();
        setter = new PropertySetter(cache, sample);
    }

    // -----------------------------------------------------------------
    //  setProperty(String, String) – happy paths
    // -----------------------------------------------------------------
    @Test
    public void testSetPropertyString() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", sample.getName());
    }

    @Test
    public void testSetPropertyInt() {
        setter.setProperty("age", "42");
        assertEquals(42, sample.getAge());
    }

    @Test
    public void testSetPropertyBoolean() {
        setter.setProperty("active", "true");
        assertTrue(sample.isActive());
    }

    // -----------------------------------------------------------------
    //  setProperty(String, String) – edge cases
    // -----------------------------------------------------------------
    @Test
    public void testSetPropertyNullValueIsIgnored() {
        // before the call the name is null
        assertNull(sample.getName());
        setter.setProperty("name", null);   // should be a no‑op, no exception
        assertNull(sample.getName());
    }

    @Test
    public void testSetPropertyNoCorrespondingSetter() {
        // there is no setter called setFoo – the method should simply warn
        // and *not* throw an exception.
        setter.setProperty("foo", "bar");
        // verify that no side‑effect occurred
        assertNull(sample.getName());
    }

    // -----------------------------------------------------------------
    //  computeAggregationType – delegating to AggregationAssessor
    // -----------------------------------------------------------------
    @Test
    public void testComputeAggregationTypeNotFound() {
        AggregationType type = setter.computeAggregationType("nonExistingProperty");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    // -----------------------------------------------------------------
    //  addComplexProperty – successful adder invocation
    // -----------------------------------------------------------------
    @Test
    public void testAddComplexPropertySuccess() {
        Child child = new Child("alice");
        // The adder method is addChild(Child)
        setter.addComplexProperty("Child", child);
        assertEquals(1, sample.getChildren().size());
        assertSame(child, sample.getChildren().get(0));
    }

    @Test
    public void testAddComplexPropertyNoAdder() {
        // Property "NoSuch" has no corresponding addNoSuch method.
        // The call should not throw and simply log an error.
        setter.addComplexProperty("NoSuch", new Object());
        // No change in the object state – children list stays empty.
        assertTrue(sample.getChildren().isEmpty());
    }

    // -----------------------------------------------------------------
    //  addBasicProperty – string based adder
    // -----------------------------------------------------------------
    @Test
    public void testAddBasicPropertySuccess() {
        // The adder method expected by the implementation is addString(String)
        setter.addBasicProperty("string", "hello");
        assertEquals(1, sample.getStrings().size());
        assertEquals("hello", sample.getStrings().get(0));
    }

    @Test
    public void testAddBasicPropertyNoAdder() {
        // No method addFoo(String) exists – should log an error, not throw.
        setter.addBasicProperty("foo", "value");
        assertTrue(sample.getStrings().isEmpty());
    }

    // -----------------------------------------------------------------
    //  setComplexProperty – successful setter invocation
    // -----------------------------------------------------------------
    @Test
    public void testSetComplexPropertySuccess() {
        Child child = new Child("bob");
        setter.setComplexProperty("Child", child); // expects setChild(Child)
        assertSame(child, sample.getChild());
    }

    @Test
    public void testSetComplexPropertyNoSetter() {
        // No setFoo method – should log a warning and leave object unchanged.
        setter.setComplexProperty("Foo", new Object());
        assertNull(sample.getChild());
    }

    // -----------------------------------------------------------------
    //  Accessor helpers
    // -----------------------------------------------------------------
    @Test
    public void testGetObjClassAndGetObj() {
        assertSame(SampleObject.class, setter.getObjClass());
        assertSame(sample, setter.getObj());
    }

    // -----------------------------------------------------------------
    //  getClassNameViaImplicitRules – delegation to AggregationAssessor
    // -----------------------------------------------------------------
    @Test
    public void testGetClassNameViaImplicitRules() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Register that the property "child" of SampleObject resolves to Child.class
        registry.add(SampleObject.class, "child", Child.class);

        Class<?> result = setter.getClassNameViaImplicitRules(
                "child",
                AggregationType.AS_COMPLEX_PROPERTY,
                registry);

        assertEquals(Child.class, result);
    }
}
