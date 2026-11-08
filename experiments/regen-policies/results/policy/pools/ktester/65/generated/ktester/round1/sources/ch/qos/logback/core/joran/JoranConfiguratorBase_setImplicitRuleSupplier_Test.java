package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.ElementPath;
import java.util.List;
import java.util.Collections;
import ch.qos.logback.core.joran.event.SaxEvent;
// import ch.qos.logback.core.joran.spi.InterpretationContext;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_setImplicitRuleSupplier_Test {

    private TestConfigurator configurator;

    /**
     * Minimal concrete subclass so we can instantiate the abstract base class.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op for test
        }

        @Override
        protected void addModelHandlerAssociations(ch.qos.logback.core.model.processor.DefaultProcessor defaultProcessor) {
            // no‑op for test
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op for test
        }

        @Override
        public ch.qos.logback.core.joran.spi.SaxEventInterpretationContext getInterpretationContext() {
            // not needed for this test
            return null;
        }

        @Override
        protected void performCheck(ch.qos.logback.core.joran.sanity.SanityChecker sc, ch.qos.logback.core.model.Model model) {
            // no‑op for test
        }

        @Override
        protected void sanityCheck(ch.qos.logback.core.model.Model topModel) {
            // no‑op for test
        }
    }

    /**
     * Simple concrete {@link Action} used for dummy supplier tests.
     */
    private static class DummyAction extends Action {

//         @Override
//         public void begin(InterpretationContext ic, String name, org.xml.sax.Attributes a) {
//             // no‑op
//         }

//         @Override
//         public void end(InterpretationContext ic, String name) {
//             // no‑op
//         }
    }

    /**
     * Minimal stub of {@link SaxEventInterpreter} that stores a supplier.
     */
    private static class TestSaxEventInterpreter extends SaxEventInterpreter {

        private Supplier<Action> implicitSupplier;

        TestSaxEventInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            this.implicitSupplier = supplier;
        }

        public Supplier<Action> getImplicitActionSupplier() {
            return implicitSupplier;
        }
    }

    /**
     * Interpreter that throws a {@link RuntimeException} when the supplier is set.
     */
    private static class ThrowingSaxEventInterpreter extends SaxEventInterpreter {

        ThrowingSaxEventInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            throw new RuntimeException("forced RuntimeException");
        }
    }

    /**
     * Interpreter that forbids overwriting an existing supplier and throws {@link IllegalStateException}.
     */
    private static class NoOverwriteSaxEventInterpreter extends SaxEventInterpreter {

        private Supplier<Action> implicitSupplier;

        NoOverwriteSaxEventInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            if (implicitSupplier != null) {
                throw new IllegalStateException("overwriting not allowed");
            }
            this.implicitSupplier = supplier;
        }

        public Supplier<Action> getImplicitActionSupplier() {
            return implicitSupplier;
        }
    }

    /**
     * Interpreter that throws an {@link AssertionError} (subclass of {@link Error}) when setting the supplier.
     */
    private static class ErrorThrowingSaxEventInterpreter extends SaxEventInterpreter {

        ErrorThrowingSaxEventInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            throw new AssertionError("forced AssertionError");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
    }

    @Test
    void setImplicitRuleSupplier_registersImplicitModelActionSupplier() {
        // Arrange
        SaxEventInterpreter interpreter = mock(SaxEventInterpreter.class);
        // Act
        configurator.setImplicitRuleSupplier(interpreter);
        // Assert – verify that the interpreter received a supplier that creates ImplicitModelAction
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Supplier<Action>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(interpreter, times(1)).setImplicitActionSupplier(captor.capture());
        Supplier<Action> supplied = captor.getValue();
        assertNotNull(supplied, "Supplier should not be null");
        Action actionInstance = supplied.get();
        assertTrue(actionInstance instanceof ImplicitModelAction, "Supplier should produce an instance of ImplicitModelAction");
    }

    @Test
    void setImplicitRuleSupplier_onRealInterpreter_returnsNonNullSupplierCreatingImplicitModelAction() {
        // Arrange
        TestSaxEventInterpreter interpreter = new TestSaxEventInterpreter();
        // Act
        configurator.setImplicitRuleSupplier(interpreter);
        // Assert
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull(supplier, "Interpreter must hold a non‑null supplier after configuration");
        Action action = supplier.get();
        assertTrue(action instanceof ImplicitModelAction, "Supplier must create an ImplicitModelAction instance");
    }

    @Test
    void setImplicitRuleSupplier_nullInterpreter_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> configurator.setImplicitRuleSupplier(null), "Passing a null interpreter should result in NullPointerException");
    }

    @Test
    void setImplicitRuleSupplier_overwritesExistingSupplier() {
        // Arrange: interpreter already returns a DummyAction supplier
        TestSaxEventInterpreter interpreter = new TestSaxEventInterpreter();
        interpreter.setImplicitActionSupplier(() -> new DummyAction());
        // Act
        configurator.setImplicitRuleSupplier(interpreter);
        // Assert – the supplier must now produce ImplicitModelAction, not DummyAction
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull(supplier, "Supplier should still be present after overwrite");
        Action action = supplier.get();
        assertTrue(action instanceof ImplicitModelAction, "Existing supplier must be overwritten with one that creates ImplicitModelAction");
    }

    @Test
    void setImplicitRuleSupplier_throwsRuntimeExceptionFromInterpreter() {
        // Arrange
        ThrowingSaxEventInterpreter interpreter = new ThrowingSaxEventInterpreter();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.setImplicitRuleSupplier(interpreter), "RuntimeException from interpreter must propagate");
        assertEquals("forced RuntimeException", ex.getMessage());
    }

    @Test
    void setImplicitRuleSupplier_throwsIllegalStateExceptionWhenOverwriteForbidden() {
        // Arrange
        NoOverwriteSaxEventInterpreter interpreter = new NoOverwriteSaxEventInterpreter();
        // pre‑set a supplier to trigger the illegal‑state condition
        interpreter.setImplicitActionSupplier(() -> new DummyAction());
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configurator.setImplicitRuleSupplier(interpreter), "IllegalStateException should propagate when interpreter forbids overwriting");
        assertEquals("overwriting not allowed", ex.getMessage());
    }

    @Test
    void setImplicitRuleSupplier_propagatesErrorFromInterpreter() {
        // Arrange
        ErrorThrowingSaxEventInterpreter interpreter = new ErrorThrowingSaxEventInterpreter();
        // Act & Assert
        AssertionError error = assertThrows(AssertionError.class, () -> configurator.setImplicitRuleSupplier(interpreter), "Error thrown by interpreter must propagate unchanged");
        assertEquals("forced AssertionError", error.getMessage());
    }
}