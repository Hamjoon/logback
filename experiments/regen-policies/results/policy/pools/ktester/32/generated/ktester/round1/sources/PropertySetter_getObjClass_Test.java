package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getObjClass_Test {

    /**
     * Shared context for all tests – it does not hold any state that needs cleanup.
     */
    private static Context context;

    /**
     * The cache used by {@link PropertySetter}. It is created once per test class.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Instance under test – recreated before each test method.
     */
    private PropertySetter setter;

    /**
     * The object whose class we expect {@link PropertySetter#getObjClass()} to return.
     */
    private DummyBean dummyBean;

    /**
     * Simple POJO used only for the purpose of this test.
     */
    static class DummyBean {
        // no members required
    }

    /**
     * User‑defined bean for the “custom class” test case.
     */
    static class CustomBean {
        // no members required
    }

    /**
     * Simple interface used to create a dynamic proxy instance.
     */
    interface SimpleService {

        void doSomething();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Context that can be reused across all tests.
        context = new ContextBase();
        // Create the BeanDescriptionCache that will be shared. It only needs the Context.
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh DummyBean and a fresh PropertySetter for each test.
        dummyBean = new DummyBean();
        setter = new PropertySetter(beanDescriptionCache, dummyBean);
        // The PropertySetter needs a Context to be able to log status messages.
        setter.setContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to release, but nullify references to help GC and avoid
        // accidental cross‑test interference.
        setter = null;
        dummyBean = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clean up static references.
        beanDescriptionCache = null;
        context = null;
    }

    /**
     * Provides a stream of arguments: target object and the expected class
     * returned by {@link PropertySetter#getObjClass()}.
     *
     * <p>Each entry corresponds to one of the groups described in the JSON test
     * specification.</p>
     */
    private static Stream<Arguments> provideTargets() {
        // normal objects
        String stringTarget = "hello";
        ArrayList<Integer> arrayListTarget = new ArrayList<>();
        // primitive wrappers
        Integer integerTarget = 42;
        Boolean booleanTarget = Boolean.TRUE;
        // custom user‑defined class
        CustomBean customBeanTarget = new CustomBean();
        // array types
        String[] stringArrayTarget = new String[] { "a", "b" };
        int[] intArrayTarget = new int[] { 1, 2, 3 };
        // dynamic proxy
        SimpleService proxyTarget = (SimpleService) Proxy.newProxyInstance(SimpleService.class.getClassLoader(), new Class<?>[] { SimpleService.class }, (proxy, method, args) -> null);
        return Stream.of(// group: getObjClass - normal behavior
        Arguments.of(stringTarget, String.class, "String instance"), Arguments.of(new ArrayList<>(), ArrayList.class, "ArrayList instance"), // group: getObjClass - primitive wrapper type
        Arguments.of(integerTarget, Integer.class, "Integer wrapper"), Arguments.of(booleanTarget, Boolean.class, "Boolean wrapper"), // group: getObjClass - custom user‑defined class
        Arguments.of(customBeanTarget, CustomBean.class, "CustomBean instance"), // group: getObjClass - array types
        Arguments.of(stringArrayTarget, String[].class, "String[] array"), Arguments.of(intArrayTarget, int[].class, "int[] primitive array"), // group: getObjClass - proxy class
        Arguments.of(proxyTarget, proxyTarget.getClass(), "dynamic proxy instance"));
    }

    @Test
    void getObjClassShouldReturnTheClassOfTheWrappedObject() {
        // GIVEN a PropertySetter wrapping a DummyBean (as in the original template)
        DummyBean dummyBean = new DummyBean();
        setter = new PropertySetter(beanDescriptionCache, dummyBean);
        setter.setContext(context);
        // WHEN
        Class<?> result = setter.getObjClass();
        // THEN
        assertNotNull(result, "getObjClass() must not return null");
        assertEquals(DummyBean.class, result, "PropertySetter#getObjClass() should return the exact class of the object supplied to the constructor");
        assertSame(dummyBean.getClass(), result, "The returned class must be the same instance as dummyBean.getClass()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideTargets")
    void getObjClassReturnsExpectedClass(Object target, Class<?> expectedClass, String description) {
        // GIVEN a PropertySetter wrapping the supplied target
        setter = new PropertySetter(beanDescriptionCache, target);
        setter.setContext(context);
        // WHEN
        Class<?> actual = setter.getObjClass();
        // THEN
        assertEquals(expectedClass, actual, () -> "Expected getObjClass() to return " + expectedClass.getName() + " for " + description);
    }

    @Test
    void getObjClassReturnsNullWhenTargetIsNull() {
        // GIVEN a PropertySetter constructed with a null target
        setter = new PropertySetter(beanDescriptionCache, null);
        setter.setContext(context);
        // WHEN
        Class<?> result = setter.getObjClass();
        // THEN
        assertNull(result, "When the target object is null, getObjClass() must return null");
    }

    @Test
    void getObjClassReturnsNullWhenInternalObjClassIsForcedToNull() throws Exception {
        // GIVEN a normal PropertySetter (non‑null target)
        DummyBean dummy = new DummyBean();
        setter = new PropertySetter(beanDescriptionCache, dummy);
        setter.setContext(context);
        // Use reflection to set the final field `objClass` to null.
        Field objClassField = PropertySetter.class.getDeclaredField("objClass");
        objClassField.setAccessible(true);
        // Remove final modifier (required for Java 12+; works for testing purposes)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(objClassField, objClassField.getModifiers() & ~Modifier.FINAL);
        objClassField.set(setter, null);
        // WHEN
        Class<?> result = setter.getObjClass();
        // THEN
        assertNull(result, "After forcing objClass to null via reflection, getObjClass() should return null");
    }
}
