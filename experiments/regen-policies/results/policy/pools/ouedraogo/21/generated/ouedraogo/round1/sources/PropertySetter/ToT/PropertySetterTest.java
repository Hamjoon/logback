package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic property setting via {@code setProperty}</li>
 *   <li>handling of {@code null} values</li>
 *   <li>complex property handling (adder / setter)</li>
 *   <li>aggregation type computation</li>
 *   <li>implicit‑rule class name resolution</li>
 *   <li>error‑path handling (missing setter/adder)</li>
 * </ul>
 *
 * <p>All supporting classes (a very small {@link SimpleContext},
 * test beans and a {@link TestablePropertySetter} that records warnings/
 * errors) are defined inside this test file to avoid any external
 * dependencies or mocking frameworks.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- *
     *  Helper classes used only by the tests                               *
     * --------------------------------------------------------------------- */

    /** Minimal {@link Context} implementation – enough for the library
     *  to accept it.  All methods return harmless defaults. */
    private static class SimpleContext implements Context {
        @Override public StatusManager getStatusManager() { return null; }
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public Map<String, String> getCopyOfPropertyMap() { return Collections.emptyMap(); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) {}
    }

    /** Simple bean with a few primitive properties and collection adder methods. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;

        private final List<String> tags = new ArrayList<>();
        private final List<ChildBean> children = new ArrayList<>();

        public void setName(String name) { this.name = name; }
        public String getName() { return name; }

        public void setAge(int age) { this.age = age; }
        public int getAge() { return age; }

        public void setActive(boolean active) { this.active = active; }
        public boolean isActive() { return active; }

        // adder for a basic (String) collection
        public void addTag(String tag) { tags.add(tag); }
        public List<String> getTags() { return tags; }

        // adder for a complex collection
        public void addChild(ChildBean child) { children.add(child); }
        public List<ChildBean> getChildren() { return children; }
    }

    /** Simple complex property used by {@link TestBean}. */
    public static class ChildBean {
        private final String id;
        public ChildBean(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /**
     * Subclass of {@link PropertySetter} that records warnings and errors
     * instead of sending them to the real {@link StatusManager}.  This makes
     * the test assertions deterministic.
     */
    private static class TestablePropertySetter extends PropertySetter {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors   = new ArrayList<>();

        public TestablePropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        @Override
        public void addWarn(String msg) {
            warnings.add(msg);
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
            warnings.add(msg + " : " + ex.getMessage());
        }

        @Override
        public void addError(String msg) {
            errors.add(msg);
        }

        @Override
        public void addError(String msg, Throwable ex) {
            errors.add(msg + " : " + ex.getMessage());
        }

        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors()   { return errors; }
    }

    /* --------------------------------------------------------------------- *
     *  Test fixtures                                                       *
     * --------------------------------------------------------------------- */

    private SimpleContext context;
    private BeanDescriptionCache beanCache;
    private TestBean testBean;
    private TestablePropertySetter setter;

    @Before
    public void setUp() {
        context = new SimpleContext();
        beanCache = new BeanDescriptionCache(context);
        testBean = new TestBean();
        setter = new TestablePropertySetter(beanCache, testBean);
        // make sure the PropertySetter knows the context (required by AggregationAssessor)
        setter.setContext(context);
    }

    /* --------------------------------------------------------------------- *
     *  1. setProperty – basic (String, int, boolean)                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetProperty_String() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", testBean.getName());
        assertTrue(setter.getWarnings().isEmpty());
    }

    @Test
    public void testSetProperty_Int() {
        setter.setProperty("age", "42");
        assertEquals(42, testBean.getAge());
        assertTrue(setter.getWarnings().isEmpty());
    }

    @Test
    public void testSetProperty_Boolean() {
        setter.setProperty("active", "true");
        assertTrue(testBean.isActive());
        assertTrue(setter.getWarnings().isEmpty());
    }

    @Test
    public void testSetProperty_NullValueIsIgnored() {
        setter.setProperty("name", null); // should be a no‑op
        assertNull(testBean.getName());
        assertTrue(setter.getWarnings().isEmpty());
    }

    @Test
    public void testSetProperty_MissingSetterProducesWarning() {
        setter.setProperty("nonExisting", "value");
        assertEquals(1, setter.getWarnings().size());
        assertTrue(setter.getWarnings().get(0).contains("No setter for property [nonExisting]"));
    }

    @Test
    public void testSetProperty_ConversionFailureProducesWarning() {
        // age expects an int – passing a non‑numeric string triggers a conversion failure
        setter.setProperty("age", "notANumber");
        assertEquals(0, testBean.getAge()); // default int value
        assertEquals(1, setter.getWarnings().size());
        assertTrue(setter.getWarnings().get(0).contains("Failed to set property [age]"));
    }

    /* --------------------------------------------------------------------- *
     *  2. addBasicProperty – collection of simple values                    *
     * --------------------------------------------------------------------- */

    @Test
    public void testAddBasicProperty_SuccessfulAdd() {
        setter.addBasicProperty("tag", "alpha");
        setter.addBasicProperty("tag", "beta");
        List<String> tags = testBean.getTags();
        assertEquals(2, tags.size());
        assertTrue(tags.contains("alpha"));
        assertTrue(tags.contains("beta"));
        assertTrue(setter.getErrors().isEmpty());
    }

    @Test
    public void testAddBasicProperty_NoAdderProducesError() {
        setter.addBasicProperty("unknown", "value");
        assertEquals(1, setter.getErrors().size());
        assertTrue(setter.getErrors().get(0).contains("No adder for property [Unknown]"));
    }

    @Test
    public void testAddBasicProperty_NullValueIsIgnored() {
        setter.addBasicProperty("tag", null);
        assertTrue(testBean.getTags().isEmpty());
        assertTrue(setter.getErrors().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  3. addComplexProperty – collection of complex objects                *
     * --------------------------------------------------------------------- */

    @Test
    public void testAddComplexProperty_SuccessfulAdd() {
        ChildBean child = new ChildBean("c1");
        // The name passed to addComplexProperty must match the capitalised property name
        // (the assessor builds "add" + name).  The adder method is addChild(ChildBean)
        setter.addComplexProperty("Child", child);
        List<ChildBean> children = testBean.getChildren();
        assertEquals(1, children.size());
        assertEquals("c1", children.get(0).getId());
        assertTrue(setter.getErrors().isEmpty());
    }

    @Test
    public void testAddComplexProperty_MissingAdderProducesError() {
        ChildBean child = new ChildBean("c2");
        setter.addComplexProperty("Missing", child);
        assertEquals(1, setter.getErrors().size());
        assertTrue(setter.getErrors().get(0).contains("Could not find method [addMissing]"));
    }

    @Test
    public void testAddComplexProperty_IncompatibleTypeProducesError() {
        // Pass a String where a ChildBean is expected – the sanity check should fail
        setter.addComplexProperty("Child", "notAChild");
        assertEquals(1, setter.getErrors().size());
        assertTrue(setter.getErrors().get(0).contains("object is not assignable"));
    }

    /* --------------------------------------------------------------------- *
     *  4. setComplexProperty – simple (non‑collection) complex setter       *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetComplexProperty_SuccessfulSet() {
        // Use the same ChildBean setter pattern but as a simple property (setChild)
        // First we extend TestBean with a simple setter for demonstration purposes.
        class BeanWithSimpleComplex {
            private ChildBean child;
            public void setChild(ChildBean child) { this.child = child; }
            public ChildBean getChild() { return child; }
        }
        BeanWithSimpleComplex bean = new BeanWithSimpleComplex();
        TestablePropertySetter ps = new TestablePropertySetter(beanCache, bean);
        ps.setContext(context);
        ChildBean child = new ChildBean("simple");
        ps.setComplexProperty("child", child);
        assertSame(child, bean.getChild());
        assertTrue(ps.getErrors().isEmpty());
    }

    @Test
    public void testSetComplexProperty_MissingSetterProducesWarn() {
        setter.setComplexProperty("doesNotExist", new Object());
        assertEquals(1, setter.getWarnings().size());
        assertTrue(setter.getWarnings().get(0).contains("Not setter method for property [doesNotExist]"));
    }

    @Test
    public void testSetComplexProperty_IncompatibleTypeProducesError() {
        // Pass a String to a setter expecting ChildBean
        setter.setComplexProperty("child", "notAChildBean");
        assertEquals(1, setter.getErrors().size());
        assertTrue(setter.getErrors().get(0).contains("is not assignable"));
    }

    /* --------------------------------------------------------------------- *
     *  5. computeAggregationType – basic sanity check                       *
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationType_BasicProperty() {
        AggregationType type = setter.computeAggregationType("name");
        // For a simple String property the assessor should classify it as BASIC
        assertEquals(AggregationType.AS_BASIC_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationType_ComplexProperty() {
        AggregationType type = setter.computeAggregationType("child");
        // ChildBean is a complex type – expect COMPLEX property classification
        assertEquals(AggregationType.AS_COMPLEX_PROPERTY, type);
    }

    @Test
    public void testComputeAggregationType_CollectionProperty() {
        AggregationType type = setter.computeAggregationType("tag");
        // The "tag" property is a collection of basic values
        assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, type);
    }

    /* --------------------------------------------------------------------- *
     *  6. getClassNameViaImplicitRules – using DefaultNestedComponentRegistry
     * --------------------------------------------------------------------- */

    @Test
    public void testGetClassNameViaImplicitRules_FindsRegisteredClass() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Register that the tag "child" under host class TestBean maps to ChildBean
        registry.add(TestBean.class, "child", ChildBean.class);

        AggregationType aggType = AggregationType.AS_COMPLEX_PROPERTY; // any non‑null value works
        Class<?> resolved = setter.getClassNameViaImplicitRules("child", aggType, registry);
        assertEquals(ChildBean.class, resolved);
    }

    @Test
    public void testGetClassNameViaImplicitRules_NoMatchReturnsNull() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> resolved = setter.getClassNameViaImplicitRules("unknown", AggregationType.NOT_FOUND, registry);
        assertNull(resolved);
    }

    /* --------------------------------------------------------------------- *
     *  7. Miscellaneous getters                                             *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetObjClass_ReturnsActualClass() {
        assertEquals(TestBean.class, setter.getObjClass());
    }

    @Test
    public void testGetObj_ReturnsOriginalObject() {
        assertSame(testBean, setter.getObj());
    }

    /* --------------------------------------------------------------------- *
     *  8. setContext – ensure the context is propagated to the assessor   *
     * --------------------------------------------------------------------- */

    @Test
    public void testSetContext_PropagatesToAggregationAssessor() {
        // The only observable effect is that the assessor no longer throws NPE when
        // it internally accesses the context.  We can verify that a subsequent call
        // works without exception.
        setter.setContext(context);
        // No exception means the context was successfully set.
        AggregationType type = setter.computeAggregationType("name");
        assertNotNull(type);
    }
}
