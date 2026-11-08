package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal property setting via {@code setProperty}</li>
 *   <li>handling of missing setters, conversion failures and null values</li>
 *   <li>complex property addition via {@code addComplexProperty}</li>
 *   <li>basic property addition via {@code addBasicProperty}</li>
 *   <li>complex property setting via {@code setComplexProperty}</li>
 *   <li>delegation methods {@code computeAggregationType} and {@code getClassNameViaImplicitRules}</li>
 * </ul>
 *
 * Only the public APIs listed in the prompt are used – no mocks or external
 * library classes are introduced.
 */
public class PropertySetterTest {

    /** Simple bean used throughout the tests. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private List<String> items = new java.util.ArrayList<>();
        private Child child;

        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setActive(boolean active) { this.active = active; }
        public boolean isActive() { return active; }

        /** adder for a basic (String) collection property */
        public void addItem(String item) { items.add(item); }
        public List<String> getItems() { return items; }

        /** complex property setter */
        public void setChild(Child child) { this.child = child; }
        public Child getChild() { return child; }
    }

    /** A second bean used as a complex property */
    public static class Child {
        private String value;
        public void setValue(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private PropertySetter propertySetter;
    private SimpleBean bean;

    @Before
    public void setUp() {
        // The library allows a null Context for BeanDescriptionCache – it works
        // because the cache only needs the Class object for reflection.
        BeanDescriptionCache cache = new BeanDescriptionCache(null);
        bean = new SimpleBean();
        propertySetter = new PropertySetter(cache, bean);
    }

    /* --------------------------------------------------------------
     *  Tests for setProperty(String, String)
     * -------------------------------------------------------------- */

    @Test
    public void setProperty_StringValue_SetsStringProperty() {
        propertySetter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void setProperty_IntValue_ConvertsAndSets() {
        propertySetter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void setProperty_BooleanValue_ConvertsAndSets() {
        propertySetter.setProperty("active", "true");
        assertTrue(bean.isActive());
    }

    @Test
    public void setProperty_NullValue_IgnoresCall() {
        bean.setName("initial");
        propertySetter.setProperty("name", null);
        assertEquals("initial", bean.getName());
    }

    @Test
    public void setProperty_MissingSetter_DoesNotThrow() {
        // "unknown" has no corresponding setter in SimpleBean.
        propertySetter.setProperty("unknown", "value");
        // No exception – just verify that existing properties stay untouched.
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void setProperty_ConversionFailure_DoesNotThrowAndLeavesDefault() {
        // "age" expects an int – passing a non‑numeric string triggers conversion failure.
        bean.setAge(10);
        propertySetter.setProperty("age", "notANumber");
        // The original value must remain unchanged.
        assertEquals(10, bean.getAge());
    }

    /* --------------------------------------------------------------
     *  Tests for addComplexProperty(String, Object)
     * -------------------------------------------------------------- */

    @Test
    public void addComplexProperty_ValidAdder_AddsToCollection() {
        // The adder for the collection is addItem(String)
        propertySetter.addComplexProperty("Item", "first");
        propertySetter.addComplexProperty("Item", "second");
        List<String> items = bean.getItems();
        assertEquals(2, items.size());
        assertTrue(items.contains("first"));
        assertTrue(items.contains("second"));
    }

    @Test
    public void addComplexProperty_WrongType_LogsErrorAndDoesNotAdd() {
        // adder expects String, we supply an Integer – should be rejected.
        propertySetter.addComplexProperty("Item", Integer.valueOf(123));
        assertTrue(bean.getItems().isEmpty());
    }

    @Test
    public void addComplexProperty_NoAdder_LogsErrorAndDoesNothing() {
        // No method named addFoo in SimpleBean.
        propertySetter.addComplexProperty("Foo", "value");
        // Verify that nothing was added to any collection.
        assertTrue(bean.getItems().isEmpty());
    }

    /* --------------------------------------------------------------
     *  Tests for addBasicProperty(String, String)
     * -------------------------------------------------------------- */

    @Test
    public void addBasicProperty_ValidAdder_ConvertsAndAdds() {
        // The method searches for an adder named addItem (capitalised internally).
        propertySetter.addBasicProperty("item", "basic1");
        propertySetter.addBasicProperty("item", "basic2");
        List<String> items = bean.getItems();
        assertEquals(2, items.size());
        assertTrue(items.contains("basic1"));
        assertTrue(items.contains("basic2"));
    }

    @Test
    public void addBasicProperty_NoAdder_LogsErrorAndDoesNothing() {
        propertySetter.addBasicProperty("nonexistent", "value");
        // No side‑effects expected.
        assertTrue(bean.getItems().isEmpty());
    }

    @Test
    public void addBasicProperty_NullValue_IgnoresCall() {
        propertySetter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty());
    }

    /* --------------------------------------------------------------
     *  Tests for setComplexProperty(String, Object)
     * -------------------------------------------------------------- */

    @Test
    public void setComplexProperty_ValidSetter_SetsComplexProperty() {
        Child child = new Child();
        child.setValue("childValue");
        propertySetter.setComplexProperty("Child", child);
        assertNotNull(bean.getChild());
        assertEquals("childValue", bean.getChild().getValue());
    }

    @Test
    public void setComplexProperty_WrongType_LogsErrorAndDoesNotSet() {
        // setChild expects a Child, we give a String.
        propertySetter.setComplexProperty("Child", "notAChild");
        assertNull(bean.getChild());
    }

    @Test
    public void setComplexProperty_NoSetter_LogsWarnAndDoesNothing() {
        propertySetter.setComplexProperty("NonExisting", new Object());
        // No exception – bean remains unchanged.
        assertNull(bean.getChild());
    }

    /* --------------------------------------------------------------
     *  Tests for delegation methods
     * -------------------------------------------------------------- */

    @Test
    public void computeAggregationType_DelegatesToAssessor() {
        // For a known simple property the assessor should return NOT_FOUND or a concrete type.
        // The exact value depends on internal rules – we only assert that the call does not fail.
        AggregationType type = propertySetter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void getClassNameViaImplicitRules_DelegatesToAssessor() {
        // Use a fresh registry – the method must return a Class or null without throwing.
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> clazz = propertySetter.getClassNameViaImplicitRules(
                "child",
                AggregationType.AS_COMPLEX_PROPERTY,
                registry);
        // The registry is empty, so the result is expected to be null.
        assertNull(clazz);
    }

    /* --------------------------------------------------------------
     *  Helper methods (none needed – all tests use the public API)
     * -------------------------------------------------------------- */
}
