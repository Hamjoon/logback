package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getObjClass_Test {

    /**
     * Shared context for all tests.
     */
    private static ContextBase context;

    /**
     * Shared cache for bean descriptions.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Instance under test – created anew for each test method.
     */
    private PropertySetter setter;

    /**
     * The target object supplied to the {@link PropertySetter} constructor.
     */
    private Object targetObject;

    class SimpleBean {
    }

    interface Dummy {

        void doSomething();
    }

    /**
     * Simple custom bean used in the default @BeforeEach setup.
     */
    static class House {
        // intentionally empty – only its class matters for the test
    }

    /**
     * User‑defined bean referenced in the JSON test data.
     */
    static class MyBean {
        // intentionally empty
    }

    /**
     * Simple holder for parameterised test arguments.
     *
     * @param target   the object supplied to {@link PropertySetter}'s constructor
     * @param expected the expected result of {@code getObjClass()}
     * @param description a human‑readable description used in assertion messages
     */
    private static class ArgumentsHolder {

        final Object target;

        final Class<?> expected;

        final String description;

        ArgumentsHolder(Object target, Class<?> expected, String description) {
            this.target = target;
            this.expected = expected;
            this.description = description;
        }
    }

    interface Dummy {

        void doSomething();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Context and a BeanDescriptionCache once for the whole suite.
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // The concrete target object can be changed inside each test method.
        // By default we use a simple custom bean (House) that is present in the
        // original Logback test sources.
        targetObject = new House();
        // Create a fresh PropertySetter for the chosen target object.
        setter = new PropertySetter(beanDescriptionCache, targetObject);
        // The PropertySetter extends ContextAwareBase, therefore we set the
        // context to avoid NPEs in other parts of the class (even though the
        // current test only calls getObjClass()).
        setter.setContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear instance fields to avoid accidental cross‑test contamination.
        setter = null;
        targetObject = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references – not strictly required but keeps the test tidy.
        beanDescriptionCache = null;
        context = null;
    }

    /**
     * Supplies a stream of test arguments for the basic functionality group.
     * Each argument consists of:
     *   - the object to be wrapped by {@link PropertySetter}
     *   - the expected {@link Class} result (or {@code null})
     *   - a description used in the assertion message
     */
    private static Stream<ArgumentsHolder> basicFunctionalityProvider() {
        return Stream.of(new ArgumentsHolder("a string", String.class, "When PropertySetter is created with a String instance, getObjClass() should return java.lang.String.class."), new ArgumentsHolder(new MyBean(), MyBean.class, "When PropertySetter is created with an instance of a user‑defined class, getObjClass() must return the exact runtime class."), new ArgumentsHolder(null, null, "If PropertySetter is constructed with a null reference, getObjClass() should return null."));
    }

    private static Stream<ArgumentsHolder> subclassHandlingProvider() {
        return Stream.of(new ArgumentsHolder(new ArrayList<>(), ArrayList.class, "When PropertySetter is created with an ArrayList instance, getObjClass() must return the actual subclass class."));
    }

    private static Stream<ArgumentsHolder> anonymousInnerClassProvider() {
        Object anon = new Object() {
        };
        return Stream.of(new ArgumentsHolder(anon, anon.getClass(), "When PropertySetter is created with an anonymous inner class, getObjClass() should return the runtime class of that anonymous class."));
    }

    private static Stream<ArgumentsHolder> arrayTypeProvider() {
        int[] array = new int[] { 1, 2, 3 };
        return Stream.of(new ArgumentsHolder(array, array.getClass(), "When PropertySetter is created with an int[] array, getObjClass() must return int[].class."));
    }

    private static Stream<ArgumentsHolder> primitiveWrapperProvider() {
        Integer wrapper = Integer.valueOf(5);
        return Stream.of(new ArgumentsHolder(wrapper, Integer.class, "When PropertySetter is created with an Integer wrapper, getObjClass() should return java.lang.Integer.class."));
    }

    private static Stream<ArgumentsHolder> dynamicProxyProvider() {
        // Simple interface for the proxy
        interface Dummy {

            void doSomething();
        }
        // InvocationHandler that does nothing
        InvocationHandler handler = (proxy, method, args) -> null;
        Dummy proxyInstance = (Dummy) Proxy.newProxyInstance(Dummy.class.getClassLoader(), new Class<?>[] { Dummy.class }, handler);
        return Stream.of(new ArgumentsHolder(proxyInstance, Proxy.class, "When PropertySetter is created with a java.lang.reflect.Proxy instance, getObjClass() must return Proxy.class."));
    }

    @Test
    void getObjClassReturnsClassOfProvidedObject() {
        // When the PropertySetter is constructed with a House instance,
        // getObjClass() must return House.class.
        assertEquals(House.class, setter.getObjClass(), "PropertySetter should return the exact runtime class of the supplied object");
    }

    @Test
    void getObjClassMatchesObjectGetClass() {
        // Verify that the value returned by getObjClass() is identical to
        // targetObject.getClass().
        Class<?> expected = targetObject.getClass();
        Class<?> actual = setter.getObjClass();
        assertSame(expected, actual, "The class returned by getObjClass() must be the same instance as targetObject.getClass()");
    }

    @Test
    void getObjClassWorksForDifferentObjectTypes() {
        // Test with a String instance.
        targetObject = "a simple string";
        setter = new PropertySetter(beanDescriptionCache, targetObject);
        setter.setContext(context);
        assertEquals(String.class, setter.getObjClass(), "PropertySetter should correctly handle java.lang.String objects");
        // Test with an Integer instance.
        targetObject = Integer.valueOf(42);
        setter = new PropertySetter(beanDescriptionCache, targetObject);
        setter.setContext(context);
        assertEquals(Integer.class, setter.getObjClass(), "PropertySetter should correctly handle java.lang.Integer objects");
        // Test with a custom bean that has no special annotations.
        class SimpleBean {
        }
        targetObject = new SimpleBean();
        setter = new PropertySetter(beanDescriptionCache, targetObject);
        setter.setContext(context);
        assertEquals(SimpleBean.class, setter.getObjClass(), "PropertySetter should correctly handle user‑defined bean classes");
    }

    @Test
    void getObjClassIsNeverNullWhenConstructedWithNonNullObject() {
        assertNotNull(setter.getObjClass(), "When a non‑null object is supplied to the constructor, getObjClass() must never be null");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicFunctionalityProvider")
    void getObjClassBasicFunctionality(ArgumentsHolder holder) {
        // Re‑create the setter with the supplied target (may be null)
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("subclassHandlingProvider")
    void getObjClassSubclassHandling(ArgumentsHolder holder) {
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("anonymousInnerClassProvider")
    void getObjClassAnonymousInnerClass(ArgumentsHolder holder) {
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("arrayTypeProvider")
    void getObjClassArrayType(ArgumentsHolder holder) {
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("primitiveWrapperProvider")
    void getObjClassPrimitiveWrapper(ArgumentsHolder holder) {
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("dynamicProxyProvider")
    void getObjClassDynamicProxy(ArgumentsHolder holder) {
        setter = new PropertySetter(beanDescriptionCache, holder.target);
        setter.setContext(context);
        assertEquals(holder.expected, setter.getObjClass(), holder.description);
    }

    @Test
    void getObjClassOnNullPropertySetterReferenceThrowsNPE() {
        PropertySetter nullSetter = null;
        assertThrows(NullPointerException.class, nullSetter::getObjClass, "Invoking getObjClass() on a null PropertySetter reference should throw NullPointerException.");
    }
}
