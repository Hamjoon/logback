package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getObjClass_Test {

    /**
     * Shared context and cache for all tests
     */
    private static Context context;

    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Instance under test – created fresh for each test method
     */
    private PropertySetter setter;

    /* --------------------------------------------------------------------- */
    static class SimpleBean {
        // no‑op – only the class type matters for the test
    }

    static class DerivedBean extends SimpleBean {
        // no‑op – only the class type matters for the test
    }

    /**
     * A custom class extending a generic collection – used in one test case
     */
    static class MyStringList extends ArrayList<String> {
        // no‑op – the existence of the class is what matters
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Context (no external resources are required)
        context = new ContextBase();
        // BeanDescriptionCache needs a Context; it does not hit any I/O
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh PropertySetter for a SimpleBean instance before each test
        setter = new PropertySetter(beanDescriptionCache, new SimpleBean());
        // The PropertySetter itself is a ContextAwareBase, so we give it the context
        setter.setContext(context);
    }

    /**
     * Supplies all test cases described in the JSON specification.
     *
     * <p>Each {@link Arguments} consists of:
     * <ol>
     *   <li>a human‑readable description (used as the test name)</li>
     *   <li>the object to be wrapped by {@link PropertySetter}</li>
     *   <li>the expected {@link Class} returned by {@code getObjClass()}</li>
     * </ol>
     */
    private static Stream<Arguments> provideObjectsForGetObjClass() {
        // Basic behaviour
        Object arrayList = new ArrayList<>();
        Object hashMap = new HashMap<String, Integer>();
        Object nullObj = null;
        // Array types
        int[] intArray = new int[] { 1, 2, 3 };
        String[][] stringMatrix = new String[][] { { "a", "b" }, { "c" } };
        // Enum and special objects
        DayOfWeek enumConstant = DayOfWeek.MONDAY;
        AtomicInteger atomicInt = new AtomicInteger(5);
        // Inheritance and anonymous class
        LinkedList<?> linkedList = new LinkedList<>();
        Object anonymous = new Object() {
        };
        // Proxy instance (implements List)
        Object proxy = Proxy.newProxyInstance(PropertySetter.class.getClassLoader(), new Class[] { List.class }, (proxyObj, method, args) -> null);
        // Custom generic subclass
        MyStringList myStringList = new MyStringList();
        return Stream.of(// ---- basic behaviour -------------------------------------------------
        Arguments.of("Standard object (ArrayList) – should return ArrayList.class", arrayList, ArrayList.class), Arguments.of("Generic collection (HashMap) – should return HashMap.class", hashMap, HashMap.class), Arguments.of("Null reference – should return null without throwing", nullObj, null), // ---- array types -----------------------------------------------------
        Arguments.of("Primitive array (int[]) – should return int[].class", intArray, int[].class), Arguments.of("Multi‑dimensional object array (String[][]) – should return String[][].class", stringMatrix, String[][].class), // ---- enum and special objects ----------------------------------------
        Arguments.of("Enum constant (DayOfWeek.MONDAY) – should return DayOfWeek.class", enumConstant, DayOfWeek.class), Arguments.of("AtomicInteger – should return AtomicInteger.class", atomicInt, AtomicInteger.class), // ---- inheritance and anonymous classes --------------------------------
        Arguments.of("LinkedList instance – should return LinkedList.class", linkedList, LinkedList.class), Arguments.of("Anonymous subclass of Object – should return its synthetic class", anonymous, anonymous.getClass()), // ---- proxy instance ---------------------------------------------------
        Arguments.of("Dynamic proxy implementing List – should return Proxy.class", proxy, Proxy.class), // ---- custom class with generic supertype --------------------------------
        Arguments.of("Custom subclass MyStringList – should return MyStringList.class", myStringList, MyStringList.class));
    }

    @Test
    void getObjClass_returnsClassOfObjectProvidedInConstructor() {
        // The object supplied in setUp() is a SimpleBean
        assertEquals(SimpleBean.class, setter.getObjClass(), "getObjClass() should return the exact runtime class of the wrapped object");
        // Verify consistency with the public getObj() accessor
        assertEquals(setter.getObj().getClass(), setter.getObjClass(), "Returned Class should match the class of the object returned by getObj()");
    }

    @Test
    void getObjClass_correctlyHandlesSubclassInstances() {
        // Use a different instance (DerivedBean) to ensure the subclass type is reported
        PropertySetter subclassSetter = new PropertySetter(beanDescriptionCache, new DerivedBean());
        subclassSetter.setContext(context);
        assertEquals(DerivedBean.class, subclassSetter.getObjClass(), "For a subclass instance, getObjClass() must return the subclass type, not the supertype");
        assertEquals(subclassSetter.getObj().getClass(), subclassSetter.getObjClass(), "Returned Class should match the class of the object returned by getObj() for subclass");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideObjectsForGetObjClass")
    void getObjClass_returnsExpectedClass(String description, Object obj, Class<?> expected) {
        // Create a fresh PropertySetter for the supplied object
        PropertySetter ps = new PropertySetter(beanDescriptionCache, obj);
        ps.setContext(context);
        // The method under test
        Class<?> actual = ps.getObjClass();
        assertEquals(expected, actual, description);
    }
}
