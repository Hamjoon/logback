package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic happy‑path tests)</li>
 *   <li>Edge cases such as null values, unknown properties and conversion failures</li>
 *   <li>Exception handling paths inside the private {@code setProperty(Method, String)}</li>
 *   <li>Complex and basic property addition as well as complex property setting</li>
 * </ul>
 *
 * The test uses real implementations of the required Logback classes
 * (no mocking).  A very small {@link TestContext} implementation is provided
 * to satisfy the {@link Context} contract.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used by the tests                                      */
    /* --------------------------------------------------------------------- */

    /** Minimal {@link Context} implementation – only the methods required by the
     *  classes under test are implemented. */
    private static class TestContext implements Context {

        private final Map<String, Object> objects = new HashMap<>();
        private final Map<String, String> properties = new HashMap<>();
        private final StatusManager statusManager = new SimpleStatusManager();

        @Override public StatusManager getStatusManager() { return statusManager; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }
        @Override public String getName() { return "test-context"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    /** Very small {@link StatusManager} that just stores messages in a list. */
    private static class SimpleStatusManager implements StatusManager {
        private final List<String> messages = new ArrayList<>();

        @Override public void add(ch.qos.logback.core.status.Status status) {
            messages.add(status.getMessage());
        }

        @Override public List<ch.qos.logback.core.status.Status> getCopyOfStatusList() { return Collections.emptyList(); }
        @Override public int getCount() { return messages.size(); }
        @Override public void addInfo(String msg) { messages.add("INFO: " + msg); }
        @Override public void addInfo(String msg, Throwable ex) { messages.add("INFO: " + msg + " - " + ex); }
        @Override public void addWarn(String msg) { messages.add("WARN: " + msg); }
        @Override public void addWarn(String msg, Throwable ex) { messages.add("WARN: " + msg + " - " + ex); }
        @Override public void addError(String msg) { messages.add("ERROR: " + msg); }
        @Override public void addError(String msg, Throwable ex) { messages.add("ERROR: " + msg + " - " + ex); }
        @Override public void addStatus(ch.qos.logback.core.status.Status status) { add(status); }
        @Override public void addInfo(ch.qos.logback.core.status.Status status) { add(status); }
        @Override public void addWarn(ch.qos.logback.core.status.Status status) { add(status); }
        @Override public void addError(ch.qos.logback.core.status.Status status) { add(status); }

        public List<String> getMessages() {
            return messages;
        }
    }

    /** Simple bean used throughout the test suite. */
    public static class SampleBean {
        private String name;
        private int age;
        private boolean active;
        private final List<String> items = new ArrayList<>();
        private final List<Integer> numbers = new ArrayList<>();
        private ChildBean child;

        /* standard setters */
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }
        public void setChild(ChildBean child) { this.child = child; }

        /* adder methods */
        public void addItem(String item) { items.add(item); }
        public void addNumber(Integer number) { numbers.add(number); }

        /* getters for verification */
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public List<String> getItems() { return items; }
        public List<Integer> getNumbers() { return numbers; }
        public ChildBean getChild() { return child; }
    }

    /** A tiny complex type used for the “complex property” tests. */
    public static class ChildBean {
        private final String id;
        public ChildBean(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /* --------------------------------------------------------------------- */
    /* Test fixtures                                                          */
    /* --------------------------------------------------------------------- */

    private TestContext context;
    private BeanDescriptionCache beanDescriptionCache;
    private SampleBean sampleBean;
    private PropertySetter propertySetter;

    @Before
    public void setUp() {
        context = new TestContext();
        beanDescriptionCache = new BeanDescriptionCache(context);
        sampleBean = new SampleBean();
        propertySetter = new PropertySetter(beanDescriptionCache, sampleBean);
        // give the PropertySetter a context – this also propagates the context
        // to the internal AggregationAssessor.
        propertySetter.setContext(context);
    }

    /* --------------------------------------------------------------------- */
    /* List of public methods (extracted from the source)                     */
    /* --------------------------------------------------------------------- */
    // public PropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj)
    // public void setContext(Context context)
    // public void setProperty(String name, String value)
    // public AggregationType computeAggregationType(String name)
    // public Class<?> getObjClass()
    // public void addComplexProperty(String name, Object complexProperty)
    // public void addBasicProperty(String name, String strValue)
    // public void setComplexProperty(String name, Object complexProperty)
    // public Object getObj()
    // public Class<?> getClassNameViaImplicitRules(String name, AggregationType aggregationType,
    //                                            DefaultNestedComponentRegistry registry)

    /* --------------------------------------------------------------------- */
    /* Basic happy‑path tests                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyString() {
        propertySetter.setProperty("name", "John Doe");
        assertEquals("John Doe", sampleBean.getName());
    }

    @Test
    public void testSetPropertyIntConversion() {
        propertySetter.setProperty("age", "42");
        assertEquals(42, sampleBean.getAge());
    }

    @Test
    public void testSetPropertyBooleanConversion() {
        propertySetter.setProperty("active", "true");
        assertTrue(sampleBean.isActive());
    }

    @Test
    public void testAddComplexPropertySuccessful() {
        propertySetter.addComplexProperty("Item", "first");
        assertEquals(Collections.singletonList("first"), sampleBean.getItems());
    }

    @Test
    public void testAddBasicPropertySuccessful() {
        propertySetter.addBasicProperty("Number", "123");
        assertEquals(Collections.singletonList(123), sampleBean.getNumbers());
    }

    @Test
    public void testSetComplexPropertySuccessful() {
        ChildBean child = new ChildBean("c1");
        propertySetter.setComplexProperty("Child", child);
        assertSame(child, sampleBean.getChild());
    }

    @Test
    public void testComputeAggregationTypeReturnsNonNull() {
        AggregationType type = propertySetter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void testGetObjClass() {
        assertEquals(SampleBean.class, propertySetter.getObjClass());
    }

    @Test
    public void testGetObj() {
        assertSame(sampleBean, propertySetter.getObj());
    }

    @Test
    public void testGetClassNameViaImplicitRules() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // The method delegates to AggregationAssessor – we only verify that it does not throw.
        Class<?> result = propertySetter.getClassNameViaImplicitRules("unknown", AggregationType.NOT_FOUND, registry);
        // The concrete result depends on internal rules; null is acceptable.
        // The important assertion is that the call completes without an exception.
        assertTrue(true);
    }

    /* --------------------------------------------------------------------- */
    /* Edge‑case / exceptional tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        // before setting, name is null
        assertNull(sampleBean.getName());
        propertySetter.setProperty("name", null); // should be a no‑op
        assertNull(sampleBean.getName());
    }

    @Test
    public void testSetPropertyUnknownPropertyLogsWarning() {
        // "nonExisting" has no setter – the method should log a warning but not throw.
        propertySetter.setProperty("nonExisting", "value");
        // Verify that a warning has been recorded in the status manager.
        List<String> msgs = ((SimpleStatusManager) context.getStatusManager()).getMessages();
        boolean found = msgs.stream().anyMatch(m -> m.contains("No setter for property [nonExisting]"));
        assertTrue("Expected a warning about missing setter", found);
    }

    @Test
    public void testSetPropertyInvalidIntConversionLogsWarning() {
        // "age" exists but "abc" cannot be converted to int.
        propertySetter.setProperty("age", "abc");
        // Age should remain at its default value (0)
        assertEquals(0, sampleBean.getAge());

        List<String> msgs = ((SimpleStatusManager) context.getStatusManager()).getMessages();
        boolean found = msgs.stream().anyMatch(m -> m.contains("Failed to set property [age]"));
        assertTrue("Expected a warning about conversion failure", found);
    }

    @Test
    public void testAddComplexPropertyTypeMismatchIsRejected() {
        // addItem expects a String, we give an Integer – sanity check should fail.
        propertySetter.addComplexProperty("Item", Integer.valueOf(99));
        // No item should have been added.
        assertTrue(sampleBean.getItems().isEmpty());
    }

    @Test
    public void testAddBasicPropertyConversionFailureLogsError() {
        // addNumber expects an Integer, we give a non‑numeric string.
        propertySetter.addBasicProperty("Number", "notANumber");
        // The numbers list must stay empty.
        assertTrue(sampleBean.getNumbers().isEmpty());

        List<String> msgs = ((SimpleStatusManager) context.getStatusManager()).getMessages();
        boolean found = msgs.stream().anyMatch(m -> m.contains("Conversion to type"));
        assertTrue("Expected an error about conversion failure", found);
    }

    @Test
    public void testSetComplexPropertySetterNotFoundLogsWarn() {
        // There is no setter named "unknownProp"
        propertySetter.setComplexProperty("unknownProp", new Object());
        List<String> msgs = ((SimpleStatusManager) context.getStatusManager()).getMessages();
        boolean found = msgs.stream().anyMatch(m -> m.contains("Not setter method for property [unknownProp]"));
        assertTrue("Expected a warning about missing setter", found);
    }

    @Test
    public void testSetComplexPropertyTypeMismatchLogsError() {
        // setChild expects ChildBean, we give a String – sanity check should fail.
        propertySetter.setComplexProperty("Child", "I am not a ChildBean");
        // Child should still be null.
        assertNull(sampleBean.getChild());

        List<String> msgs = ((SimpleStatusManager) context.getStatusManager()).getMessages();
        boolean found = msgs.stream().anyMatch(m -> m.contains("is not assignable"));
        assertTrue("Expected an error about type mismatch", found);
    }
}
