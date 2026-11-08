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
import ch.qos.logback.core.util.PropertySetterException;

/**
 * Unit tests for {@link PropertySetter}. The tests cover all public and
 * package‑private behaviour without using any mocking framework – real objects
 * are created for every dependency.
 */
public class PropertySetterTest {

    // -----------------------------------------------------------------------
    // Helper classes used throughout the tests
    // -----------------------------------------------------------------------

    public static class Child {
        private String id;
        public Child() {}
        public Child(String id) { this.id = id; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class Instantiable {
        public Instantiable() {}
    }

    public static class NonInstantiable {
        private NonInstantiable() {}
    }

    public static class AnnotatedType {
        private String value;
        public AnnotatedType() {}
        public String getValue() { return value; }
        public void setValue(String v) { this.value = v; }
    }

    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> items = new ArrayList<>();
        private final List<Child> children = new ArrayList<>();
        private AnnotatedType special;

        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setActive(boolean active) { this.active = active; }
        public boolean isActive() { return active; }

        // adder for a basic property collection
        public void addItem(String item) { items.add(item); }
        public List<String> getItems() { return items; }

        // adder for a complex property collection
        public void addChild(Child child) { children.add(child); }
        public List<Child> getChildren() { return children; }

        // complex property with @DefaultClass annotation
        @DefaultClass(AnnotatedType.class)
        public void setSpecial(AnnotatedType special) { this.special = special; }
        public AnnotatedType getSpecial() { return special; }

        // setter expecting an Instantiable class (used for concrete‑type tests)
        public void setInst(Instantiable inst) { /* no‑op */ }

        // setter expecting a NonInstantiable class (used for concrete‑type tests)
        public void setNonInst(NonInstantiable nonInst) { /* no‑op */ }
    }

    private BeanDescriptionCache cache;
    private PropertySetter setter;
    private SimpleBean bean;

    @Before
    public void setUp() {
        // Context is not needed for the logic under test; passing null is safe.
        cache = new BeanDescriptionCache(null);
        bean = new SimpleBean();
        setter = new PropertySetter(cache, bean);
    }

    // -----------------------------------------------------------------------
    // setProperty(..) – basic conversion and missing‑setter handling
    // -----------------------------------------------------------------------

