package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getDefaultClassNameByAnnonation_Test {

    private static Context context;

    private static BeanDescriptionCache beanDescriptionCache;

    private PropertySetter setter;

    private BeanWithAnnotatedSetter bean;

    /**
     * Simple component used for the @DefaultClass annotation.
     */
    public static class FooImpl {
    }

    /**
     * Dummy classes referenced by various @DefaultClass annotations.
     */
    public static class SomeClass {
    }

    public static class OtherClass {
    }

    /**
     * Bean that contains a setter method annotated with @DefaultClass(SomeClass).
     */
    public static class BeanWithAnnotatedSetter {

        @DefaultClass(FooImpl.class)
        public void setFoo(FooImpl foo) {
            /* no‑op */
        }

        // a method without the annotation – used for the negative test case
        public void setBar(String bar) {
            /* no‑op */
        }
    }

    /**
     * Collection of static classes used for the edge‑case tests.
     */
    public static class TestClasses {

        public static class AnnotatedClass {

            @DefaultClass(SomeClass.class)
            public void annotatedMethod() {
            }
        }

        public static class NonAnnotatedClass {

            public void nonAnnotatedMethod() {
            }
        }

        public static class AnnotatedClassDifferent {

            @DefaultClass(OtherClass.class)
            public void annotatedMethod() {
            }
        }

        public static class AnnotatedClassWithInterface {

            // interface as annotation value
            @DefaultClass(List.class)
            public void annotatedMethod() {
            }
        }
    }

    public static class AnnotatedClass {

        @DefaultClass(SomeClass.class)
        public void annotatedMethod() {
        }
    }

    public static class NonAnnotatedClass {

        public void nonAnnotatedMethod() {
        }
    }

    public static class AnnotatedClassDifferent {

        @DefaultClass(OtherClass.class)
        public void annotatedMethod() {
        }
    }

    public static class AnnotatedClassWithInterface {

        // interface as annotation value
        @DefaultClass(List.class)
        public void annotatedMethod() {
        }
    }

    public static class AnnotatedClass {

        @DefaultClass(SomeClass.class)
        public void annotatedMethod() {
        }
    }

    public static class NonAnnotatedClass {

        public void nonAnnotatedMethod() {
        }
    }

    public static class AnnotatedClassDifferent {

        @DefaultClass(OtherClass.class)
        public void annotatedMethod() {
        }
    }

    public static class AnnotatedClassWithInterface {

        // interface as annotation value
        @DefaultClass(List.class)
        public void annotatedMethod() {
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a real Context is cheap and does not require any external resources
        context = new ContextBase();
        beanDescriptionCache = new BeanDescriptionCache(context);
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new BeanWithAnnotatedSetter();
        setter = new PropertySetter(beanDescriptionCache, bean);
        // PropertySetter extends ContextAwareBase, so we need to inject the context
        setter.setContext(context);
    }

    @AfterAll
    static void teardownAfterAll() {
        // release static references
        context = null;
        beanDescriptionCache = null;
    }

    /**
     * Supplies the arguments for {@link #edgeCases(String, Method, Class)}.
     */
    private static Stream<Arguments> edgeCaseProvider() throws NoSuchMethodException {
        // Methods used in the test cases
        Method annotatedMethod = TestClasses.AnnotatedClass.class.getMethod("annotatedMethod");
        Method nonAnnotatedMethod = TestClasses.NonAnnotatedClass.class.getMethod("nonAnnotatedMethod");
        Method differentAnnotatedMethod = TestClasses.AnnotatedClassDifferent.class.getMethod("annotatedMethod");
        Method interfaceAnnotatedMethod = TestClasses.AnnotatedClassWithInterface.class.getMethod("annotatedMethod");
        return Stream.of(// name = null, method annotated with SomeClass
        Arguments.of(null, annotatedMethod, SomeClass.class), // name = empty string, method annotated with SomeClass
        Arguments.of("", annotatedMethod, SomeClass.class), // relevantMethod = null → should safely return null
        Arguments.of("anyName", null, null), // Different @DefaultClass value (OtherClass)
        Arguments.of("anyName", differentAnnotatedMethod, OtherClass.class), // Annotation value is an interface (java.util.List)
        Arguments.of("anyName", interfaceAnnotatedMethod, List.class));
    }

    /**
     * Verify that when the relevant setter method is annotated with {@link DefaultClass},
     * {@code getDefaultClassNameByAnnonation} returns the class supplied by the annotation.
     */
    @Test
    void shouldReturnAnnotatedDefaultClass() throws Exception {
        // obtain the method that PropertySetter would consider “relevant”
        Method relevantMethod = setter.getRelevantMethod("foo", AggregationType.AS_COMPLEX_PROPERTY);
        assertNotNull(relevantMethod, "Relevant method for 'foo' should be found");
        Class<?> result = setter.getDefaultClassNameByAnnonation("foo", relevantMethod);
        assertEquals(FooImpl.class, result, "The class returned must match the @DefaultClass value");
    }

    /**
     * Verify that when the relevant setter method has no {@link DefaultClass} annotation,
     * {@code getDefaultClassNameByAnnonation} returns {@code null}.
     */
    @Test
    void shouldReturnNullWhenNoAnnotationPresent() throws Exception {
        Method relevantMethod = setter.getRelevantMethod("bar", AggregationType.AS_BASIC_PROPERTY);
        assertNotNull(relevantMethod, "Relevant method for 'bar' should be found");
        Class<?> result = setter.getDefaultClassNameByAnnonation("bar", relevantMethod);
        assertNull(result, "No @DefaultClass annotation → result must be null");
    }

    /**
     * Parameterised test that covers all edge‑case scenarios described in the
     * supplied JSON.  The method under test does not use the {@code name}
     * argument, so variations of {@code null} and empty strings are exercised
     * to prove that they are ignored.
     */
    @ParameterizedTest(name = "{index}: name={0}, method={1}, expected={2}")
    @MethodSource("edgeCaseProvider")
    void edgeCases(String name, Method relevantMethod, Class<?> expected) {
        // Use a generic bean – the method under test only inspects the Method argument.
        PropertySetter genericSetter = new PropertySetter(beanDescriptionCache, new Object());
        genericSetter.setContext(context);
        Class<?> actual = genericSetter.getDefaultClassNameByAnnonation(name, relevantMethod);
        assertEquals(expected, actual, () -> String.format("For name='%s' and method=%s expected %s but got %s", name, relevantMethod == null ? "null" : relevantMethod.getName(), expected, actual));
    }
}
