package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.InvocationTargetException;

@ExtendWith(MockitoExtension.class)
class PropertySetter_invokeMethodWithSingleParameterOnThisObject_Test {

    private Context context;

    private BeanDescriptionCache beanDescriptionCache;

    private Dummy dummy;

    private PropertySetter setter;

    private TestObject testObject;

    /**
     * Simple bean used as the target of reflection calls.
     */
    static class Dummy {

        private String value;

        private boolean failCalled = false;

        public void setValue(String v) {
            this.value = v;
        }

        public String getValue() {
            return value;
        }

        public void failMethod(Object o) {
            failCalled = true;
            throw new IllegalArgumentException("forced failure");
        }

        public boolean isFailCalled() {
            return failCalled;
        }
    }

    /**
     * Simple bean used for most instance‑method tests.
     */
    static class TestObject {

        private boolean flag;

        private int number;

        private String objectValue;

        private int count;

        private static boolean staticFlag;

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        private void privateMethod(String s) {
            // intentionally left blank – only used to trigger IllegalAccessException
        }

        public void methodThatThrows(String s) {
            throw new IllegalStateException("forced exception");
        }

        public void setObject(Object o) {
            this.objectValue = (String) o;
        }

        public String getObjectValue() {
            return objectValue;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public static void staticSetFlag(boolean flag) {
            staticFlag = flag;
        }

        public static boolean isStaticFlag() {
            return staticFlag;
        }

        public void noArgMethod() {
            // no‑op
        }
    }

    /**
     * Unrelated class – used to test invoking a Method that belongs to a different class.
     */
    static class UnrelatedClass {

        public void unrelatedMethod(String s) {
            // no‑op
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
        testObject = new TestObject();
        // Spy the PropertySetter so we can verify calls to addError().
        setter = Mockito.spy(new PropertySetter(beanDescriptionCache, testObject));
        setter.setContext(context);
        // Reset static flag before each test.
        TestObject.staticFlag = false;
    }

    /**
     * Verify that a normal method is invoked correctly and the target object's state changes.
     */
    @Test
    void invokeMethodWithSingleParameterOnThisObject_successfulInvocation() throws Exception {
        // Obtain the setter method via reflection.
        Method method = Dummy.class.getMethod("setValue", String.class);
        // Call the package‑private method directly (same package).
        setter.invokeMethodWithSingleParameterOnThisObject(method, "hello");
        // The dummy instance should now contain the new value.
        assertEquals("hello", dummy.getValue(), "The dummy object's setter should have been invoked.");
        // No error should have been recorded.
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Verify that when the reflected method throws an exception,
     * {@code addError} is called with a message that contains the method name and
     * the parameter type.
     */
    @Test
    void invokeMethodWithSingleParameterOnThisObject_failureInvocation_logsError() throws Exception {
        // Obtain a method that always throws.
        Method method = Dummy.class.getMethod("failMethod", Object.class);
        // The call should be caught inside the method under test; it must not propagate.
        assertDoesNotThrow(() -> setter.invokeMethodWithSingleParameterOnThisObject(method, "anyParam"));
        // The dummy's method should have been entered (failCalled = true) before the exception.
        assertTrue(dummy.isFailCalled(), "The target method should have been executed.");
        // Verify that addError was called exactly once with a message containing the method name.
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thrCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msgCaptor.capture(), thrCaptor.capture());
        String loggedMessage = msgCaptor.getValue();
        Throwable loggedThrowable = thrCaptor.getValue();
        assertNotNull(loggedMessage, "Error message should not be null");
        assertTrue(loggedMessage.contains("failMethod"), "Error message should contain the failing method name");
        assertTrue(loggedMessage.contains("java.lang.String"), "Error message should contain the parameter type name");
        assertNotNull(loggedThrowable, "Throwable passed to addError should not be null");
        assertTrue(loggedThrowable instanceof IllegalArgumentException, "The logged throwable should be the original exception type");
    }

    /**
     * Successful invocation – primitive boolean parameter.
     */
    @Test
    void successfulInvocation_setFlag_boolean() throws Exception {
        Method method = TestObject.class.getMethod("setFlag", boolean.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, true);
        assertTrue(testObject.isFlag());
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Exception handling – type mismatch (String supplied for int parameter).
     */
    @Test
    void exception_typeMismatch_addErrorLogged() throws Exception {
        Method method = TestObject.class.getMethod("setNumber", int.class);
        // Passing a String should cause IllegalArgumentException.
        setter.invokeMethodWithSingleParameterOnThisObject(method, "not-a-number");
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("setNumber"));
        assertTrue(msg.getValue().contains(TestObject.class.getName()));
        assertTrue(msg.getValue().contains("java.lang.String"));
        assertTrue(thr.getValue() instanceof IllegalArgumentException);
    }

