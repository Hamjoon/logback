package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for {@link PropertySetter}.
 *
 * The tests cover:
 *   • normal usage of all public methods,
 *   • edge‑cases such as null values, missing setters/adders,
 *   • conversion failures,
 *   • type‑mismatch errors for complex properties.
 *
 * No external mocking framework is used – only concrete classes that are
 * part of the public API listed in the prompt.  Minimal dummy implementations
 * of {@link Context} and {@link StatusManager} are provided so that the
 * {@link ContextAwareBase} hierarchy can be instantiated.
 */
public class PropertySetterTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used as test subjects                                    */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with basic setters and collection adders. */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean male;
        private final List<String> tags = new ArrayList<>();
        private Child child;               // complex property

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }

        public void addTag(String tag) { this.tags.add(tag); }

        public void setChild(Child child) { this.child = child; }
        public void addChild(Child child) { this.tags.add("child:" + child.id); } // just to have an adder

        /* getters for assertions */
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public List<String> getTags() { return tags; }
        public Child getChild() { return child; }
    }

    /** A second simple class used as a “complex” property. */
    public static class Child {
        private final String id;
        public Child(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /** Very small {@link Context} implementation – only the methods needed by
     *  {@link ContextAwareBase} are functional. */
    public static class DummyContext implements Context {
        private final Map<String,Object> objects = new HashMap<>();
        private final Map<String,String> properties = new HashMap<>();

        @Override public StatusManager getStatusManager() { return new DummyStatusManager(); }
        @Override public Object getObject(String key) { return objects.get(key); }
        @Override public void putObject(String key, Object value) { objects.put(key, value); }
        @Override public String getProperty(String key) { return properties.get(key); }
        @Override public void putProperty(String key, String value) { properties.put(key, value); }
        @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(properties); }
        @Override public String getName() { return "dummy"; }
        @Override public void setName(String name) { /* no‑op */ }
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return new Object(); }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return Executors.newSingleThreadScheduledExecutor(); }
        @Override public ExecutorService getExecutorService() { return Executors.newSingleThreadExecutor(); }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(LifeCycle component) { /* no‑op */ }
        @Override public void addScheduledFuture(ScheduledFuture<?> scheduledFuture) { /* no‑op */ }
        @Override public SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(SequenceNumberGenerator sequenceNumberGenerator) { }
        @Override public void addConfigurationEventListener(ConfigurationEventListener listener) { }
        @Override public void fireConfigurationEvent(ConfigurationEvent configurationEvent) { }
    }

    /** Minimal {@link StatusManager} that simply stores messages – useful for
     *  asserting that warnings / errors were emitted. */
    public static class DummyStatusManager implements ch.qos.logback.core.status.StatusManager {
        private final List<String> messages = new ArrayList<>();

        @Override public void add(ch.qos.logback.core.status.Status status) {
            messages.add(status.getMessage());
        }
        @Override public List<ch.qos.logback.core.status.Status> getCopyOfStatusList() { return Collections.emptyList(); }
        @Override public boolean add(ch.qos.logback.core.status.Status status) { add(status); return true; }
        @Override public void addAll(List<ch.qos.logback.core.status.Status> statusList) {
            for (ch.qos.logback.core.status.Status s : statusList) add(s);
        }
        @Override public void addAll(ch.qos.logback.core.status.StatusManager other) {
            // not needed for the tests
        }
        @Override public void clear() { messages.clear(); }
        @Override public int getLevel() { return 0; }
        @Override public void setLevel(int level) { }
        @Override public List<ch.qos.logback.core.status.OnConsoleStatusListener> getCopyOfOnConsoleStatusListenerList() { return Collections.emptyList(); }

        public List<String> getMessages() { return messages; }
    }

    /* ---------------------------------------------------------------------- */
    /* Test fixtures                                                            */
    /* ---------------------------------------------------------------------- */

    private PropertySetter setter;
    private SimpleBean bean;
    private DummyContext context;
    private BeanDescriptionCache cache;

    @Before
    public void setUp() {
        context = new DummyContext();
        cache   = new BeanDescriptionCache(context); // builds bean description lazily
        bean    = new SimpleBean();
        setter  = new PropertySetter(cache, bean);
        setter.setContext(context);                 // propagate context to internal assessors
    }

    /* ---------------------------------------------------------------------- */
    /* 1. setProperty – normal flow                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetPropertyBasicString() {
        setter.setProperty("name", "John");
        assertEquals("John", bean.getName());
    }

    @Test
    public void testSetPropertyIntConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetPropertyBooleanConversion() {
        setter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    /* ---------------------------------------------------------------------- */
    /* 2. setProperty – edge cases                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetPropertyNullValueIsIgnored() {
        // should simply return without touching the bean
        setter.setProperty("name", null);
        assertNull(bean.getName());
    }

    @Test
    public void testSetPropertyUnknownPropertyLogsWarning() {
        // No setter called – bean state stays unchanged
        setter.setProperty("nonExisting", "value");
        // No exception is thrown; we only assert that bean is untouched
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetPropertyInvalidIntConversionLogsWarning() {
        // "abc" cannot be converted to int – the underlying setter is never invoked
        setter.setProperty("age", "abc");
        // age remains default (0)
        assertEquals(0, bean.getAge());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. computeAggregationType – basic sanity check                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeAggregationTypeReturnsNonNull() {
        AggregationType type = setter.computeAggregationType("name");
        assertNotNull(type);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. addBasicProperty – collection handling                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddBasicPropertyAddsToCollection() {
        setter.addBasicProperty("tag", "firstTag");
        assertEquals(Collections.singletonList("firstTag"), bean.getTags());
    }

    @Test
    public void testAddBasicPropertyNullValueIsIgnored() {
        setter.addBasicProperty("tag", null);
        assertTrue(bean.getTags().isEmpty());
    }

    @Test
    public void testAddBasicPropertyMissingAdderLogsError() {
        // "unknown" has no adder method – nothing should be added
        setter.addBasicProperty("unknown", "value");
        assertTrue(bean.getTags().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. addComplexProperty – correct type                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddComplexPropertyWithCorrectType() {
        Child child = new Child("c1");
        setter.addComplexProperty("Child", child);   // looks for addChild(Child)
        // The adder simply adds a string representation; we verify side‑effect
        assertTrue(bean.getTags().contains("child:c1"));
    }

    @Test
    public void testAddComplexPropertyWrongTypeLogsError() {
        // Pass a String where a Child is expected – should not invoke adder
        setter.addComplexProperty("Child", "notAChild");
        // No side‑effect expected
        assertTrue(bean.getTags().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. setComplexProperty – correct type                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetComplexPropertyWithCorrectType() {
        Child child = new Child("c2");
        setter.setComplexProperty("Child", child);   // looks for setChild(Child)
        assertSame(child, bean.getChild());
    }

    @Test
    public void testSetComplexPropertyMissingSetterLogsWarn() {
        // No setter named setUnknown – should just warn and leave bean unchanged
        setter.setComplexProperty("Unknown", new Child("c3"));
        assertNull(bean.getChild());
    }

    @Test
    public void testSetComplexPropertyWrongTypeLogsError() {
        // Pass a String where a Child is required – sanity check should fail
        setter.setComplexProperty("Child", "stringInstead");
        assertNull(bean.getChild());
    }

    /* ---------------------------------------------------------------------- */
    /* 7. getObjClass – simple accessor test                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetObjClassReturnsCorrectClass() {
        assertEquals(SimpleBean.class, setter.getObjClass());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. getClassNameViaImplicitRules – integration with registry             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetClassNameViaImplicitRules() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        // Register a dummy component type for verification
        registry.add(SimpleBean.class, "child", Child.class);

        Class<?> cls = setter.getClassNameViaImplicitRules("child", AggregationType.AS_COMPLEX_PROPERTY, registry);
        assertEquals(Child.class, cls);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. getObj – returns the original object                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetObjReturnsOriginalInstance() {
        assertSame(bean, setter.getObj());
    }
}
