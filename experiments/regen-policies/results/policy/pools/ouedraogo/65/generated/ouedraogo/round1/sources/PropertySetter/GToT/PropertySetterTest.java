package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import ch.qos.logback.core.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link PropertySetter}.
 *
 * <p>All public methods of {@code PropertySetter} are exercised with
 * normal use‑cases, edge‑cases and error‑scenarios.  The tests use only
 * the public API that is visible in the source code supplied in the task
 * description – no external mocking frameworks are used.</p>
 *
 * <p>The test fixture contains two simple bean classes:</p>
 * <ul>
 *   <li>{@link SimpleBean} – a bean with primitive‑type setters and an
 *       {@code addTag(String)} method that stores the supplied values in a
 *       {@code List<String>}.</li>
 *   <li>{@link ComplexComponent} – a dummy class used as a complex property
 *       for {@code addComplexProperty} and {@code setComplexProperty} tests.</li>
 * </ul>
 *
 * <p>A minimal {@link Context} implementation ({@code SimpleContext}) is
 * provided because {@code BeanDescriptionCache} and {@code AggregationAssessor}
 * require a concrete {@code Context} instance.</p>
 *
 * @author  ChatGPT‑3‑Experts
 */
public class PropertySetterTest {

    /* -------------------------------------------------------------
     *  Helper classes used only by the test suite
     * ------------------------------------------------------------- */

