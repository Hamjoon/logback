package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertySetter_setContext_Test {

    /**
     * The Logback context used throughout the tests.
     */
    private Context context;

    /**
     * The {@link PropertySetter} instance under test.
     */
    private PropertySetter setter;

    /**
     * Mock that replaces the real {@code AggregationAssessor}.
     */
    @Mock
    private AggregationAssessor mockAggregationAssessor;

    class ThrowingSuperPropertySetter extends PropertySetter {

        ThrowingSuperPropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        @Override
        public void setContext(Context ctx) {
            // Simulate a failure in the super implementation.
            throw new IllegalArgumentException("simulated super failure");
        }
    }

    class ThrowingSuperPropertySetter extends PropertySetter {

        ThrowingSuperPropertySetter(BeanDescriptionCache cache, Object obj) {
            super(cache, obj);
        }

        @Override
        public void setContext(Context ctx) {
            // Simulate a failure in the super implementation.
            throw new IllegalArgumentException("simulated super failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise a fresh Logback context and the objects required by PropertySetter.
        context = new ContextBase();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        // Use a simple POJO as the target object – the actual type is irrelevant for setContext().
        Object dummyTarget = new Object();
        setter = new PropertySetter(cache, dummyTarget);
        // Replace the real AggregationAssessor with the Mockito mock.
        injectMockAggregationAssessor(setter, mockAggregationAssessor);
        // Initialise a fresh Logback context and the objects required by PropertySetter.
        context = new ContextBase();
        BeanDescriptionCache cache = new BeanDescriptionCache(context);
        // the target object is irrelevant for setContext()
        Object dummyTarget = new Object();
        setter = new PropertySetter(cache, dummyTarget);
        // Replace the real AggregationAssessor with the Mockito mock.
        injectMockAggregationAssessor(setter, mockAggregationAssessor);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockAggregationAssessor);
        // Reset the mock to avoid cross‑test interference.
        reset(mockAggregationAssessor);
    }

    /**
     * Helper to inject the mock into the {@code setter}.
     */
    private static void injectMockAggregationAssessor(PropertySetter setter, AggregationAssessor mock) throws Exception {
        Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, mock);
    }

    /**
     * Helper to read the protected {@code context} field from {@link ContextAwareBase}.
     */
    private static Context readContextFromBase(PropertySetter setter) throws Exception {
        Field f = ContextAwareBase.class.getDeclaredField("context");
        f.setAccessible(true);
        return (Context) f.get(setter);
    }

    @Test
    void setContext_delegatesToAggregationAssessor_andStoresContext() throws Exception {
        // WHEN
        setter.setContext(context);
        // THEN
        // 1. The AggregationAssessor must receive the same context.
        verify(mockAggregationAssessor, times(1)).setContext(context);
        // 2. The ContextAwareBase part of PropertySetter must hold the context.
        Context stored = readContextFromBase(setter);
        assertNotNull(stored, "Context should be stored in the base class");
        assertSame(context, stored, "Stored context must be the same instance that was passed");
    }

    @Test
    void setContext_multipleInvocations_updateAggregationAssessorEachTime() throws Exception {
        // GIVEN
        Context anotherContext = new ContextBase();
        // WHEN – first call
        setter.setContext(context);
        // WHEN – second call
        setter.setContext(anotherContext);
        // THEN – the mock should have been called twice with the respective arguments.
        verify(mockAggregationAssessor, times(1)).setContext(context);
        verify(mockAggregationAssessor, times(1)).setContext(anotherContext);
    }

    /**
     * --------------------------------------------------------------
     *  2. Passing {@code null} as the context.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_nullContext_isForwardedAndStoredAsNull() throws Exception {
        // WHEN
        setter.setContext(null);
        // THEN
        verify(mockAggregationAssessor, times(1)).setContext(null);
        Context stored = readContextFromBase(setter);
        assertNull(stored, "Stored context should be null when null is supplied");
    }

    /**
     * --------------------------------------------------------------
     *  4. Repeated calls with the *same* Context instance.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_repeatedSameInstance_forwardsEachTime() throws Exception {
        Context same = new ContextBase();
        setter.setContext(same);
        // second call with identical instance
        setter.setContext(same);
        verify(mockAggregationAssessor, times(2)).setContext(same);
        Context stored = readContextFromBase(setter);
        assertSame(same, stored, "Stored context should remain the same instance");
    }

    /**
     * --------------------------------------------------------------
     *  5. Transition from null → non‑null.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_nullThenNonNull_transitionsCorrectly() throws Exception {
        Context afterNull = new ContextBase();
        setter.setContext(null);
        setter.setContext(afterNull);
        verify(mockAggregationAssessor, times(1)).setContext(null);
        verify(mockAggregationAssessor, times(1)).setContext(afterNull);
        Context stored = readContextFromBase(setter);
        assertSame(afterNull, stored, "After the transition the stored context must be the non‑null one");
    }

    /**
     * --------------------------------------------------------------
     *  6. Transition from non‑null → null.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_nonNullThenNull_clearsStoredContext() throws Exception {
        Context first = new ContextBase();
        setter.setContext(first);
        setter.setContext(null);
        verify(mockAggregationAssessor, times(1)).setContext(first);
        verify(mockAggregationAssessor, times(1)).setContext(null);
        Context stored = readContextFromBase(setter);
        assertNull(stored, "Stored context should be null after being cleared");
    }

    /**
     * --------------------------------------------------------------
     *  7. Propagation of an exception thrown by AggregationAssessor.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_aggregationAssessorThrows_exceptionBubblesUp() {
        Context bad = new ContextBase();
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(mockAggregationAssessor).setContext(bad);
        RuntimeException caught = assertThrows(RuntimeException.class, () -> setter.setContext(bad), "Exception from AggregationAssessor must propagate");
        assertSame(boom, caught, "The same exception instance should be re‑thrown");
    }

    /**
     * --------------------------------------------------------------
     *  8. No AggregationAssessor (field left null) – NPE expected.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_nullAggregationAssessor_throwsNullPointerException() throws Exception {
        // Remove the mock – set the field to null via reflection.
        Field f = PropertySetter.class.getDeclaredField("aggregationAssessor");
        f.setAccessible(true);
        f.set(setter, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> setter.setContext(new ContextBase()), "Calling setContext with a null AggregationAssessor should throw NPE");
        // message is not important, just the type
        assertNotNull(npe.getMessage());
    }

    /**
     * --------------------------------------------------------------
     *  9. Simulated exception from the superclass implementation.
     *
     *  Because {@link ContextAwareBase#setContext(Context)} does not throw,
     *  we simulate the situation by overriding {@code setContext} in a
     *  test‑specific subclass and throwing before delegating to the
     *  AggregationAssessor. The goal is to verify that when an exception
     *  occurs *before* the assessor is called, the assessor is never
     *  invoked and the exception propagates.
     * --------------------------------------------------------------
     */
    @Test
    void setContext_superThrows_exceptionPropagatesAndAssessorNotCalled() {
        // A subclass that throws an IllegalArgumentException before any other logic.
        class ThrowingSuperPropertySetter extends PropertySetter {

            ThrowingSuperPropertySetter(BeanDescriptionCache cache, Object obj) {
                super(cache, obj);
            }

            @Override
            public void setContext(Context ctx) {
                // Simulate a failure in the super implementation.
                throw new IllegalArgumentException("simulated super failure");
            }
        }
        BeanDescriptionCache cache = new BeanDescriptionCache(new ContextBase());
        PropertySetter throwingSetter = new ThrowingSuperPropertySetter(cache, new Object());
        // Inject the mock (won't be used because the method throws first).
        try {
            injectMockAggregationAssessor(throwingSetter, mockAggregationAssessor);
        } catch (Exception e) {
            fail("Reflection setup failed", e);
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> throwingSetter.setContext(new ContextBase()), "Exception from super.setContext must propagate");
        assertEquals("simulated super failure", ex.getMessage());
        // Verify that the assessor was never touched.
        verifyNoInteractions(mockAggregationAssessor);
    }
}
