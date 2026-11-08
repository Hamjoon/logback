package ch.qos.logback.core.joran.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;

/**
 * Comprehensive unit tests for {@link PropertySetter}.
 *
 * The tests use real concrete classes only (no mocks or stubs of library
 * interfaces).  A minimal bean class is defined in the test source to act as
 * the target object on which properties are set.
 */
public class PropertySetterTest {

    /** Simple bean used as the target of property setting. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean flag;
        private String basicItem;
        private ComplexBean complexItem;

        // standard setters (used by setProperty)
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setFlag(boolean flag) { this.flag = flag; }

        // adder for a basic (String) property
        public void addItem(String item) { this.basicItem = item; }

        // setter for a complex property
        public void setComplex(ComplexBean complex) { this.complexItem = complex; }

        // adder for a complex property (collection style)
        public void addComplex(ComplexBean complex) { this.complexItem = complex; }

        // getters used for assertions
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isFlag() { return flag; }
        public String getBasicItem() { return basicItem; }
        public ComplexBean getComplexItem() { return complexItem; }
    }

    /** Simple complex type used for complex‑property tests. */
    public static class ComplexBean {
        private final String id;
        public ComplexBean(String id) { this.id = id; }
        public String getId() { return id; }
    }

    private PropertySetter propertySetter;
    private TestBean bean;

    @Before
    public void setUp() {
        // BeanDescriptionCache only needs a Context instance; passing null is safe for the
        // parts we exercise (no status handling is verified here).
        BeanDescriptionCache cache = new BeanDescriptionCache(null);
        bean = new TestBean();
        propertySetter = new PropertySetter(cache, bean);
    }

    // -------------------------------------------------------------------------
    // Tests for setProperty (basic scalar conversion)
    // -------------------------------------------------------------------------

    @Test
    public void testSetProperty_String() {
        propertySetter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetProperty_IntConversion() {
        propertySetter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_BooleanConversion() {
        propertySetter.setProperty("flag", "true");
        assertTrue(bean.isFlag());

        propertySetter.setProperty("flag", "false");
        assertFalse(bean.isFlag());
    }

    @Test
    public void testSetProperty_NullValueIsIgnored() {
        // No exception must be thrown and the existing value stays unchanged.
        bean.setName("initial");
        propertySetter.setProperty("name", null);
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetProperty_NoSuchSetterProducesNoException() {
        // The method should simply log a warning; we only verify that execution continues.
        propertySetter.setProperty("nonExisting", "value");
        // bean state must be unchanged.
        assertNull(bean.getName());
    }

    // -------------------------------------------------------------------------
    // Tests for addBasicProperty (adder with basic type conversion)
    // -------------------------------------------------------------------------

    @Test
    public void testAddBasicProperty_Successful() {
        // The adder method is named addItem(String).  The property name supplied to
        // addBasicProperty is "item" (lower‑case); the method capitalises the first
        // letter internally.
        propertySetter.addBasicProperty("item", "basicValue");
        assertEquals("basicValue", bean.getBasicItem());
    }

    @Test
    public void testAddBasicProperty_NullValueIsIgnored() {
        bean.addItem("alreadySet");
        propertySetter.addBasicProperty("item", null);
        // Value must stay the same.
        assertEquals("alreadySet", bean.getBasicItem());
    }

    @Test
    public void testAddBasicProperty_NoAdderFound() {
        // "unknown" does not correspond to any adder method; the call should not throw.
        propertySetter.addBasicProperty("unknown", "value");
        // No side‑effects expected.
        assertNull(bean.getBasicItem());
    }

    // -------------------------------------------------------------------------
    // Tests for addComplexProperty (adder with object argument)
    // -------------------------------------------------------------------------

    @Test
    public void testAddComplexProperty_Successful() {
        ComplexBean complex = new ComplexBean("c1");
        // The adder method is addComplex(ComplexBean)
        propertySetter.addComplexProperty("Complex", complex);
        assertSame(complex, bean.getComplexItem());
    }

    @Test
    public void testAddComplexProperty_WrongParameterTypeIsRejectedGracefully() {
        // Pass a String where a ComplexBean is expected; the method should detect the
        // type mismatch and simply return without throwing.
        propertySetter.addComplexProperty("Complex", "notAComplexBean");
        // The bean's complexItem must remain null.
        assertNull(bean.getComplexItem());
    }

    // -------------------------------------------------------------------------
    // Tests for setComplexProperty (setter with object argument)
    // -------------------------------------------------------------------------

    @Test
    public void testSetComplexProperty_Successful() {
        ComplexBean complex = new ComplexBean("c2");
        propertySetter.setComplexProperty("Complex", complex);
        assertSame(complex, bean.getComplexItem());
    }

    @Test
    public void testSetComplexProperty_NoSetterFound() {
        // Property name that does not match any setter.
        propertySetter.setComplexProperty("NonExisting", new ComplexBean("x"));
        // No exception, bean unchanged.
        assertNull(bean.getComplexItem());
    }

    // -------------------------------------------------------------------------
    // Tests for computeAggregationType and related helpers
    // -------------------------------------------------------------------------

    @Test
    public void testComputeAggregationType_UnknownPropertyReturnsNotFound() {
        AggregationType type = propertySetter.computeAggregationType("doesNotExist");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testGetClassNameViaImplicitRules_ReturnsNullWhenNoRuleMatches() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = propertySetter.getClassNameViaImplicitRules(
                "unknownTag", AggregationType.AS_BASIC_PROPERTY, registry);
        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // Simple accessor tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetObjClass_ReturnsTargetClass() {
        assertEquals(TestBean.class, propertySetter.getObjClass());
    }

    @Test
    public void testGetObj_ReturnsTargetInstance() {
        assertSame(bean, propertySetter.getObj());
    }

    // -------------------------------------------------------------------------
    // Helper method to retrieve the underlying Method objects (used for sanity‑check verification)
    // -------------------------------------------------------------------------

    @Test
    public void testSanityCheckRejectsMethodWithWrongParameterCount() throws Exception {
        // Create a dummy method with two parameters via reflection on a helper class.
        class Dummy {
            public void setTwo(String a, String b) {}
        }
        Method twoParamMethod = Dummy.class.getMethod("setTwo", String.class, String.class);
        // Directly invoke the private sanity‑check method via reflection.
        Method sanityCheck = PropertySetter.class.getDeclaredMethod(
                "isSanityCheckSuccessful", String.class, Method.class, Class[].class, Object.class);
        sanityCheck.setAccessible(true);
        boolean result = (boolean) sanityCheck.invoke(propertySetter,
                "two", twoParamMethod, twoParamMethod.getParameterTypes(), "value");
        assertFalse(result);
    }

    @Test
    public void testSanityCheckAcceptsCompatibleType() throws Exception {
        Method setter = TestBean.class.getMethod("setName", String.class);
        Method sanityCheck = PropertySetter.class.getDeclaredMethod(
                "isSanityCheckSuccessful", String.class, Method.class, Class[].class, Object.class);
        sanityCheck.setAccessible(true);
        boolean result = (boolean) sanityCheck.invoke(propertySetter,
                "name", setter, setter.getParameterTypes(), "anyString");
        assertTrue(result);
    }
}
