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
 * The tests cover:
 * <ul>
 *   <li>normal property setting (String, int, boolean)</li>
 *   <li>edge cases – null values, missing setters, conversion failures</li>
 *   <li>aggregation type computation (basic, complex, collection, not‑found)</li>
 *   <li>instantiability checks</li>
 *   <li>complex‑property addition / setting with sanity checks</li>
 *   <li>annotation handling and implicit default‑component resolution</li>
 * </ul>
 * <p>
 * A small set of test beans is defined as static inner classes; no external
 * mocking frameworks are used – all objects are instantiated directly.
 */
public class PropertySetterTest {

    /* ----------------------------------------------------
     * Helper classes used by the tests
     * ---------------------------------------------------- */

    /** Minimal {@code Context} implementation – only needed to satisfy the constructor of {@link BeanDescriptionCache}. */
    private static class DummyContext implements ch.qos.logback.core.spi.Context {
        @Override public String getName() { return "dummy"; }
        @Override public void addStatus(ch.qos.logback.core.status.Status status) {}
        @Override public void addInfo(String msg) {}
        @Override public void addInfo(String msg, Throwable ex) {}
        @Override public void addWarn(String msg) {}
        @Override public void addWarn(String msg, Throwable ex) {}
        @Override public void addError(String msg) {}
        @Override public void addError(String msg, Throwable ex) {}
        @Override public void addStatusListener(ch.qos.logback.core.status.StatusListener listener) {}
        @Override public void detachStatusListener(ch.qos.logback.core.status.StatusListener listener) {}
        @Override public void reset() {}
        @Override public void start() {}
        @Override public void stop() {}
        @Override public boolean isStarted() { return true; }
        @Override public void setName(String name) {}
        @Override public void setObject(String key, Object value) {}
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public void removeObject(String key) {}
        @Override public java.util.Map<String, Object> getCopyOfPropertyMap() { return null; }
    }

    /** Simple bean with primitive‑type setters and a collection adder. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> items = new ArrayList<>();

        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setActive(boolean active) { this.active = active; }
        public boolean isActive() { return active; }

        public void addItem(String item) { items.add(item); }
        public List<String> getItems() { return items; }
    }

    /** Bean that expects a complex object via setter. */
    public static class ComplexBean {
        private ComplexComponent component;

        public void setComponent(ComplexComponent component) { this.component = component; }
        public ComplexComponent getComponent() { return component; }
    }

    /** A dummy complex component used in the tests. */
    public static class ComplexComponent {}

    /** Bean whose setter is annotated with {@link DefaultClass}. */
    public static class AnnotatedBean {
        private AnnotatedComponent comp;

        @DefaultClass(AnnotatedComponent.class)
        public void setComp(AnnotatedComponent comp) { this.comp = comp; }
        public AnnotatedComponent getComp() { return comp; }
    }

    public static class AnnotatedComponent {}

    /** Bean with an adder that expects a complex type – used for collection aggregation tests. */
    public static class CollectionBean {
        private final List<ComplexComponent> components = new ArrayList<>();

        public void addComponent(ComplexComponent c) { components.add(c); }
        public List<ComplexComponent> getComponents() { return components; }
    }

    /** Bean without a default constructor – used for {@code isUnequivocallyInstantiable} negative test. */
    public static class NoDefaultCtorBean {
        private NoDefaultCtorBean() {} // private, no accessible default ctor
    }

    /** Subclass of {@link PropertySetter} that captures messages emitted via the
     *  {@code ContextAwareBase} API. This allows assertions on warnings / errors
     *  without needing a real {@code StatusManager}. */
    private static class TestablePropertySetter extends PropertySetter {
        final List<String> warnings = new ArrayList<>();
        final List<String> errors   = new ArrayList<>();
        final List<String> infos    = new ArrayList<>();

        public TestablePropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        @Override public void addWarn(String msg) { warnings.add(msg); }
        @Override public void addWarn(String msg, Throwable ex) { warnings.add(msg + " : " + ex.getMessage()); }

        @Override public void addError(String msg) { errors.add(msg); }
        @Override public void addError(String msg, Throwable ex) { errors.add(msg + " : " + ex.getMessage()); }

