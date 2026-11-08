package ch.qos.logback.core.joran.util;

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
 * The tests cover:
 * <ul>
 *   <li>Extraction of public method signatures.</li>
 *   <li>Basic successful usage of {@code setProperty}, {@code addBasicProperty},
 *       {@code addComplexProperty} and {@code setComplexProperty}.</li>
 *   <li>Edge‑case handling such as null values, missing setters/adders,
 *       conversion failures and type‑mismatch sanity checks.</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are real instances
 * created through the public constructors that appear in the supplied source
 * code.
 */
public class PropertySetterTest {

    /* ----------------------------------------------------------------------
     * Helper classes used as test subjects
     * ---------------------------------------------------------------------- */

    /** Simple bean with primitive‑type setters and an adder for a collection. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> tags = new ArrayList<>();

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }

        public void addTag(String tag) { this.tags.add(tag); }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public List<String> getTags() { return tags; }
    }

    /** Bean used to test complex‑object handling (setter expects an object). */
    public static class ComplexBean {
        private SimpleBean child;

        public void setChild(SimpleBean child) { this.child = child; }
        public SimpleBean getChild() { return child; }
    }

    /* ----------------------------------------------------------------------
     * The objects under test
     * ---------------------------------------------------------------------- */

    private BeanDescriptionCache beanCache;
    private PropertySetter setterForSimple;
    private PropertySetter setterForComplex;

    @Before
    public void setUp() {
        /*
         * The library’s BeanDescriptionCache requires a Context instance.
         * The simplest way to satisfy the constructor without pulling in
         * additional classes is to pass {@code null}.  In the current
         * Logback implementation the cache tolerates a null context for
         * reflection‑only operations (which is all we need for these tests).
         */
        beanCache = new BeanDescriptionCache((Context) null);

        SimpleBean simpleBean = new SimpleBean();
        ComplexBean complexBean = new ComplexBean();

        setterForSimple = new PropertySetter(beanCache, simpleBean);
        setterForComplex = new PropertySetter(beanCache, complexBean);
    }

    /* ----------------------------------------------------------------------
     * 1.  Public method signatures (extracted manually from the source)
     * ---------------------------------------------------------------------- */

    /**
     * The public API of {@link PropertySetter} that must be covered.
     *
     * <pre>
     * public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
     * public void setContext(Context context)
     * public void setProperty(String name, String value)
     * public AggregationType computeAggregationType(String name)
     * public Class<?> getObjClass()
     * public void addComplexProperty(String name, Object complexProperty)
     * public void addBasicProperty(String name, String strValue)
     * public void setComplexProperty(String name, Object complexProperty)
     * public Object getObj()
     * public Class<?> getClassNameViaImplicitRules(String name,
     *          AggregationType aggregationType,
     *          DefaultNestedComponentRegistry registry)
     * </pre>
     */

    /* ----------------------------------------------------------------------
     * 2.  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetProperty_String() {
        setterForSimple.setProperty("name", "John Doe");
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetProperty_Int() {
        setterForSimple.setProperty("age", "42");
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_Boolean() {
        setterForSimple.setProperty("active", "true");
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        assertTrue(bean.isActive());
    }

    @Test
    public void testAddBasicProperty_Collection() {
        // The adder method is addTag(String)
        setterForSimple.addBasicProperty("tag", "alpha");
        setterForSimple.addBasicProperty("tag", "beta");

        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        List<String> tags = bean.getTags();
        assertEquals(2, tags.size());
        assertTrue(tags.contains("alpha"));
        assertTrue(tags.contains("beta"));
    }

    @Test
    public void testAddComplexProperty_Success() {
        SimpleBean child = new SimpleBean();
        child.setName("Child");
        // ComplexBean has a setter setChild(SimpleBean)
        setterForComplex.addComplexProperty("Child", child);

        ComplexBean bean = (ComplexBean) setterForComplex.getObj();
        assertNotNull(bean.getChild());
        assertEquals("Child", bean.getChild().getName());
    }

    @Test
    public void testSetComplexProperty_Success() {
        SimpleBean child = new SimpleBean();
        child.setName("Nested");
        // setComplexProperty ultimately invokes setChild(...)
        setterForComplex.setComplexProperty("child", child);

        ComplexBean bean = (ComplexBean) setterForComplex.getObj();
        assertNotNull(bean.getChild());
        assertEquals("Nested", bean.getChild().getName());
    }

    @Test
    public void testGetObjClass() {
        assertEquals(SimpleBean.class, setterForSimple.getObjClass());
    }

    @Test
    public void testGetObj() {
        assertTrue(setterForSimple.getObj() instanceof SimpleBean);
    }

    @Test
    public void testComputeAggregationType_NotFound() {
        // "nonExisting" does not map to any property; the assessor should
        // return NOT_FOUND.
        AggregationType type = setterForSimple.computeAggregationType("nonExisting");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    /* ----------------------------------------------------------------------
     * 3.  Edge‑case and exception handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetProperty_NullValue_IsIgnored() {
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        bean.setName("initial");
        setterForSimple.setProperty("name", null); // should be ignored
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetProperty_NoSuchSetter_WarnsButDoesNotThrow() {
        // Property "unknown" has no setter – the method should simply return.
        // No exception must be thrown and the bean stays unchanged.
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        bean.setName("unchanged");
        setterForSimple.setProperty("unknown", "value");
        assertEquals("unchanged", bean.getName());
    }

    @Test
    public void testSetProperty_InvalidConversion_WarnsButDoesNotThrow() {
        // The "age" setter expects an int; passing a non‑numeric string should
        // cause a conversion failure that is caught internally.
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        bean.setAge(10); // set a known value first
        setterForSimple.setProperty("age", "notANumber");
        // value must remain unchanged because conversion failed.
        assertEquals(10, bean.getAge());
    }

    @Test
    public void testAddBasicProperty_NullString_IsIgnored() {
        SimpleBean bean = (SimpleBean) setterForSimple.getObj();
        int initialSize = bean.getTags().size();
        setterForSimple.addBasicProperty("tag", null);
        assertEquals(initialSize, bean.getTags().size());
    }

    @Test
    public void testAddComplexProperty_WrongParameterType() {
        // ComplexBean expects SimpleBean, we give a String – sanity check should
        // fail and the child must stay null.
        setterForComplex.addComplexProperty("Child", "I am not a bean");
        ComplexBean bean = (ComplexBean) setterForComplex.getObj();
        assertNull(bean.getChild());
    }

    @Test
    public void testSetComplexProperty_WrongParameterType() {
        // Same sanity‑check scenario but via setComplexProperty()
        setterForComplex.setComplexProperty("child", "bad type");
        ComplexBean bean = (ComplexBean) setterForComplex.getObj();
        assertNull(bean.getChild());
    }

    @Test
    public void testGetClassNameViaImplicitRules_DelegatesToAssessor() {
        // The method simply forwards the call; we verify that it does not throw
        // and returns a Class (or null) for a known property.
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = setterForSimple.getClassNameViaImplicitRules(
                "tag",
                AggregationType.AS_BASIC_PROPERTY_COLLECTION,
                registry);
        // For the simple bean there is no implicit rule, so result should be null.
        assertNull(result);
    }
}
