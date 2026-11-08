package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setContext_Test {

    /**
     * Mocked bean‑description cache – the real implementation is not needed for this test.
     */
    @Mock
    BeanDescriptionCache beanDescriptionCache;

    /**
     * Mocked {@link Context} used as the argument to {@code setContext}.
     */
    @Mock
    Context firstContext;

    /**
     * Second mock context to verify that a later call overwrites the previous one.
     */
    @Mock
    Context secondContext;

    /**
     * Instance under test.
     */
    PropertySetter setter;

    /**
     * Helper mock used in most test cases.
     */
    @Mock
    AggregationAssessor mockAssessor;

    /**
     * Simple mock Context implementation used for normal & null tests.
     */
    static class SimpleMockContext implements Context {

        private final String name;

        SimpleMockContext(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "SimpleMockContext[" + name + "]";
        }
        // All other Context methods can remain unimplemented for the test.
    }

    /**
     * Context subclass that overrides {@code equals} and {@code hashCode}.
     */
    static class SpecialMockContext implements Context {

        private final String id;

        SpecialMockContext(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SpecialMockContext))
                return false;
            return id.equals(((SpecialMockContext) o).id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "SpecialMockContext[" + id + "]";
        }
        // Remaining Context methods are irrelevant for these tests.
    }

    /**
     * Simple holder for a Context argument and a flag indicating whether an
     *  exception is expected.
     */
    private static class ArgumentsHolder {

        final Context context;

        final boolean expectException;

        ArgumentsHolder(Context context, boolean expectException) {
            this.context = context;
            this.expectException = expectException;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The object whose properties will be set can be any plain object.
        Object dummyTarget = new Object();
        setter = new PropertySetter(beanDescriptionCache, dummyTarget);
        // The target object can be any simple POJO.
        Object dummyTarget = new Object();
        setter = new PropertySetter(beanDescriptionCache, dummyTarget);
        // Replace the real aggregationAssessor with a Mockito mock via reflection.
        Field assessorField = PropertySetter.class.getDeclaredField("aggregationAssessor");
        assessorField.setAccessible(true);
        assessorField.set(setter, mockAssessor);
    }

    /**
     * Parameter source for normal and null context tests.
     */
    static Stream<ArgumentsHolder> normalAndNullContexts() {
        return Stream.of(new ArgumentsHolder(new SimpleMockContext("testContext"), false), new ArgumentsHolder(null, false));
    }

    @Test
    void setContextStoresContextInBaseClass() {
        // When
        setter.setContext(firstContext);
        // Then
        assertEquals(firstContext, setter.getContext(), "The context stored in PropertySetter should be the one supplied to setContext()");
    }

    @Test
    void setContextCanBeCalledMultipleTimesAndOverridesPreviousValue() {
        // First call
        setter.setContext(firstContext);
        assertEquals(firstContext, setter.getContext(), "After the first call the stored context should match the first argument");
        // Second call with a different context
        setter.setContext(secondContext);
        assertEquals(secondContext, setter.getContext(), "A subsequent call to setContext should replace the previously stored context");
    }

    @ParameterizedTest(name = "setContext with context={0}")
    @MethodSource("normalAndNullContexts")
    void setContextForwardsToBaseAndAssessor(ArgumentsHolder holder) {
        // Arrange
        Context ctx = holder.context;
        // Act
        setter.setContext(ctx);
        // Assert – the ContextAwareBase part must return the same instance.
        assertSame(ctx, setter.getContext(), "PropertySetter#getContext() should return the exact Context passed to setContext");
        // Verify that the AggregationAssessor received exactly one call with the same argument.
        verify(mockAssessor, times(1)).setContext(ctx);
        verifyNoMoreInteractions(mockAssessor);
    }

    @Test
    void setContextAcceptsNullWithoutException() {
        // Act & Assert – no exception should be thrown.
        assertDoesNotThrow(() -> setter.setContext(null), "setContext(null) must not throw any exception");
        // The stored context should be null.
        assertNull(setter.getContext(), "After passing null, getContext() must return null");
        // AggregationAssessor must still receive the null argument.
        verify(mockAssessor, times(1)).setContext(null);
    }

    @Test
    void multipleSetContextCallsOverridePreviousValue() {
        Context first = new SimpleMockContext("first");
        Context second = new SimpleMockContext("second");
        // First call
        setter.setContext(first);
        assertSame(first, setter.getContext(), "After first call, getContext() should return the first Context");
        // Second call
        setter.setContext(second);
        assertSame(second, setter.getContext(), "After second call, getContext() should return the second Context");
        // Verify ordering of calls on the mock assessor.
        InOrder inOrder = inOrder(mockAssessor);
        inOrder.verify(mockAssessor).setContext(first);
        inOrder.verify(mockAssessor).setContext(second);
        verifyNoMoreInteractions(mockAssessor);
    }

    @Test
    void setContextInvokesSuperclassBeforeAggregationAssessor() {
        Context ctx = new SimpleMockContext("ordered");
        // Replace the mockAssessor with a custom Answer that checks the state
        // of the PropertySetter *after* super.setContext has been executed.
        doAnswer(invocation -> {
            // At this point, the superclass should already have stored the context.
            assertSame(ctx, setter.getContext(), "When AggregationAssessor#setContext is called, the superclass must have already stored the Context");
            return null;
        }).when(mockAssessor).setContext(ctx);
        // Act
        setter.setContext(ctx);
        // Verify that the assessor was indeed called once.
        verify(mockAssessor, times(1)).setContext(ctx);
    }

    @Test
    void setContextThrowsWhenAggregationAssessorIsNull() throws Exception {
        // Make the aggregationAssessor field null via reflection.
        Field assessorField = PropertySetter.class.getDeclaredField("aggregationAssessor");
        assessorField.setAccessible(true);
        assessorField.set(setter, null);
        Context ctx = new SimpleMockContext("nullAssessor");
        // Expect a NullPointerException because the method tries to call
        // aggregationAssessor.setContext(context).
        assertThrows(NullPointerException.class, () -> setter.setContext(ctx), "If aggregationAssessor is null, setContext should throw NPE");
    }

    @Test
    void setContextPreservesSpecialContextInstance() {
        Context special = new SpecialMockContext("special");
        setter.setContext(special);
        // The getter must return the exact same instance (not a copy).
        assertSame(special, setter.getContext(), "PropertySetter must store and return the original SpecialMockContext instance");
        // Verify that the assessor received the exact same instance.
        verify(mockAssessor, times(1)).setContext(special);
    }
}
