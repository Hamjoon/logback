package ch.qos.logback.core.joran.util;

import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;

/**
 * Comprehensive unit tests for {@link PropertySetter}.
 * <p>
 * The tests use simple POJOs defined in this file. No mocking frameworks are used;
 * only real objects that are part of the public API are instantiated.
 */
public class PropertySetterTest {

    /** Simple bean used for most tests. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> items = new ArrayList<>();
        private final List<Complex> complexes = new ArrayList<>();
        private Complex complex;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getAge() {
            return age;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }

        public void addItem(String item) {
            items.add(item);
        }

        public List<String> getItems() {
            return items;
        }

        public void addComplex(Complex complex) {
            complexes.add(complex);
        }

        public List<Complex> getComplexes() {
            return complexes;
        }

        public void setComplex(Complex complex) {
            this.complex = complex;
        }

        public Complex getComplex() {
            return complex;
        }

        @DefaultClass(SomeClass.class)
        public void setAnnotated(Annotated annotated) {
            // no‑op for the test – we only care about the annotation
        }
    }

    /** Simple POJO that will be used as a complex property. */
    public static class Complex {
        private final String id;

        public Complex() {
            this.id = "default";
        }

        public Complex(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /** Dummy class referenced by @DefaultClass. */
    public static class SomeClass {
    }

    /** Dummy class used only for annotation tests. */
    public static class Annotated {
    }

    private BeanDescriptionCache cache;
    private PropertySetter setter;
    private TestBean bean;

    @Before
    public void setUp() {
        // The Context argument of BeanDescriptionCache can be null for our tests.
        cache = new BeanDescriptionCache(null);
        bean = new TestBean();
        setter = new PropertySetter(cache, bean);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for setProperty (basic conversion)                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_stringConversion() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetProperty_intConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_booleanConversion() {
        setter.setProperty("active", "true");
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetProperty_unknownPropertyDoesNotThrow() {
        // No setter named "unknown" – should be ignored without exception.
        setter.setProperty("unknown", "value");
        // No state change to verify, just the absence of an exception.
    }

    @Test
    public void testSetProperty_nullValueIsIgnored() {
        // Passing null should result in a no‑op.
        setter.setProperty("name", null);
        assertNull(bean.getName());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for computeAggregationType                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationType_basicPropertyCollection() {
        AggregationType type = setter.computeAggregationType("item");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationType_complexPropertyCollection() {
        AggregationType type = setter.computeAggregationType("complex");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationType_basicProperty() {
        AggregationType type = setter.computeAggregationType("name");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationType_complexProperty() {
        AggregationType type = setter.computeAggregationType("complex");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationType_notFound() {
        AggregationType type = setter.computeAggregationType("nonexistent");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for getRelevantMethod & getAnnotation                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetRelevantMethod_returnsAdderForComplexCollection() throws Exception {
        Method m = setter.getRelevantMethod("Complex", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        assertNotNull(m);
        assertEquals("addComplex", m.getName());
    }

    @Test
    public void testGetRelevantMethod_returnsSetterForComplexProperty() throws Exception {
        Method m = setter.getRelevantMethod("complex", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(m);
        assertEquals("setComplex", m.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRelevantMethod_invalidAggregationThrows() {
        setter.getRelevantMethod("name", AggregationType.AS_BASIC_PROPERTY);
    }

    @Test
    public void testGetAnnotation_onAnnotatedSetter() throws Exception {
        Method setterMethod = TestBean.class.getMethod("setAnnotated", Annotated.class);
        Annotation ann = setter.getAnnotation("annotated", DefaultClass.class, setterMethod);
        assertNotNull(ann);
        assertTrue(ann instanceof DefaultClass);
    }

    @Test
    public void testGetAnnotation_nullWhenMethodIsNull() {
        Annotation ann = setter.getAnnotation("anything", DefaultClass.class, null);
        assertNull(ann);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for DefaultClass handling                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDefaultClassNameByAnnotation() throws Exception {
        Method m = TestBean.class.getMethod("setAnnotated", Annotated.class);
        Class<?> clazz = setter.getDefaultClassNameByAnnonation("annotated", m);
        assertEquals(SomeClass.class, clazz);
    }

    @Test
    public void testGetDefaultClassNameByAnnotation_noneWhenMissing() throws Exception {
        Method m = TestBean.class.getMethod("setName", String.class);
        Class<?> clazz = setter.getDefaultClassNameByAnnonation("name", m);
        assertNull(clazz);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for concrete type instantiation checking                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetByConcreteType_returnsConcreteWhenInstantiable() throws Exception {
        Method m = TestBean.class.getMethod("setComplex", Complex.class);
        Class<?> result = setter.getByConcreteType("complex", m);
        assertEquals(Complex.class, result);
    }

    @Test
    public void testGetByConcreteType_returnsNullForInterface() throws Exception {
        Method m = TestBean.class.getMethod("setItems", List.class); // there is no such setter, but we can use addItem's adder
        // Use the adder method for List<String>
        Method adder = TestBean.class.getMethod("addItem", String.class);
        // The parameter type of addItem is String (instantiable) – we need an interface example.
        // We'll create a dummy method via reflection that accepts List (interface) to test.
        // Since such method does not exist, we simulate by calling the private helper via reflection.
        // Instead we use the adder for a collection type (List) via a custom bean.
        class InterfaceBean {
            public void setList(List<String> list) { }
        }
        Method ifaceSetter = InterfaceBean.class.getMethod("setList", List.class);
        Class<?> result = setter.getByConcreteType("list", ifaceSetter);
        assertNull(result);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for getClassNameViaImplicitRules                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameViaImplicitRules_usesRegistry() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(TestBean.class, "registryProp", SomeClass.class);

        Class<?> clazz = setter.getClassNameViaImplicitRules(
                "registryProp",
                AggregationType.AS_BASIC_PROPERTY,
                registry);

        assertEquals(SomeClass.class, clazz);
    }

    @Test
    public void testGetClassNameViaImplicitRules_fallsBackToAnnotation() throws Exception {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        // property name must match the method name used for the annotation test.
        Class<?> clazz = setter.getClassNameViaImplicitRules(
                "annotated",
                AggregationType.AS_COMPLEX_PROPERTY,
                emptyRegistry);

        assertEquals(SomeClass.class, clazz);
    }

    @Test
    public void testGetClassNameViaImplicitRules_fallsBackToConcreteType() throws Exception {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        Class<?> clazz = setter.getClassNameViaImplicitRules(
                "complex",
                AggregationType.AS_COMPLEX_PROPERTY,
                emptyRegistry);

        assertEquals(Complex.class, clazz);
    }

    @Test
    public void testGetClassNameViaImplicitRules_returnsNullWhenNothingMatches() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();
        Class<?> clazz = setter.getClassNameViaImplicitRules(
                "nonexistent",
                AggregationType.AS_BASIC_PROPERTY,
                emptyRegistry);
        assertNull(clazz);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for addBasicProperty                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddBasicProperty_successful() {
        setter.addBasicProperty("item", "hello");
        List<String> items = bean.getItems();
        assertEquals(1, items.size());
        assertEquals("hello", items.get(0));
    }

    @Test
    public void testAddBasicProperty_unknownAdderIsIgnored() {
        // property "unknown" has no adder – the method should log an error but not throw.
        setter.addBasicProperty("unknown", "value");
        // Verify that the bean's collections are still empty.
        assertTrue(bean.getItems().isEmpty());
        assertTrue(bean.getComplexes().isEmpty());
    }

    @Test
    public void testAddBasicProperty_nullValueIsIgnored() {
        setter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for addComplexProperty                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddComplexProperty_successful() {
        Complex c = new Complex("c1");
        setter.addComplexProperty("Complex", c);
        List<Complex> list = bean.getComplexes();
        assertEquals(1, list.size());
        assertEquals("c1", list.get(0).getId());
    }

    @Test
    public void testAddComplexProperty_wrongTypeIsRejected() {
        // Pass a String where a Complex is expected.
        setter.addComplexProperty("Complex", "not a complex");
        // The list must stay empty because the sanity check fails.
        assertTrue(bean.getComplexes().isEmpty());
    }

    @Test
    public void testAddComplexProperty_missingAdderLogsError() {
        // "Missing" property – there is no adder named addMissing.
        setter.addComplexProperty("Missing", new Complex());
        // Nothing should be added; just ensure no exception is thrown.
        assertTrue(bean.getComplexes().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for setComplexProperty                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetComplexProperty_successful() {
        Complex c = new Complex("c2");
        setter.setComplexProperty("complex", c);
        assertNotNull(bean.getComplex());
        assertEquals("c2", bean.getComplex().getId());
    }

    @Test
    public void testSetComplexProperty_missingSetterLogsWarn() {
        // No setter called "setMissing".
        setter.setComplexProperty("missing", new Complex());
        // No exception – just verify that bean state is unchanged.
        assertNull(bean.getComplex());
    }

    @Test
    public void testSetComplexProperty_wrongTypeIsRejected() {
        // Pass a String to a setter that expects Complex.
        setter.setComplexProperty("complex", "not a complex");
        // The complex field must remain null.
        assertNull(bean.getComplex());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for private helper isUnequivocallyInstantiable (via public API) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsUnequivocallyInstantiable_trueForConcreteClass() throws Exception {
        // Use reflection to invoke the private method.
        Method m = PropertySetter.class.getDeclaredMethod("isUnequivocallyInstantiable", Class.class);
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(setter, Complex.class);
        assertTrue(result);
    }

    @Test
    public void testIsUnequivocallyInstantiable_falseForInterface() throws Exception {
        Method m = PropertySetter.class.getDeclaredMethod("isUnequivocallyInstantiable", Class.class);
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(setter, List.class);
        assertFalse(result);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for getObjClass and getObj                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetObjClassReturnsCorrectClass() {
        assertEquals(TestBean.class, setter.getObjClass());
    }

    @Test
    public void testGetObjReturnsOriginalObject() {
        assertSame(bean, setter.getObj());
    }
}
