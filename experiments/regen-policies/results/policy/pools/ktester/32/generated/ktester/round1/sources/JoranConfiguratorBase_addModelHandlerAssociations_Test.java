package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.sift.SiftModel;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addModelHandlerAssociations_Test {

    @Mock
    private DefaultProcessor // Mockito mock of the processor
    mockProcessor;

    private TestJoranConfiguratorBase configurator;

    /**
     * Concrete subclass that only supplies the abstract members of {@link JoranConfiguratorBase}.
     */
    private static class TestJoranConfiguratorBase extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op – not needed for this test
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op – not needed for this test
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // not used in this test
            return null;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // no‑op – not needed for this test
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // no‑op – not needed for this test
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op – not needed for this test
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestJoranConfiguratorBase();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated
        reset(mockProcessor);
    }

    @Test
    void addModelHandlerAssociations_registersAllSupportedHandlers() {
        // WHEN
        configurator.addModelHandlerAssociations(mockProcessor);
        // THEN – verify that a handler is added for each model class
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        verify(mockProcessor).addHandler(eq(ParamModel.class), any());
        verify(mockProcessor).addHandler(eq(PropertyModel.class), any());
        verify(mockProcessor).addHandler(eq(TimestampModel.class), any());
        verify(mockProcessor).addHandler(eq(StatusListenerModel.class), any());
        verify(mockProcessor).addHandler(eq(ImplicitModel.class), any());
        verify(mockProcessor).addHandler(eq(IfModel.class), any());
        verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        verify(mockProcessor).addHandler(eq(ElseModel.class), any());
        verify(mockProcessor).addHandler(eq(SiftModel.class), any());
        // Ensure no unexpected interactions occurred
        verifyNoMoreInteractions(mockProcessor);
    }

    /* --------------------------------------------------------------
     *  Null processor – should throw NPE
     * -------------------------------------------------------------- */
    @Test
    void addModelHandlerAssociations_nullProcessor_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> configurator.addModelHandlerAssociations(null), "Calling addModelHandlerAssociations with a null processor must throw NPE");
    }

    /* --------------------------------------------------------------
     *  Exception on first add (ImportModel)
     * -------------------------------------------------------------- */
    @Test
    void addModelHandlerAssociations_exceptionOnFirstAdd_propagatesException() {
        // Simulate RuntimeException when handling ImportModel
        doThrow(new RuntimeException("ImportModel failure")).when(mockProcessor).addHandler(eq(ImportModel.class), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("ImportModel failure", thrown.getMessage());
        // Verify that only the first call was attempted
        InOrder inOrder = inOrder(mockProcessor);
        inOrder.verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verifyNoMoreInteractions(mockProcessor);
    }

    /* --------------------------------------------------------------
     *  Exception on a middle add (ThenModel)
     * -------------------------------------------------------------- */
    @Test
    void addModelHandlerAssociations_exceptionOnMiddleAdd_propagatesException() {
        // Throw on ThenModel registration; earlier calls succeed
        doThrow(new RuntimeException("ThenModel failure")).when(mockProcessor).addHandler(eq(ThenModel.class), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("ThenModel failure", thrown.getMessage());
        // Verify the sequence up to the failing call
        InOrder inOrder = inOrder(mockProcessor);
        inOrder.verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ParamModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(PropertyModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(TimestampModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(StatusListenerModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ImplicitModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IfModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        // No further interactions after the exception
        verifyNoMoreInteractions(mockProcessor);
    }

    /* --------------------------------------------------------------
     *  Exception on last add (SiftModel)
     * -------------------------------------------------------------- */
    @Test
    void addModelHandlerAssociations_exceptionOnLastAdd_propagatesException() {
        // Throw on SiftModel registration; all previous registrations succeed
        doThrow(new RuntimeException("SiftModel failure")).when(mockProcessor).addHandler(eq(SiftModel.class), any());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("SiftModel failure", thrown.getMessage());
        // Verify that every preceding handler was added before the failure
        InOrder inOrder = inOrder(mockProcessor);
        inOrder.verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ParamModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(PropertyModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(TimestampModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(StatusListenerModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ImplicitModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IfModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ElseModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(SiftModel.class), any());
        // No further interactions after the exception
        verifyNoMoreInteractions(mockProcessor);
    }
}
