package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.mockito.InOrder;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;

class JoranConfiguratorBase_buildModelInterpretationContext_Test {

    /**
     * Holds the mocked construction of {@link SaxEventInterpretationContext}.
     */
//     private MockedConstruction<SaxEventInterpretationContext> mockedContext;

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}
     * required for testing.  All abstract methods are stubbed because they are
     * not relevant for the {@code buildModelInterpretationContext()} behaviour.
     *
     * @param <E> the generic type parameter of the base class (unused here)
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No rule associations needed for this test.
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No model‑handler associations needed for this test.
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // Not used in the current test scenario.
            return null;
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No implicit rule supplier required for this test.
        }
    }

    /**
     * Subclass that simulates a failure in the superclass implementation
     * by throwing an {@link AssertionError} before delegating to the
     * {@code JoranConfiguratorBase} logic.
     */
    private static class ErrorThrowingConfigurator extends TestConfigurator {

        @Override
        public void buildModelInterpretationContext() {
            // Simulate the superclass throwing an Error.
            throw new AssertionError("simulated super error");
        }
    }

    /**
     * Subclass used to verify call ordering.  It records a flag when the
     * (simulated) super method is entered; the flag is verified via an
     * {@link InOrder} check.
     */
    private static class OrderingConfigurator extends TestConfigurator {

        private boolean superCalled = false;

        /**
         * Called from the overridden super method to record the call.
         */
        void recordSuperCalled() {
            superCalled = true;
        }

        @Override
        public void buildModelInterpretationContext() {
            // Simulate the super call – we invoke a helper that the test can spy on.
            // this represents the super implementation
            recordSuperCalled();
            // Now delegate to the real JoranConfiguratorBase logic.
            super.buildModelInterpretationContext();
        }
    }

    /**
     * Injects a {@link ModelInterpretationContext} mock into the protected
     * {@code modelInterpretationContext} field of the configurator via reflection.
     */
    private static void injectModelInterpretationContext(JoranConfiguratorBase<?> configurator, ModelInterpretationContext mockCtx) {
        try {
            Field f = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
            f.setAccessible(true);
            f.set(configurator, mockCtx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Directly sets the protected {@code modelInterpretationContext} field to the
     * supplied value (may be {@code null}).
     */
    private static void setModelInterpretationContextField(JoranConfiguratorBase<?> configurator, ModelInterpretationContext value) {
        try {
            Field f = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
            f.setAccessible(true);
            f.set(configurator, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsRuntimeException() {
        // GIVEN
        TestConfigurator configurator = new TestConfigurator();
        ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
        doThrow(new RuntimeException("boom")).when(mockCtx).createAppenderBags();
        injectModelInterpretationContext(configurator, mockCtx);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, configurator::buildModelInterpretationContext, "Exception from createAppenderBags must propagate");
        assertEquals("boom", ex.getMessage());
        verify(mockCtx, times(1)).createAppenderBags();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsError() {
        // GIVEN
        TestConfigurator configurator = new TestConfigurator();
        ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
        doThrow(new AssertionError("assertion failure")).when(mockCtx).createAppenderBags();
        injectModelInterpretationContext(configurator, mockCtx);
        // WHEN / THEN
        AssertionError err = assertThrows(AssertionError.class, configurator::buildModelInterpretationContext, "Error from createAppenderBags must propagate");
        assertEquals("assertion failure", err.getMessage());
        verify(mockCtx, times(1)).createAppenderBags();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_multipleCallsInvokeCreateAppenderBagsEachTime() {
        // GIVEN
        TestConfigurator configurator = new TestConfigurator();
        ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
        injectModelInterpretationContext(configurator, mockCtx);
        // WHEN
        configurator.buildModelInterpretationContext();
        configurator.buildModelInterpretationContext();
        // THEN
        verify(mockCtx, times(2)).createAppenderBags();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_nullModelInterpretationContext_throwsNPE() throws Exception {
        // GIVEN
        TestConfigurator configurator = new TestConfigurator();
        // Force the protected field to null (it would normally be set by the
        // superclass).  Using reflection avoids touching private implementation.
        setModelInterpretationContextField(configurator, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, configurator::buildModelInterpretationContext, "Calling createAppenderBags on a null context must throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_superThrowsError_propagatesAndNoCreateAppenderBags() {
        // GIVEN
        ErrorThrowingConfigurator configurator = new ErrorThrowingConfigurator();
        // Even though we provide a mock context, the overridden method throws
        // before it can be used.
        ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
        injectModelInterpretationContext(configurator, mockCtx);
        // WHEN / THEN
        AssertionError err = assertThrows(AssertionError.class, configurator::buildModelInterpretationContext, "Error thrown by the (simulated) super method must propagate");
        assertEquals("simulated super error", err.getMessage());
        // verify that createAppenderBags was never invoked
        verifyNoInteractions(mockCtx);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_superCalledBeforeCreateAppenderBags() {
        // GIVEN
        OrderingConfigurator configurator = new OrderingConfigurator();
        ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
        injectModelInterpretationContext(configurator, mockCtx);
        // WHEN
        configurator.buildModelInterpretationContext();
        // THEN – the flag set by the overridden super method must be true
        // before createAppenderBags() is invoked.
        InOrder inOrder = inOrder(configurator, mockCtx);
        // called inside super
        inOrder.verify(configurator).recordSuperCalled();
        inOrder.verify(mockCtx).createAppenderBags();
    }
}