    @Test
    public void testSetPropertyBasicTypes() {
        setter.setProperty("name", "John");
        setter.setProperty("age", "42");
        setter.setProperty("active", "true");

        assertEquals("John", bean.getName());
        assertEquals(42, bean.getAge());
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetPropertyIgnoresNullValue() {
        setter.setProperty("name", null); // should be a no‑op, no exception
        assertNull(bean.getName());
    }

    @Test
    public void testSetPropertyWhenNoSetterExists() {
        // No setter named setUnknown – should not throw and simply log a warning.
        setter.setProperty("unknown", "value");
        // bean does not have such a property, nothing to assert besides no exception.
    }

    // -----------------------------------------------------------------------
    // computeAggregationType(..) – detection of aggregation kinds
    // -----------------------------------------------------------------------

    @Test
    public void testComputeAggregationTypeForBasicPropertySetter() {
        AggregationType type = setter.computeAggregationType("age");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationTypeForComplexPropertySetter() {
        AggregationType type = setter.computeAggregationType("special");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationTypeForBasicPropertyCollection() {
        AggregationType type = setter.computeAggregationType("item");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationTypeForComplexPropertyCollection() {
        AggregationType type = setter.computeAggregationType("child");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationTypeNotFound() {
        AggregationType type = setter.computeAggregationType("nonExisting");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    // -----------------------------------------------------------------------
    // addBasicProperty(..) – using an adder that expects a simple type
    // -----------------------------------------------------------------------

    @Test
    public void testAddBasicPropertyAddsStringToCollection() {
        setter.addBasicProperty("item", "first");
        setter.addBasicProperty("item", "second");
        List<String> items = bean.getItems();
        assertEquals(2, items.size());
        assertTrue(items.contains("first"));
        assertTrue(items.contains("second"));
    }

    @Test
    public void testAddBasicPropertyWhenAdderMissing() {
        // "nonexistent" has no adder – the method should log an error but not throw.
        setter.addBasicProperty("nonexistent", "value");
        // No side‑effect to verify, just ensure no exception is raised.
    }

    // -----------------------------------------------------------------------
    // addComplexProperty(..) – using an adder that expects a complex object
    // -----------------------------------------------------------------------

    @Test
    public void testAddComplexPropertyAddsChild() {
        Child c1 = new Child("c1");
        Child c2 = new Child("c2");
        setter.addComplexProperty("Child", c1);
        setter.addComplexProperty("Child", c2);
        List<Child> children = bean.getChildren();
        assertEquals(2, children.size());
        assertEquals("c1", children.get(0).getId());
        assertEquals("c2", children.get(1).getId());
    }

    @Test
    public void testAddComplexPropertyWithWrongTypeLogsError() {
        // Pass a String to the addChild(Child) adder – should fail the sanity check.
        setter.addComplexProperty("Child", "notAChild");
        // The collection must stay empty.
        assertTrue(bean.getChildren().isEmpty());
    }

    // -----------------------------------------------------------------------
    // setComplexProperty(..) – setter that expects a complex object
    // -----------------------------------------------------------------------

    @Test
    public void testSetComplexPropertyWithCorrectType() {
        AnnotatedType at = new AnnotatedType();
        at.setValue("hello");
        setter.setComplexProperty("Special", at);
        assertSame(at, bean.getSpecial());
    }

    @Test
    public void testSetComplexPropertyWithIncorrectTypeLogsError() {
        // Pass an incompatible type (String) to setSpecial(AnnotatedType)
        setter.setComplexProperty("Special", "bad");
        assertNull(bean.getSpecial());
    }

    // -----------------------------------------------------------------------
    // getRelevantMethod(..) – internal routing based on AggregationType
    // -----------------------------------------------------------------------

    @Test
    public void testGetRelevantMethodForComplexProperty() throws Exception {
        Method m = setter.getRelevantMethod("special", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(m);
        assertEquals("setSpecial", m.getName());
    }

    @Test
    public void testGetRelevantMethodForComplexPropertyCollection() throws Exception {
        Method m = setter.getRelevantMethod("child", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        assertNotNull(m);
        assertEquals("addChild", m.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRelevantMethodWithInvalidAggregationTypeThrows() {
        setter.getRelevantMethod("any", AggregationType.AS_BASIC_PROPERTY);
    }

    // -----------------------------------------------------------------------
    // Annotation handling – @DefaultClass
    // -----------------------------------------------------------------------

    @Test
    public void testGetAnnotationReturnsAnnotationWhenPresent() throws Exception {
        Method setterMethod = SimpleBean.class.getMethod("setSpecial", AnnotatedType.class);
        Annotation ann = setter.getAnnotation("special", DefaultClass.class, setterMethod);
        assertNotNull(ann);
        assertTrue(ann instanceof DefaultClass);
    }

    @Test
    public void testGetAnnotationReturnsNullWhenMethodIsNull() {
        Annotation ann = setter.getAnnotation("any", DefaultClass.class, null);
        assertNull(ann);
    }

    @Test
    public void testGetDefaultClassNameByAnnotation() throws Exception {
        Method method = SimpleBean.class.getMethod("setSpecial", AnnotatedType.class);
        Class<?> clazz = setter.getDefaultClassNameByAnnonation("special", method);
        assertEquals(AnnotatedType.class, clazz);
    }

    @Test
    public void testGetDefaultClassNameByAnnotationReturnsNullWhenMissing() throws Exception {
        Method method = SimpleBean.class.getMethod("setName", String.class);
        Class<?> clazz = setter.getDefaultClassNameByAnnonation("name", method);
        assertNull(clazz);
    }

    // -----------------------------------------------------------------------
    // Concrete‑type resolution – isUnequivocallyInstantiable()
    // -----------------------------------------------------------------------

    @Test
    public void testGetByConcreteTypeReturnsInstantiableClass() throws Exception {
        Method method = SimpleBean.class.getMethod("setInst", Instantiable.class);
        Class<?> resolved = setter.getByConcreteType("inst", method);
        assertEquals(Instantiable.class, resolved);
    }

    @Test
    public void testGetByConcreteTypeReturnsNullWhenNotInstantiable() throws Exception {
        Method method = SimpleBean.class.getMethod("setNonInst", NonInstantiable.class);
        Class<?> resolved = setter.getByConcreteType("nonInst", method);
        assertNull(resolved);
    }

    // -----------------------------------------------------------------------
    // getClassNameViaImplicitRules – registry, annotation, concrete type
    // -----------------------------------------------------------------------

    @Test
    public void testGetClassNameViaImplicitRulesUsesRegistryFirst() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Register a default component type for property "name" on SimpleBean.
        registry.add(SimpleBean.class, "name", String.class);

        Class<?> result = setter.getClassNameViaImplicitRules("name",
                AggregationType.AS_BASIC_PROPERTY, registry);
        assertEquals(String.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRulesFallsBackToAnnotation() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        Class<?> result = setter.getClassNameViaImplicitRules("special",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        // The setter for "special" is annotated with @DefaultClass(AnnotatedType.class)
        assertEquals(AnnotatedType.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRulesFallsBackToConcreteType() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        Class<?> result = setter.getClassNameViaImplicitRules("inst",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        assertEquals(Instantiable.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRulesReturnsNullWhenNothingMatches() {
        DefaultNestedComponentRegistry emptyRegistry = new DefaultNestedComponentRegistry();

        Class<?> result = setter.getClassNameViaImplicitRules("nonExisting",
                AggregationType.AS_COMPLEX_PROPERTY, emptyRegistry);
        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // Private helper exposure – getParameterClassForMethod (indirectly via
    // computeRawAggregationType). Already exercised through computeAggregationType.
    // -----------------------------------------------------------------------

    @Test
    public void testComputeRawAggregationTypeBasicAndComplex() throws Exception {
        Method basicSetter = SimpleBean.class.getMethod("setAge", int.class);
        Method complexSetter = SimpleBean.class.getMethod("setSpecial", AnnotatedType.class);

        // Use reflection to invoke the private method computeRawAggregationType
        java.lang.reflect.Method privateMethod = PropertySetter.class
                .getDeclaredMethod("computeRawAggregationType", Method.class);
        privateMethod.setAccessible(true);

        AggregationType basic = (AggregationType) privateMethod.invoke(setter, basicSetter);
        AggregationType complex = (AggregationType) privateMethod.invoke(setter, complexSetter);

        assertEquals(AggregationType.AS_BASIC_PROPERTY, basic);
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, complex);
    }
}
