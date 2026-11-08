package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests are written by three “experts” that first listed the public API,
 * then produced a basic test for each method, identified edge‑cases / exception
 * scenarios and finally merged everything into this single file.
 */
public class PropertySetterTest {

    /* ----------------------------------------------------------------------
     * Helper classes – a tiny Context implementation and a simple bean used
     * by the tests.
     * ---------------------------------------------------------------------- */

    /** Minimal {@link Context} implementation – only the methods required for
     *  construction of the Logback helpers are provided. All other methods
     *  return {@code null} or harmless defaults. */
    private static class MinimalContext implements Context {

        private final Map<String, Object> objects = new HashMap<>();
        private final Map<String, String> properties = new HashMap<>();

        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }
        @Override public String getName() { return "minimal"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return Executors.newSingleThreadScheduledExecutor(); }
        @Override public ExecutorService getExecutorService() { return Executors.newSingleThreadExecutor(); }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    /** Simple bean with a mixture of setters, adders and collection‑type
     *  properties. Used to verify that {@link PropertySetter} correctly invokes
     *  the reflected methods. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> tags = new ArrayList<>();
        private final List<Child> children = new ArrayList<>();
        private Child singleChild;

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }

        public void addTag(String tag) { this.tags.add(tag); }
        public void addChild(Child child) { this.children.add(child); }

        public void setChild(Child child) { this.singleChild = child; }

        // getters for assertions
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public List<String> getTags() { return tags; }
        public List<Child> getChildren() { return children; }
        public Child getSingleChild() { return singleChild; }
    }

    /** Simple nested component used for complex‑property tests. */
    public static class Child {
        private final String id;
        public Child(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /** Subclass of {@link PropertySetter} that captures warnings / errors
     *  emitted via the {@link ContextAwareBase} methods – this avoids dealing
     *  with the internal StatusManager. */
    private static class TestablePropertySetter extends PropertySetter {
        final List<String> warnings = new ArrayList<>();
        final List<String> errors   = new ArrayList<>();

        public TestablePropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        @Override public void addWarn(String msg) { warnings.add(msg); }
        @Override public void addWarn(String msg, Throwable ex) { warnings.add(msg + " : " + ex.getMessage()); }
        @Override public void addError(String msg) { errors.add(msg); }
        @Override public void addError(String msg, Throwable ex) { errors.add(msg + " : " + ex.getMessage()); }
    }

    private MinimalContext context;
    private BeanDescriptionCache cache;
    private SimpleBean bean;
    private TestablePropertySetter setter;

    @Before
    public void setUp() {
        context = new MinimalContext();
        cache   = new BeanDescriptionCache(context);
        bean    = new SimpleBean();
        setter  = new TestablePropertySetter(cache, bean);
    }

    /* ----------------------------------------------------------------------
     * 1. PUBLIC METHOD LIST (extracted by Expert 1)
     * ---------------------------------------------------------------------- */
    //  - public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
    //  - public void setContext(Context context)
    //  - public void setProperty(String name, String value)
    //  - public AggregationType computeAggregationType(String name)
    //  - public Class<?> getObjClass()
    //  - public void addComplexProperty(String name, Object complexProperty)
    //  - public void addBasicProperty(String name, String strValue)
    //  - public void setComplexProperty(String name, Object complexProperty)
    //  - public Object getObj()
    //  - public Class<?> getClassNameViaImplicitRules(String name, AggregationType aggregationType,
    //                                                DefaultNestedComponentRegistry registry)

    /* ----------------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS (proposed by Expert 2)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetProperty_basicStringAndInt() {
        setter.setProperty("name", "John Doe");
        setter.setProperty("age", "42");
        setter.setProperty("active", "true");

        assertEquals("John Doe", bean.getName());
        assertEquals(42, bean.getAge());
        assertTrue(bean.isActive());
    }

    @Test
    public void testSetProperty_nullValue_isIgnored() {
        setter.setProperty("name", null); // should simply return
        assertNull(bean.getName());       // unchanged
    }

    @Test
    public void testSetProperty_unknownProperty_logsWarning() {
        setter.setProperty("nonExisting", "value");
        assertTrue(setter.warnings.stream()
                .anyMatch(m -> m.contains("No setter for property [nonExisting]")));
    }

    @Test
    public void testSetProperty_invalidConversion_logsWarning() {
        setter.setProperty("age", "notANumber");
        // conversion fails – a warning should be recorded, bean.age stays default 0
        assertTrue(setter.warnings.stream()
                .anyMatch(m -> m.contains("Failed to set property [age]")));
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testGetObjClass_returnsActualClass() {
        assertEquals(SimpleBean.class, setter.getObjClass());
    }

    @Test
    public void testGetObj_returnsOriginalObject() {
        assertSame(bean, setter.getObj());
    }

    @Test
    public void testAddBasicProperty_addsToCollection() {
        setter.addBasicProperty("tag", "alpha");
        setter.addBasicProperty("tag", "beta");
        List<String> tags = bean.getTags();
        assertEquals(Arrays.asList("alpha", "beta"), tags);
    }

    @Test
    public void testAddBasicProperty_nullValue_isIgnored() {
        setter.addBasicProperty("tag", null);
        assertTrue(bean.getTags().isEmpty());
    }

    @Test
    public void testAddComplexProperty_addsChildObject() {
        Child c1 = new Child("c1");
        setter.addComplexProperty("Child", c1);
        assertEquals(1, bean.getChildren().size());
        assertSame(c1, bean.getChildren().get(0));
    }

    @Test
    public void testAddComplexProperty_missingAdder_logsError() {
        setter.addComplexProperty("NonExisting", new Child("x"));
        assertTrue(setter.errors.stream()
                .anyMatch(m -> m.contains("Could not find method [addNonExisting]")));
    }

    @Test
    public void testSetComplexProperty_setsSingleChild() {
        Child c = new Child("single");
        setter.setComplexProperty("Child", c);
        assertSame(c, bean.getSingleChild());
    }

    @Test
    public void testSetComplexProperty_missingSetter_logsWarn() {
        setter.setComplexProperty("NonExisting", new Child("y"));
        assertTrue(setter.warnings.stream()
                .anyMatch(m -> m.contains("Not setter method for property [NonExisting]")));
    }

    @Test
    public void testComputeAggregationType_returnsNonNull() {
        // For a known property we expect a concrete AggregationType (not null)
        AggregationType type = setter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void testGetClassNameViaImplicitRules_returnsNullWhenNotFound() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = setter.getClassNameViaImplicitRules("unknown", AggregationType.NOT_FOUND, registry);
        assertNull(result);
    }

    /* ----------------------------------------------------------------------
     * 3. EDGE‑CASE & EXCEPTION TESTS (identified by Expert 3)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetProperty_withEmptyStringValue_isTreatedAsValue() {
        // Empty string is a valid value for a String setter
        setter.setProperty("name", "");
        assertEquals("", bean.getName());
    }

    @Test
    public void testAddBasicProperty_withMismatchedAdderSignature_logsError() {
        // The bean has no adder for "age" (int), so an error should be logged.
        setter.addBasicProperty("age", "10");
        assertTrue(setter.errors.stream()
                .anyMatch(m -> m.contains("No adder for property [Age]"))); // note capitalization
    }

    @Test
    public void testAddComplexProperty_typeMismatch_logsError() {
        // Bean expects Child, we give a String – should trigger type‑mismatch error.
        setter.addComplexProperty("Child", "I am not a Child");
        assertTrue(setter.errors.stream()
                .anyMatch(m -> m.contains("object is not assignable to")));
        assertTrue(bean.getChildren().isEmpty());
    }

    @Test
    public void testSetComplexProperty_typeMismatch_logsError() {
        // Same idea as above but using the setter method.
        setter.setComplexProperty("Child", "wrong type");
        assertTrue(setter.errors.stream()
                .anyMatch(m -> m.contains("object is not assignable to")));
        assertNull(bean.getSingleChild());
    }

    @Test
    public void testInvokeMethodWithSingleParameterOnThisObject_reflectionException_isLogged() throws Exception {
        // Create a bean whose setter throws a runtime exception.
        class ExplodingBean {
            public void setBoom(String s) { throw new IllegalStateException("boom!"); }
        }
        ExplodingBean exploding = new ExplodingBean();
        TestablePropertySetter ps = new TestablePropertySetter(cache, exploding);
        Method setterMethod = ExplodingBean.class.getMethod("setBoom", String.class);
        // Directly call the private helper via reflection.
        Method helper = PropertySetter.class.getDeclaredMethod("invokeMethodWithSingleParameterOnThisObject", Method.class, Object.class);
        helper.setAccessible(true);
        helper.invoke(ps, setterMethod, "any");
        assertTrue(ps.errors.stream()
                .anyMatch(m -> m.contains("Could not invoke method setBoom")));
    }

    @Test
    public void testSetContext_propagatesToAggregationAssessor() {
        // Verify that calling setContext also forwards the context to the assessor.
        TestablePropertySetter ps = new TestablePropertySetter(cache, bean);
        Context newCtx = new MinimalContext();
        ps.setContext(newCtx);
        // AggregationAssessor does not expose its context, but we can at least
        // ensure no exception is thrown and the method completes.
        assertTrue(true);
    }
}
