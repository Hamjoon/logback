package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.annotation.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getAnnotation_Test {

    /**
     * Mocked cache – the real cache is not needed for the getAnnotation tests.
     */
    @Mock
    BeanDescriptionCache beanDescriptionCache;

    /**
     * Mocked bean description – returned by the mocked cache.
     */
    @Mock
    BeanDescription beanDescription;

    /**
     * Instance of the class under test.
     */
    PropertySetter setter;

    /**
     * Dummy component class used in the {@link DefaultClass} annotation.
     */
    static class DummyComponent {
    }

    /**
     * Helper class containing a method annotated with {@link DefaultClass}.
     */
    static class TestBean {

        @DefaultClass(DummyComponent.class)
        public void annotatedMethod() {
            // no-op
        }

        public void nonAnnotatedMethod() {
            // no-op
        }
    }

    /**
     * Class that contains methods annotated with the sample annotations.
     */
    static class SampleClass {

        @SampleAnnotation
        public void annotatedMethod() {
            // no‑op
        }

        public void nonAnnotatedMethod() {
            // no‑op
        }

        @SampleAnnotation
        @AnotherAnnotation
        public void multiAnnotatedMethod() {
            // no‑op
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The PropertySetter constructor calls beanDescriptionCache.getBeanDescription(...).
        // Stub that call to return a harmless mock.
        when(beanDescriptionCache.getBeanDescription(any(Class.class))).thenReturn(beanDescription);
        // Use a simple object as the target – the actual type is irrelevant for getAnnotation.
        setter = new PropertySetter(beanDescriptionCache, new TestBean());
    }

    @Test
    void getAnnotation_returnsAnnotationWhenMethodIsAnnotated() throws NoSuchMethodException {
        // Arrange: obtain the method that carries the @DefaultClass annotation.
        Method method = TestBean.class.getMethod("annotatedMethod");
        // Act: invoke the package‑private getAnnotation method.
        DefaultClass annotation = setter.getAnnotation("unusedName", DefaultClass.class, method);
        // Assert: the annotation is present and its value points to DummyComponent.
        assertNotNull(annotation, "Annotation should not be null for an annotated method");
        assertEquals(DummyComponent.class, annotation.value(), "Annotation value should match the class specified in @DefaultClass");
    }

    @Test
    void getAnnotation_returnsNullWhenMethodIsNotAnnotated() throws NoSuchMethodException {
        // Arrange: obtain a method that has no @DefaultClass annotation.
        Method method = TestBean.class.getMethod("nonAnnotatedMethod");
        // Act: ask for a DefaultClass annotation – it should be absent.
        DefaultClass annotation = setter.getAnnotation("unusedName", DefaultClass.class, method);
        // Assert: null is returned because the method lacks the requested annotation.
        assertNull(annotation, "Annotation should be null when the method is not annotated");
    }

    @Test
    void getAnnotation_returnsNullWhenRelevantMethodIsNull() {
        // Act: call the method with a null Method reference.
        DefaultClass annotation = setter.getAnnotation("anyName", DefaultClass.class, null);
        // Assert: the contract specifies a null return in this case.
        assertNull(annotation, "Annotation should be null when relevantMethod is null");
    }

    @Test
    void getAnnotation_nullAnnotationClass_throwsNullPointerException() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("annotatedMethod");
        // The contract of Method#getAnnotation(null) is to throw NPE.
        assertThrows(NullPointerException.class, () -> setter.getAnnotation("anyName", null, method), "Calling getAnnotation with a null annotation class should propagate NullPointerException");
    }

    @Test
    void getAnnotation_nonAnnotationClass_throwsIllegalArgumentException() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("annotatedMethod");
        // Passing a non‑annotation type (String.class) causes IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> setter.getAnnotation("anyName", String.class, method), "Calling getAnnotation with a non‑annotation class should propagate IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] name=''{0}'' → annotation present")
    @CsvSource({ "'firstName'", "'differentName'" })
    void getAnnotation_nameParameterIsIgnored(String name) throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("annotatedMethod");
        SampleAnnotation annotation = setter.getAnnotation(name, SampleAnnotation.class, method);
        assertNotNull(annotation, "Annotation should be returned regardless of the 'name' argument");
    }

    @Test
    void getAnnotation_multipleAnnotationsRetrieveSpecificOne() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("multiAnnotatedMethod");
        AnotherAnnotation another = setter.getAnnotation("anyName", AnotherAnnotation.class, method);
        SampleAnnotation sample = setter.getAnnotation("anyName", SampleAnnotation.class, method);
        assertNotNull(another, "Should retrieve the @AnotherAnnotation instance");
        assertNotNull(sample, "Should retrieve the @SampleAnnotation instance");
        // Ensure the two annotations are distinct objects of different types.
        assertTrue(another.annotationType() == AnotherAnnotation.class);
        assertTrue(sample.annotationType() == SampleAnnotation.class);
    }

    @Test
    void getAnnotation_annotationAbsentOnMethodReturnsNull() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("nonAnnotatedMethod");
        SampleAnnotation annotation = setter.getAnnotation("anyName", SampleAnnotation.class, method);
        assertNull(annotation, "When the method lacks the requested annotation, null must be returned");
    }
}
