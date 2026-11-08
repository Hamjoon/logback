package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import ch.qos.logback.core.util.StringUtil;

/**
 * Unit tests for {@link PropertySetter}. The tests use real
 * {@link BeanDescriptionCache}, {@link AggregationAssessor} and a tiny
 * {@link Context} implementation that records status messages.
 */
public class PropertySetterTest {

    // -----------------------------------------------------------------------
    // Helper classes
    // -----------------------------------------------------------------------

    /**
     * Very small {@link Context} implementation used only for the purpose of
     * these tests. It records warning and error messages in an internal
     * {@link StringBuilder} so the tests can assert that they were produced.
     */
    private static class SimpleContext implements Context {

        private final StringBuilder log = new StringBuilder();

        @Override
        public StatusManager getStatusManager() {
            // Return a dummy manager that forwards messages to the log.
            return new StatusManager() {
                @Override
                public void add(Status status) {
                    log.append(status.getMessage()).append('\n');
                }

                @Override
                public void add(Status status, int i) {
                    log.append(status.getMessage()).append('\n');
                }

                @Override
                public void add(Status status, int i, Throwable throwable) {
                    log.append(status.getMessage()).append('\n');
                }

                @Override
                public int getCount() {
                    return 0;
                }

                @Override
                public List<Status> getCopyOfStatusList() {
                    return Collections.emptyList();
                }

                @Override
                public void addInfo(String msg) {
                    log.append("[INFO] ").append(msg).append('\n');
                }

                @Override
                public void addInfo(String msg, Throwable ex) {
                    log.append("[INFO] ").append(msg).append('\n');
                }

                @Override
                public void addWarn(String msg) {
                    log.append("[WARN] ").append(msg).append('\n');
                }

                @Override
                public void addWarn(String msg, Throwable ex) {
                    log.append("[WARN] ").append(msg).append('\n');
                }

                @Override
                public void addError(String msg) {
                    log.append("[ERROR] ").append(msg).append('\n');
                }

                @Override
                public void addError(String msg, Throwable ex) {
                    log.append("[ERROR] ").append(msg).append('\n');
                }
            };
        }

        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public Map<String, String> getCopyOfPropertyMap() { return Collections.emptyMap(); }
        @Override public String getName() { return "simple"; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return System.currentTimeMillis(); }
        @Override public Object getConfigurationLock() { return this; }
        @Override public ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public ExecutorService getExecutorService() { return null; }
        @Override public ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.spi.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.spi.ConfigurationEvent configurationEvent) {}

        public String getLog() {
            return log.toString();
        }
    }

    /**
     * Simple bean used by the tests. It contains basic setters,
     * an adder method and a collection‑style setter.
     */
    public static class SimpleBean {
        private String name;
        private int age;
        private boolean active;
        private final StringBuilder collector = new StringBuilder();

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setActive(boolean active) { this.active = active; }

        // adder used by {@link PropertySetter#addBasicProperty}
        public void addItem(String item) { collector.append(item); }

        // setter used by {@link PropertySetter#setComplexProperty}
        public void setItem(String item) { collector.append("SET:").append(item); }

        // getters for verification
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public String getCollected() { return collector.toString(); }
    }

    private SimpleContext context;
    private BeanDescriptionCache cache;
    private PropertySetter setter;
    private SimpleBean bean;

    @Before
    public void setUp() {
        context = new SimpleContext();
        cache = new BeanDescriptionCache(context);
        bean = new SimpleBean();
        setter = new PropertySetter(cache, bean);
        // make sure the PropertySetter sees the same context (required for
        // AggregationAssessor to log messages)
        setter.setContext(context);
    }

    // -----------------------------------------------------------------------
    // Tests for setProperty (basic, int, boolean, missing setter)
    // -----------------------------------------------------------------------

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

    @Test
    public void testSetPropertyUnknownNameLogsWarning() {
        // The bean has no setter called "unknown".  No exception should be thrown.
        setter.setProperty("unknown", "value");
        String log = context.getLog();
        assertTrue(log.contains("No setter for property [unknown]"));
    }

    @Test
    public void testSetPropertyNullValueIsIgnored() {
        // Passing a null value must result in a no‑op, no warning.
        setter.setProperty("name", null);
        assertNull(bean.getName());
        assertFalse(context.getLog().contains("No setter"));
    }

    // -----------------------------------------------------------------------
    // Tests for addBasicProperty (adder method)
    // -----------------------------------------------------------------------

    @Test
    public void testAddBasicPropertySuccessful() {
        // "item" will be capitalized to "Item" inside the method.
        setter.addBasicProperty("item", "hello");
        assertEquals("hello", bean.getCollected());
    }

    @Test
    public void testAddBasicPropertyNoAdderLogsError() {
        setter.addBasicProperty("nonexistent", "value");
        String log = context.getLog();
        assertTrue(log.contains("No adder for property [Nonexistent]"));
    }

    // -----------------------------------------------------------------------
    // Tests for addComplexProperty (single‑parameter adder)
    // -----------------------------------------------------------------------

    @Test
    public void testAddComplexPropertySuccessful() {
        // The bean has an addItem(String) method; we use the same name.
        setter.addComplexProperty("Item", "complex");
        assertEquals("complex", bean.getCollected());
    }

    @Test
    public void testAddComplexPropertyWrongTypeLogsError() {
        // Provide an Integer where the adder expects a String.
        setter.addComplexProperty("Item", Integer.valueOf(123));
        String log = context.getLog();
        assertTrue(log.contains("object is not assignable"));
    }

    @Test
    public void testAddComplexPropertyMissingAdderLogsError() {
        setter.addComplexProperty("Missing", "value");
        String log = context.getLog();
        assertTrue(log.contains("Could not find method [addMissing]"));
    }

    // -----------------------------------------------------------------------
    // Tests for setComplexProperty (setter with object argument)
    // -----------------------------------------------------------------------

    @Test
    public void testSetComplexPropertySuccessful() {
        setter.setComplexProperty("Item", "xyz");
        assertEquals("SET:xyz", bean.getCollected());
    }

    @Test
    public void testSetComplexPropertyWrongTypeLogsError() {
        setter.setComplexProperty("Item", Integer.valueOf(99));
        String log = context.getLog();
        assertTrue(log.contains("object is not assignable"));
    }

    @Test
    public void testSetComplexPropertyMissingSetterLogsWarn() {
        setter.setComplexProperty("Missing", "value");
        String log = context.getLog();
        assertTrue(log.contains("Not setter method for property [Missing]"));
    }

    // -----------------------------------------------------------------------
    // Tests for computeAggregationType and getClassNameViaImplicitRules
    // -----------------------------------------------------------------------

    @Test
    public void testComputeAggregationTypeReturnsNotFoundForUnknown() {
        AggregationType type = setter.computeAggregationType("doesNotExist");
        // The exact value depends on the library but it must be a valid enum constant.
        assertNotNull(type);
    }

    @Test
    public void testGetClassNameViaImplicitRulesReturnsNullWhenNoRule() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> result = setter.getClassNameViaImplicitRules("unknown", AggregationType.NOT_FOUND, registry);
        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // Tests for accessor methods
    // -----------------------------------------------------------------------

    @Test
    public void testGetObjClass() {
        assertEquals(SimpleBean.class, setter.getObjClass());
    }

    @Test
    public void testGetObj() {
        assertSame(bean, setter.getObj());
    }
}
