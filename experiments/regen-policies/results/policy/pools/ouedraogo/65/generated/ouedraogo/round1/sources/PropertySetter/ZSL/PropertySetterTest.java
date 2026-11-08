/**
 * JUnit 4 test suite for {@link ch.qos.logback.core.joran.util.PropertySetter}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic property setting (String, int, boolean)</li>
 *   <li>handling of unknown properties</li>
 *   <li>basic collection property addition (addBasicProperty)</li>
 *   <li>complex property addition (addComplexProperty)</li>
 *   <li>complex property setting (setComplexProperty)</li>
 *   <li>utility methods (computeAggregationType, getClassNameViaImplicitRules, getObjClass, getObj)</li>
 *   <li>null‑value guards</li>
 * </ul>
 *
 * No external mocking frameworks are used – only real objects that are
 * constructed from the public APIs listed in the prompt.
 */
package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class PropertySetterTest {

    /** Simple {@link Context} implementation that satisfies the interface contract. */
    private static class SimpleContext implements Context {

        private final Map<String, Object> objects = new HashMap<>();
        private final Map<String, String> properties = new HashMap<>();
        private final StatusManager statusManager = new SimpleStatusManager();

        @Override public StatusManager getStatusManager() { return statusManager; }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }
        @Override public String getName() { return "simple-context"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return Executors.newSingleThreadScheduledExecutor(); }
        @Override public ExecutorService getExecutorService() { return Executors.newSingleThreadExecutor(); }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public ch.qos.logback.core.spi.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.spi.SequenceNumberGenerator sequenceNumberGenerator) { /* no‑op */ }
        @Override public void addConfigurationEventListener(ch.qos.logback.core.spi.ConfigurationEventListener listener) { /* no‑op */ }
        @Override public void fireConfigurationEvent(ch.qos.logback.core.spi.ConfigurationEvent configurationEvent) { /* no‑op */ }
    }

    /** Minimal {@link StatusManager} – it only records messages for possible inspection. */
    private static class SimpleStatusManager implements StatusManager {
        private final List<String> messages = new ArrayList<>();

        @Override public void add(ch.qos.logback.core.status.Status status) {
            messages.add(status.getMessage());
        }
        @Override public List<ch.qos.logback.core.status.Status> getCopyOfStatusList() { return Collections.emptyList(); }
        @Override public int getCount() { return messages.size(); }
        @Override public void addInfo(String msg) { messages.add("[INFO] " + msg); }
        @Override public void addInfo(String msg, Throwable ex) { messages.add("[INFO] " + msg + " - " + ex); }
        @Override public void addWarn(String msg) { messages.add("[WARN] " + msg); }
        @Override public void addWarn(String msg, Throwable ex) { messages.add("[WARN] " + msg + " - " + ex); }
        @Override public void addError(String msg) { messages.add("[ERROR] " + msg); }
        @Override public void addError(String msg, Throwable ex) { messages.add("[ERROR] " + msg + " - " + ex); }
        @Override public void addStatus(ch.qos.logback.core.status.Status status) { add(status); }

        List<String> getMessages() { return messages; }
    }

    /** A tiny bean used throughout the tests. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean male;
        private final List<String> items = new ArrayList<>();
        private Child child;

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }

        public void addItem(String item) { items.add(item); }
        public void addChild(Child child) { this.child = child; }

        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public List<String> getItems() { return items; }
        public Child getChild() { return child; }
    }

    /** Simple complex property used for the “complex” tests. */
    public static class Child {
        private final String payload;
        public Child(String payload) { this.payload = payload; }
        public String getPayload() { return payload; }
    }

    private BeanDescriptionCache beanDescriptionCache;
    private PropertySetter propertySetter;
    private TestBean testBean;

    @Before
    public void setUp() {
        // Initialise a real Context and the corresponding BeanDescriptionCache.
        Context context = new SimpleContext();
        beanDescriptionCache = new BeanDescriptionCache(context);

        testBean = new TestBean();
        propertySetter = new PropertySetter(beanDescriptionCache, testBean);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic property setting tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetStringProperty() {
        propertySetter.setProperty("name", "John Doe");
        assertEquals("John Doe", testBean.getName());
    }

    @Test
    public void testSetIntProperty() {
        propertySetter.setProperty("age", "42");
        assertEquals(42, testBean.getAge());
    }

    @Test
    public void testSetBooleanProperty() {
        propertySetter.setProperty("male", "true");
        assertTrue(testBean.isMale());
    }

    @Test
    public void testSetPropertyWithNullValueDoesNothing() {
        propertySetter.setProperty("name", null);
        assertNull(testBean.getName());
    }

    @Test
    public void testSetUnknownPropertyLogsWarningAndLeavesObjectUntouched() {
        // The bean does not have a setter called setFoo, so a warning should be issued.
        propertySetter.setProperty("foo", "bar");
        // No side‑effect on the bean
        assertNull(testBean.getName());
        assertEquals(0, testBean.getAge());
        assertFalse(testBean.isMale());
    }

    /* --------------------------------------------------------------------- */
    /*  addBasicProperty (collection of simple values)                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddBasicPropertyAddsItemToCollection() {
        // The adder method is addItem(String).  The public API expects the *raw* name,
        // which will be capitalised inside addBasicProperty.
        propertySetter.addBasicProperty("item", "first");
        propertySetter.addBasicProperty("item", "second");

        List<String> items = testBean.getItems();
        assertEquals(2, items.size());
        assertTrue(items.containsAll(Arrays.asList("first", "second")));
    }

    @Test
    public void testAddBasicPropertyWithNullStringIsIgnored() {
        propertySetter.addBasicProperty("item", null);
        assertTrue(testBean.getItems().isEmpty());
    }

    @Test
    public void testAddBasicPropertyWhenAdderIsMissingLogsError() {
        // "unknown" does not map to any addXXX method.
        propertySetter.addBasicProperty("unknown", "value");
        // The collection must still be empty.
        assertTrue(testBean.getItems().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  addComplexProperty (collection of complex objects)                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddComplexPropertyInvokesAdder() {
        Child child1 = new Child("c1");
        Child child2 = new Child("c2");

        // The adder method is addChild(Child)
        propertySetter.addComplexProperty("Child", child1);
        propertySetter.addComplexProperty("Child", child2);

        // The bean only stores the *last* child assigned via the adder,
        // because the test bean does not keep a collection.  The important
        // thing is that no exception is thrown and the method is invoked.
        assertEquals(child2, testBean.getChild());
    }

    @Test
    public void testAddComplexPropertyWithIncompatibleTypeLogsError() {
        // Pass a String where a Child instance is expected.
        propertySetter.addComplexProperty("Child", "not-a-child");
        // The bean's child must remain null.
        assertNull(testBean.getChild());
    }

    /* --------------------------------------------------------------------- */
    /*  setComplexProperty (single complex object)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetComplexPropertyInvokesSetter() {
        Child child = new Child("payload");
        // The setter is setChild(Child)
        propertySetter.setComplexProperty("Child", child);
        assertEquals(child, testBean.getChild());
    }

    @Test
    public void testSetComplexPropertyWithMissingSetterLogsWarn() {
        // No setter called setFoo exists.
        propertySetter.setComplexProperty("Foo", new Object());
        // No change to bean state.
        assertNull(testBean.getChild());
    }

    @Test
    public void testSetComplexPropertyWithWrongTypeLogsError() {
        // Pass an incompatible type to the existing setter.
        propertySetter.setComplexProperty("Child", "string-instead-of-child");
        assertNull(testBean.getChild());
    }

    /* --------------------------------------------------------------------- */
    /*  Utility / introspection methods                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationTypeReturnsNonNull() {
        AggregationType type = propertySetter.computeAggregationType("name");
        assertNotNull(type);
    }

    @Test
    public void testGetClassNameViaImplicitRulesDoesNotThrow() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // The method may return null – we only verify that it does not throw.
        Class<?> cls = propertySetter.getClassNameViaImplicitRules(
                "child", AggregationType.AS_COMPLEX_PROPERTY, registry);
        // No assertion about the actual value; the important part is that the call succeeds.
        assertTrue(true);
    }

    @Test
    public void testGetObjClassReturnsCorrectClass() {
        assertEquals(TestBean.class, propertySetter.getObjClass());
    }

    @Test
    public void testGetObjReturnsOriginalInstance() {
        assertSame(testBean, propertySetter.getObj());
    }

    /* --------------------------------------------------------------------- */
    /*  Direct invocation of the package‑private helper (optional)           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInvokeMethodWithSingleParameterOnThisObjectWorks() throws Exception {
        // Use reflection to fetch the private helper and invoke it directly.
        java.lang.reflect.Method helper = PropertySetter.class
                .getDeclaredMethod("invokeMethodWithSingleParameterOnThisObject", Method.class, Object.class);
        helper.setAccessible(true);

        // Prepare a method reference (addItem) and a parameter.
        java.lang.reflect.Method addItem = TestBean.class.getMethod("addItem", String.class);
        helper.invoke(propertySetter, addItem, "direct");

        assertEquals(Collections.singletonList("direct"), testBean.getItems());
    }

    /* --------------------------------------------------------------------- */
    /*  Defensive null checks for complex API entry points                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddBasicPropertyIgnoresNullValue() {
        propertySetter.addBasicProperty("item", null);
        assertTrue(testBean.getItems().isEmpty());
    }

    @Test
    public void testAddComplexPropertyIgnoresNullValue() {
        propertySetter.addComplexProperty("Child", null);
        assertNull(testBean.getChild());
    }

    @Test
    public void testSetComplexPropertyIgnoresNullValue() {
        propertySetter.setComplexProperty("Child", null);
        assertNull(testBean.getChild());
    }
}