        @Override public void addInfo(String msg) { infos.add(msg); }
        @Override public void addInfo(String msg, Throwable ex) { infos.add(msg + " : " + ex.getMessage()); }
    }

    private BeanDescriptionCache cache;
    private DummyContext dummyContext;

    @Before
    public void setUp() {
        dummyContext = new DummyContext();
        cache = new BeanDescriptionCache(dummyContext);
    }

    /* ----------------------------------------------------
     *  Tests for setProperty(...)
     * ---------------------------------------------------- */

    @Test
    public void testSetProperty_StringValue() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
        assertTrue(ps.warnings.isEmpty());
        assertTrue(ps.errors.isEmpty());
    }

    @Test
    public void testSetProperty_IntConversion() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setProperty("age", "33");
        assertEquals(33, bean.getAge());
    }

    @Test
    public void testSetProperty_BooleanConversion() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setProperty("active", "true");
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetProperty_NullValueIsIgnored() {
        SimpleBean bean = new SimpleBean();
        bean.setName("original");
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setProperty("name", null);
        assertEquals("original", bean.getName());
        assertTrue(ps.warnings.isEmpty());
    }

    @Test
    public void testSetProperty_MissingSetterProducesWarning() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setProperty("nonExisting", "value");
        assertEquals(1, ps.warnings.size());
        assertTrue(ps.warnings.get(0).contains("No setter for property [nonExisting]"));
    }

    @Test
    public void testSetProperty_ConversionFailureProducesWarning() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        // "abc" cannot be converted to int for the "age" property
        ps.setProperty("age", "abc");
        assertEquals(1, ps.warnings.size());
        assertTrue(ps.warnings.get(0).contains("Failed to set property [age]"));
    }

    /* ----------------------------------------------------
     *  Tests for computeAggregationType(...)
     * ---------------------------------------------------- */

    @Test
    public void testComputeAggregationType_BasicPropertyCollection() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        AggregationType type = ps.computeAggregationType("item");
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    @Test
    public void testComputeAggregationType_ComplexProperty() {
        ComplexBean bean = new ComplexBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        AggregationType type = ps.computeAggregationType("component");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationType_NotFound() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        AggregationType type = ps.computeAggregationType("unknownProp");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testComputeAggregationType_ComplexPropertyCollection() {
        CollectionBean bean = new CollectionBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        AggregationType type = ps.computeAggregationType("component");
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, type);
    }

    /* ----------------------------------------------------
     *  Tests for isUnequivocallyInstantiable(...)
     * ---------------------------------------------------- */

    @Test
    public void testIsUnequivocallyInstantiable_WithDefaultConstructor() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method m = PropertySetter.class.getDeclaredMethod("isUnequivocallyInstantiable", Class.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(ps, SimpleBean.class);
        assertTrue(result);
    }

    @Test
    public void testIsUnequivocallyInstantiable_InterfaceReturnsFalse() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method m = PropertySetter.class.getDeclaredMethod("isUnequivocallyInstantiable", Class.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(ps, java.util.List.class);
        assertFalse(result);
    }

    @Test
    public void testIsUnequivocallyInstantiable_NoDefaultCtorReturnsFalse() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method m = PropertySetter.class.getDeclaredMethod("isUnequivocallyInstantiable", Class.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(ps, NoDefaultCtorBean.class);
        assertFalse(result);
    }

    /* ----------------------------------------------------
     *  Tests for addComplexProperty(...)
     * ---------------------------------------------------- */

    @Test
    public void testAddComplexProperty_SuccessfulAdderInvocation() {
        CollectionBean bean = new CollectionBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ComplexComponent comp = new ComplexComponent();
        ps.addComplexProperty("Component", comp);
        assertEquals(1, bean.getComponents().size());
        assertSame(comp, bean.getComponents().get(0));
        assertTrue(ps.errors.isEmpty());
    }

    @Test
    public void testAddComplexProperty_WrongTypeProducesError() {
        CollectionBean bean = new CollectionBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        // pass a String where ComplexComponent is expected
        ps.addComplexProperty("Component", "notAComponent");
        assertEquals(1, ps.errors.size());
        assertTrue(ps.errors.get(0).contains("is not assignable"));
        assertTrue(bean.getComponents().isEmpty());
    }

    @Test
    public void testAddComplexProperty_NoAdderMethodProducesError() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.addComplexProperty("NonExisting", new Object());
        assertEquals(1, ps.errors.size());
        assertTrue(ps.errors.get(0).contains("Could not find method [addNonExisting]"));
    }

    /* ----------------------------------------------------
     *  Tests for addBasicProperty(...)
     * ---------------------------------------------------- */

    @Test
    public void testAddBasicProperty_SuccessfulStringAddition() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.addBasicProperty("item", "hello");
        assertEquals(1, bean.getItems().size());
        assertEquals("hello", bean.getItems().get(0));
        assertTrue(ps.errors.isEmpty());
    }

    @Test
    public void testAddBasicProperty_ConversionFailureProducesError() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        // No adder for "age" (int) exists, but we purposefully call addBasicProperty
        // on a non‑existing adder to hit the early error path.
        ps.addBasicProperty("age", "notANumber");
        assertEquals(1, ps.errors.size());
        assertTrue(ps.errors.get(0).contains("No adder for property [Age]"));
    }

    /* ----------------------------------------------------
     *  Tests for setComplexProperty(...)
     * ---------------------------------------------------- */

    @Test
    public void testSetComplexProperty_Success() {
        ComplexBean bean = new ComplexBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ComplexComponent comp = new ComplexComponent();
        ps.setComplexProperty("component", comp);
        assertSame(comp, bean.getComponent());
        assertTrue(ps.errors.isEmpty());
    }

    @Test
    public void testSetComplexProperty_WrongTypeProducesError() {
        ComplexBean bean = new ComplexBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setComplexProperty("component", "stringInsteadOfComponent");
        assertEquals(1, ps.errors.size());
        assertTrue(ps.errors.get(0).contains("is not assignable"));
        assertNull(bean.getComponent());
    }

    @Test
    public void testSetComplexProperty_MissingSetterProducesWarn() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.setComplexProperty("nonExisting", new Object());
        assertEquals(1, ps.warnings.size());
        assertTrue(ps.warnings.get(0).contains("Not setter method for property [nonExisting]"));
    }

    /* ----------------------------------------------------
     *  Tests for getRelevantMethod(...)
     * ---------------------------------------------------- */

    @Test
    public void testGetRelevantMethod_ComplexProperty() throws Exception {
        ComplexBean bean = new ComplexBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method m = ps.getRelevantMethod("component", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(m);
        assertEquals("setComponent", m.getName());
    }

    @Test
    public void testGetRelevantMethod_ComplexPropertyCollection() throws Exception {
        CollectionBean bean = new CollectionBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method m = ps.getRelevantMethod("component", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
        assertNotNull(m);
        assertEquals("addComponent", m.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRelevantMethod_UnsupportedAggregationThrows() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        ps.getRelevantMethod("name", AggregationType.AS_BASIC_PROPERTY); // not allowed here
    }

    /* ----------------------------------------------------
     *  Tests for annotation handling
     * ---------------------------------------------------- */

    @Test
    public void testGetAnnotation_PresentOnMethod() throws Exception {
        AnnotatedBean bean = new AnnotatedBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method setter = ps.findSetterMethod("comp");
        DefaultClass ann = ps.getAnnotation("comp", DefaultClass.class, setter);
        assertNotNull(ann);
        assertEquals(AnnotatedComponent.class, ann.value());
    }

    @Test
    public void testGetAnnotation_NoAnnotationReturnsNull() throws Exception {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method setter = ps.findSetterMethod("name");
        DefaultClass ann = ps.getAnnotation("name", DefaultClass.class, setter);
        assertNull(ann);
    }

    @Test
    public void testGetDefaultClassNameByAnnotation_Found() throws Exception {
        AnnotatedBean bean = new AnnotatedBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method setter = ps.findSetterMethod("comp");
        Class<?> result = ps.getDefaultClassNameByAnnonation("comp", setter);
        assertEquals(AnnotatedComponent.class, result);
    }

    @Test
    public void testGetDefaultClassNameByAnnotation_NotFound() throws Exception {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Method setter = ps.findSetterMethod("name");
        Class<?> result = ps.getDefaultClassNameByAnnonation("name", setter);
        assertNull(result);
    }

    /* ----------------------------------------------------
     *  Tests for getClassNameViaImplicitRules(...)
     * ---------------------------------------------------- */

    @Test
    public void testGetClassNameViaImplicitRules_RegistryProvidesResult() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        registry.add(SimpleBean.class, "name", String.class); // arbitrary mapping
        Class<?> result = ps.getClassNameViaImplicitRules("name", AggregationType.AS_BASIC_PROPERTY, registry);
        assertEquals(String.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_AnnotationProvidesResult() {
        AnnotatedBean bean = new AnnotatedBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry(); // empty
        Class<?> result = ps.getClassNameViaImplicitRules("comp", AggregationType.AS_COMPLEX_PROPERTY, registry);
        assertEquals(AnnotatedComponent.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_ConcreteTypeProvidesResult() {
        ComplexBean bean = new ComplexBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry(); // empty
        Class<?> result = ps.getClassNameViaImplicitRules("component", AggregationType.AS_COMPLEX_PROPERTY, registry);
        // ComplexComponent has a public default ctor → unequivocally instantiable
        assertEquals(ComplexComponent.class, result);
    }

    @Test
    public void testGetClassNameViaImplicitRules_NotFoundReturnsNull() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry(); // empty
        Class<?> result = ps.getClassNameViaImplicitRules("unknown", AggregationType.AS_BASIC_PROPERTY, registry);
        assertNull(result);
    }

    /* ----------------------------------------------------
     *  Miscellaneous utility tests
     * ---------------------------------------------------- */

    @Test
    public void testCapitalizeFirstLetter() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method m = PropertySetter.class.getDeclaredMethod("capitalizeFirstLetter", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(ps, "john");
        assertEquals("John", result);
    }

    @Test
    public void testGetParameterClassForMethod_ValidMethod() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method setter = ps.findSetterMethod("age"); // setAge(int)
        Method m = PropertySetter.class.getDeclaredMethod("getParameterClassForMethod", Method.class);
        m.setAccessible(true);
        Class<?> param = (Class<?>) m.invoke(ps, setter);
        assertEquals(int.class, param);
    }

    @Test
    public void testGetParameterClassForMethod_NullMethodReturnsNull() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method m = PropertySetter.class.getDeclaredMethod("getParameterClassForMethod", Method.class);
        m.setAccessible(true);
        assertNull(m.invoke(ps, (Object) null));
    }

    @Test
    public void testComputeRawAggregationType_Basic() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new SimpleBean());
        Method setter = ps.findSetterMethod("name"); // String
        Method m = PropertySetter.class.getDeclaredMethod("computeRawAggregationType", Method.class);
        m.setAccessible(true);
        AggregationType type = (AggregationType) m.invoke(ps, setter);
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeRawAggregationType_Complex() throws Exception {
        TestablePropertySetter ps = new TestablePropertySetter(cache, new ComplexBean());
        Method setter = ps.findSetterMethod("component"); // ComplexComponent
        Method m = PropertySetter.class.getDeclaredMethod("computeRawAggregationType", Method.class);
        m.setAccessible(true);
        AggregationType type = (AggregationType) m.invoke(ps, setter);
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testComputeRawAggregationType_NotFoundWhenMethodHasNoParams() throws Exception {
        // create a bean with a method that takes no arguments (should be ignored)
        class NoParamBean {
            public void setSomething() {}
        }
        TestablePropertySetter ps = new TestablePropertySetter(cache, new NoParamBean());
        Method setter = ps.findSetterMethod("something");
        Method m = PropertySetter.class.getDeclaredMethod("computeRawAggregationType", Method.class);
        m.setAccessible(true);
        AggregationType type = (AggregationType) m.invoke(ps, setter);
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testGetObjClass() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        assertEquals(SimpleBean.class, ps.getObjClass());
    }

    @Test
    public void testGetObj() {
        SimpleBean bean = new SimpleBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        assertSame(bean, ps.getObj());
    }
}
