package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.InvocationTargetException;

@ExtendWith(MockitoExtension.class)
class PropertySetter_invokeMethodWithSingleParameterOnThisObject_Test {

    private static Context context;

    private static BeanDescriptionCache beanDescriptionCache;

    // the instance under test
    private PropertySetter setter;

    // object for the success scenario
    private SimpleTarget simpleTarget;

    // object for the failure scenario
    private ExceptionTarget exceptionTarget;

    /**
     * Target class whose method always throws an exception – used to test error handling.
     */
    static class ExceptionTarget {

        public void explode(Object o) throws Exception {
            throw new Exception("boom");
        }
    }

    /**
     * Simple target used for normal successful invocations.
     */
    static class SimpleTarget {

        private String received;

        private int intValue;

        private Object objectValue;

        public void setValue(String value) {
            this.received = value;
        }

        public void setInt(int i) {
            this.intValue = i;
        }

        public void setObject(Object o) {
            this.objectValue = o;
        }

        public String getReceived() {
            return received;
        }

        public int getIntValue() {
            return intValue;
        }

        public Object getObjectValue() {
            return objectValue;
        }
    }

    /**
     * Target whose method always throws a checked exception.
     */
    static class CheckedExceptionTarget {

        public void methodThrowingChecked(String s) throws Exception {
            throw new Exception("checked");
        }
    }

    /**
     * Target whose method always throws a runtime exception.
     */
    static class RuntimeExceptionTarget {

        public void throwingMethod(String s) {
            throw new RuntimeException("runtime");
        }
    }

    /**
     * Target whose method throws an {@link Error}.
     */
    static class ErrorThrowingTarget {

        public void errorThrowingMethod(String s) {
            throw new AssertionError("error");
        }
    }

    /**
     * Target with a private method (illegal access).
     */
    static class PrivateMethodTarget {

        private void privateMethod(String s) {
            // never reached
        }
    }

    /**
     * Target with a static method.
     */
    static class StaticTarget {

        static String lastArg;

        public static void staticMethod(String s) {
            lastArg = s;
        }
    }

    /**
     * Target with a method that takes no arguments.
     */
    static class NoArgTarget {

        public void noArgMethod() {
            // nothing
        }
    }

    /**
     * Target whose method expects an int primitive.
     */
    static class PrimitiveIntTarget {

        private int value;

