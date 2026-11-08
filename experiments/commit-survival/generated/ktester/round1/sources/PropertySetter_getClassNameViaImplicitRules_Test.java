package ch.qos.logback.core.joran.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getClassNameViaImplicitRules_Test {

    /**
     * A minimal Logback context – required by {@link BeanDescriptionCache}.
     */
    private static final ContextBase CONTEXT = new ContextBase();

    /**
     * Real cache used by the {@link PropertySetter} under test.
     */
    private static BeanDescriptionCache beanDescriptionCache;

    /**
     * The bean on which the {@link PropertySetter} will operate.
     */
    private TestBean testBean;

    /**
     * The {@link PropertySetter} instance under test.
     */
    private PropertySetter setter;

    /**
     * Mocked registry – allows us to control the first decision point.
     */
    @Mock
    private DefaultNestedComponentRegistry registryMock;

    /**
     * Simple interface – used as the parameter type for the annotated setter.
     */
    public interface AnnotatedComponent {
    }

    /**
     * Implementation that the @DefaultClass annotation points to.
     */
    public static class AnnotatedComponentImpl implements AnnotatedComponent {
    }

    /**
     * Simple interface – used as the parameter type for the plain setter.
     */
    public interface PlainComponent {
    }

    /**
     * Concrete class used by the registry‑driven test.
     */
    public static class ConcreteComponent {
    }

    /**
     * Bean containing the various setter methods used by the tests.
     */
    public static class TestBean {

        // ---------- 1. Registry‑driven case (no setter needed) ----------
        // No method required – the registry short‑circuits the logic.
        // ---------- 2. Annotation‑driven case ----------
        @ch.qos.logback.core.joran.spi.DefaultClass(AnnotatedComponentImpl.class)
        public void setAnnotated(AnnotatedComponent component) {
        }

        // ---------- 3. Concrete‑type fallback case ----------
        public void setPlain(PlainComponent component) {
        }

        // ---------- 4. Encoder – annotated with @DefaultClass ----------
        @ch.qos.logback.core.joran.spi.DefaultClass(DefaultEncoder.class)
        public void setEncoder(Encoder encoder) {
        }

        // ---------- 5. Layout – no annotation, concrete type ----------
        public void setLayout(PatternLayout layout) {
        }

        // ---------- 6. Filter – two overloads to simulate aggregation influence ----------
        // AS_ATTRIBUTE → annotated method
        @ch.qos.logback.core.joran.spi.DefaultClass(Filter.class)
        public void setFilter(Filter filter) {
        }

        // AS_ELEMENT → non‑annotated method, concrete type DefaultFilter
        public void setFilter(DefaultFilter filter) {
        }

        // ---------- 7. Property with no resolvable concrete type ----------
        public void setUnknown(Object unknown) {
        }
    }

    public interface Encoder {
    }

    public static class DefaultEncoder implements Encoder {
    }

    public interface PatternLayout {
    }

    public interface Filter {
    }

    public static class DefaultFilter implements Filter {
    }

    public static class Foo {
    }

    public static class Bar {
    }

    @BeforeAll
    static void setupBeforeAll() {
        beanDescriptionCache = new BeanDescriptionCache(CONTEXT);
    }

    @BeforeEach
    void setupBeforeEach() {
        testBean = new TestBean();
        setter = new PropertySetter(beanDescriptionCache, testBean);
        // PropertySetter extends ContextAwareBase – we need a context for status handling
        setter.setContext(CONTEXT);
    }

    /**
     * Provides arguments for the “registry provides component type” group.
     */
    static Stream<Arguments> registryProvidesComponentType() {
        return Stream.of(Arguments.of("appender", AggregationType.AS_ELEMENT, Foo.class, "mockRegistryReturningFoo"));
    }

    /**
     * Provides arguments for the “annotation provides default class” group.
     */
    static Stream<Arguments> annotationProvidesDefaultClass() {
        return Stream.of(Arguments.of("encoder", AggregationType.AS_ELEMENT, DefaultEncoder.class, "mockEmptyRegistry"));
    }

    /**
     * Provides arguments for the “fallback to concrete type” group.
     */
    static Stream<Arguments> fallbackToConcreteType() {
        return Stream.of(Arguments.of("layout", AggregationType.AS_ELEMENT, PatternLayout.class, "mockEmptyRegistry"));
    }

    /**
     * Provides arguments for the “aggregation type influences method selection” group.
     */
    static Stream<Arguments> aggregationInfluence() {
        return Stream.of(// AS_ATTRIBUTE → annotated method → returns Filter.class
        Arguments.of("filter", AggregationType.AS_ATTRIBUTE, Filter.class, "mockEmptyRegistry"), // AS_ELEMENT → non‑annotated method → concrete type DefaultFilter.class
        Arguments.of("filter", AggregationType.AS_ELEMENT, DefaultFilter.class, "mockEmptyRegistry"));
    }

    /**
     * Provides arguments for the “null arguments handling” group (excluding NPE from registry).
     */
    static Stream<Arguments> nullArguments() {
        return Stream.of(// null name
        Arguments.of(null, AggregationType.AS_ELEMENT, null, "mockEmptyRegistry", null), // null aggregation type
        Arguments.of("appender", null, null, "mockEmptyRegistry", null));
    }

    /**
     * When the {@link DefaultNestedComponentRegistry} returns a non‑null class,
     * {@code getClassNameViaImplicitRules} must return that class immediately,
     * without consulting the bean’s methods or annotations.
     */
    @Test
    void registryProvidesResult() {
        // Arrange – the registry should return a concrete class for the property name
        when(registryMock.findDefaultComponentType(TestBean.class, "myComponent")).thenReturn(ConcreteComponent.class);
        // Act
        Class<?> result = setter.getClassNameViaImplicitRules("myComponent", AggregationType.AS_COMPLEX_PROPERTY, registryMock);
        // Assert
        assertEquals(ConcreteComponent.class, result, "The class returned by the registry should be propagated unchanged.");
        // Verify that no further reflection was performed (method lookup would be a
        // separate interaction; we simply ensure the mock was called exactly once).
        verify(registryMock, times(1)).findDefaultComponentType(TestBean.class, "myComponent");
    }

    /**
     * When the registry returns {@code null} but the relevant setter method is
     * annotated with {@code @DefaultClass}, the annotation’s value must be
     * returned.
     */
    @Test
    void annotationProvidesResult() {
        // Registry returns null – this forces the method‑lookup path.
        when(registryMock.findDefaultComponentType(TestBean.class, "annotated")).thenReturn(null);
        // Act
        Class<?> result = setter.getClassNameViaImplicitRules("annotated", AggregationType.AS_COMPLEX_PROPERTY, registryMock);
        // Assert – the method {@code setAnnotated(AnnotatedComponent)} is annotated
        // with @DefaultClass(AnnotatedComponentImpl.class), therefore that class must be returned.
        assertEquals(AnnotatedComponentImpl.class, result, "The class defined by @DefaultClass on the setter should be used.");
    }

    /**
     * When neither the registry nor an annotation can supply a class, the
     * concrete type of the setter method’s single parameter is used.
     */
    @Test
    void concreteParameterTypeIsUsedWhenNoRegistryOrAnnotation() {
        // Registry returns null – forces the fallback logic.
        when(registryMock.findDefaultComponentType(TestBean.class, "plain")).thenReturn(null);
        // Act
        Class<?> result = setter.getClassNameViaImplicitRules("plain", AggregationType.AS_COMPLEX_PROPERTY, registryMock);
        // Assert – the method {@code setPlain(PlainComponent)} has no @DefaultClass,
        // so the parameter type (PlainComponent.class) must be returned.
        assertEquals(PlainComponent.class, result, "When no registry entry and no @DefaultClass are present, the parameter type should be used.");
    }

    /**
     * If the property name cannot be resolved to a setter method, the method must
     * return {@code null}.
     */
    @Test
    void unknownPropertyReturnsNull() {
        when(registryMock.findDefaultComponentType(TestBean.class, "nonExisting")).thenReturn(null);
        Class<?> result = setter.getClassNameViaImplicitRules("nonExisting", AggregationType.AS_COMPLEX_PROPERTY, registryMock);
        assertNull(result, "A property without a matching setter should yield null.");
    }

    /**
     * Registry returns a concrete class – the method must return it immediately.
     */
    @ParameterizedTest
    @MethodSource("registryProvidesComponentType")
    void registryProvidesComponentType(String name, AggregationType aggregationType, Class<?> expected, String mockName) {
        // arrange specific mock behaviour
        when(registryMock.findDefaultComponentType(TestBean.class, name)).thenReturn(expected);
        // act
        Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registryMock);
        // assert
        assertEquals(expected, result, "When the registry supplies a class it must be returned unchanged.");
        verify(registryMock, times(1)).findDefaultComponentType(TestBean.class, name);
    }

    /**
     * Neither the registry nor an annotation can supply a class – the concrete
     * parameter type of the setter method must be used.
     */
    @ParameterizedTest
    @MethodSource("fallbackToConcreteType")
    void fallbackToConcreteType(String name, AggregationType aggregationType, Class<?> expected, String mockName) {
        when(registryMock.findDefaultComponentType(TestBean.class, name)).thenReturn(null);
        Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registryMock);
        assertEquals(expected, result, "When no registry entry and no @DefaultClass are present, the parameter type should be used.");
    }

    /**
     * Different {@link AggregationType}s select different overloads of the same
     * property name, influencing whether an annotation is present.
     */
    @ParameterizedTest
    @MethodSource("aggregationInfluence")
    void aggregationTypeInfluencesMethodSelection(String name, AggregationType aggregationType, Class<?> expected, String mockName) {
        when(registryMock.findDefaultComponentType(TestBean.class, name)).thenReturn(null);
        // Use a specialised setter that overrides getRelevantMethod to mimic the
        // real Logback selection logic.
        PropertySetter aggSetter = new PropertySetter(beanDescriptionCache, testBean) {

            @Override
            Method getRelevantMethod(String propName, AggregationType aggType) {
                // locate the method on TestBean that matches both name and aggregation type
                try {
                    if ("filter".equals(propName) && aggType == AggregationType.AS_ATTRIBUTE) {
                        return TestBean.class.getMethod("setFilter", Filter.class);
                    }
                    if ("filter".equals(propName) && aggType == AggregationType.AS_ELEMENT) {
                        return TestBean.class.getMethod("setFilter", DefaultFilter.class);
                    }
                    // fallback to default implementation for other cases
                    return super.getRelevantMethod(propName, aggType);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        };
        aggSetter.setContext(CONTEXT);
        Class<?> result = aggSetter.getClassNameViaImplicitRules(name, aggregationType, registryMock);
        assertEquals(expected, result, "Aggregation type should drive method selection and thus the resulting class.");
    }

    /**
     * Null arguments (name or aggregation type) lead to a {@code null} result
     * because {@code getRelevantMethod} cannot locate a method.
     */
    @ParameterizedTest
    @MethodSource("nullArguments")
    void nullArgumentsHandling(String name, AggregationType aggregationType, Class<?> expected, String mockName, Class<?> ignored) {
        // The mock registry is not consulted when name is null or aggregationType is null,
        // but we still provide a mock to avoid NPE in the call.
        when(registryMock.findDefaultComponentType(any(), any())).thenReturn(null);
        Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registryMock);
        assertNull(result, "Null name or null aggregation type must result in null.");
    }

    /**
     * Passing a {@code null} registry triggers a {@link NullPointerException}
     * when the method attempts to call {@code findDefaultComponentType}.
     */
    @Test
    void nullRegistryThrowsNPE() {
        assertThrows(NullPointerException.class, () -> setter.getClassNameViaImplicitRules("appender", AggregationType.AS_ELEMENT, null), "A null registry must cause a NullPointerException.");
    }

    /**
     * When the registry supplies a class, it overrides any annotation or concrete
     * type that might also be applicable.
     */
    @Test
    void registryOverridesAnnotationAndConcreteType() {
        when(registryMock.findDefaultComponentType(TestBean.class, "encoder")).thenReturn(// registry wins
        Bar.class);
        Class<?> result = setter.getClassNameViaImplicitRules("encoder", AggregationType.AS_ELEMENT, registryMock);
        assertEquals(Bar.class, result, "Registry result must take precedence over annotation and concrete type.");
    }

    /**
     * Instantiating {@link PropertySetter} with a {@code null} target object
     * causes a {@link NullPointerException} when the focal method dereferences it.
     */
    @Test
    void nullObjInPropertySetterTriggersNPE() {
        PropertySetter nullObjSetter = new PropertySetter(beanDescriptionCache, null);
        nullObjSetter.setContext(CONTEXT);
        // registry mock is irrelevant – NPE occurs before its use
        assertThrows(NullPointerException.class, () -> nullObjSetter.getClassNameViaImplicitRules("appender", AggregationType.AS_ELEMENT, registryMock));
    }

    /**
     * If the supplied {@link DefaultNestedComponentRegistry} throws a runtime
     * exception, the focal method must let it propagate unchanged.
     */
    @Test
    void registryThrowsRuntimeException() {
        DefaultNestedComponentRegistry throwingRegistry = mock(DefaultNestedComponentRegistry.class);
        when(throwingRegistry.findDefaultComponentType(any(), any())).thenThrow(new IllegalStateException("boom"));
        assertThrows(IllegalStateException.class, () -> setter.getClassNameViaImplicitRules("appender", AggregationType.AS_ELEMENT, throwingRegistry), "Exceptions from the registry must propagate.");
    }

    /**
     * A subclass that overrides {@code getRelevantMethod} to throw a
     * {@link SecurityException}. The exception must bubble up.
     */
    @Test
    void getRelevantMethodThrowsException() {
        PropertySetter faultySetter = new PropertySetter(beanDescriptionCache, testBean) {

            @Override
            Method getRelevantMethod(String name, AggregationType aggregationType) {
                throw new SecurityException("access denied");
            }
        };
        faultySetter.setContext(CONTEXT);
        assertThrows(SecurityException.class, () -> faultySetter.getClassNameViaImplicitRules("faultyProperty", AggregationType.AS_ELEMENT, registryMock), "SecurityException from getRelevantMethod must propagate.");
    }

    /**
     * A subclass that overrides {@code getDefaultClassNameByAnnonation} to throw
     * a generic {@link RuntimeException}. The exception must propagate.
     */
    @Test
    void getDefaultClassNameByAnnotationThrowsException() {
        PropertySetter faultySetter = new PropertySetter(beanDescriptionCache, testBean) {

            @Override
            Class<?> getDefaultClassNameByAnnonation(String name, Method relevantMethod) {
                throw new RuntimeException("annotation failure");
            }
        };
        faultySetter.setContext(CONTEXT);
        when(registryMock.findDefaultComponentType(TestBean.class, "annotated")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> faultySetter.getClassNameViaImplicitRules("annotated", AggregationType.AS_ELEMENT, registryMock), "RuntimeException from getDefaultClassNameByAnnonation must propagate.");
    }

    /**
     * A subclass that overrides {@code getByConcreteType} to throw a checked
     * {@link ClassNotFoundException}. The exception should be propagated as
     * a {@link ClassNotFoundException}.
     */
    @Test
    void getByConcreteTypeThrowsException() {
        PropertySetter faultySetter = new PropertySetter(beanDescriptionCache, testBean) {

            @Override
            Class<?> getByConcreteType(String name, Method relevantMethod) throws ClassNotFoundException {
                throw new ClassNotFoundException("cannot resolve");
            }
        };
        faultySetter.setContext(CONTEXT);
        when(registryMock.findDefaultComponentType(TestBean.class, "concreteProperty")).thenReturn(null);
        // The overridden method declares a checked exception; reflection will wrap it.
        assertThrows(ClassNotFoundException.class, () -> faultySetter.getClassNameViaImplicitRules("concreteProperty", AggregationType.AS_ELEMENT, registryMock), "ClassNotFoundException from getByConcreteType must propagate.");
    }

    /**
     * When the concrete‑type resolution returns {@code null}, the focal method
     * must finally return {@code null}.
     */
    @Test
    void concreteTypeResolutionReturnsNull() {
        // Create a setter that forces getByConcreteType to return null.
        PropertySetter nullConcreteSetter = new PropertySetter(beanDescriptionCache, testBean) {

            @Override
            Class<?> getByConcreteType(String name, Method relevantMethod) {
                // simulate inability to resolve concrete type
                return null;
            }
        };
        nullConcreteSetter.setContext(CONTEXT);
        when(registryMock.findDefaultComponentType(TestBean.class, "unknown")).thenReturn(null);
        Class<?> result = nullConcreteSetter.getClassNameViaImplicitRules("unknown", AggregationType.AS_ELEMENT, registryMock);
        assertNull(result, "If concrete type cannot be resolved, the method must return null.");
    }

    /**
     * Registry implementation that rejects a {@code null} property name and
     * throws {@link NullPointerException}. The focal method must let this
     * exception propagate.
     */
    @Test
    void nullNameCausesRegistryToThrowNPE() {
        DefaultNestedComponentRegistry npeRegistry = mock(DefaultNestedComponentRegistry.class);
        when(npeRegistry.findDefaultComponentType(TestBean.class, null)).thenThrow(new NullPointerException("null name not allowed"));
        assertThrows(NullPointerException.class, () -> setter.getClassNameViaImplicitRules(null, AggregationType.AS_ELEMENT, npeRegistry), "NullPointerException from the registry (null property name) must propagate.");
    }
}
