package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.util.AggregationAssessor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getClassNameViaImplicitRules_Test {

    private Context context;

    private BeanDescriptionCache beanCache;

    private PropertySetter setter;

    private DummyHost dummyHost;

    @Mock
    private AggregationAssessor // will be injected into the setter
    mockAssessor;

    /**
     * Simple component class used in the implicit‑rule test.
     */
    static class DummyComponent {
    }

    /**
     * Host class that contains an {@code addDummyComponent(DummyComponent)} method.
     * According to Logback's implicit‑rule algorithm, a property name
     * {@code "dummyComponent"} with {@link AggregationType#AS_COMPLEX_PROPERTY}
     * should resolve to {@link DummyComponent}.
     */
    static class DummyHost {

        public void addDummyComponent(DummyComponent component) {
            // no‑op – the method exists only so that the implicit rule can discover the type
        }
    }

    /* --------------------------------------------------------------------- */
    static class SimpleComponent {
    }

    static class ComplexComponent {
    }

    static class RegistryBasedComponent {
    }

    /**
     * Simple custom registry that pretends to know a single component.
     */
    private static class CustomRegistry extends DefaultNestedComponentRegistry {

        @Override
        public Class<?> getComponentClass(String name) {
            if ("registryBasedComponent".equals(name)) {
                return RegistryBasedComponent.class;
            }
            return super.getComponentClass(name);
        }
    }

    /**
     * Custom registry that maps "listComponent" to {@link LinkedList}.
     */
    private static class CustomLinkedListRegistry extends DefaultNestedComponentRegistry {

        @Override
        public Class<?> getComponentClass(String name) {
            if ("listComponent".equals(name)) {
                return LinkedList.class;
            }
            return super.getComponentClass(name);
        }
    }

    /**
     * Registry that deliberately throws an IllegalStateException.
     */
    private static class RegistryThrowingIllegalState extends DefaultNestedComponentRegistry {

        @Override
        public Class<?> getComponentClass(String name) {
            throw new IllegalStateException("registry failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Logback context and the BeanDescriptionCache required by PropertySetter.
        context = new ContextBase();
        beanCache = new BeanDescriptionCache(context);
        // Initialise the host object and the PropertySetter under test.
        dummyHost = new DummyHost();
        setter = new PropertySetter(beanCache, dummyHost);
        setter.setContext(context);
        context = new ContextBase();
        beanCache = new BeanDescriptionCache(context);
        // The real constructor expects a BeanDescriptionCache and the target object.
        setter = new PropertySetter(beanCache, new Object());
        // Replace the final field 'aggregationAssessor' with our mock.
        Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mockAssessor);
    }

    /**
     * Group 1 – basic happy‑path scenarios.
     */
    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// {"simpleComponent", SIMPLE, new DefaultNestedComponentRegistry()}
        Arguments.of("simpleComponent", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), SimpleComponent.class, null, "Known component with AggregationType.SIMPLE should return SimpleComponent.class"), // {"listComponent", LIST, new DefaultNestedComponentRegistry()}
        Arguments.of("listComponent", AggregationType.LIST, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), ArrayList.class, null, "Known list component with AggregationType.LIST should return ArrayList.class"), // {"unknownComponent", SIMPLE, new DefaultNestedComponentRegistry()}
        Arguments.of("unknownComponent", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, null, "Unknown component name should result in null"), // {null, SIMPLE, new DefaultNestedComponentRegistry()}
        Arguments.of(null, AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, IllegalArgumentException.class, "Null component name should cause IllegalArgumentException"), // {"registryBasedComponent", SIMPLE, customRegistry}
        Arguments.of("registryBasedComponent", AggregationType.SIMPLE, new CustomRegistry(), RegistryBasedComponent.class, null, "Custom registry should influence the returned class"));
    }

    /**
     * Group 2 – additional edge cases (empty names, whitespace, mismatched aggregation, etc.).
     */
    private static Stream<Arguments> additionalScenarios() {
        return Stream.of(Arguments.of("", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, null, "Empty name → assessor returns null"), Arguments.of("   ", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, null, "Whitespace‑only name → assessor returns null"), Arguments.of("listComponent", AggregationType.MAP, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, null, "Known name but unsupported MAP aggregation → null"), Arguments.of("arrayComponent", AggregationType.ARRAY, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), ArrayList.class, null, "Array‑style component with ARRAY aggregation → ArrayList.class"), Arguments.of("simpleComponent", null, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, IllegalArgumentException.class, "Null AggregationType should cause IllegalArgumentException"), Arguments.of("simpleComponent", AggregationType.SIMPLE, null, null, IllegalArgumentException.class, "Null registry should cause IllegalArgumentException"), Arguments.of("complexComponent", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), ComplexComponent.class, null, "Known complex component with SIMPLE aggregation → ComplexComponent.class"), Arguments.of("listComponent", AggregationType.LIST, new CustomLinkedListRegistry(), LinkedList.class, null, "Custom registry maps listComponent to LinkedList"), Arguments.of("unknownComponent", AggregationType.LIST, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), null, null, "Unknown component with LIST aggregation → null"));
    }

    /**
     * Group 3 – exception propagation tests.
     */
    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(Arguments.of("invalid!Name", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), IllegalArgumentException.class, "Illegal character in name → IllegalArgumentException"), Arguments.of("my component", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), IllegalArgumentException.class, "Whitespace inside name → IllegalArgumentException"), Arguments.of("simpleComponent", AggregationType.SIMPLE, new RegistryThrowingIllegalState(), IllegalStateException.class, "Registry throws IllegalStateException → propagate"), Arguments.of("simpleComponent", AggregationType.SIMPLE, (DefaultNestedComponentRegistry) new DefaultNestedComponentRegistry(), RuntimeException.class, "Assessor throws generic RuntimeException → propagate"));
    }

    /**
     * Verifies that an implicit rule based on an {@code addXxx} method is honoured.
     * The property name {@code "dummyComponent"} together with
     * {@link AggregationType#AS_COMPLEX_PROPERTY} should resolve to {@link DummyComponent}.
     */
    @Test
    void implicitRuleResolvesToComponentClass() {
        // GIVEN
        String propertyName = "dummyComponent";
        AggregationType aggregationType = AggregationType.AS_COMPLEX_PROPERTY;
        // not needed for this rule
        DefaultNestedComponentRegistry registry = null;
        // WHEN
        Class<?> result = setter.getClassNameViaImplicitRules(propertyName, aggregationType, registry);
        // THEN
        assertNotNull(result, "The implicit rule should have resolved a class");
        assertEquals(DummyComponent.class, result, "The resolved class should be the type of the parameter of addDummyComponent()");
    }

    /**
     * When the aggregation type does not correspond to a complex property,
     * the implicit‑rule assessor should return {@code null} (no default class can be inferred).
     */
    @Test
    void nonComplexAggregationReturnsNull() {
        // GIVEN
        String propertyName = "dummyComponent";
        // not a complex property
        AggregationType aggregationType = AggregationType.AS_BASIC_PROPERTY;
        DefaultNestedComponentRegistry registry = null;
        // WHEN
        Class<?> result = setter.getClassNameViaImplicitRules(propertyName, aggregationType, registry);
        // THEN
        assertNull(result, "For a non‑complex aggregation type the implicit rule should not resolve a class");
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("basicScenarios")
    void testBasicScenarios(String name, AggregationType aggregationType, DefaultNestedComponentRegistry registry, Class<?> expectedClass, Class<? extends Throwable> expectedException, String description) throws Exception {
        // -----------------------------------------------------------------
        // Configure mock behaviour according to the expectation of this case
        // -----------------------------------------------------------------
        if (expectedException != null) {
            when(mockAssessor.getClassNameViaImplicitRules(name, aggregationType, registry)).thenThrow(expectedException.getDeclaredConstructor().newInstance());
        } else {
            when(mockAssessor.getClassNameViaImplicitRules(name, aggregationType, registry)).thenReturn(expectedClass);
        }
        // -------------------------------------------------------------
        // Invoke the method under test and verify the outcome
        // -------------------------------------------------------------
        if (expectedException != null) {
            assertThrows(expectedException, () -> setter.getClassNameViaImplicitRules(name, aggregationType, registry), "Expected exception to be propagated");
        } else {
            Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registry);
            assertEquals(expectedClass, result, "Returned class should match the mock assessor's result");
        }
        // Verify that the delegation really happened
        verify(mockAssessor, times(1)).getClassNameViaImplicitRules(name, aggregationType, registry);
        verifyNoMoreInteractions(mockAssessor);
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("additionalScenarios")
    void testAdditionalScenarios(String name, AggregationType aggregationType, DefaultNestedComponentRegistry registry, Class<?> expectedClass, Class<? extends Throwable> expectedException, String description) throws Exception {
        if (expectedException != null) {
            when(mockAssessor.getClassNameViaImplicitRules(name, aggregationType, registry)).thenThrow(expectedException.getDeclaredConstructor().newInstance());
        } else {
            when(mockAssessor.getClassNameViaImplicitRules(name, aggregationType, registry)).thenReturn(expectedClass);
        }
        if (expectedException != null) {
            assertThrows(expectedException, () -> setter.getClassNameViaImplicitRules(name, aggregationType, registry), "Expected exception to be propagated");
        } else {
            Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registry);
            assertEquals(expectedClass, result, "Returned class should match the mock assessor's result");
        }
        verify(mockAssessor, times(1)).getClassNameViaImplicitRules(name, aggregationType, registry);
        verifyNoMoreInteractions(mockAssessor);
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("exceptionScenarios")
    void testExceptionScenarios(String name, AggregationType aggregationType, DefaultNestedComponentRegistry registry, Class<? extends Throwable> expectedException, String description) throws Exception {
        // The mock assessor is configured to throw the required exception.
        when(mockAssessor.getClassNameViaImplicitRules(name, aggregationType, registry)).thenThrow(expectedException.getDeclaredConstructor().newInstance());
        assertThrows(expectedException, () -> setter.getClassNameViaImplicitRules(name, aggregationType, registry), "Exception should propagate unchanged");
        verify(mockAssessor, times(1)).getClassNameViaImplicitRules(name, aggregationType, registry);
        verifyNoMoreInteractions(mockAssessor);
    }
}
