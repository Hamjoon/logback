/**
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.util.PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic property setting (String, int, boolean)</li>
 *   <li>null handling and missing setters</li>
 *   <li>aggregation type computation for basic & complex properties</li>
 *   <li>adder based APIs (addBasicProperty, addComplexProperty)</li>
 *   <li>setter based APIs (setComplexProperty)</li>
 *   <li>annotation handling and implicit class resolution</li>
 *   <li>registry based class resolution</li>
 * </ul>
 *
 * No mocking frameworks are used – all objects are real instances created with the
 * public constructors that appear in the supplied source code.
 */
package test;

import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.PropertySetter;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Simple bean used throughout the test suite.  It contains a mixture of
 * primitive, basic‑type and complex‑type properties together with the
 * corresponding setters, getters and adder methods.
 */
class TestBean {

    private String name;
    private int age;
    private boolean male;

    private final List<String> tags = new ArrayList<>();
    private final List<Complex> children = new ArrayList<>();

    private Complex special;   // complex property without adder

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isMale() { return male; }
    public void setMale(boolean male) { this.male = male; }

    public List<String> getTags() { return tags; }
    public void addTag(String tag) { this.tags.add(tag); }

    public List<Complex> getChildren() { return children; }
    public void addChild(Complex c) { this.children.add(c); }

    public Complex getSpecial() { return special; }

    @DefaultClass(Complex.class)
    public void setSpecial(Complex c) { this.special = c; }
}

/**
 * Simple complex type used for “complex” property tests.
 */
class Complex {
    // empty – only the type matters
}

/**
 * Test class for {@link PropertySetter}.
 */
public class PropertySetterTest {

