package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import static org.mockito.junit.jupiter.MockitoExtension.*;

// Mockito is not strictly required here but kept for consistency
@ExtendWith(MockitoExtension.class)
class PropertySetter_getObj_Test {

    /**
     * Shared test context for all test methods.
     */
    private static Context sharedContext;

    /**
     * Shared cache used by the {@link PropertySetter} under test.
     */
    private static BeanDescriptionCache sharedCache;

    /**
     * Instance under test – created fresh for each test method.
     */
    private PropertySetter setter;

    /**
     * Simple POJO used as the target object for the {@link PropertySetter}.
     */
    private House house;

    class DummyBean {

        private String size;

        private String capacity;

        public void setSize(String size) {
            this.size = size;
        }

        public void setCapacity(String capacity) {
            this.capacity = capacity;
        }
    }

    class ComplexBean {

        private String someProp;

        public void setSomeProp(String value) {
            this.someProp = value;
        }
    }

    /**
     * Simple POJO used by the default test fixture. It does not need any
     * properties for the {@code getObj()} tests but is kept to reflect the
     * original test suite's intent.
     */
    class House {
        // no fields required for the current tests
    }

    class DummyBean {

        private String size;

        private String capacity;

        public void setSize(String size) {
            this.size = size;
        }

        public void setCapacity(String capacity) {
            this.capacity = capacity;
        }
    }

    class ComplexBean {

        private String someProp;

        public void setSomeProp(String value) {
            this.someProp = value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Context once for the whole test suite.
        sharedContext = new ContextBase();
        // Initialise the BeanDescriptionCache that will be reused.
        sharedCache = new BeanDescriptionCache(sharedContext);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh target object for every test to avoid cross‑test contamination.
        house = new House();
        // Construct the PropertySetter with the shared cache and the fresh target.
        setter = new PropertySetter(sharedCache, house);
        // The PropertySetter needs a Context to be able to log status messages.
        setter.setContext(sharedContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nullify references – helps the garbage collector and makes the test intent clear.
        setter = null;
        house = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources.
        sharedCache = null;
        sharedContext = null;
    }

    private static Stream<Arguments> provideVariousObjects() {
        // Integer (boxed primitive)
        Integer boxedInt = Integer.valueOf(42);
        // int array
        int[] intArray = new int[] { 1, 2, 3 };
        // HashMap with a single entry
        HashMap<String, String> map = new HashMap<>();
        map.put("key", "value");
        // BeanDescriptionCache (framework specific)
        // reuse the shared instance
        BeanDescriptionCache cache = sharedCache;
        return Stream.of(Arguments.of(boxedInt), Arguments.of(intArray), Arguments.of(map), Arguments.of(cache));
    }

    @Test
    void getObj_shouldReturnObjectPassedToConstructor() {
        // GIVEN – the setter has been created in @BeforeEach with 'house' as the target.
        // WHEN
        Object returned = setter.getObj();
        // THEN
        assertNotNull(returned, "getObj() must not return null");
        assertSame(house, returned, "getObj() must return the exact instance supplied to the constructor");
    }

    @Test
    void getObj_shouldReturnSameInstanceAcrossMultipleCalls() {
        // GIVEN – a setter already created.
        // WHEN
        Object firstCall = setter.getObj();
        Object secondCall = setter.getObj();
        // THEN
        assertSame(firstCall, secondCall, "Multiple invocations of getObj() must return the same instance");
    }

    @Test
    @DisplayName("getObj() returns null when constructed with a null reference")
    void getObj_returnsNullWhenConstructedWithNull() {
        // GIVEN a PropertySetter built with a null target
        PropertySetter nullSetter = new PropertySetter(sharedCache, null);
        nullSetter.setContext(sharedContext);
        // WHEN
        Object result = nullSetter.getObj();
        // THEN
        assertNull(result, "getObj() must return null when the constructor argument is null");
    }

    @Test
    @DisplayName("Mutable object mutation is reflected by getObj()")
    void getObj_reflectsMutationOfMutableObject() {
        // GIVEN a mutable object (StringBuilder) supplied to the setter
        StringBuilder sb = new StringBuilder("init");
        PropertySetter mutableSetter = new PropertySetter(sharedCache, sb);
        mutableSetter.setContext(sharedContext);
        // WHEN the external code mutates the object
        sb.append("changed");
        // THEN getObj() returns the same instance and sees the mutated state
        Object returned = mutableSetter.getObj();
        assertSame(sb, returned, "Reference must stay identical");
        assertEquals("initchanged", ((StringBuilder) returned).toString(), "The mutated state must be visible through getObj()");
    }

    @Test
    @DisplayName("getObj() unchanged after addBasicProperty / setProperty calls")
    void getObj_staysSameAfterBasicPropertyCalls() throws Exception {
        // A dummy bean with a matching setter for "size" and "capacity"
        class DummyBean {

            private String size;

            private String capacity;

            public void setSize(String size) {
                this.size = size;
            }

            public void setCapacity(String capacity) {
                this.capacity = capacity;
            }
        }
        DummyBean bean = new DummyBean();
        PropertySetter ps = new PropertySetter(sharedCache, bean);
        ps.setContext(sharedContext);
        // invoke the two public methods that manipulate properties
        ps.addBasicProperty("size", "10");
        ps.setProperty("capacity", "20");
        // getObj() must still be the original bean instance
        assertSame(bean, ps.getObj(), "After invoking other setters, getObj() must still return the original reference");
    }

    @Test
    @DisplayName("getObj() unchanged after setComplexProperty call")
    void getObj_staysSameAfterComplexPropertyCall() throws Exception {
        // Bean with a setter matching the complex property name
        class ComplexBean {

            private String someProp;

            public void setSomeProp(String value) {
                this.someProp = value;
            }
        }
        ComplexBean bean = new ComplexBean();
        PropertySetter ps = new PropertySetter(sharedCache, bean);
        ps.setContext(sharedContext);
        // invoke the complex property setter
        ps.setComplexProperty("someProp", "value");
        // verify reference equality
        assertSame(bean, ps.getObj(), "Complex property manipulation must not alter the stored object reference");
    }

    @ParameterizedTest(name = "{index}: obj={0}")
    @MethodSource("provideVariousObjects")
    @DisplayName("getObj() returns the exact object supplied at construction")
    void getObj_returnsExactSuppliedInstance(Object supplied) {
        PropertySetter ps = new PropertySetter(sharedCache, supplied);
        ps.setContext(sharedContext);
        assertSame(supplied, ps.getObj(), "The getter must return the exact instance that was given to the constructor");
    }

    @Test
    @DisplayName("Concurrent reads of getObj() all see the same reference")
    void getObj_isThreadSafeForReadOnlyAccess() throws Exception {
        // GIVEN a PropertySetter wrapping an AtomicReference
        AtomicReference<String> atomicRef = new AtomicReference<>("initial");
        PropertySetter ps = new PropertySetter(sharedCache, atomicRef);
        ps.setContext(sharedContext);
        // WHEN multiple threads concurrently call getObj()
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> ps.getObj()));
        }
        // THEN each future must contain the same AtomicReference instance
        // may throw ExecutionException/InterruptedException
        Object first = futures.get(0).get();
        for (Future<Object> f : futures) {
            assertSame(first, f.get(), "All threads must observe the identical object reference from getObj()");
        }
        executor.shutdownNow();
    }
}