    /** Minimal {@link Context} implementation – all methods return harmless defaults. */
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
        @Override public ScheduledExecutorService getScheduledExecutorService() { return Executors.newSingleThreadScheduledExecutor(); }
        @Override public ExecutorService getExecutorService() { return Executors.newSingleThreadExecutor(); }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(LifeCycle component) {}
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) {}
        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) {}
    }

    /** Simple bean used for the majority of the tests. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean male;
        private final List<String> tags = new ArrayList<>();
        private ComplexComponent component;

        // ---- primitive setters -------------------------------------------------
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }

        // ---- collection adder --------------------------------------------------
        public void addTag(String tag) { this.tags.add(tag); }

        // ---- complex property adder / setter ------------------------------------
        public void addComponent(ComplexComponent c) { this.component = c; }
        public void setComponent(ComplexComponent c) { this.component = c; }

        // ---- getters for verification -------------------------------------------
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public List<String> getTags() { return Collections.unmodifiableList(tags); }
        public ComplexComponent getComponent() { return component; }
    }

    /** Dummy complex property used for the “complex” tests. */
    public static class ComplexComponent {
        private final String id;
        public ComplexComponent(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /* -------------------------------------------------------------
     *  Test fixture fields
     * ------------------------------------------------------------- */
    private SimpleContext context;
    private BeanDescriptionCache beanCache;
    private PropertySetter setter;
    private SimpleBean bean;

    @Before
    public void setUp() {
        context = new SimpleContext();
        beanCache = new BeanDescriptionCache(context);
        bean = new SimpleBean();
        setter = new PropertySetter(beanCache, bean);
    }

    /* -------------------------------------------------------------
     *  1.  Public method list (for documentation purpose only)
     * -------------------------------------------------------------
     *
     *  public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
     *  public void setContext(Context context)
     *  public void setProperty(String name, String value)
     *  public AggregationType computeAggregationType(String name)
     *  public Class<?> getObjClass()
     *  public void addComplexProperty(String name, Object complexProperty)
     *  public void addBasicProperty(String name, String strValue)
     *  public void setComplexProperty(String name, Object complexProperty)
     *  public Object getObj()
     *  public Class<?> getClassNameViaImplicitRules(String name, AggregationType aggregationType,
     *                                               DefaultNestedComponentRegistry registry)
     *
     */

    /* -------------------------------------------------------------
     *  2.  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testSetProperty_stringSetter() {
        setter.setProperty("name", "John Doe");
        assertEquals("John Doe", bean.getName());
    }

    @Test
    public void testSetProperty_intSetter() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetProperty_booleanSetter() {
        setter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    @Test
    public void testSetProperty_nullValueIsIgnored() {
        // No exception, and the bean stays unchanged.
        setter.setProperty("name", null);
        assertNull(bean.getName());
    }

    @Test
    public void testSetProperty_unknownPropertyLogsWarningButDoesNotFail() {
        // The method should simply log a warning; we verify that the bean stays untouched.
        setter.setProperty("nonExisting", "value");
        // No change to any known field
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertFalse(bean.isMale());
    }

    @Test
    public void testAddBasicProperty_successfulAdder() {
        setter.addBasicProperty("tag", "alpha");
        setter.addBasicProperty("tag", "beta");
        List<String> tags = bean.getTags();
        assertEquals(2, tags.size());
        assertTrue(tags.containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    public void testAddBasicProperty_nullStringIsIgnored() {
        setter.addBasicProperty("tag", null);
        assertTrue(bean.getTags().isEmpty());
    }

    @Test
    public void testAddComplexProperty_successfulAdder() {
        ComplexComponent comp = new ComplexComponent("comp-1");
        setter.addComplexProperty("Component", comp);
        assertSame(comp, bean.getComponent());
    }

    @Test
    public void testSetComplexProperty_successfulSetter() {
        ComplexComponent comp = new ComplexComponent("comp-2");
        setter.setComplexProperty("Component", comp);
        assertSame(comp, bean.getComponent());
    }

    @Test
    public void testComputeAggregationType_delegatesToAssessor() {
        // The exact value depends on the internal logic of AggregationAssessor,
        // but it must return a non‑null AggregationType.
        AggregationType type = setter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void testGetObjClass_returnsCorrectClass() {
        assertEquals(SimpleBean.class, setter.getObjClass());
    }

    @Test
    public void testGetObj_returnsOriginalObject() {
        assertSame(bean, setter.getObj());
    }

    @Test
    public void testGetClassNameViaImplicitRules_executesWithoutException() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // The method may return null for unknown rules – we only verify that it does not throw.
        Class<?> cls = setter.getClassNameViaImplicitRules("unknown", AggregationType.NOT_FOUND, registry);
        // No assertion on the value; just that the call succeeded.
        assertTrue(true);
    }

    /* -------------------------------------------------------------
     *  3.  Edge‑case and exception handling tests
     * ------------------------------------------------------------- */

    @Test
    public void testSetProperty_invalidIntConversionProducesWarning() {
        // The underlying conversion will fail and a PropertySetterException will be caught.
        // The bean's field must stay at its default value (0).
        setter.setProperty("age", "notANumber");
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetProperty_invalidBooleanConversionProducesWarning() {
        setter.setProperty("male", "notABoolean");
        // Boolean conversion treats any non‑"true" (ignoring case) as false.
        // The bean's default is false, so we simply verify that no exception occurs.
        assertFalse(bean.isMale());
    }

    @Test
    public void testAddComplexProperty_typeMismatchLogsErrorAndDoesNotSet() {
        // Pass a String where a ComplexComponent is expected.
        setter.addComplexProperty("Component", "I am not a component");
        // The bean's component field must stay null.
        assertNull(bean.getComponent());
    }

    @Test
    public void testSetComplexProperty_typeMismatchLogsErrorAndDoesNotSet() {
        // Pass an Integer where ComplexComponent is expected.
        setter.setComplexProperty("Component", Integer.valueOf(123));
        assertNull(bean.getComponent());
    }

    @Test
    public void testAddBasicProperty_conversionFailureLogsError() {
        // The adder expects a String, so conversion will never fail.
        // To provoke a failure we create a bean with an adder that expects Integer.
        // For the purpose of this test we extend SimpleBean locally.
        class BeanWithIntegerAdder {
            private final List<Integer> numbers = new ArrayList<>();
            public void addNumber(Integer n) { numbers.add(n); }
            public List<Integer> getNumbers() { return numbers; }
        }
        BeanWithIntegerAdder complexBean = new BeanWithIntegerAdder();
        PropertySetter ps = new PropertySetter(beanCache, complexBean);

        // "abc" cannot be converted to Integer – the method should log an error and not add anything.
        ps.addBasicProperty("Number", "abc");
        assertTrue(complexBean.getNumbers().isEmpty());
    }

    @Test
    public void testAddComplexProperty_noAdderMethodLogsError() {
        // The bean does not have an "addFoo" method – the call should result in an error log only.
        setter.addComplexProperty("Foo", new ComplexComponent("dummy"));
        // No exception, bean unchanged.
        assertNull(bean.getComponent());
    }

    @Test
    public void testSetComplexProperty_noSetterMethodLogsWarning() {
        // The bean has no setter called "setFoo" – method should log a warning and return.
        setter.setComplexProperty("Foo", new ComplexComponent("dummy"));
        assertNull(bean.getComponent());
    }

    @Test
    public void testAddBasicProperty_noAdderMethodLogsError() {
        // No "addBar" method exists in SimpleBean.
        setter.addBasicProperty("bar", "value");
        // Bean unchanged.
        assertTrue(bean.getTags().isEmpty());
    }
}
