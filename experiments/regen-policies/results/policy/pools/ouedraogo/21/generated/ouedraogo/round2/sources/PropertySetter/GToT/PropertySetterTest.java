package ch.qos.logback.core.joran.util;
import ch.qos.logback.core.spi.ConfigurationEvent;
import ch.qos.logback.core.spi.ConfigurationEventListener;
import ch.qos.logback.core.spi.SequenceNumberGenerator;
import ch.qos.logback.core.status.StatusManager;


import ch.qos.logback.core.Context;
import ch.qos.logback.core.Status;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link PropertySetter}.
 *
 * The test suite covers:
 *   – normal behaviour of every public method,
 *   – edge‑cases (null values, missing setters/adder, type mismatches),
 *   – conversion failures,
 *   – that warnings / errors are recorded via the StatusManager.
 *
 * All auxiliary classes (a simple bean, a tiny {@link Context} implementation
 * and a {@link StatusManager} that stores messages) are defined inside the test
 * file so that no external mocking framework is required.
 */
public class PropertySetterTest {

    /* --------------------------------------------------------------------- */
    /*  Helper classes – a very small bean and a minimal Context/Status   */
    /* --------------------------------------------------------------------- */

    /** Simple bean used for all setter / adder tests. */
    public static class TestBean {
        private String name;
        private int age;
        private boolean male;
        private final List<String> items = new ArrayList<>();
        private final List<SubBean> subBeans = new ArrayList<>();
        private SubBean singleSubBean;

        // ----- basic setters ------------------------------------------------
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setMale(boolean male) { this.male = male; }

        // ----- adder for basic property (String) ---------------------------
        public void addItem(String item) { items.add(item); }

        // ----- adder for complex property (SubBean) ------------------------
        public void addSubBean(SubBean sb) { subBeans.add(sb); }

        // ----- complex setter ------------------------------------------------
        public void setSubBean(SubBean sb) { this.singleSubBean = sb; }

        // ----- adder that expects an int (used to provoke conversion failure)
        public void addCount(int count) { /* no‑op for test */ }

        // ----- getters used by assertions ------------------------------------
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isMale() { return male; }
        public List<String> getItems() { return items; }
        public List<SubBean> getSubBeans() { return subBeans; }
        public SubBean getSingleSubBean() { return singleSubBean; }
    }

