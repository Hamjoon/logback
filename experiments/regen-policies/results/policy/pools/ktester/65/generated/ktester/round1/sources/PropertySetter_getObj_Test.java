package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class PropertySetter_getObj_Test {

    /**
     * Shared {@link Context} for the whole test suite.
     */
    private static Context staticContext;

    /**
     * Instance under test.
     */
    private PropertySetter setter;

    /**
     * The concrete object supplied to the {@link PropertySetter} constructor.
     */
    private House house;

    /**
     * Simple interface used for Mockito mock and dynamic proxy tests.
     */
    interface DummyService {

        void doWork();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a real ContextBase is cheap and sufficient for the cache
        staticContext = new ContextBase();
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh objects for each test to avoid cross‑test interference
        house = new House();
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        setter = new PropertySetter(cache, house);
        setter.setContext(staticContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references – not strictly required but makes the intent clear
        setter = null;
        house = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        staticContext = null;
    }

    /**
     * Parameter source for immutable wrapper tests.
     */
    private static Stream<Object> immutableWrapperProvider() {
        return Stream.of(Integer.valueOf(42), Long.valueOf(123L), Boolean.TRUE, "immutable-string");
    }

    @Test
    void getObjReturnsTheObjectPassedToConstructor() {
        // the getter must return the exact same instance (reference equality)
        assertSame(house, setter.getObj(), "PropertySetter#getObj() should return the object supplied to the constructor");
    }

    @Test
    void getObjReturnsNullWhenConstructedWithNull() {
        // construct a setter with a null target object
        PropertySetter nullSetter = new PropertySetter(new BeanDescriptionCache(staticContext), null);
        nullSetter.setContext(staticContext);
        assertNull(nullSetter.getObj(), "PropertySetter#getObj() should return null when the constructor argument is null");
    }

    @ParameterizedTest(name = "getObj() returns the same immutable wrapper instance: {0}")
    @MethodSource("immutableWrapperProvider")
    void getObjReturnsSameImmutableWrapperInstance(Object immutable) {
        // Build a PropertySetter with the immutable object and verify reference equality.
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter ps = new PropertySetter(cache, immutable);
        ps.setContext(staticContext);
        assertSame(immutable, ps.getObj(), "The getter must return the exact same immutable instance that was supplied");
    }

    @Test
    void getObjReturnsSameMutableStringBuilderInstance() {
        // Create a mutable StringBuilder, pass it to the setter, mutate it afterwards,
        // and verify that the getter returns the very same instance with updated state.
        StringBuilder sb = new StringBuilder("initial");
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter ps = new PropertySetter(cache, sb);
        ps.setContext(staticContext);
        // Mutate after construction
        sb.append("-appended");
        assertSame(sb, ps.getObj(), "The getter must return the original mutable StringBuilder instance");
        assertEquals("initial-appended", ((StringBuilder) ps.getObj()).toString(), "The returned StringBuilder should reflect the mutation performed after construction");
    }

    @Test
    void getObjReturnsSameMockitoMockInstance() {
        // Create a Mockito mock, hand it to PropertySetter and verify identity.
        DummyService mock = Mockito.mock(DummyService.class);
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter ps = new PropertySetter(cache, mock);
        ps.setContext(staticContext);
        assertSame(mock, ps.getObj(), "PropertySetter#getObj() should return the exact mock instance supplied to the constructor");
    }

    @Test
    void getObjReturnsSameDynamicProxyInstance() {
        // Build a java.lang.reflect.Proxy implementing DummyService.
        InvocationHandler handler = (proxy, method, args) -> null;
        DummyService proxyInstance = (DummyService) Proxy.newProxyInstance(DummyService.class.getClassLoader(), new Class[] { DummyService.class }, handler);
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter ps = new PropertySetter(cache, proxyInstance);
        ps.setContext(staticContext);
        assertSame(proxyInstance, ps.getObj(), "PropertySetter#getObj() should return the exact proxy instance supplied to the constructor");
    }

    @Test
    void getObjReturnsSameInstanceInAnonymousSubclass() {
        // Use an anonymous subclass of PropertySetter; the target object is a plain Object.
        Object target = new Object();
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter anonymousSetter = new PropertySetter(cache, target) {
        };
        anonymousSetter.setContext(staticContext);
        assertSame(target, anonymousSetter.getObj(), "Even in an anonymous subclass, getObj() must return the exact target reference");
    }

    @Test
    void multipleSettersShareSameTargetObject() {
        // One mutable List is shared between two distinct PropertySetter instances.
        List<String> sharedList = new ArrayList<>();
        sharedList.add("first");
        BeanDescriptionCache cache = new BeanDescriptionCache(staticContext);
        PropertySetter setterA = new PropertySetter(cache, sharedList);
        PropertySetter setterB = new PropertySetter(cache, sharedList);
        setterA.setContext(staticContext);
        setterB.setContext(staticContext);
        assertSame(sharedList, setterA.getObj(), "First setter should return the shared list instance");
        assertSame(sharedList, setterB.getObj(), "Second setter should also return the same shared list instance");
        // Mutate through one reference and verify the change is visible via the other.
        sharedList.add("second");
        assertEquals(2, ((List<?>) setterA.getObj()).size(), "Mutation via the shared object must be reflected when accessed through either setter");
        assertEquals(2, ((List<?>) setterB.getObj()).size(), "Mutation via the shared object must be reflected when accessed through either setter");
    }
}