    /**
     * Exception handling – IllegalAccessException when invoking a private method.
     */
    @Test
    void exception_privateMethod_illegalAccess() throws Exception {
        Method method = TestObject.class.getDeclaredMethod("privateMethod", String.class);
        // Do NOT set accessible – should trigger IllegalAccessException.
        setter.invokeMethodWithSingleParameterOnThisObject(method, "test");
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("privateMethod"));
        assertTrue(msg.getValue().contains(TestObject.class.getName()));
        assertTrue(msg.getValue().contains("java.lang.String"));
        assertTrue(thr.getValue() instanceof IllegalAccessException);
    }

    /**
     * Exception handling – InvocationTargetException when the target method throws.
     */
    @Test
    void exception_methodThrowsInvocationTargetException() throws Exception {
        Method method = TestObject.class.getMethod("methodThatThrows", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, "trigger");
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("methodThatThrows"));
        assertTrue(msg.getValue().contains(TestObject.class.getName()));
        assertTrue(msg.getValue().contains("java.lang.String"));
        // The caught exception is the wrapper InvocationTargetException.
        assertTrue(thr.getValue() instanceof InvocationTargetException);
    }

    /**
     * Null parameter handling – NPE occurs before the internal try‑catch.
     */
    @Test
    void nullParameter_throwsNullPointerException_beforeLogging() throws Exception {
        Method method = TestObject.class.getMethod("setFlag", boolean.class);
        // The method itself is fine, but parameter is null → NPE at parameter.getClass()
        assertThrows(NullPointerException.class, () -> setter.invokeMethodWithSingleParameterOnThisObject(method, null));
        // No error should be logged because the NPE happens before the catch block.
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Successful invocation – subclass parameter (String passed to Object).
     */
    @Test
    void successfulInvocation_subclassParameter() throws Exception {
        Method method = TestObject.class.getMethod("setObject", Object.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, "string-value");
        assertEquals("string-value", testObject.getObjectValue());
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Successful invocation – primitive int with Integer wrapper (autounboxing).
     */
    @Test
    void successfulInvocation_primitiveIntWithWrapper() throws Exception {
        Method method = TestObject.class.getMethod("setCount", int.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, Integer.valueOf(42));
        assertEquals(42, testObject.getCount());
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Successful invocation – static method (obj reference ignored).
     */
    @Test
    void successfulInvocation_staticMethod() throws Exception {
        Method method = TestObject.class.getMethod("staticSetFlag", boolean.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, false);
        assertFalse(TestObject.isStaticFlag());
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Exception handling – null Method argument leads to NPE before addError.
     */
    @Test
    void nullMethodArgument_throwsNullPointerException_beforeLogging() throws Exception {
        // Parameter is non‑null to avoid NPE on parameter.getClass()
        assertThrows(NullPointerException.class, () -> setter.invokeMethodWithSingleParameterOnThisObject(null, "any"));
        // Because the NPE occurs while building the error message, addError is never called.
        verify(setter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Exception handling – null target object (obj field is null).
     */
    @Test
    void nullTargetObject_addErrorLogged() throws Exception {
        // Construct a PropertySetter whose obj field is null.
        PropertySetter nullObjSetter = Mockito.spy(new PropertySetter(beanDescriptionCache, null));
        nullObjSetter.setContext(context);
        Method method = TestObject.class.getMethod("setFlag", boolean.class);
        nullObjSetter.invokeMethodWithSingleParameterOnThisObject(method, true);
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(nullObjSetter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("setFlag"));
        // The code attempts to call obj.getClass().getName(); this throws NPE before addError,
        // however in the current implementation the NPE is caught and the message construction
        // fails, resulting in a NullPointerException being propagated. To keep the test
        // aligned with the actual behaviour, we assert that a NullPointerException was logged.
        assertTrue(thr.getValue() instanceof NullPointerException);
    }

    /**
     * Exception handling – method from unrelated class.
     */
    @Test
    void unrelatedMethodInvocation_addErrorLogged() throws Exception {
        Method method = UnrelatedClass.class.getMethod("unrelatedMethod", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, "test");
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("unrelatedMethod"));
        assertTrue(msg.getValue().contains(TestObject.class.getName()));
        assertTrue(msg.getValue().contains("java.lang.String"));
        assertTrue(thr.getValue() instanceof IllegalArgumentException);
    }

    /**
     * Exception handling – wrong number of parameters (method expects none).
     */
    @Test
    void wrongArgumentCount_addErrorLogged() throws Exception {
        Method method = TestObject.class.getMethod("noArgMethod");
        // Supplying an argument where none are expected.
        setter.invokeMethodWithSingleParameterOnThisObject(method, "unused");
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> thr = ArgumentCaptor.forClass(Throwable.class);
        verify(setter, times(1)).addError(msg.capture(), thr.capture());
        assertTrue(msg.getValue().contains("noArgMethod"));
        assertTrue(msg.getValue().contains(TestObject.class.getName()));
        assertTrue(msg.getValue().contains("java.lang.String"));
        assertTrue(thr.getValue() instanceof IllegalArgumentException);
    }
}
