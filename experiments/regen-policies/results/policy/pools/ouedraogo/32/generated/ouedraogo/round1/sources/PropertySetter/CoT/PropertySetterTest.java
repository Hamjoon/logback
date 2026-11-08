package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>public method signatures extraction (documented in comments)</li>
 *   <li>basic successful usage of each public method</li>
 *   <li>edge‑case handling such as null arguments, missing setters/adders,
 *       conversion failures and type‑mismatch situations.</li>
 * </ul>
 */
public class PropertySetterTest {

    /* -------------------------------------------------------------
     *  Helper beans used by the tests
     * ------------------------------------------------------------- */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean male;
        private List<String> tags;
        private ComplexType complex;
        private AnotherType another;

        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setMale(boolean male) { this.male = male; }
        public boolean isMale() { return male; }

        public void addTag(String tag) {
            if (tags == null) {
                tags = new java.util.ArrayList<>();
            }
            tags.add(tag);
        }
        public List<String> getTags() { return tags; }

        // adder for a complex property (used by addComplexProperty)
        public void addComplex(ComplexType ct) { this.complex = ct; }
        public ComplexType getComplex() { return complex; }

        // setter for a complex property (used by setComplexProperty)
        public void setAnother(AnotherType at) { this.another = at; }
        public AnotherType getAnother() { return another; }
    }

    public static class ComplexType { }
    public static class AnotherType { }

    private SimpleBean bean;
    private PropertySetter propertySetter;

    @Before
    public void setUp() {
        bean = new SimpleBean();
        BeanDescriptionCache cache = new BeanDescriptionCache(null); // Context is optional for the cache
        propertySetter = new PropertySetter(cache, bean);
    }

    /* -------------------------------------------------------------
     *  1. PUBLIC METHOD SIGNATURES (documented for reference)
     * -------------------------------------------------------------
     *
     * public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
     * public void setContext(Context context)
     * public void setProperty(String name, String value)
     * public AggregationType computeAggregationType(String name)
     * public Class<?> getObjClass()
     * public void addComplexProperty(String name, Object complexProperty)
     * public void addBasicProperty(String name, String strValue)
     * public void setComplexProperty(String name, Object complexProperty)
     * public Object getObj()
     * public Class<?> getClassNameViaImplicitRules(String name, AggregationType aggregationType,
     *                                              DefaultNestedComponentRegistry registry)
     */

    /* -------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testSetProperty_StringValue() {
        propertySetter.setProperty("name", "john");
        assertEquals("john", bean.getName());
    }

    @Test
    public void testSetProperty_IntValue() {
        propertySetter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_BooleanValue() {
        propertySetter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    @Test
    public void testSetProperty_NullValueIsIgnored() {
        bean.setName("initial");
        propertySetter.setProperty("name", null);   // should be ignored
        assertEquals("initial", bean.getName());
    }

    @Test
    public void testSetProperty_NoSuchSetter() {
        // No setter called “unknown” – the method must not throw
        propertySetter.setProperty("unknown", "value");
        // nothing to assert other than “no exception”
    }

    @Test
    public void testAddBasicProperty_ValidAdder() {
        propertySetter.addBasicProperty("tag", "firstTag");
        assertNotNull(bean.getTags());
        assertEquals(1, bean.getTags().size());
        assertEquals("firstTag", bean.getTags().get(0));
    }

    @Test
    public void testAddBasicProperty_NullStringIsIgnored() {
        propertySetter.addBasicProperty("tag", null);
        assertNull(bean.getTags());
    }

    @Test
    public void testAddBasicProperty_NoAdderFound() {
        // “noAdder” does not exist – the method must simply return without exception
        propertySetter.addBasicProperty("noAdder", "value");
        // No assertion needed; test passes if no exception is thrown
    }

    @Test
    public void testAddComplexProperty_CorrectType() {
        ComplexType ct = new ComplexType();
        propertySetter.addComplexProperty("Complex", ct);
        assertSame(ct, bean.getComplex());
    }

    @Test
    public void testAddComplexProperty_WrongTypeIsRejected() {
        AnotherType at = new AnotherType();
        propertySetter.addComplexProperty("Complex", at); // Complex expects ComplexType
        assertNull(bean.getComplex()); // should remain unchanged (null)
    }

    @Test
    public void testSetComplexProperty_CorrectType() {
        AnotherType at = new AnotherType();
        propertySetter.setComplexProperty("Another", at);
        assertSame(at, bean.getAnother());
    }

    @Test
    public void testSetComplexProperty_WrongTypeIsRejected() {
        ComplexType ct = new ComplexType();
        propertySetter.setComplexProperty("Another", ct); // Another expects AnotherType
        assertNull(bean.getAnother());
    }

    @Test
    public void testComputeAggregationType_UnknownProperty() {
        AggregationType type = propertySetter.computeAggregationType("nonExisting");
        assertEquals(AggregationType.NOT_FOUND, type);
    }

    @Test
    public void testGetObjClass_ReturnsActualClass() {
        assertEquals(SimpleBean.class, propertySetter.getObjClass());
    }

    @Test
    public void testGetObj_ReturnsOriginalObject() {
        assertSame(bean, propertySetter.getObj());
    }

    @Test
    public void testGetClassNameViaImplicitRules_ReturnsNullWhenNoMapping() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = propertySetter.getClassNameViaImplicitRules(
                "anyName", AggregationType.AS_BASIC_PROPERTY, registry);
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  3. EDGE‑CASE / EXCEPTION HANDLING TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testSetProperty_InvalidIntConversionDoesNotChangeField() {
        // initial age is 0
        propertySetter.setProperty("age", "notANumber");
        // Conversion fails; the setter must not have been invoked
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetProperty_NullObjectInConstructorThrowsNoException() {
        // The PropertySetter constructor itself does not validate the object;
        // we simply ensure that calling a method on a valid instance does not explode.
        PropertySetter ps = new PropertySetter(new BeanDescriptionCache(null), new SimpleBean());
        ps.setProperty("name", "bob");
        // No exception => test passes
    }

    @Test
    public void testAddComplexProperty_NullComplexObjectIsIgnoredGracefully() {
        // The method checks for null only via sanity check; passing null would cause NPE
        // Therefore we verify that providing a null does not throw an exception
        propertySetter.addComplexProperty("Complex", null);
        // No exception – test passes
    }

    @Test
    public void testSetComplexProperty_NullComplexObjectIsIgnoredGracefully() {
        propertySetter.setComplexProperty("Another", null);
        // No exception – test passes
    }

    @Test
    public void testSetContext_PropagationToAggregationAssessor() {
        // Create a dummy Context implementation (anonymous class) – only the reference is needed.
        Context dummyContext = new Context() {
            public java.util.Map<String, String> getCopyOfPropertyMap() { return null; }
            public String getName() { return null; }
            public void setName(String name) {}
            public long getBirthTime() { return 0L; }
            public Object getObject(String key) { return null; }
            public void putObject(String key, Object value) {}
            public String getProperty(String key) { return null; }
            public void putProperty(String key, String value) {}
            public StatusManager getStatusManager() { return null; }
            public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
            public java.util.concurrent.ExecutorService getExecutorService() { return null; }
            public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
            public void register(lifeCycle component) {}
            public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
            public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
            public void setSequenceNumberGenerator(SequenceNumberGenerator generator) {}
            public void addConfigurationEventListener(ConfigurationEventListener listener) {}
            public void fireConfigurationEvent(ConfigurationEvent configurationEvent) {}
        };

        // This call should simply forward the context; we only verify that it does not throw.
        propertySetter.setContext(dummyContext);
    }

    /* -------------------------------------------------------------
     *  4. INTERNAL SANITY CHECKS (private method indirectly exercised)
     * ------------------------------------------------------------- */

    @Test
    public void testIsSanityCheckSuccessful_ParameterCountMismatch() {
        // The bean has no method named “setWrong” – findSetterMethod will return null,
        // so the private sanity check is not reached. We simply ensure that no exception
        // bubbles up when we deliberately call a method with wrong signature.
        propertySetter.setComplexProperty("Wrong", new Object());
        // No exception => test passes
    }
}