    /** Simple nested bean used as a complex property */
    public static class SubBean {
        private final String id;
        SubBean(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /** Minimal StatusManager that just records messages. */
    public static class RecordingStatusManager implements StatusManager {
        private final List<Status> statusList = new ArrayList<>();

        @Override public void add(Status status) { statusList.add(status); }
        @Override public void add(Status status, int i) { statusList.add(status); }
        @Override public List<Status> getCopyOfStatusList() { return new ArrayList<>(statusList); }
        @Override public void reset() { statusList.clear(); }
        @Override public boolean add(Status status) { statusList.add(status); return true; }

        public List<String> getMessages() {
            List<String> msgs = new ArrayList<>();
            for (Status s : statusList) {
                msgs.add(s.getMessage());
            }
            return msgs;
        }
    }

    /** Very small Context implementation – only getStatusManager is needed. */
    public static class SimpleContext implements Context {
        private final RecordingStatusManager sm = new RecordingStatusManager();

        @Override public StatusManager getStatusManager() { return sm; }
        // --------------------------------------------------------------------
        // The remaining Context methods are not needed for the tests; we
        // provide trivial implementations.
        // --------------------------------------------------------------------
        @Override public Object getObject(String key) { return null; }
        @Override public void putObject(String key, Object value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public java.util.Map<String, String> getCopyOfPropertyMap() { return null; }
        @Override public String getName() { return null; }
        @Override public void setName(String name) {}
        @Override public long getBirthTime() { return 0L; }
        @Override public Object getConfigurationLock() { return null; }
        @Override public java.util.concurrent.ScheduledExecutorService getScheduledExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getExecutorService() { return null; }
        @Override public java.util.concurrent.ExecutorService getAlternateExecutorService() { return null; }
        @Override public void register(ch.qos.logback.core.spi.LifeCycle component) {}
        @Override public void addScheduledFuture(java.util.concurrent.ScheduledFuture<?> scheduledFuture) {}
        @Override public ch.qos.logback.core.util.SequenceNumberGenerator getSequenceNumberGenerator() { return null; }
        @Override public void setSequenceNumberGenerator(ch.qos.logback.core.util.SequenceNumberGenerator sequenceNumberGenerator) {}
        @Override public void addConfigurationEventListener(ch.qos.logback.core.joran.event.ConfigurationEventListener listener) {}
        @Override public void fireConfigurationEvent(ch.qos.logback.core.joran.event.ConfigurationEvent configurationEvent) {}
    }

    private SimpleContext context;
    private BeanDescriptionCache cache;
    private TestBean bean;
    private PropertySetter setter;

    @Before
    public void setUp() {
        context = new SimpleContext();
        cache   = new BeanDescriptionCache(context);
        bean    = new TestBean();
        setter  = new PropertySetter(cache, bean);
        setter.setContext(context);   // propagate context to AggregationAssessor
    }

    /* --------------------------------------------------------------------- */
    /*  1. Constructor & trivial accessors                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructorStoresObject() {
        assertSame(bean, setter.getObj());
    }

    @Test
    public void getObjClassReturnsExactClass() {
        assertEquals(TestBean.class, setter.getObjClass());
    }

    /* --------------------------------------------------------------------- */
    /*  2. setProperty – normal paths                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void setPropertyString() {
        setter.setProperty("name", "Alice");
        assertEquals("Alice", bean.getName());
    }

    @Test
    public void setPropertyIntConversion() {
        setter.setProperty("age", "42");
        assertEquals(42, bean.getAge());
    }

    @Test
    public void setPropertyBooleanConversion() {
        setter.setProperty("male", "true");
        assertTrue(bean.isMale());
    }

    /* --------------------------------------------------------------------- */
    /*  3. setProperty – edge cases                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void setPropertyWithNullValueDoesNothing() {
        setter.setProperty("name", null);   // should simply return
        assertNull(bean.getName());        // unchanged
    }

    @Test
    public void setPropertyWhenNoSetterLogsWarning() {
        setter.setProperty("nonExisting", "value");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("No setter for property [nonExisting]")));
    }

    @Test
    public void setPropertyConversionFailureLogsWarning() {
        setter.setProperty("age", "notANumber");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Failed to set property [age]")));
        // value must stay at default (0)
        assertEquals(0, bean.getAge());
    }

    /* --------------------------------------------------------------------- */
    /*  4. addComplexProperty – normal & error paths                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void addComplexPropertySuccess() {
        SubBean sb = new SubBean("sb1");
        setter.addComplexProperty("SubBean", sb);   // maps to addSubBean(SubBean)
        assertEquals(1, bean.getSubBeans().size());
        assertSame(sb, bean.getSubBeans().get(0));
    }

    @Test
    public void addComplexPropertyNoAdderLogsError() {
        setter.addComplexProperty("UnknownComplex", new Object());
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Could not find method [addUnknownComplex]")));
    }

    @Test
    public void addComplexPropertyTypeMismatchLogsError() {
        // adder expects SubBean, we give a plain String
        setter.addComplexProperty("SubBean", "I am not a SubBean");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("is not assignable")));
        assertTrue(bean.getSubBeans().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  5. addBasicProperty – normal & error paths                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void addBasicPropertySuccess() {
        setter.addBasicProperty("item", "first");
        assertEquals(1, bean.getItems().size());
        assertEquals("first", bean.getItems().get(0));
    }

    @Test
    public void addBasicPropertyNullValueDoesNothing() {
        setter.addBasicProperty("item", null);
        assertTrue(bean.getItems().isEmpty());
    }

    @Test
    public void addBasicPropertyNoAdderLogsError() {
        setter.addBasicProperty("unknown", "value");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("No adder for property [unknown]")));
    }

    @Test
    public void addBasicPropertyConversionFailureLogsError() {
        // adder expects int (addCount), we pass a non‑numeric String
        setter.addBasicProperty("count", "abc");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Conversion to type")));
    }

    /* --------------------------------------------------------------------- */
    /*  6. setComplexProperty – normal & error paths                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void setComplexPropertySuccess() {
        SubBean sb = new SubBean("single");
        setter.setComplexProperty("SubBean", sb);   // maps to setSubBean(SubBean)
        assertSame(sb, bean.getSingleSubBean());
    }

    @Test
    public void setComplexPropertyNoSetterLogsWarn() {
        setter.setComplexProperty("NonExisting", new Object());
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("Not setter method for property [NonExisting]")));
    }

    @Test
    public void setComplexPropertyTypeMismatchLogsError() {
        setter.setComplexProperty("SubBean", "not a SubBean");
        List<String> msgs = ((RecordingStatusManager) context.getStatusManager()).getMessages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("is not assignable")));
        assertNull(bean.getSingleSubBean());
    }

    /* --------------------------------------------------------------------- */
    /*  7. computeAggregationType – delegating call (basic sanity)           */
    /* --------------------------------------------------------------------- */

    @Test
    public void computeAggregationTypeReturnsNonNull() {
        AggregationType at = setter.computeAggregationType("name");
        assertNotNull(at);
    }

    /* --------------------------------------------------------------------- */
    /*  8. getClassNameViaImplicitRules – delegating call                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void getClassNameViaImplicitRulesDoesNotThrow() {
        DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
        Class<?> clazz = setter.getClassNameViaImplicitRules("name", AggregationType.AS_BASIC_PROPERTY, registry);
        // The method may legitimately return null (no implicit rule).  The important
        // thing is that it does not throw.
        // No assertion on the value itself.
    }

    /* --------------------------------------------------------------------- */
    /*  9. setContext – ensure the call propagates without error             */
    /* --------------------------------------------------------------------- */

    @Test
    public void setContextPropagatesToAggregationAssessor() {
        // No explicit observable effect – the test simply ensures that the call
        // does not raise an exception.
        setter.setContext(context);
    }
}
