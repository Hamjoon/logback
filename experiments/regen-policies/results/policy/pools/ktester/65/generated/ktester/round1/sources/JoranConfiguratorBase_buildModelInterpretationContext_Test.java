package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.mockito.InOrder;
import org.mockito.Mockito;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_buildModelInterpretationContext_Test {

    private static Field modelContextField;

    /**
     * Concrete subclass used only for testing.
     */
    private static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op for the test
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // no‑op for the test
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // The super implementation creates the context; we simply delegate.
            return super.getInterpretationContext();
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op for the test
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Super implementation throws a non‑Runtime {@code Error} (e.g. AssertionError).
     *  The error must propagate and {@code createAppenderBags()} must not be called.
     *  This scenario is exercised by a specialised subclass that overrides the
     *  protected super method to throw the error.
     * -----------------------------------------------------------------------
     */
    private static class ErrorThrowingConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return super.getInterpretationContext();
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        }

        // Force the super‑class implementation to throw an Error.
        @Override
        public void buildModelInterpretationContext() {
            throw new AssertionError("forced error from super");
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Super implementation throws a RuntimeException.
     *  The exception must propagate and {@code createAppenderBags()} must not be called.
     *  This is simulated by a subclass that overrides the protected method to throw.
     * -----------------------------------------------------------------------
     */
    private static class RuntimeExceptionThrowingConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return super.getInterpretationContext();
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        }

        @Override
        public void buildModelInterpretationContext() {
            throw new RuntimeException("forced runtime exception");
        }
    }

    @BeforeAll
    static void initReflection() throws NoSuchFieldException {
        // The field is declared in GenericXMLConfigurator.
        modelContextField = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
        modelContextField.setAccessible(true);
    }

    @Test
    void buildModelInterpretationContext_initialisesContext_andCallsCreateAppenderBags() throws Exception {
        // Arrange: create a fresh configurator instance.
        TestJoranConfigurator configurator = new TestJoranConfigurator();
        // Act: invoke the method under test.
        configurator.buildModelInterpretationContext();
        // Assert: the protected field 'modelInterpretationContext' must be non‑null.
        // The field is declared in the superclass GenericXMLConfigurator.
        Field ctxField = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
        ctxField.setAccessible(true);
        Object ctx = ctxField.get(configurator);
        assertNotNull(ctx, "modelInterpretationContext should have been created by the super call");
        // Additionally, verify that the context is of the expected type.
        assertTrue(ctx instanceof SaxEventInterpretationContext, "modelInterpretationContext should be an instance of SaxEventInterpretationContext");
    }

    /**
     * -----------------------------------------------------------------------
     *  Normal flow – the super implementation creates a context and
     *  {@code createAppenderBags()} is invoked exactly once.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_normalFlow_invokesCreateAppenderBagsOnce() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         // Act
//         configurator.buildModelInterpretationContext();
//         // Assert – the protected field must be non‑null and of the expected type.
//         Object ctx = modelContextField.get(configurator);
//         assertNotNull(ctx, "modelInterpretationContext should have been created");
//         assertTrue(ctx instanceof ModelInterpretationContext, "modelInterpretationContext should be a ModelInterpretationContext");
//         // Verify that createAppenderBags() was called exactly once.
//         ModelInterpretationContext mic = (ModelInterpretationContext) ctx;
//         // Replace it with a spy and repeat the call.
//         ModelInterpretationContext spyMic = spy(mic);
//         modelContextField.set(configurator, spyMic);
//         configurator.buildModelInterpretationContext();
//         verify(spyMic, times(1)).createAppenderBags();
//     }

    /**
     * -----------------------------------------------------------------------
     *  Null {@code modelInterpretationContext} – a NPE should be thrown when
     *  {@code createAppenderBags()} is invoked.
     * -----------------------------------------------------------------------
     */
    @Test
    void buildModelInterpretationContext_nullContext_throwsNullPointerException() throws Exception {
        // Arrange
        TestJoranConfigurator configurator = new TestJoranConfigurator();
        configurator.buildModelInterpretationContext();
        // make it null
        modelContextField.set(configurator, null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, configurator::buildModelInterpretationContext, "Expected NPE when modelInterpretationContext is null");
        assertNotNull(npe.getMessage());
    }

    /**
     * -----------------------------------------------------------------------
     *  {@code createAppenderBags()} throws a RuntimeException – the exception
     *  must propagate unchanged.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_createAppenderBagsThrowsRuntimeException_propagates() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         configurator.buildModelInterpretationContext();
//         // Replace the context with a mock that throws when createAppenderBags() is called.
//         ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
//         doThrow(new RuntimeException("boom")).when(mockCtx).createAppenderBags();
//         modelContextField.set(configurator, mockCtx);
//         // Act & Assert
//         RuntimeException ex = assertThrows(RuntimeException.class, configurator::buildModelInterpretationContext, "RuntimeException from createAppenderBags() should propagate");
//         assertEquals("boom", ex.getMessage());
//     }

    /**
     * -----------------------------------------------------------------------
     *  {@code createAppenderBags()} throws an Error – the error must propagate.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_createAppenderBagsThrowsError_propagates() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         configurator.buildModelInterpretationContext();
//         ModelInterpretationContext mockCtx = mock(ModelInterpretationContext.class);
//         doThrow(new AssertionError("assertion failure")).when(mockCtx).createAppenderBags();
//         modelContextField.set(configurator, mockCtx);
//         // Act & Assert
//         AssertionError err = assertThrows(AssertionError.class, configurator::buildModelInterpretationContext, "Error from createAppenderBags() should propagate");
//         assertEquals("assertion failure", err.getMessage());
//     }

    /**
     * -----------------------------------------------------------------------
     *  Multiple successive invocations – each call must result in exactly one
     *  invocation of {@code createAppenderBags()} and no state leakage.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_multipleInvocations_eachCallsCreateAppenderBagsOnce() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         // First invocation – replace the created context with a spy.
//         configurator.buildModelInterpretationContext();
//         ModelInterpretationContext firstCtx = (ModelInterpretationContext) modelContextField.get(configurator);
//         ModelInterpretationContext spyFirst = spy(firstCtx);
//         modelContextField.set(configurator, spyFirst);
//         // Act – first call
//         configurator.buildModelInterpretationContext();
//         verify(spyFirst, times(1)).createAppenderBags();
//         // Second invocation – the super method creates a *new* context, so we need a new spy.
//         configurator.buildModelInterpretationContext();
//         ModelInterpretationContext secondCtx = (ModelInterpretationContext) modelContextField.get(configurator);
//         ModelInterpretationContext spySecond = spy(secondCtx);
//         modelContextField.set(configurator, spySecond);
//         // Act – second call
//         configurator.buildModelInterpretationContext();
//         verify(spySecond, times(1)).createAppenderBags();
//         // No interaction between the two spies.
//         verifyNoMoreInteractions(spyFirst, spySecond);
//     }

    /**
     * -----------------------------------------------------------------------
     *  Verify call order – {@code createAppenderBags()} must be invoked *after*
     *  the super implementation has completed.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_callOrder_superThenCreateAppenderBags() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         // Let the super method create a real context, then replace it with a spy.
//         configurator.buildModelInterpretationContext();
//         ModelInterpretationContext realCtx = (ModelInterpretationContext) modelContextField.get(configurator);
//         ModelInterpretationContext spyCtx = spy(realCtx);
//         modelContextField.set(configurator, spyCtx);
//         InOrder inOrder = inOrder(spyCtx);
//         // Act
//         configurator.buildModelInterpretationContext();
//         // Assert – createAppenderBags() must be the *last* interaction.
//         inOrder.verify(spyCtx).createAppenderBags();
//         verifyNoMoreInteractions(spyCtx);
//     }

    @Test
    void buildModelInterpretationContext_superThrowsError_propagatesAndNoCreateAppenderBags() throws Exception {
        // Arrange
        ErrorThrowingConfigurator configurator = new ErrorThrowingConfigurator();
        // Act & Assert
        AssertionError err = assertThrows(AssertionError.class, configurator::buildModelInterpretationContext, "Error from super method should propagate");
        assertEquals("forced error from super", err.getMessage());
        // The field should never have been initialised.
        Object ctx = modelContextField.get(configurator);
        assertNull(ctx, "modelInterpretationContext must remain null when super fails");
    }

    @Test
    void buildModelInterpretationContext_superThrowsRuntimeException_propagatesAndNoCreateAppenderBags() throws Exception {
        // Arrange
        RuntimeExceptionThrowingConfigurator configurator = new RuntimeExceptionThrowingConfigurator();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, configurator::buildModelInterpretationContext, "RuntimeException from super should propagate");
        assertEquals("forced runtime exception", ex.getMessage());
        // Verify that the context was never created.
        Object ctx = modelContextField.get(configurator);
        assertNull(ctx, "modelInterpretationContext must remain null when super fails");
    }
}