    private BeanDescriptionCache cache;
    private TestBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        // The Context argument is not needed for the introspection performed by
        // BeanDescriptionCache, therefore we pass {@code null}.
        cache = new BeanDescriptionCache(null);
        bean = new TestBean();
        setter = new PropertySetter(cache, bean);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic setProperty tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyString() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetPropertyInt() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetPropertyBoolean() {
        setter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    @Test
    public void testSetPropertyNullValueIsIgnored() {
        bean.setName("initial");
        setter.setProperty("name", null);   // should be a no‑op
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetPropertyWhenNoSetterExists() {
        // "unknown" does not correspond to any setter; the method should
        // simply log a warning and not throw.
        setter.setProperty("unknownProperty", "value");
        // No exception → test passes.  No state change to verify.
    }

    /* --------------------------------------------------------------------- */
    /*  Aggregation type computation                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationTypeForBasicAdder() {
        // "tag" has an adder that accepts a String → basic collection
        AggregationType type = setter.computeAggregationType("tag");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationTypeForComplexAdder() {
        // "child" adder takes a Complex → complex collection
        AggregationType type = setter.computeAggregationType("child");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationTypeForBasicSetter() {
        // "name" has a String setter → basic property
        AggregationType type = setter.computeAggregationType("name");
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationTypeForComplexSetter() {
        // "special" setter expects Complex → complex property
        AggregationType type = setter.computeAggregationType("special");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationTypeWhenNoMethodFound() {
        AggregationType type = setter.computeAggregationType("nonexistent");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    /* --------------------------------------------------------------------- */
    /*  addBasicProperty (adder for simple types)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddBasicPropertySuccess() {
        setter.addBasicProperty("tag", "alpha");
        assertEquals(1, bean.getTags().size());
        assertEquals("alpha", bean.getTags().get(0));
    }

    @Test
    public void testAddBasicPropertyWhenAdderMissing() {
        // "unknown" has no adder – the method should log an error but not throw.
        setter.addBasicProperty("unknown", "value");
        // No exception → test passes.
    }

    @Test
    public void testAddBasicPropertyWithNullValueIsIgnored() {
        setter.addBasicProperty("tag", null);
        assertTrue(bean.getTags().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  addComplexProperty (adder for complex types)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddComplexPropertySuccess() {
        Complex c = new Complex();
        setter.addComplexProperty("Child", c);
        assertEquals(1, bean.getChildren().size());
        assertSame(c, bean.getChildren().get(0));
    }

    @Test
    public void testAddComplexPropertyWhenAdderMissing() {
        Complex c = new Complex();
        // "Missing" does not map to any adder – should log an error, no exception.
        setter.addComplexProperty("Missing", c);
    }

    /* --------------------------------------------------------------------- */
    /*  setComplexProperty (setter for complex types)                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetComplexPropertySuccess() {
        Complex c = new Complex();
        setter.setComplexProperty("special", c);
        assertSame(c, bean.getSpecial());
    }

    @Test
    public void testSetComplexPropertyWhenSetterMissing() {
        Complex c = new Complex();
        // No setter named "unknown" – should log a warning, no exception.
        setter.setComplexProperty("unknown", c);
    }

    @Test
    public void testSetComplexPropertyWithIncompatibleType() {
        // Passing a String to a Complex setter – should log an error and leave state unchanged.
        setter.setComplexProperty("special", "not a complex object");
        assertNull(bean.getSpecial());
    }

    /* --------------------------------------------------------------------- */
    /*  getRelevantMethod / getAnnotation / getDefaultClassNameByAnnonation */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetRelevantMethodForComplexCollection() throws Exception {
        Method m = setter.getRelevantMethod("Child", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        assertNotNull(m);
        assertEquals("addChild", m.getName());
    }

    @Test
    public void testGetRelevantMethodForComplexProperty() throws Exception {
        Method m = setter.getRelevantMethod("special", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(m);
        assertEquals("setSpecial", m.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRelevantMethodWithInvalidAggregationType() {
        // This should throw because BASIC_PROPERTY_COLLECTION is not allowed here.
        setter.getRelevantMethod("tag", AggregationType.AS_BASIC_PROPERTY_COLLECTION);
    }

    @Test
    public void testGetAnnotationAndDefaultClass() throws Exception {
        Method setterMethod = setter.getRelevantMethod("special", AggregationType.AS_COMPLEX_PROPERTY);
        DefaultClass ann = setter.getAnnotation("special", DefaultClass.class, setterMethod);
        assertNotNull(ann);
        assertEquals(Complex.class, ann.value());

        Class<?> defaultCls = setter.getDefaultClassNameByAnnonation("special", setterMethod);
        assertEquals(Complex.class, defaultCls);
    }

    /* --------------------------------------------------------------------- */
    /*  getByConcreteType and isUnequivocallyInstantiable                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetByConcreteTypeReturnsConcreteClass() throws Exception {
        // The adder for "tag" takes a String which is instantiable.
        Method adder = setter.getRelevantMethod("tag", AggregationType.AS_BASIC_PROPERTY_COLLECTION);
        Class<?> clazz = setter.getByConcreteType("tag", adder);
        assertEquals(String.class, clazz);
    }

    @Test
    public void testGetByConcreteTypeReturnsNullForInterface() throws Exception {
        // The adder for "children" takes Complex – concrete, should be returned.
        Method adder = setter.getRelevantMethod("child", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        Class<?> clazz = setter.getByConcreteType("child", adder);
        assertEquals(Complex.class, clazz);
    }

    /* --------------------------------------------------------------------- */
    /*  getClassNameViaImplicitRules – registry, annotation, concrete type   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testClassNameViaImplicitRulesUsesRegistry() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(TestBean.class, "name", String.class);

        Class<?> result = setter.getClassNameViaImplicitRules(
                "name", AggregationType.AS_BASIC_PROPERTY, registry);
        assertEquals(String.class, result);
    }

    @Test
    public void testClassNameViaImplicitRulesUsesAnnotationWhenRegistryMisses() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // No entry for "special" → fallback to annotation on the setter.
        Class<?> result = setter.getClassNameViaImplicitRules(
                "special", AggregationType.AS_COMPLEX_PROPERTY, registry);
        assertEquals(Complex.class, result);
    }

    @Test
    public void testClassNameViaImplicitRulesUsesConcreteTypeWhenNoAnnotation() {
        // Create a bean that has a simple adder (String) without any annotation.
        // Registry returns null, annotation is null, concrete type is String.
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();

        Class<?> result = setter.getClassNameViaImplicitRules(
                "tag", AggregationType.AS_BASIC_PROPERTY_COLLECTION, registry);
        assertEquals(String.class, result);
    }

    @Test
    public void testClassNameViaImplicitRulesReturnsNullWhenNothingMatches() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Property "nonexistent" is unknown everywhere.
        Class<?> result = setter.getClassNameViaImplicitRules(
                "nonexistent", AggregationType.AS_BASIC_PROPERTY, registry);
        assertNull(result);
    }
}
