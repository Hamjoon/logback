package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getByConcreteType_Test {

    /**
     * Shared context used to create {@link BeanDescriptionCache}.
     */
    private static Context context;

    /**
     * Shared cache – created once for the whole test suite.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * Instance under test – recreated before each test method.
     */
    private PropertySetter setter;

    /**
     * Dummy target object – the real object is irrelevant for the tests.
     */
    private DummyTarget dummyTarget;

    /**
     * Simple concrete class with a public no‑arg constructor – used to test the
     * “instantiable” path.
     */
    public static class SimpleConcrete {

        public SimpleConcrete() {
        }
    }

    /**
     * Abstract base class – used to test the “non‑instantiable” path.
     */
    public static abstract class AbstractBase {
    }

    /**
     * Dummy target object that declares a handful of setter‑like methods with
     * different parameter signatures. The methods themselves are never invoked;
     * they merely exist so that we can obtain {@link Method} objects for testing.
     */
    public static class DummyTarget {

        public void setDate(Date d) {
            // no‑op
        }

        public void setList(List<?> l) {
            // no‑op
        }

        public void setInt(int i) {
            // no‑op
        }

        public void setStringArray(String[] arr) {
            // no‑op
        }

        public void setAbstractList(AbstractList<?> al) {
            // no‑op
        }

        public void setFile(File f) {
            // no‑op
        }

        public void setCollections(Collections c) {
            // no‑op
        }

        public void setDayOfWeek(DayOfWeek d) {
            // no‑op
        }

        public void setStringBuilder(StringBuilder sb) {
            // no‑op
        }

        public void setTwoParams(String s, Integer i) {
            // no‑op
        }

        public void noParamMethod() {
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
        dummyTarget = new DummyTarget();
        // The real constructor of PropertySetter takes a BeanDescriptionCache and the target object.
        setter = new PropertySetter(beanDescriptionCache, dummyTarget);
        setter.setContext(context);
    }

    /**
     * Provides a stream of arguments for the {@link #testGetByConcreteTypeVarious}
     * parameterised test. Each argument consists of:
     * <ul>
     *   <li>human‑readable description (used as the test name)</li>
     *   <li>the {@link Method} to be passed to {@code getByConcreteType}</li>
     *   <li>the expected {@link Class} result (or {@code null})</li>
     * </ul>
     */
    static Stream<Arguments> provideMethodsForConcreteType() throws NoSuchMethodException {
        return Stream.of(// no‑parameter method → null
        Arguments.of("no‑parameter method", DummyTarget.class.getMethod("noParamMethod"), null), // java.util.Date – concrete class with public no‑arg ctor → Date.class
        Arguments.of("java.util.Date (concrete, public no‑arg ctor)", DummyTarget.class.getMethod("setDate", Date.class), Date.class), // java.util.List – interface → null
        Arguments.of("java.util.List (interface)", DummyTarget.class.getMethod("setList", List.class), null), // primitive int → null
        Arguments.of("primitive int", DummyTarget.class.getMethod("setInt", int.class), null), // String[] (array) → null
        Arguments.of("String[] (array)", DummyTarget.class.getMethod("setStringArray", String[].class), null), // java.util.AbstractList – abstract class → null
        Arguments.of("java.util.AbstractList (abstract class)", DummyTarget.class.getMethod("setAbstractList", AbstractList.class), null), // java.io.File – concrete but no public no‑arg ctor → null
        Arguments.of("java.io.File (no public no‑arg ctor)", DummyTarget.class.getMethod("setFile", File.class), null), // java.util.Collections – utility class with private ctor → null
        Arguments.of("java.util.Collections (private ctor)", DummyTarget.class.getMethod("setCollections", Collections.class), null), // java.time.DayOfWeek – enum → null
        Arguments.of("java.time.DayOfWeek (enum)", DummyTarget.class.getMethod("setDayOfWeek", DayOfWeek.class), null), // java.lang.StringBuilder – concrete with public no‑arg ctor → StringBuilder.class
        Arguments.of("java.lang.StringBuilder (concrete, public no‑arg ctor)", DummyTarget.class.getMethod("setStringBuilder", StringBuilder.class), StringBuilder.class), // null Method argument → null (behaviour defined by the first group of test cases)
        Arguments.of("null Method argument", null, null));
    }

    /**
     * Verify that a concrete class with a public no‑arg constructor is considered
     * unequivocally instantiable and is returned by {@code getByConcreteType}.
     */
    @Test
    void concreteInstantiableParameter() throws Exception {
        Method method = DummyTarget.class.getMethod("setConcrete", SimpleConcrete.class);
        Class<?> result = setter.getByConcreteType("concrete", method);
        assertNotNull(result, "Result should not be null for a concrete, instantiable type");
        assertEquals(SimpleConcrete.class, result, "Returned class should be the concrete parameter type");
    }

    /**
     * Verify that an interface parameter leads to a {@code null} result.
     */
    @Test
    void interfaceParameter() throws Exception {
        Method method = DummyTarget.class.getMethod("setInterface", java.util.List.class);
        Class<?> result = setter.getByConcreteType("list", method);
        assertNull(result, "Result should be null for an interface parameter");
    }

    /**
     * Verify that an abstract class parameter leads to a {@code null} result.
     */
    @Test
    void abstractClassParameter() throws Exception {
        Method method = DummyTarget.class.getMethod("setAbstract", AbstractBase.class);
        Class<?> result = setter.getByConcreteType("abstract", method);
        assertNull(result, "Result should be null for an abstract class parameter");
    }

    /**
     * Verify that a method without parameters yields {@code null}.
     */
    @Test
    void methodWithoutParameters() throws Exception {
        Method method = DummyTarget.class.getMethod("noParamMethod");
        Class<?> result = setter.getByConcreteType("noparam", method);
        assertNull(result, "Result should be null when the method has no parameters");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideMethodsForConcreteType")
    void testGetByConcreteTypeVarious(String description, Method method, Class<?> expected) {
        // Act
        Class<?> result = setter.getByConcreteType("anyName", method);
        // Assert
        if (expected == null) {
            assertNull(result, description + " – expected null");
        } else {
            assertNotNull(result, description + " – expected non‑null");
            assertEquals(expected, result, description + " – expected class mismatch");
        }
    }

    @Test
    @DisplayName("Passing a null Method should throw NullPointerException")
    void getByConcreteType_nullMethod_throwsNPE() {
        assertThrows(NullPointerException.class, () -> setter.getByConcreteType("anyName", null), "Expected NullPointerException when Method argument is null");
    }

    @Test
    @DisplayName("Method with more than one parameter should cause IllegalArgumentException")
    void getByConcreteType_twoParameters_throwsIAE() throws NoSuchMethodException {
        Method twoParamMethod = DummyTarget.class.getMethod("setTwoParams", String.class, Integer.class);
        assertThrows(IllegalArgumentException.class, () -> setter.getByConcreteType("anyName", twoParamMethod), "Expected IllegalArgumentException for a method with > 1 parameter");
    }
}
