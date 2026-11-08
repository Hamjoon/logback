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
import ch.qos.logback.core.joran.spi.DefaultClass;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_addComplexProperty_Test {

    private static Context context;

    private static BeanDescriptionCache beanDescriptionCache;

    private PropertySetter setter;

    private StatusChecker statusChecker;

    /**
     * Bean that has a proper `addDoor(Object)` method.
     */
    public static class DoorHolder {

        boolean called = false;

        Object argument = null;

        public void addDoor(Object o) {
            called = true;
            argument = o;
        }
    }

    /**
     * Bean that has an `addDoor(String)` method – wrong parameter type.
     */
    public static class DoorHolderWithWrongParam {

        boolean called = false;

        public void addDoor(String s) {
            // should never be invoked for an Object argument
            called = true;
        }
    }

    /**
     * Bean that does not define any `addXxx` method.
     */
    public static class NoAdderBean {
        // intentionally empty
    }

    /**
     * Bean with a proper addAppender(DummyAppender) method.
     */
    public static class AppenderHolder {

        boolean called = false;

        Object argument = null;

        public void addAppender(DummyAppender a) {
            called = true;
            argument = a;
        }
    }

    /**
     * Bean with a wrong‑type adder (expects String).
     */
    public static class WrongParamHolder {

        boolean called = false;

        public void addAppender(String s) {
            called = true;
        }
    }

    /**
     * Bean with a private adder method.
     */
    public static class PrivateAdderHolder {

        boolean called = false;

        private void addPrivate(Object o) {
            called = true;
        }
    }

    /**
     * Bean with a protected adder method (used for the SecurityException test).
     */
    public static class ProtectedAdderHolder {

        boolean called = false;

        protected void addProtected(Object o) {
            called = true;
        }
    }

    /**
     * Bean with a multi‑parameter adder (invalid signature).
     */
    public static class MultiParamAdderHolder {

        public void addMultiParam(Object a, Object b) {
        }
    }

    /**
     * Bean with a primitive‑type adder (int).
     */
    public static class PrimitiveAdderHolder {

        public void addCount(int i) {
        }
    }

    /**
     * Bean whose adder throws a runtime exception (wrapped in InvocationTargetException).
     */
    public static class FaultyComponentHolder {

        public void addFaulty(Object o) {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Simple dummy classes for type‑compatibility tests.
     */
    public static class DummyAppender {
    }

    public static class SubDummyAppender extends DummyAppender {
    }

    /**
     * Simple utility to check whether any error status has been recorded.
     */
    private static class StatusChecker {

        private final Context context;

        StatusChecker(Context ctx) {
            this.context = ctx;
        }

        boolean containsMessage(String fragment) {
            return context.getStatusManager().getCopyOfStatusList().stream().anyMatch(s -> s.getMessage().contains(fragment));
        }

        boolean hasError() {
            return context.getStatusManager().getCopyOfStatusList().stream().anyMatch(s -> s.getLevel() == ch.qos.logback.core.status.Status.ERROR);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test creates its own PropertySetter instance
        // the concrete bean will be supplied inside the test method
        statusChecker = new StatusChecker(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear any status messages that might have been added during the test
        StatusManager sm = context.getStatusManager();
        if (sm != null) {
            sm.clear();
        }
    }

    @Test
    void addComplexProperty_successfulInvocation() {
        // group: Adder method exists and sanity check passes
        AppenderHolder bean = new AppenderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        DummyAppender complex = new DummyAppender();
        setter.addComplexProperty("appender", complex);
        assertTrue(bean.called, "addAppender should have been invoked");
        assertSame(complex, bean.argument, "Argument must be the same instance");
    }

    @Test
    void addComplexProperty_noAdderMethod_logsError() {
        // Arrange
        NoAdderBean bean = new NoAdderBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Object complex = new Object();
        // Act
        setter.addComplexProperty("door", complex);
        // Assert – an error status should have been added
        boolean errorFound = statusChecker.containsMessage("Could not find method [adddoor] in class [" + bean.getClass().getName() + "].");
        assertTrue(errorFound, "Expected error status when adder method cannot be found");
    }

    @Test
    void addComplexProperty_sanityCheckFails_doesNotInvokeMethod() {
        // Arrange
        DoorHolderWithWrongParam bean = new DoorHolderWithWrongParam();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // not a String, so sanity check should fail
        Object complex = new Object();
        // Act
        setter.addComplexProperty("door", complex);
        // Assert – the method should not have been called
        assertFalse(bean.called, "addDoor should NOT be invoked when parameter type mismatches");
    }

    @Test
    void addComplexProperty_sanityCheckFails_dueToWrongParamType() {
        // group: Adder method exists but sanity check fails
        WrongParamHolder bean = new WrongParamHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // not a String
        DummyAppender complex = new DummyAppender();
        setter.addComplexProperty("appender", complex);
        assertFalse(bean.called, "addAppender should NOT be invoked when sanity check fails");
        // No error is logged – the method simply returns after the sanity check.
        assertFalse(statusChecker.hasError(), "No error status should be recorded");
    }

    @Test
    void addComplexProperty_nullName_logsError() {
        // group: Null name argument
        NoAdderBean bean = new NoAdderBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        DummyAppender complex = new DummyAppender();
        setter.addComplexProperty(null, complex);
        boolean errorFound = statusChecker.containsMessage("Could not find method [addnull] in class [" + bean.getClass().getName() + "].");
        assertTrue(errorFound, "Expected error status for null property name");
    }

    @Test
    void addComplexProperty_emptyName_logsError() {
        // group: Empty name argument
        NoAdderBean bean = new NoAdderBean();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        DummyAppender complex = new DummyAppender();
        setter.addComplexProperty("", complex);
        boolean errorFound = statusChecker.containsMessage("Could not find method [add] in class [" + bean.getClass().getName() + "].");
        assertTrue(errorFound, "Expected error status for empty property name");
    }

    @Test
    void addComplexProperty_nullComplexProperty_sanityCheckFails() {
        // group: Null complexProperty with existing adder method
        AppenderHolder bean = new AppenderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        setter.addComplexProperty("appender", null);
        assertFalse(bean.called, "Adder must not be invoked when complexProperty is null");
        // No error is logged – the sanity check simply returns false.
        assertFalse(statusChecker.hasError(), "No error status should be recorded");
    }

    @Test
    void addComplexProperty_adderThrowsInvocationTargetException_logsError() {
        // group: Adder method throws InvocationTargetException
        FaultyComponentHolder bean = new FaultyComponentHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Object complex = new Object();
        setter.addComplexProperty("faulty", complex);
        boolean errorFound = statusChecker.containsMessage("Exception while invoking method [addFaulty]");
        assertTrue(errorFound, "Expected error status when adder throws an exception");
    }

    @Test
    void addComplexProperty_multiParameterAdder_isRejectedBySanityCheck() {
        // group: Adder method with more than one parameter (invalid signature)
        MultiParamAdderHolder bean = new MultiParamAdderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        DummyAppender complex = new DummyAppender();
        setter.addComplexProperty("multiParam", complex);
        // sanity check fails → method not invoked, no error logged.
        assertFalse(statusChecker.hasError(), "No error should be logged for invalid signature");
    }

    @Test
    void addComplexProperty_privateAdder_logsError() {
        // group: Adder method is private (accessibility issue)
        PrivateAdderHolder bean = new PrivateAdderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        Object complex = new Object();
        setter.addComplexProperty("private", complex);
        boolean errorFound = statusChecker.containsMessage("Illegal access");
        assertTrue(errorFound, "Expected error status for private adder method");
    }

    @Test
    void addComplexProperty_polymorphicCompatibility_successfulInvocation() {
        // group: Adder method exists and accepts a superclass type (polymorphic compatibility)
        AppenderHolder bean = new AppenderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        SubDummyAppender complex = new SubDummyAppender();
        setter.addComplexProperty("appender", complex);
        assertTrue(bean.called, "addAppender should be invoked with subclass instance");
        assertSame(complex, bean.argument, "Argument should be the subclass instance");
    }

    @Test
    void addComplexProperty_sanityCheckThrowsRuntimeException_propagates() {
        // group: Sanity check throws RuntimeException
        AppenderHolder bean = new AppenderHolder();
        PropertySetter real = new PropertySetter(beanDescriptionCache, bean);
        PropertySetter spy = Mockito.spy(real);
        spy.setContext(context);
        // Force isSanityCheckSuccessful to throw
        doThrow(new RuntimeException("sanity failure")).when(spy).isSanityCheckSuccessful(anyString(), any(Method.class), any(), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.addComplexProperty("appender", new DummyAppender()), "RuntimeException should propagate out of addComplexProperty");
        assertEquals("sanity failure", thrown.getMessage());
    }

    @Test
    void addComplexProperty_primitiveParameterMismatch_logsError() {
        // group: IllegalArgumentException due to primitive parameter type mismatch
        PrimitiveAdderHolder bean = new PrimitiveAdderHolder();
        setter = new PropertySetter(beanDescriptionCache, bean);
        setter.setContext(context);
        // Passing null for a primitive int should cause IllegalArgumentException inside Method.invoke
        setter.addComplexProperty("count", null);
        boolean errorFound = statusChecker.containsMessage("Illegal argument");
        assertTrue(errorFound, "Expected error status for primitive‑type mismatch");
    }

    @Test
    void addComplexProperty_securityExceptionDuringInvocation_logsError() throws Exception {
        // group: SecurityException when accessing non‑public adder method
        // We simulate the SecurityException by spying on the setter and making
        // invokeMethodWithSingleParameterOnThisObject throw it.
        ProtectedAdderHolder bean = new ProtectedAdderHolder();
        PropertySetter real = new PropertySetter(beanDescriptionCache, bean);
        PropertySetter spy = Mockito.spy(real);
        spy.setContext(context);
        doThrow(new SecurityException("access denied")).when(spy).invokeMethodWithSingleParameterOnThisObject(any(Method.class), any());
        spy.addComplexProperty("protected", new Object());
        boolean errorFound = statusChecker.containsMessage("SecurityException");
        assertTrue(errorFound, "Expected error status when a SecurityException occurs");
    }
}
