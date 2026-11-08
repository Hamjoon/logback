package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.RuleStore;
import java.util.Collections;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_setImplicitRuleSupplier_Test {

    private TestConfigurator configurator;

    /**
     * Concrete configurator used only for exercising the protected method.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        // The abstract methods from the real hierarchy are given empty implementations
        // because they are irrelevant for this test.
        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op
        }

        @Override
        protected void addModelHandlerAssociations(ch.qos.logback.core.model.processor.DefaultProcessor defaultProcessor) {
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
        protected void performCheck(ch.qos.logback.core.joran.sanity.SanityChecker sc, ch.qos.logback.core.model.Model model) {
            // no‑op
        }

        @Override
        protected void sanityCheck(ch.qos.logback.core.model.Model topModel) {
            // no‑op
        }

        /**
         * Exposes the protected method for the test.
         */
        void invokeSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            setImplicitRuleSupplier(interpreter);
        }
    }

    /**
     * Minimal concrete implementation of {@link SaxEventInterpreter} that stores the supplied
     * {@link Supplier}.
     */
    private static class TestInterpreter extends SaxEventInterpreter {

        private Supplier<Action> implicitActionSupplier;

        TestInterpreter() {
            // The real constructor expects a Context, RuleStore, ElementPath and a List<SaxEvent>.
            // For the purpose of this unit test we can safely pass nulls / empty list.
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            this.implicitActionSupplier = supplier;
        }

        Supplier<Action> getImplicitActionSupplier() {
            return implicitActionSupplier;
        }
    }

    /**
     * Same as {@link TestInterpreter} but used to emphasise polymorphic handling.
     */
    private static class CustomInterpreter extends TestInterpreter {
    }

    /**
     * Interpreter that deliberately throws an exception when {@code setImplicitActionSupplier}
     * is invoked.
     */
    private static class ThrowingInterpreter extends SaxEventInterpreter {

        ThrowingInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> supplier) {
            throw new UnsupportedOperationException("setImplicitActionSupplier is not supported");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
    }

//     @Test
//     void setImplicitRuleSupplier_registersImplicitModelActionSupplier(@Mock SaxEventInterpreter interpreter) {
//         // GIVEN
//         TestInterpreter interpreter = new TestInterpreter();
//         // WHEN – invoke the method under test
//         configurator.invokeSetImplicitRuleSupplier(interpreter);
//         // THEN – verify that the interpreter received a supplier that creates ImplicitModelAction
//         Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
//         assertNotNull(supplier, "Supplier should not be null");
//         Action produced = supplier.get();
//         assertTrue(produced instanceof ImplicitModelAction, "Supplier must create an ImplicitModelAction instance");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_withConcreteInterpreter_storesSupplier() {
        // GIVEN
        TestInterpreter interpreter = new TestInterpreter();
        // WHEN
        configurator.invokeSetImplicitRuleSupplier(interpreter);
        // THEN
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull(supplier, "Interpreter must have a non‑null implicit action supplier");
        assertTrue(supplier.get() instanceof ImplicitModelAction, "Supplier must create an ImplicitModelAction instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_nullInterpreter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> configurator.invokeSetImplicitRuleSupplier(null), "Passing null should result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_withSubclassInterpreter_storesSupplier() {
        // GIVEN
        CustomInterpreter interpreter = new CustomInterpreter();
        // WHEN
        configurator.invokeSetImplicitRuleSupplier(interpreter);
        // THEN
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull(supplier, "Custom interpreter must receive a non‑null supplier");
        assertTrue(supplier.get() instanceof ImplicitModelAction, "Supplier must create an ImplicitModelAction instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void setImplicitRuleSupplier_interpreterThrowsException_propagates() {
        ThrowingInterpreter interpreter = new ThrowingInterpreter();
        assertThrows(UnsupportedOperationException.class, () -> configurator.invokeSetImplicitRuleSupplier(interpreter), "Exception from interpreter should propagate unchanged");
    }
}