        public void setInt(int i) {
            this.value = i;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Target used for the “null target object” scenario.
     */
    static class DummyTarget {

        public void setValue(String s) {
            // no‑op
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh target objects for each test
        simpleTarget = new SimpleTarget();
        exceptionTarget = new ExceptionTarget();
        // create a real PropertySetter – we will spy it when we need to verify addError()
        setter = new PropertySetter(beanDescriptionCache, simpleTarget);
        setter.setContext(context);
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_successfulInvocation() throws Exception {
        // Obtain the Method object for SimpleTarget#setValue(String)
        Method method = SimpleTarget.class.getMethod("setValue", String.class);
        // Call the focal method
        setter.invokeMethodWithSingleParameterOnThisObject(method, "hello");
        // Verify that the target object's state was changed
        assertEquals("hello", simpleTarget.getReceived(), "The method should have been invoked and the value stored in the target object");
    }

    @Test
    void invokeMethodWithSingleParameterOnThisObject_handlesInvocationException() throws Exception {
        // Create a spy so we can verify the protected addError(...) call
        PropertySetter spySetter = Mockito.spy(new PropertySetter(beanDescriptionCache, exceptionTarget));
        spySetter.setContext(context);
        // Obtain the Method object that will throw an exception
        Method method = ExceptionTarget.class.getMethod("explode", Object.class);
        // Invoke the focal method – it should catch the exception and call addError
        spySetter.invokeMethodWithSingleParameterOnThisObject(method, new Object());
        // Verify that addError was called exactly once with a message containing the method name
        verify(spySetter, times(1)).addError(contains("Could not invoke method explode"), any(Throwable.class));
    }

    /**
     * Successful invocation of a public setter with a matching String parameter.
     */
    @Test
    void successfulInvocation_stringParameter() throws Exception {
        Method method = SimpleTarget.class.getMethod("setValue", String.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, "test");
        assertEquals("test", simpleTarget.getReceived(), "The setter should have stored the supplied string");
    }

    /**
     * Successful invocation of a method that expects a primitive int (autounboxing).
     */
    @Test
    void successfulInvocation_primitiveIntParameter() throws Exception {
        Method method = SimpleTarget.class.getMethod("setInt", int.class);
        // Integer autounboxed
        setter.invokeMethodWithSingleParameterOnThisObject(method, 42);
        assertEquals(42, simpleTarget.getIntValue(), "The primitive int setter should have stored the value 42");
    }

    /**
     * Successful invocation of a method that expects an Object (subclass argument).
     */
    @Test
    void successfulInvocation_subclassParameter() throws Exception {
        Method method = SimpleTarget.class.getMethod("setObject", Object.class);
        setter.invokeMethodWithSingleParameterOnThisObject(method, "a string");
        assertEquals("a string", simpleTarget.getObjectValue(), "The Object setter should have received the String instance");
    }

    /**
     * Successful invocation of a static method via reflection.
     */
    @Test
    void successfulInvocation_staticMethod() throws Exception {
        // PropertySetter holds a SimpleTarget instance, but static method belongs to StaticTarget
        Method staticMethod = StaticTarget.class.getMethod("staticMethod", String.class);
        // Use a spy to verify that no error is logged
        PropertySetter spySetter = Mockito.spy(new PropertySetter(beanDescriptionCache, simpleTarget));
        spySetter.setContext(context);
        spySetter.invokeMethodWithSingleParameterOnThisObject(staticMethod, "static call");
        assertEquals("static call", StaticTarget.lastArg, "The static method should have been executed and stored its argument");
        verify(spySetter, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Invocation with a mismatched parameter type (String setter + Integer argument).
     */
    @Test
    void mismatchedParameterType_logsError() throws Exception {
        Method method = SimpleTarget.class.getMethod("setValue", String.class);
        PropertySetter spy = Mockito.spy(new PropertySetter(beanDescriptionCache, simpleTarget));
        spy.setContext(context);
        // Integer → IllegalArgumentException
        spy.invokeMethodWithSingleParameterOnThisObject(method, 123);
        verify(spy, times(1)).addError(contains("Could not invoke method setValue"), any(Throwable.class));
    }

    /**
     * Invocation where the target method throws a runtime exception (wrapped in InvocationTargetException).
     */
    @Test
    void methodThrowsRuntimeException_logsError() throws Exception {
        RuntimeExceptionTarget target = new RuntimeExceptionTarget();
        PropertySetter spy = Mockito.spy(new PropertySetter(beanDescriptionCache, target));
        spy.setContext(context);
        Method method = RuntimeExceptionTarget.class.getMethod("throwingMethod", String.class);
        spy.invokeMethodWithSingleParameterOnThisObject(method, "any");
        verify(spy, times(1)).addError(contains("Could not invoke method throwingMethod"), any(Throwable.class));
    }

    /**
     * Invocation where the target method throws a checked exception.
     */
    @Test
    void methodThrowsCheckedException_logsError() throws Exception {
        CheckedExceptionTarget target = new CheckedExceptionTarget();
        PropertySetter spy = Mockito.spy(new PropertySetter(beanDescriptionCache, target));
        spy.setContext(context);
        Method method = CheckedExceptionTarget.class.getMethod("methodThrowingChecked", String.class);
        spy.invokeMethodWithSingleParameterOnThisObject(method, "trigger");
        verify(spy, times(1)).addError(contains("Could not invoke method methodThrowingChecked"), any(Throwable.class));
    }

    /**
     * Invocation where the target method throws an {@link Error}; the error should propagate.
     */
    @Test
    void methodThrowsError_propagates() throws Exception {
        ErrorThrowingTarget target = new ErrorThrowingTarget();
        PropertySetter ps = new PropertySetter(beanDescriptionCache, target);
        ps.setContext(context);
        Method method = ErrorThrowingTarget.class.getMethod("errorThrowingMethod", String.class);
        AssertionError thrown = assertThrows(AssertionError.class, () -> ps.invokeMethodWithSingleParameterOnThisObject(method, "any"));
        assertEquals("error", thrown.getMessage());
    }

    /**
     * Invocation of a private method (illegal access).
     */
    @Test
    void privateMethod_illegalAccess_logsError() throws Exception {
        PrivateMethodTarget target = new PrivateMethodTarget();
        PropertySetter spy = Mockito.spy(new PropertySetter(beanDescriptionCache, target));
        spy.setContext(context);
        Method privateMethod = PrivateMethodTarget.class.getDeclaredMethod("privateMethod", String.class);
        // Do NOT setAccessible(true) – we want the IllegalAccessException path
        spy.invokeMethodWithSingleParameterOnThisObject(privateMethod, "secret");
        verify(spy, times(1)).addError(contains("Could not invoke method privateMethod"), any(Throwable.class));
    }

    /**
     * Invocation of a method that expects no parameters but receives one.
     */
    @Test
    void noArgMethod_illegalArgument_logsError() throws Exception {
        NoArgTarget target = new NoArgTarget();
        PropertySetter spy = Mockito.spy(new PropertySetter(beanDescriptionCache, target));
        spy.setContext(context);
        Method method = NoArgTarget.class.getMethod("noArgMethod");
        spy.invokeMethodWithSingleParameterOnThisObject(method, "unused");
        verify(spy, times(1)).addError(contains("Could not invoke method noArgMethod"), any(Throwable.class));
    }

    /**
     * Passing a null parameter causes a NullPointerException before the try‑block.
     */
    @Test
    void nullParameter_throwsNullPointerException() throws Exception {
        Method method = SimpleTarget.class.getMethod("setValue", String.class);
        // The NPE occurs at parameter.getClass(); the catch block is never reached.
        NullPointerException npe = assertThrows(NullPointerException.class, () -> setter.invokeMethodWithSingleParameterOnThisObject(method, null));
        // Ensure no error was logged
        PropertySetter spy = Mockito.spy(setter);
        verify(spy, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Passing a null Method reference results in a NullPointerException that propagates.
     */
    @Test
    void nullMethodReference_throwsNullPointerException() {
        // The method argument is null – the catch block itself throws another NPE.
        NullPointerException npe = assertThrows(NullPointerException.class, () -> setter.invokeMethodWithSingleParameterOnThisObject(null, "any"));
        // No interaction with addError should have happened
        PropertySetter spy = Mockito.spy(setter);
        verify(spy, never()).addError(anyString(), any(Throwable.class));
    }

    /**
     * Invoking a method when the PropertySetter was constructed with a null target object.
     */
    @Test
    void nullTargetObject_throwsNullPointerException() throws Exception {
        PropertySetter ps = new PropertySetter(beanDescriptionCache, null);
        ps.setContext(context);
        Method method = DummyTarget.class.getMethod("setValue", String.class);
        // First NPE from method.invoke because obj is null; second NPE in catch block while building message.
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ps.invokeMethodWithSingleParameterOnThisObject(method, "test"));
        // No error should be logged because the NPE occurs while constructing the error message.
        PropertySetter spy = Mockito.spy(ps);
        verify(spy, never()).addError(anyString(), any(Throwable.class));
    }
}
