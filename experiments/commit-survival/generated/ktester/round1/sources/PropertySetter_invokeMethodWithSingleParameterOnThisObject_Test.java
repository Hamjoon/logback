package ch.qos.logback.core.joran.util;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PropertySetter_invokeMethodWithSingleParameterOnThisObject_Test {

    private static ContextBase context;

    private static BeanDescriptionCache cache;

    private PropertySetter setter;

    private SampleBean sampleBean;

    // the object on which methods will be invoked
    private TestTarget target;

    /**
     * A minimal bean used for the successful‑invocation test.
     */
    public static class SampleBean {

        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Simple bean with a variety of methods used to drive the test scenarios.
     */
    public static class TestTarget {

        // fields used to verify successful invocations
        String successValue;

        int intValue;

        Number numberValue;

        Object objectValue;

        static String staticValue;

        // -----------------------------------------------------------------
        // successful paths
        // -----------------------------------------------------------------
        public void successMethod(String s) {
            this.successValue = s;
        }

        public void primitiveIntMethod(int i) {
            this.intValue = i;
        }

        public void numberParameterMethod(Number n) {
            this.numberValue = n;
        }

        public void objectParameterMethod(Object o) {
            this.objectValue = o;
        }

        public static void staticMethod(String s) {
            staticValue = s;
        }

        // -----------------------------------------------------------------
        // error‑producing paths
        // -----------------------------------------------------------------
        public void illegalArgumentMethod(String s) {
            // No body – the IllegalArgumentException is thrown by reflection before entry
        }

        public void throwingMethod(String s) throws IOException {
            throw new IOException("forced IO failure");
        }

        public void nullParameterMethod(String s) {
            // Force a NPE when a null argument is supplied
            // will throw NPE if s == null
            s.length();
        }

        private void privateMethod(String s) {
            // never reached – reflection should block access
        }

        public void runtimeExceptionMethod(String s) {
            throw new RuntimeException("boom");
        }

        public void zeroArgMethod() {
            // No parameters – invoking with an argument will cause IllegalArgumentException
        }

        public void errorThrowingMethod(String s) {
            throw new AssertionError("assertion failure");
        }
    }

    /**
     * A different class whose method will be used to test foreign‑method invocation.
     */
    public static class AnotherClass {

        public void foreignMethod(String s) {
            // no-op
        }
    }

    /**
     * Simple custom bean used for object‑parameter tests.
     */
    public static class CustomBean {
        // empty – just a distinct type
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a real ContextBase is used so that the error status can be inspected later
        context = new ContextBase();
        cache = new BeanDescriptionCache(context);
        context = new ContextBase();
        cache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        sampleBean = new SampleBean();
        setter = new PropertySetter(cache, sampleBean);
        setter.setContext(context);
        target = new TestTarget();
        // the real constructor used by Logback: PropertySetter(BeanDescriptionCache, Object)
        setter = new PropertySetter(cache, target);
        setter.setContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear any status that might have been added during a test
        StatusManager sm = context.getStatusManager();
        sm.clear();
        // clear any status that might have been added during a test
        StatusManager sm = context.getStatusManager();
        sm.clear();
    }

    /**
     * Returns the list of error {@link Status} objects currently stored in the context.
     */
    private List<Status> errorStatuses() {
        return context.getStatusManager().getCopyOfStatusList().stream().filter(s -> s.getLevel() == Status.ERROR).toList();
    }

    /**
     * Asserts that exactly one error status exists and that its message and cause match expectations.
     */
    private void assertErrorLogged(String expectedMethodName, Class<? extends Throwable> expectedCause) {
        List<Status> errors = errorStatuses();
        assertEquals(1, errors.size(), "Exactly one error status should have been recorded");
        Status err = errors.get(0);
        assertTrue(err.getMessage().contains("Could not invoke method " + expectedMethodName), "Error message must contain the method name");
        assertNotNull(err.getThrowable(), "Error status must contain a throwable");
        assertEquals(expectedCause, err.getThrowable().getClass(), "Cause type must match expected");
    }

    /**
     * Asserts that no error status has been recorded.
     */
    private void assertNoErrorLogged() {
        assertTrue(errorStatuses().isEmpty(), "No error status should have been recorded");
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_successfulInvocation() throws Exception {
        // obtain the setter method of SampleBean (setValue(String))
        Method method = SampleBean.class.getMethod("setValue", String.class);
        // invoke the method via PropertySetter
        setter.invokeMethodWithSingleParameterOnThisObject(method, "hello");
        // verify that the bean received the value
        assertEquals("hello", sampleBean.getValue(), "The SampleBean should have received the value passed to invokeMethodWithSingleParameterOnThisObject");
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_reflectiveExceptionIsHandled() throws Exception {
        // create a mock Method that throws an exception when invoked
        Method mockMethod = Mockito.mock(Method.class);
        Mockito.when(mockMethod.getName()).thenReturn("badMethod");
        // the reflective call will throw an IllegalAccessException
        Mockito.doThrow(new IllegalAccessException("forced failure")).when(mockMethod).invoke(Mockito.any(), Mockito.any());
        // the parameter can be any object – its class is used only for the error message
        Object parameter = new Object();
        // invoke – the method must **not** propagate the exception
        setter.invokeMethodWithSingleParameterOnThisObject(mockMethod, parameter);
        // verify that an error status has been recorded in the context
        boolean errorFound = context.getStatusManager().getCopyOfStatusList().stream().anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().contains("Could not invoke method badMethod"));
        assertTrue(errorFound, "An error status containing the method name should have been added to the context");
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_success_String() throws Exception {
        Method m = TestTarget.class.getMethod("successMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, "OK");
        assertEquals("OK", target.successValue);
        assertNoErrorLogged();
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_success_primitiveInt() throws Exception {
        Method m = TestTarget.class.getMethod("primitiveIntMethod", int.class);
        // autoboxed Integer
        setter.invokeMethodWithSingleParameterOnThisObject(m, 42);
        assertEquals(42, target.intValue);
        assertNoErrorLogged();
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_success_NumberSubclass() throws Exception {
        Method m = TestTarget.class.getMethod("numberParameterMethod", Number.class);
        Integer param = 7;
        setter.invokeMethodWithSingleParameterOnThisObject(m, param);
        assertEquals(param, target.numberValue);
        assertNoErrorLogged();
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_success_Object() throws Exception {
        Method m = TestTarget.class.getMethod("objectParameterMethod", Object.class);
        CustomBean bean = new CustomBean();
        setter.invokeMethodWithSingleParameterOnThisObject(m, bean);
        assertSame(bean, target.objectValue);
        assertNoErrorLogged();
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_success_staticMethod() throws Exception {
        Method m = TestTarget.class.getMethod("staticMethod", String.class);
        // static method does not modify instance state; we only verify no error
        setter.invokeMethodWithSingleParameterOnThisObject(m, "static");
        assertEquals("static", TestTarget.staticValue);
        assertNoErrorLogged();
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_illegalArgument() throws Exception {
        Method m = TestTarget.class.getMethod("illegalArgumentMethod", String.class);
        // pass an Integer – Method.invoke will throw IllegalArgumentException
        setter.invokeMethodWithSingleParameterOnThisObject(m, 123);
        assertErrorLogged("illegalArgumentMethod", IllegalArgumentException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_throwingCheckedException() throws Exception {
        Method m = TestTarget.class.getMethod("throwingMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, "trigger");
        // The checked exception is wrapped in InvocationTargetException
        assertErrorLogged("throwingMethod", InvocationTargetException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_nullParameter() throws Exception {
        Method m = TestTarget.class.getMethod("nullParameterMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, null);
        // The method itself will throw NullPointerException
        assertErrorLogged("nullParameterMethod", NullPointerException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_primitiveInt_nullArgument() throws Exception {
        Method m = TestTarget.class.getMethod("primitiveIntMethod", int.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, null);
        // Null cannot be unboxed to a primitive → NullPointerException
        assertErrorLogged("primitiveIntMethod", NullPointerException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_nullMethodArgument() {
        // Passing null as the Method reference itself
        setter.invokeMethodWithSingleParameterOnThisObject(null, "any");
        assertErrorLogged("null", NullPointerException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_privateMethod() throws Exception {
        Method m = TestTarget.class.getDeclaredMethod("privateMethod", String.class);
        // Do NOT set accessible – invoke should fail with IllegalAccessException
        setter.invokeMethodWithSingleParameterOnThisObject(m, "secret");
        assertErrorLogged("privateMethod", IllegalAccessException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_runtimeException() throws Exception {
        Method m = TestTarget.class.getMethod("runtimeExceptionMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, "boom");
        // RuntimeException is wrapped in InvocationTargetException
        assertErrorLogged("runtimeExceptionMethod", InvocationTargetException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_zeroArgMethod() throws Exception {
        Method m = TestTarget.class.getMethod("zeroArgMethod");
        setter.invokeMethodWithSingleParameterOnThisObject(m, "unused");
        // Wrong argument count → IllegalArgumentException
        assertErrorLogged("zeroArgMethod", IllegalArgumentException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_foreignMethod() throws Exception {
        Method m = AnotherClass.class.getMethod("foreignMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, "test");
        // The declaring class is not assignable from the target object → IllegalArgumentException
        assertErrorLogged("foreignMethod", IllegalArgumentException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_primitiveInt_longArgument() throws Exception {
        Method m = TestTarget.class.getMethod("primitiveIntMethod", int.class);
        // Long cannot be converted to int
        setter.invokeMethodWithSingleParameterOnThisObject(m, 123L);
        assertErrorLogged("primitiveIntMethod", IllegalArgumentException.class);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_errorThrowingMethod() throws Exception {
        Method m = TestTarget.class.getMethod("errorThrowingMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(m, "ignore");
        // AssertionError wrapped in InvocationTargetException
        assertErrorLogged("errorThrowingMethod", InvocationTargetException.class);
    }
}
