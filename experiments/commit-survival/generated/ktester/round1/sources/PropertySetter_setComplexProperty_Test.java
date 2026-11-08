package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.testUtil.StatusChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setComplexProperty_Test {

    /**
     * Shared test context used by all tests.
     */
    private static ContextBase context;

    /**
     * Shared bean‑description cache – it is cheap to reuse it across tests.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Helper to inspect status messages produced by {@link PropertySetter}.
     */
    private StatusChecker statusChecker;

    /**
     * Bean without a setter for the requested property.
     */
    public static class NoSetterBean {
        // intentionally empty
    }

    /**
     * Simple bean with a single‑parameter setter (used for the happy‑path test).
     */
    public static class House {

        private Object door;

        public void setDoor(Object door) {
            this.door = door;
        }

        public Object getDoor() {
            return door;
        }
    }

    /**
     * Bean whose setter deliberately throws a checked exception.
     */
    public static class ThrowingBean {

        public void setWindow(Object o) {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Bean with a primitive‑type setter (int).
     */
    public static class PrimitiveBean {

        public void setIntProperty(int i) {
            /* no‑op */
        }
    }

    /**
     * Bean with a multi‑parameter setter – should be rejected by the sanity check.
     */
    public static class MultiParamBean {

        public void setMulti(Object a, Object b) {
            /* no‑op */
        }
    }

    /**
     * Bean with a private setter – will cause IllegalAccessException when invoked via reflection.
     */
    public static class PrivateSetterBean {

        @SuppressWarnings("unused")
        private void setSecret(Object o) {
            /* no‑op */
        }
    }

    /**
     * Bean whose setter throws a checked exception wrapped in InvocationTargetException.
     */
    public static class CheckedExceptionBean {

        public void setChecked(Object o) throws Exception {
            throw new Exception("checked");
        }
    }

    /**
     * Bean whose setter throws a RuntimeException.
     */
    public static class RuntimeExceptionBean {

        public void setRuntime(Object o) {
            throw new RuntimeException("runtime");
        }
    }

    /**
     * Testable subclass that lets the test inject behaviour for the three
     * package‑private helper methods used by {@code setComplexProperty}.
     */
    static class TestablePropertySetter extends PropertySetter {

        private Function<String, Method> findMethodFn;

        private BiPredicate<String, Object> sanityPredicate;

        private BiConsumer<Method, Object> invokeConsumer;

        TestablePropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        /**
         * Allows the test to decide which {@link Method} is returned for a given name.
         */
        void setFindMethodFn(Function<String, Method> fn) {
            this.findMethodFn = fn;
        }

        /**
         * Allows the test to decide whether the sanity check passes.
         */
        void setSanityPredicate(BiPredicate<String, Object> predicate) {
            this.sanityPredicate = predicate;
        }

        /**
         * Allows the test to decide what happens when the setter is finally invoked.
         */
        void setInvokeConsumer(BiConsumer<Method, Object> consumer) {
            this.invokeConsumer = consumer;
        }

        @Override
        Method findSetterMethod(String name) {
            return (findMethodFn != null) ? findMethodFn.apply(name) : super.findSetterMethod(name);
        }

        @Override
        boolean isSanityCheckSuccessful(String name, Method method, Class<?>[] params, Object complexProperty) {
            if (sanityPredicate != null) {
                // The predicate decides the result; it may also log a warning.
                return sanityPredicate.test(name, complexProperty);
            }
            return super.isSanityCheckSuccessful(name, method, params, complexProperty);
        }

        @Override
        void invokeMethodWithSingleParameterOnThisObject(Method method, Object parameter) {
            if (invokeConsumer != null) {
                invokeConsumer.accept(method, parameter);
            } else {
                super.invokeMethodWithSingleParameterOnThisObject(method, parameter);
            }
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the status manager before each test so that we only see messages from the current test.
        context.getStatusManager().clear();
        statusChecker = new StatusChecker(context);
        context.getStatusManager().clear();
        statusChecker = new StatusChecker(context);
    }

    @Test
    void shouldInvokeExistingComplexPropertySetter() {
        // GIVEN
        House house = new House();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Object expectedDoor = new Object();
        // WHEN
        setter.setComplexProperty("door", expectedDoor);
        // THEN
        assertSame(expectedDoor, house.getDoor(), "The complex property setter should have been invoked and stored the value.");
        // No warnings or errors should have been emitted.
        assertTrue(statusChecker.getStatusList().isEmpty(), "No status messages should be present for a successful call.");
    }

    @Test
    void shouldLogWarningWhenNoSetterExists() {
        // GIVEN
        NoSetterBean bean = new NoSetterBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Object someValue = new Object();
        // WHEN
        setter.setComplexProperty("nonExisting", someValue);
        // THEN
        // The bean must remain unchanged (there is nothing to change) and a warning must be recorded.
        assertTrue(statusChecker.containsMessageContaining("Not setter method for property [nonExisting]"), "A warning about the missing setter should have been logged.");
    }

    @Test
    void shouldLogErrorWhenSetterThrowsException() {
        // GIVEN
        ThrowingBean bean = new ThrowingBean();
        PropertySetter setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Object payload = new Object();
        // WHEN
        setter.setComplexProperty("window", payload);
        // THEN
        // Verify that an error status entry was created.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"), "An error should be logged when the underlying setter throws an exception.");
        // Also ensure that the underlying exception is attached to the status.
        boolean exceptionFound = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof IllegalStateException);
        assertTrue(exceptionFound, "The original exception should be attached to the error status.");
    }

    @Test
    void setterMethodNotFound_logsWarning() {
        // GIVEN – a bean with no setter and a PropertySetter that returns null for the method lookup.
        NoSetterBean bean = new NoSetterBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // simulate “no setter”
        setter.setFindMethodFn(name -> null);
        // WHEN
        setter.setComplexProperty("nonExistingProperty", new Object());
        // THEN – a warning containing the property name must be recorded.
        assertTrue(statusChecker.containsMessageContaining("Not setter method for property [nonExistingProperty]"), "Expected a warning when the setter method cannot be found.");
    }

    @Test
    void sanityCheckFails_logsWarning() throws NoSuchMethodException {
        // GIVEN – a bean with a valid setter, but the sanity check deliberately fails.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Method method = House.class.getMethod("setDoor", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((name, value) -> {
            // Log the warning exactly as the real implementation would.
            setter.addWarn("Sanity check failed for property [" + name + "]");
            // indicate failure
            return false;
        });
        // WHEN
        setter.setComplexProperty("door", new Object());
        // THEN – a warning must be present and the setter must not have been invoked.
        assertTrue(statusChecker.containsMessageContaining("Sanity check failed for property [door]"));
        assertNull(house.getDoor(), "The setter should not have been invoked when sanity check fails.");
    }

    @Test
    void successfulInvocation_noStatus() throws NoSuchMethodException {
        // GIVEN – a bean with a proper setter and all checks passing.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Method method = House.class.getMethod("setDoor", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((name, value) -> true);
        setter.setInvokeConsumer((m, p) -> {
            try {
                m.invoke(house, p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Object expected = new Object();
        // WHEN
        setter.setComplexProperty("door", expected);
        // THEN – the property is set and no warnings/errors are emitted.
        assertSame(expected, house.getDoor(), "Setter should have been invoked with the supplied object.");
        assertTrue(statusChecker.getStatusList().isEmpty(), "A successful call must not generate any status messages.");
    }

    @Test
    void invocationThrowsException_logsError() throws NoSuchMethodException {
        // GIVEN – a bean whose setter throws an unchecked exception.
        ThrowingBean bean = new ThrowingBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = ThrowingBean.class.getMethod("setWindow", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            // simulate the real reflection call which will throw the exception from the bean
            m.invoke(bean, p);
        });
        // WHEN
        setter.setComplexProperty("window", new Object());
        // THEN – an error status containing the original exception must be recorded.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"), "An error should be logged when the underlying setter throws an exception.");
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof IllegalStateException);
        assertTrue(found, "The original IllegalStateException should be attached to the error status.");
    }

    @Test
    void nullPropertyName_logsWarning() {
        // GIVEN – null property name leads to findSetterMethod returning null.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        // null name also results in null
        setter.setFindMethodFn(name -> null);
        // WHEN
        setter.setComplexProperty(null, new Object());
        // THEN – a warning mentioning “null” must be logged.
        assertTrue(statusChecker.containsMessageContaining("Not setter method for property [null]"));
    }

    @Test
    void emptyPropertyName_logsWarning() {
        // GIVEN – empty string also results in no setter.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        setter.setFindMethodFn(name -> null);
        // WHEN
        setter.setComplexProperty("", new Object());
        // THEN – warning about empty property name.
        assertTrue(statusChecker.containsMessageContaining("Not setter method for property []"));
    }

    @Test
    void nullComplexProperty_objectSetter_invokesWithNull() throws NoSuchMethodException {
        // GIVEN – a setter that accepts an Object; null value should be allowed.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Method method = House.class.getMethod("setDoor", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            try {
                m.invoke(house, p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // WHEN
        setter.setComplexProperty("door", null);
        // THEN – the setter is called with null and no status messages appear.
        assertNull(house.getDoor(), "Setter should have been invoked with null.");
        assertTrue(statusChecker.getStatusList().isEmpty(), "No warnings or errors should be emitted for a valid null value.");
    }

    @Test
    void nullComplexProperty_primitiveSetter_failsSanityCheck() throws NoSuchMethodException {
        // GIVEN – a primitive‑type setter; passing null should fail the sanity check.
        PrimitiveBean bean = new PrimitiveBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = PrimitiveBean.class.getMethod("setIntProperty", int.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> {
            setter.addWarn("Sanity check failed for property [" + n + "] – null not allowed for primitive");
            return false;
        });
        // WHEN
        setter.setComplexProperty("intProperty", null);
        // THEN – a warning must be logged and the setter not invoked.
        assertTrue(statusChecker.containsMessageContaining("Sanity check failed for property [intProperty]"));
    }

    @Test
    void multiParameterSetter_failsSanityCheck() throws NoSuchMethodException {
        // GIVEN – a setter with more than one parameter; the sanity check should reject it.
        MultiParamBean bean = new MultiParamBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = MultiParamBean.class.getMethod("setMulti", Object.class, Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> {
            setter.addWarn("Sanity check failed for property [" + n + "] – setter has " + method.getParameterCount() + " parameters");
            return false;
        });
        // WHEN
        setter.setComplexProperty("multiParamProperty", new Object());
        // THEN – warning about multiple parameters.
        assertTrue(statusChecker.containsMessageContaining("setter has 2 parameters"));
    }

    @Test
    void illegalAccessExceptionDuringInvocation_logsError() throws NoSuchMethodException {
        // GIVEN – a private setter that will cause IllegalAccessException when invoked via reflection.
        PrivateSetterBean bean = new PrivateSetterBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = PrivateSetterBean.class.getDeclaredMethod("setSecret", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            // Force an IllegalAccessException by not setting accessible.
            m.invoke(bean, p);
        });
        // WHEN
        setter.setComplexProperty("privateSetter", new Object());
        // THEN – an error status with IllegalAccessException as the cause.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"));
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof IllegalAccessException);
        assertTrue(found, "IllegalAccessException should be attached to the error status.");
    }

    @Test
    void invocationTargetExceptionWrappedCause_logsError() throws NoSuchMethodException {
        // GIVEN – a setter that throws a checked exception wrapped in InvocationTargetException.
        CheckedExceptionBean bean = new CheckedExceptionBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = CheckedExceptionBean.class.getMethod("setChecked", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            try {
                m.invoke(bean, p);
            } catch (InvocationTargetException e) {
                // re‑throw to be caught by the outer try‑catch in setComplexProperty
                throw e;
            }
        });
        // WHEN
        setter.setComplexProperty("setterThrowingCheckedException", new Object());
        // THEN – error status containing the InvocationTargetException.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"));
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof InvocationTargetException);
        assertTrue(found, "InvocationTargetException should be attached to the error status.");
    }

    @Test
    void runtimeExceptionDuringInvocation_logsError() throws NoSuchMethodException {
        // GIVEN – a setter that throws a RuntimeException.
        RuntimeExceptionBean bean = new RuntimeExceptionBean();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Method method = RuntimeExceptionBean.class.getMethod("setRuntime", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            m.invoke(bean, p);
        });
        // WHEN
        setter.setComplexProperty("setterThrowingRuntimeException", new Object());
        // THEN – error status with RuntimeException as the cause.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"));
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof RuntimeException);
        assertTrue(found, "RuntimeException should be attached to the error status.");
    }

    @Test
    void sanityCheckThrowsUnexpectedException_logsError() throws NoSuchMethodException {
        // GIVEN – a sanity‑check that throws an unexpected NullPointerException.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Method method = House.class.getMethod("setDoor", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> {
            throw new NullPointerException("unexpected");
        });
        // WHEN
        setter.setComplexProperty("door", new Object());
        // THEN – the NullPointerException should be caught and logged as an error.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"));
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof NullPointerException);
        assertTrue(found, "The unexpected NullPointerException should be attached to the error status.");
    }

    @Test
    void illegalArgumentExceptionDuringInvocation_logsError() throws NoSuchMethodException {
        // GIVEN – a setter that will receive an incompatible argument type, causing IllegalArgumentException.
        House house = new House();
        TestablePropertySetter setter = new TestablePropertySetter(beanDescriptionCache, house);
        setter.setContext(context);
        Method method = House.class.getMethod("setDoor", Object.class);
        setter.setFindMethodFn(name -> method);
        setter.setSanityPredicate((n, v) -> true);
        setter.setInvokeConsumer((m, p) -> {
            // Force an IllegalArgumentException by passing a primitive where an Object is expected.
            // This is artificial; we simply throw the exception ourselves.
            throw new IllegalArgumentException("type mismatch");
        });
        // WHEN
        setter.setComplexProperty("illegalArgProperty", new Object());
        // THEN – error status with IllegalArgumentException as the cause.
        assertTrue(statusChecker.containsMessageContaining("Could not set component"));
        boolean found = statusChecker.getStatusList().stream().anyMatch(s -> s.getThrowable() instanceof IllegalArgumentException);
        assertTrue(found, "IllegalArgumentException should be attached to the error status.");
    }
}
