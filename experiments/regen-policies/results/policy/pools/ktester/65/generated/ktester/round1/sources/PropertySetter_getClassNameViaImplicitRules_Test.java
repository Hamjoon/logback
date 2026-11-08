package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.util.AggregationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_getClassNameViaImplicitRules_Test {

    class DummyParent {
        /* empty placeholder */
    }

    /**
     * Helper that builds the objects required to construct a {@link PropertySetter}.
     */
    private static PropertySetter createPropertySetter(BeanDescriptionCache beanCache) {
        // The constructor used in the original test suite takes a BeanDescriptionCache
        // and a dummy parent object. The exact type of the parent is irrelevant for the
        // delegation behaviour, therefore we pass a simple new Object().
        return new PropertySetter(beanCache, new Object());
    }

    /**
     * Provides the arguments for the normal delegation tests (happy‑path, nulls, exceptions).
     */
    private static Stream<Arguments> delegationCases() {
        return Stream.of(// BASIC_PROPERTY – returns concrete class
        Arguments.of("appender", AggregationType.BASIC_PROPERTY, new DefaultNestedComponentRegistry(), ch.qos.logback.core.Appender.class, null), // COMPLEX_PROPERTY – registry is null, still returns a class
        Arguments.of("layout", AggregationType.COMPLEX_PROPERTY, null, ch.qos.logback.core.LayoutBase.class, null), // unknown name – assessor returns null
        Arguments.of("unknown", AggregationType.BASIC_PROPERTY, new DefaultNestedComponentRegistry(), null, null), // assessor throws RuntimeException
        Arguments.of("error", AggregationType.COMPLEX_PROPERTY, new DefaultNestedComponentRegistry(), null, RuntimeException.class), // AS_LIST – returns a list implementation
        Arguments.of("list", AggregationType.AS_LIST, new DefaultNestedComponentRegistry(), java.util.ArrayList.class, null), // null name – returns null
        Arguments.of(null, AggregationType.BASIC_PROPERTY, new DefaultNestedComponentRegistry(), null, null), // empty name – returns null
        Arguments.of("", AggregationType.BASIC_PROPERTY, new DefaultNestedComponentRegistry(), null, null), // null aggregationType – NPE from assessor
        Arguments.of("appender", null, new DefaultNestedComponentRegistry(), null, NullPointerException.class), // AS_ARRAY – treated like list, returns ArrayList
        Arguments.of("list", AggregationType.AS_ARRAY, new DefaultNestedComponentRegistry(), java.util.ArrayList.class, null), // custom component – returns user defined class
        Arguments.of("custom", AggregationType.COMPLEX_PROPERTY, new DefaultNestedComponentRegistry(), com.example.CustomComponent.class, null), // null registry for complex property – NPE from assessor
        Arguments.of("customComponent", AggregationType.COMPLEX_PROPERTY, null, null, NullPointerException.class), // assessor throws IllegalArgumentException for unsupported aggregation type
        Arguments.of("appender", AggregationType.AS_ARRAY, new DefaultNestedComponentRegistry(), null, IllegalArgumentException.class));
    }

    /**
     * Verifies that {@code getClassNameViaImplicitRules} forwards the call to the
     * {@link AggregationAssessor} created inside the {@link PropertySetter} constructor
     * and returns exactly the value supplied by that assessor.
     */
    @Test
    void testGetClassNameViaImplicitRules_delegatesToAggregationAssessor() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        class DummyParent {
            /* empty placeholder */
        }
        DummyParent dummyParent = new DummyParent();
        // a real Context is needed only to build a BeanDescriptionCache
        Context context = new ContextBase();
        BeanDescriptionCache beanCache = new BeanDescriptionCache(context);
        // Prepare a mock for AggregationAssessor that will be constructed inside
        // PropertySetter. The mock will return String.class when invoked with the
        // expected arguments.
        try (MockedConstruction<AggregationAssessor> mockedAssessor = mockConstruction(AggregationAssessor.class, (mock, ctx) -> when(mock.getClassNameViaImplicitRules(eq("myProperty"), eq(AggregationType.AS_COMPLEX_PROPERTY), any(DefaultNestedComponentRegistry.class))).thenReturn(String.class))) {
            // The PropertySetter under test; during its construction it will create
            // an AggregationAssessor instance which is intercepted by the above mock.
            PropertySetter setter = new PropertySetter(beanCache, dummyParent);
            // -----------------------------------------------------------------
            // Act
            // -----------------------------------------------------------------
            Class<?> result = setter.getClassNameViaImplicitRules("myProperty", AggregationType.AS_COMPLEX_PROPERTY, new DefaultNestedComponentRegistry());
            // -----------------------------------------------------------------
            // Assert
            // -----------------------------------------------------------------
            assertEquals(String.class, result, "The method should return the class supplied by the mocked AggregationAssessor.");
            // Verify that the mocked AggregationAssessor received exactly one call with the
            // expected arguments.
            AggregationAssessor createdMock = mockedAssessor.constructed().get(0);
            verify(createdMock, times(1)).getClassNameViaImplicitRules(eq("myProperty"), eq(AggregationType.AS_COMPLEX_PROPERTY), any(DefaultNestedComponentRegistry.class));
        }
    }

    /**
     * Parameterised test that verifies the delegation contract for all normal cases.
     *
     * <p>For each argument set we configure a {@code MockedConstruction<AggregationAssessor>}
     * that mimics the behaviour described in the JSON test case (return a class,
     * return {@code null} or throw an exception). The {@link PropertySetter} under test
     * is then exercised and the outcome is asserted.</p>
     */
    @ParameterizedTest(name = "[{index}] name=''{0}'', type={1}, registry={2}")
    @MethodSource("delegationCases")
    void testGetClassNameViaImplicitRules_delegation(String name, AggregationType aggregationType, DefaultNestedComponentRegistry registry, Class<?> expectedClass, Class<? extends Throwable> expectedException) {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        Context context = new ContextBase();
        BeanDescriptionCache beanCache = new BeanDescriptionCache(context);
        // Mock the AggregationAssessor that PropertySetter creates internally.
        try (MockedConstruction<AggregationAssessor> mocked = mockConstruction(AggregationAssessor.class, (mock, ctx) -> {
            // configure the mock according to the expected outcome of this case
            if (expectedException != null) {
                when(mock.getClassNameViaImplicitRules(eq(name), eq(aggregationType), any())).thenThrow(expectedException);
            } else {
                when(mock.getClassNameViaImplicitRules(eq(name), eq(aggregationType), any())).thenReturn(expectedClass);
            }
        })) {
            PropertySetter setter = createPropertySetter(beanCache);
            // -----------------------------------------------------------------
            // Act & Assert
            // -----------------------------------------------------------------
            if (expectedException != null) {
                // The method should propagate the exception from the assessor.
                assertThrows(expectedException, () -> setter.getClassNameViaImplicitRules(name, aggregationType, registry));
            } else {
                // Normal return value (may be null)
                Class<?> result = setter.getClassNameViaImplicitRules(name, aggregationType, registry);
                assertEquals(expectedClass, result, () -> "Expected class " + expectedClass + " but got " + result);
            }
            // Verify that the mock was called exactly once with the supplied arguments.
            AggregationAssessor createdMock = mocked.constructed().get(0);
            verify(createdMock, times(1)).getClassNameViaImplicitRules(eq(name), eq(aggregationType), any());
        }
    }

    /**
     * Test that a {@link PropertySetter} instantiated with a {@code null}
     * {@code aggregationAssessor} throws {@link NullPointerException} when the
     * delegating method is invoked.
     *
     * <p>This scenario cannot be produced via the public constructor, therefore
     * reflection is used to set the private field to {@code null} after construction.</p>
     */
    @Test
    void testGetClassNameViaImplicitRules_nullAggregationAssessor_throwsNPE() throws Exception {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        Context context = new ContextBase();
        BeanDescriptionCache beanCache = new BeanDescriptionCache(context);
        PropertySetter setter = createPropertySetter(beanCache);
        // Force the private field `aggregationAssessor` to null via reflection.
        Field field = PropertySetter.class.getDeclaredField("aggregationAssessor");
        field.setAccessible(true);
        field.set(setter, null);
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        assertThrows(NullPointerException.class, () -> setter.getClassNameViaImplicitRules("appender", AggregationType.BASIC_PROPERTY, new DefaultNestedComponentRegistry()));
    }
}
