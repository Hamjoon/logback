package ch.qos.logback.core.joran.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;

/**
 * Unit tests for {@link PropertySetter}.  The tests use simple POJO
 * classes that expose the different kinds of setters / adders that
 * {@code PropertySetter} expects to find via reflection.
 *
 * The {@link BeanDescriptionCache} is instantiated with a {@code null}
 * {@link ch.qos.logback.core.Context} – the cache tolerates a null
 * context because it only needs the class information to build a
 * {@link ch.qos.logback.core.joran.util.beans.BeanDescription}.
 *
 * All public behaviours of {@code PropertySetter} are exercised:
 *
 * <ul>
 *   <li>setting simple {@code String}, {@code int} and {@code boolean}
 *       properties</li>
 *   <li>handling of unknown properties and conversion errors</li>
 *   <li>adding basic (String) and complex (object) properties via adder
 *       methods</li>
 *   <li>setting complex properties via a traditional setter</li>
 *   <li>exposing helper methods such as {@code computeAggregationType},
 *       {@code getObjClass} and {@code getClassNameViaImplicitRules}</li>
 * </ul>
 */
public class PropertySetterTest {

    /** Simple bean used for the majority of the tests. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private ChildBean child;               // complex property (setter)
        private ChildBean addedChild;           // complex property (adder)
        private String addedTag;                // basic property (adder)

        /* ----- simple setters ----- */
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }

        /* ----- complex setter ----- */
        public void setChild(ChildBean child) { this.child = child; }

        /* ----- adder methods ----- */
        public void addChild(ChildBean child) { this.addedChild = child; }
        public void addTag(String tag) { this.addedTag = tag; }

        /* ----- getters for assertions ----- */
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public ChildBean getChild() { return child; }
        public ChildBean getAddedChild() { return addedChild; }
        public String getAddedTag() { return addedTag; }
    }

    /** Simple complex type used for adder / setter tests. */
    public static class ChildBean {
        private final String id;
        public ChildBean(String id) { this.id = id; }
        public String getId() { return id; }
    }

    private PropertySetter setter;
    private TestBean bean;

    @Before
    public void setUp() {
        bean = new TestBean();
        // BeanDescriptionCache tolerates a null Context – it only needs the class to build a description.
        BeanDescriptionCache cache = new BeanDescriptionCache(null);
        setter = new PropertySetter(cache, bean);
    }

    /* ----------------------------------------------------
     *  Simple property setting – String, int, boolean
     * ---------------------------------------------------- */
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
        setter.setProperty("active", "true");
        assertTrue(bean.isActive());
    }

    /* ----------------------------------------------------
     *  Conversion failure – int property receives non‑numeric value
     * ---------------------------------------------------- */
    @Test
    public void testSetPropertyInvalidConversion() {
        // Invalid integer – the setter will not be invoked, the field stays at its default value (0)
        setter.setProperty("age", "not-a-number");
        assertEquals(0, bean.getAge());
    }

    /* ----------------------------------------------------
     *  Unknown property – no matching setter
     * ---------------------------------------------------- */
    @Test
    public void testSetPropertyUnknownProperty() {
        // No setter named setUnknown, should not throw and simply log a warning.
        setter.setProperty("unknown", "value");
        // Verify that existing fields are untouched.
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertFalse(bean.isActive());
    }

    /* ----------------------------------------------------
     *  Adding a basic (String) property via an adder method.
     * ---------------------------------------------------- */
    @Test
    public void testAddBasicProperty() {
        // The adder method is addTag(String).  The PropertySetter capitalises the first
        // letter of the supplied name, so we pass "tag".
        setter.addBasicProperty("tag", "myTagValue");
        assertEquals("myTagValue", bean.getAddedTag());
    }

    @Test
    public void testAddBasicPropertyNullValueIsIgnored() {
        // Passing a null value must be ignored – the adder should never be invoked.
        setter.addBasicProperty("tag", null);
        assertNull(bean.getAddedTag());
    }

    /* ----------------------------------------------------
     *  Adding a complex property via an adder method.
     * ---------------------------------------------------- */
    @Test
    public void testAddComplexProperty() {
        ChildBean child = new ChildBean("c1");
        // The adder method is addChild(ChildBean)
        setter.addComplexProperty("Child", child);
        assertSame(child, bean.getAddedChild());
    }

    @Test
    public void testAddComplexPropertyWithWrongType() {
        // Pass a String where a ChildBean is expected – the sanity check should log an error
        // and the method must not be invoked.
        setter.addComplexProperty("Child", "notAChildBean");
        assertNull(bean.getAddedChild());
    }

    /* ----------------------------------------------------
     *  Setting a complex property via a regular setter.
     * ---------------------------------------------------- */
    @Test
    public void testSetComplexProperty() {
        ChildBean child = new ChildBean("c2");
        // The setter method is setChild(ChildBean)
        setter.setComplexProperty("child", child);
        assertSame(child, bean.getChild());
    }

    @Test
    public void testSetComplexPropertyNoSetter() {
        // No setter called "setMissing", should only log a warning.
        ChildBean child = new ChildBean("c3");
        setter.setComplexProperty("missing", child);
        // Verify that nothing was set on the bean.
        assertNull(bean.getChild());
    }

    /* ----------------------------------------------------
     *  Miscellaneous helper methods
     * ---------------------------------------------------- */
    @Test
    public void testGetObjClass() {
        assertEquals(TestBean.class, setter.getObjClass());
    }

    @Test
    public void testComputeAggregationTypeNotFound() {
        // The bean does not declare any collection‑type property named "foo",
        // therefore the assessor should return NOT_FOUND.
        AggregationType type = setter.computeAggregationType("foo");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testGetClassNameViaImplicitRules() {
        // Use a fresh DefaultNestedComponentRegistry – the bean does not define any
        // implicit rules, so the result should be null.
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = setter.getClassNameViaImplicitRules(
                "anyName", AggregationType.AS_BASIC_PROPERTY, registry);
        assertNull(result);
    }

    /* ----------------------------------------------------
     *  Direct invocation of the private helper via reflection
     *  (sanity‑check logic) – ensures that the method behaves as
     *  expected when the parameter type is incompatible.
     * ---------------------------------------------------- */
    @Test
    public void testIsSanityCheckSuccessfulViaReflection() throws Exception {
        Method method = PropertySetter.class
                .getDeclaredMethod("isSanityCheckSuccessful", String.class, Method.class,
                        Class[].class, Object.class);
        method.setAccessible(true);

        // Prepare a method that expects a ChildBean
        Method adder = TestBean.class.getMethod("addChild", ChildBean.class);
        Class<?>[] paramTypes = adder.getParameterTypes();

        // Pass an incompatible argument (String) – the method should return false.
        boolean ok = (boolean) method.invoke(setter, "Child", adder, paramTypes, "bad");
        assertFalse(ok);
    }
}
