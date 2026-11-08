package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.action.Action;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_setImplicitRuleSupplier_Test {

    /**
     * Concrete subclass that provides no‑op implementations for the abstract members.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // no‑op
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return null;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // no‑op
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // no‑op
        }
    }

    @Test
    void setImplicitRuleSupplier_delegatesToInterpreter() {
        // Arrange
        SaxEventInterpreter interpreter = mock(SaxEventInterpreter.class);
        TestConfigurator configurator = new TestConfigurator();
        // Act
        configurator.setImplicitRuleSupplier(interpreter);
        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Supplier<Action>> captor = (ArgumentCaptor<Supplier<Action>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass((Class) Supplier.class);
        verify(interpreter, times(1)).setImplicitActionSupplier(captor.capture());
        Supplier<Action> rawSupplier = captor.getValue();
        @SuppressWarnings("unchecked")
        Supplier<ImplicitModelAction> supplier = () -> (ImplicitModelAction) rawSupplier.get();
        assertNotNull(supplier, "Supplier passed to interpreter must not be null");
        // The supplier should create an ImplicitModelAction instance.
        ImplicitModelAction action = supplier.get();
        assertNotNull(action, "Supplier must produce a non‑null action");
        assertTrue(action instanceof ImplicitModelAction, "Supplier must produce an instance of ImplicitModelAction");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_nullInterpreter_throwsNPE() {
        TestConfigurator configurator = new TestConfigurator();
        // The method is expected to NPE because it dereferences the interpreter.
        assertThrows(NullPointerException.class, () -> configurator.setImplicitRuleSupplier(null), "Calling setImplicitRuleSupplier with null should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_supplierCreatesDistinctInstances() {
        // Arrange
        SaxEventInterpreter interpreter = mock(SaxEventInterpreter.class);
        TestConfigurator configurator = new TestConfigurator();
        // Act
        configurator.setImplicitRuleSupplier(interpreter);
        // Capture the supplier that the configurator passed to the interpreter.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Supplier<Action>> captor = (ArgumentCaptor<Supplier<Action>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass((Class) Supplier.class);
        verify(interpreter).setImplicitActionSupplier(captor.capture());
        Supplier<Action> rawSupplier = captor.getValue();
        @SuppressWarnings("unchecked")
        Supplier<ImplicitModelAction> supplier = () -> (ImplicitModelAction) rawSupplier.get();
        // Invoke the supplier twice.
        ImplicitModelAction first = supplier.get();
        ImplicitModelAction second = supplier.get();
        // Assert
        assertNotNull(first, "First supplied action must not be null");
        assertNotNull(second, "Second supplied action must not be null");
        assertNotSame(first, second, "Each supplier invocation should return a new instance");
        assertTrue(first instanceof ImplicitModelAction && second instanceof ImplicitModelAction, "Both objects must be instances of ImplicitModelAction");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_interpreterThrowsRuntimeException_propagates() {
        // Arrange
        SaxEventInterpreter interpreter = mock(SaxEventInterpreter.class);
        doThrow(new RuntimeException("boom")).when(interpreter).setImplicitActionSupplier(any());
        TestConfigurator configurator = new TestConfigurator();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.setImplicitRuleSupplier(interpreter), "Exception from interpreter.setImplicitActionSupplier should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_invokedMultipleTimes_callsInterpreterEachTime() {
        // Arrange
        SaxEventInterpreter interpreter = mock(SaxEventInterpreter.class);
        TestConfigurator configurator = new TestConfigurator();
        // Act – call the method twice.
        configurator.setImplicitRuleSupplier(interpreter);
        configurator.setImplicitRuleSupplier(interpreter);
        // Assert – verify that setImplicitActionSupplier was invoked twice.
        verify(interpreter, times(2)).setImplicitActionSupplier(any());
    }
}