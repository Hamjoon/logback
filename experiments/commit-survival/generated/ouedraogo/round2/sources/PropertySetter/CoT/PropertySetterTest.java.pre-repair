package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests use real bean classes (defined as static inner classes) and the
 * real {@link BeanDescriptionCache} implementation – no mocking or subclassing
 * of library classes is performed, as required.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public methods of PropertySetter (extracted)                     */
    /* --------------------------------------------------------------------- */
    // public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
    // public void setProperty(String name, String value)
    // public AggregationType computeAggregationType(String name)
    // public Class<?> getObjClass()
    // public void addComplexProperty(String name, Object complexProperty)
    // public void addBasicProperty(String name, String strValue)
    // public void setComplexProperty(String name, Object complexProperty)
    // public Object getObj()
    // public Class<?> getClassNameViaImplicitRules(String name,
    //                                            AggregationType aggregationType,
    //                                            DefaultNestedComponentRegistry registry)

    private BeanDescriptionCache cache;
    private SimpleBean simpleBean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        // The Context parameter of BeanDescriptionCache can be null for the
        // purpose of these unit tests (the implementation tolerates it).
        cache = new BeanDescriptionCache(null);
        simpleBean = new SimpleBean();
        setter = new PropertySetter(cache, simpleBean);
    }

    /* --------------------------------------------------------------------- */
    /*  2.  Basic functionality tests                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_StringValue() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", simpleBean.getName());
    }

    @Test
    public void testSetProperty_IntConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, simpleBean.getAge());
    }

    @Test
    public void testSetProperty_BooleanConversion() {
        setter.setProperty("male", "true");
        assertTrue(simpleBean.isMale());
    }

    @Test
    public void testSetProperty_NullValue_IsIgnored() {
        simpleBean.setName("initial");
        setter.setProperty("name", null); // should be a no‑op
        assertEquals("initial", simpleBean.getName());
    }

    @Test
    public void testSetProperty_NoSuchSetter_WarnsButDoesNotThrow() {
        // No setter called "unknown"
        setter.setProperty("unknown", "value");
        // No exception – the internal warning is logged through ContextAwareBase.
        // Verify that bean state stayed unchanged.
        assertNull(simpleBean.getName());
        assertEquals(0, simpleBean.getAge());
    }

    @Test
    public void testComputeAggregationType_AdderBasicProperty() {
        // "item" has an adder that accepts a String → basic collection
        AggregationType type = setter.computeAggregationType("item");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationType_SetterBasicProperty() {
        // "name" has a simple String setter
        AggregationType type = setter.computeAggregationType("name");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationType_SetterComplexProperty() {
        // "child" has a setter that accepts a ChildBean (complex type)
        AggregationType type = setter.computeAggregationType("child");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testAddBasicProperty_ValidAdder() {
        setter.addBasicProperty("item", "first");
        assertEquals(1, simpleBean.getItems().size());
        assertEquals("first", simpleBean.getItems().get(0));
    }

    @Test
    public void testAddComplexProperty_ValidAdder() {
        ChildBean child = new ChildBean();
        child.setLabel("child1");
        setter.addComplexProperty("child", child);
        assertEquals(child, simpleBean.getChild());
    }

    @Test
    public void testSetComplexProperty_ValidSetter() {
        ChildBean child = new ChildBean();
        child.setLabel("c2");
        setter.setComplexProperty("child", child);
        assertEquals(child, simpleBean.getChild());
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Edge‑case and exception handling tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_ConversionFails_ThrowsPropertySetterExceptionHandled() {
        // The "age" setter expects an int; providing a non‑numeric string triggers
        // a conversion failure which is wrapped in PropertySetterException and then
        // logged as a warning. No exception bubbles up to the caller.
        setter.setProperty("age", "notANumber");
        // Value should stay at default (0)
        assertEquals(0, simpleBean.getAge());
    }

    @Test
    public void testAddComplexProperty_NoAdder_LogsError() {
        // "nonexistent" has no adder method – the call should simply return
        // after logging an error, without throwing.
        ChildBean child = new ChildBean();
        setter.addComplexProperty("nonexistent", child);
        // bean state unchanged
        assertNull(simpleBean.getChild());
    }

    @Test
    public void testAddBasicProperty_NoAdder_LogsError() {
        // "nonexistent" has no adder
        setter.addBasicProperty("nonexistent", "value");
        // No change in bean
        assertTrue(simpleBean.getItems().isEmpty());
    }

    @Test
    public void testAddComplexProperty_TypeMismatch_LogsError() {
        // The adder expects a ChildBean, we give a String → type mismatch.
        setter.addComplexProperty("child", "I am not a ChildBean");
        // The bean's child should remain null.
        assertNull(simpleBean.getChild());
    }

    @Test
    public void testGetClassNameViaImplicitRules_RegistryProvidesResult() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(SimpleBean.class, "child", AlternateChild.class);

        Class<?> result = setter.getClassNameViaImplicitRules(
                "child",
                AggregationType.AS_COMPLEX_PROPERTY,
                registry);

        assertEquals(AlternateChild.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_AnnotationProvidesResult() {
        // The adder method for "annotated" is annotated with @DefaultClass(AnnotatedChild.class)
        Class<?> result = setter.getClassNameViaImplicitRules(
                "annotated",
                AggregationType.AS_COMPLEX_PROPERTY_COLLECTION,
                new DefaultNestedComponentRegistry());

        assertEquals(AnnotatedChild.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_ConcreteInstantiableReturnsClass() {
        // For property "child" the setter expects ChildBean which has a public no‑arg ctor.
        Class<?> result = setter.getClassNameViaImplicitRules(
                "child",
                AggregationType.AS_COMPLEX_PROPERTY,
                new DefaultNestedComponentRegistry());

        assertEquals(ChildBean.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_NoneFoundReturnsNull() {
        // Property "nothing" does not exist anywhere.
        Class<?> result = setter.getClassNameViaImplicitRules(
                "nothing",
                AggregationType.AS_BASIC_PROPERTY,
                new DefaultNestedComponentRegistry());

        assertNull(result);
    }

    @Test
    public void testGetRelevantMethod_IllegalAggregationType_ThrowsException() {
        try {
            setter.getRelevantMethod("any", AggregationType.AS_BASIC_PROPERTY);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testGetAnnotation_ReturnsAnnotationWhenPresent() throws NoSuchMethodException {
        Method adder = SimpleBean.class.getMethod("addAnnotated", AnnotatedChild.class);
        DefaultClass ann = setter.getAnnotation("annotated", DefaultClass.class, adder);
        assertNotNull(ann);
        assertEquals(AnnotatedChild.class, ann.value());
    }

    @Test
    public void testGetAnnotation_ReturnsNullWhenMethodIsNull() {
        DefaultClass ann = setter.getAnnotation("any", DefaultClass.class, null);
        assertNull(ann);
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Helper bean definitions                                          */
    /* --------------------------------------------------------------------- */

    /**
     * Simple bean used for most tests. It contains:
     * <ul>
     *   <li>String property {@code name}</li>
     *   <li>int property {@code age}</li>
     *   <li>boolean property {@code male}</li>
     *   <li>List&lt;String&gt; collection via {@code addItem(String)}</li>
     *   <li>ChildBean property via {@code setChild(ChildBean)} and {@code addChild(ChildBean)}</li>
     * </ul>
     */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean male;
        private final List<String> items = new ArrayList<>();
        private ChildBean child;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public boolean isMale() { return male; }
        public void setMale(boolean male) { this.male = male; }

        public List<String> getItems() { return items; }
        public void addItem(String item) { this.items.add(item); }

        public ChildBean getChild() { return child; }
        public void setChild(ChildBean child) { this.child = child; }

        // adder used for annotation test
        @DefaultClass(AnnotatedChild.class)
        public void addAnnotated(AnnotatedChild child) { this.child = child; }
    }

    /** Simple complex property class */
    public static class ChildBean {
        private String label;
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    /** Alternate class used to verify registry lookup */
    public static class AlternateChild extends ChildBean { }

    /** Class used for annotation‑driven default component */
    public static class AnnotatedChild extends ChildBean { }
}
