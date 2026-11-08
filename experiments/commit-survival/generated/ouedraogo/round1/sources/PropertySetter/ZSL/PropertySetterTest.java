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
 * <p>
 * The tests use real objects only – no mocking – and exercise every public
 * method as well as the important private helper paths via the public API.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------
     *  Test fixtures
     * -------------------------------------------------------------- */

    /** Simple bean used throughout the tests. */
    public static class TestBean {
        String stringProp;
        int intProp;
        boolean boolProp;
        ComplexBean complexBean;
        Runnable runnable;               // interface – used for “not instantiable” test
        final List<String> items = new ArrayList<>();

        /* ----- setters ----- */
        public void setStringProp(String s) {
            this.stringProp = s;
        }

        public void setIntProp(int i) {
            this.intProp = i;
        }

        public void setBoolProp(boolean b) {
            this.boolProp = b;
        }

        public void setComplexBean(ComplexBean cb) {
            this.complexBean = cb;
        }

        public void setRunnable(Runnable r) {
            this.runnable = r;
        }

        /* ----- adder ----- */
        public void addItem(String item) {
            items.add(item);
        }

        public void addComplexBean(ComplexBean cb) {
            this.complexBean = cb;
        }

        /* ----- annotated setter ----- */
        @DefaultClass(ComplexBean.class)
        public void setAnnotatedComplex(ComplexBean cb) {
            this.complexBean = cb;
        }
    }

    /** Simple complex type used for the “complex property” tests. */
    public static class ComplexBean {
        // empty – only the existence of a public no‑arg constructor matters
    }

    private BeanDescriptionCache cache;
    private TestBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        // Context is not required for the exercised code path – passing null is safe.
        cache = new BeanDescriptionCache(null);
        bean = new TestBean();
        setter = new PropertySetter(cache, bean);
    }

    /* --------------------------------------------------------------
     *  setProperty(..) – basic conversion tests
     * -------------------------------------------------------------- */

    @Test
    public void setProperty_stringIsSetDirectly() {
        setter.setProperty("stringProp", "hello");
        assertEquals("hello", bean.stringProp);
    }

    @Test
    public void setProperty_intIsConvertedFromString() {
        setter.setProperty("intProp", "123");
        assertEquals(123, bean.intProp);
    }

    @Test
    public void setProperty_booleanIsConvertedFromString() {
        setter.setProperty("boolProp", "true");
        assertTrue(bean.boolProp);
    }

    @Test
    public void setProperty_unknownPropertyDoesNotThrow() {
        // No setter named "unknown" – should simply log a warning and do nothing.
        setter.setProperty("unknown", "value");
        // No exception → test passes.  Verify that no bean field was altered.
        assertNull(bean.stringProp);
        assertEquals(0, bean.intProp);
        assertFalse(bean.boolProp);
    }

    @Test
    public void setProperty_nullValueIsIgnored() {
        setter.setProperty("stringProp", null);
        // field stays null
        assertNull(bean.stringProp);
    }

    /* --------------------------------------------------------------
     *  computeAggregationType(..)
     * -------------------------------------------------------------- */

    @Test
    public void computeAggregationType_forAdderReturningBasicProperty() {
        AggregationType type = setter.computeAggregationType("item");
        // addItem(String) → basic property collection
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void computeAggregationType_forSetterReturningComplexProperty() {
        AggregationType type = setter.computeAggregationType("complexBean");
        // setComplexBean(ComplexBean) → complex property
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void computeAggregationType_propertyNotFound() {
        AggregationType type = setter.computeAggregationType("nonExisting");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    /* --------------------------------------------------------------
     *  getRelevantMethod(..) – exercised via public API
     * -------------------------------------------------------------- */

    @Test
    public void getRelevantMethod_returnsAdderForComplexCollection() throws Exception {
        Method adder = setter.getRelevantMethod("Item", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        assertNotNull(adder);
        assertEquals("addItem", adder.getName());
    }

    @Test
    public void getRelevantMethod_returnsSetterForComplexProperty() throws Exception {
        Method setterMethod = setter.getRelevantMethod("complexBean", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(setterMethod);
        assertEquals("setComplexBean", setterMethod.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void getRelevantMethod_invalidAggregationThrows() {
        setter.getRelevantMethod("any", AggregationType.AS_BASIC_PROPERTY);
    }

    /* --------------------------------------------------------------
     *  Annotation handling – @DefaultClass
     * -------------------------------------------------------------- */

    @Test
    public void getDefaultClassNameByAnnotation_returnsValueFromAnnotation() throws Exception {
        // The relevant method for the property “annotatedComplex” is the setter.
        Method relevant = setter.getRelevantMethod("annotatedComplex",
                AggregationType.AS_COMPLEX_PROPERTY);
        Class<?> cls = setter.getDefaultClassNameByAnnonation("annotatedComplex", relevant);
        assertEquals(ComplexBean.class, cls);
    }

    @Test
    public void getDefaultClassNameByAnnotation_returnsNullWhenNoAnnotation() throws Exception {
        Method relevant = setter.getRelevantMethod("stringProp",
                AggregationType.AS_BASIC_PROPERTY);
        Class<?> cls = setter.getDefaultClassNameByAnnonation("stringProp", relevant);
        assertNull(cls);
    }

    /* --------------------------------------------------------------
     *  getByConcreteType – concrete class with public no‑arg ctor
     * -------------------------------------------------------------- */

    @Test
    public void getByConcreteType_returnsConcreteClassWhenInstantiable() throws Exception {
        Method setterMethod = setter.getRelevantMethod("complexBean",
                AggregationType.AS_COMPLEX_PROPERTY);
        Class<?> result = setter.getByConcreteType("complexBean", setterMethod);
        assertEquals(ComplexBean.class, result);
    }

    @Test
    public void getByConcreteType_returnsNullWhenInterface() throws Exception {
        Method setterMethod = setter.getRelevantMethod("runnable",
                AggregationType.AS_COMPLEX_PROPERTY);
        Class<?> result = setter.getByConcreteType("runnable", setterMethod);
        assertNull(result);
    }

    /* --------------------------------------------------------------
     *  getClassNameViaImplicitRules – registry overrides everything
     * -------------------------------------------------------------- */

    @Test
    public void getClassNameViaImplicitRules_usesRegistryResultWhenPresent() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(TestBean.class, "stringProp", Integer.class); // arbitrary class

        Class<?> result = setter.getClassNameViaImplicitRules("stringProp",
                AggregationType.AS_BASIC_PROPERTY_COLLECTION, registry);
        assertEquals(Integer.class, result);
    }

    @Test
    public void getClassNameViaImplicitRules_fallsBackToAnnotationThenConcrete() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        // No registry entry → should resolve via annotation on setAnnotatedComplex
        Class<?> result = setter.getClassNameViaImplicitRules("annotatedComplex",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        assertEquals(ComplexBean.class, result);
    }

    @Test
    public void getClassNameViaImplicitRules_returnsConcreteWhenNoAnnotationAndInstantiable() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        Class<?> result = setter.getClassNameViaImplicitRules("complexBean",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        assertEquals(ComplexBean.class, result);
    }

    @Test
    public void getClassNameViaImplicitRules_returnsNullWhenNotInstantiable() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        // Runnable is an interface → not unequivocally instantiable
        Class<?> result = setter.getClassNameViaImplicitRules("runnable",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        assertNull(result);
    }

    /* --------------------------------------------------------------
     *  addComplexProperty – uses adder method
     * -------------------------------------------------------------- */

    @Test
    public void addComplexProperty_callsAdderWhenTypesMatch() {
        ComplexBean cb = new ComplexBean();
        // adder method is addComplexBean(ComplexBean)
        setter.addComplexProperty("ComplexBean", cb);
        assertSame(cb, bean.complexBean);
    }

    @Test
    public void addComplexProperty_ignoresWhenNoAdderFound() {
        // property “nonexistent” has no adder – expect no exception and no change.
        setter.addComplexProperty("Nonexistent", new ComplexBean());
        assertNull(bean.complexBean);
    }

    /* --------------------------------------------------------------
     *  addBasicProperty – string conversion via adder
     * -------------------------------------------------------------- */

    @Test
    public void addBasicProperty_addsStringToCollection() {
        setter.addBasicProperty("item", "first");
        setter.addBasicProperty("item", "second");
        assertEquals(2, bean.items.size());
        assertEquals("first", bean.items.get(0));
        assertEquals("second", bean.items.get(1));
    }

    @Test
    public void addBasicProperty_ignoresNullValue() {
        setter.addBasicProperty("item", null);
        assertTrue(bean.items.isEmpty());
    }

    @Test
    public void addBasicProperty_logsErrorWhenNoAdder() {
        // Use a property name that does not have an adder.
        setter.addBasicProperty("nonexistent", "value");
        // No exception – the method simply logs an error internally.
        assertTrue(bean.items.isEmpty());
    }

    /* --------------------------------------------------------------
     *  setComplexProperty – setter invocation with type checks
     * -------------------------------------------------------------- */

    @Test
    public void setComplexProperty_callsSetterWhenTypeIsAssignable() {
        ComplexBean cb = new ComplexBean();
        setter.setComplexProperty("complexBean", cb);
        assertSame(cb, bean.complexBean);
    }

    @Test
    public void setComplexProperty_warnsWhenSetterMissing() {
        // Property “nonexistent” has no setter – method should return silently.
        setter.setComplexProperty("nonexistent", new ComplexBean());
        // No change to bean state.
        assertNull(bean.complexBean);
    }

    @Test
    public void setComplexProperty_errorsWhenTypeMismatch() {
        // Pass a Runnable to a setter that expects ComplexBean.
        setter.setComplexProperty("complexBean", (Runnable) () -> {});
        // The setter should not have been invoked.
        assertNull(bean.complexBean);
    }

    /* --------------------------------------------------------------
     *  Helper method – expose private getAnnotation for completeness
     * -------------------------------------------------------------- */

    @Test
    public void getAnnotation_returnsAnnotationFromMethod() throws Exception {
        Method method = TestBean.class.getMethod("setAnnotatedComplex", ComplexBean.class);
        Annotation ann = setter.getAnnotation("annotatedComplex", DefaultClass.class, method);
        assertNotNull(ann);
        assertTrue(ann instanceof DefaultClass);
        assertEquals(ComplexBean.class, ((DefaultClass) ann).value());
    }

    @Test
    public void getAnnotation_returnsNullWhenMethodIsNull() {
        Annotation ann = setter.getAnnotation("any", DefaultClass.class, null);
        assertNull(ann);
    }
}
