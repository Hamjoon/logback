package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_sanityCheck_Test {

    /**
     * Minimal {@link Model} implementation that carries a simple identifier.
     */
    private static class TestModel extends Model {

        private final String id;

        TestModel(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }
    }

    /**
     * Concrete subclass of {@link JoranConfiguratorBase} that records the
     * arguments received by {@code performCheck} and can be instructed to
     * throw specific exceptions based on the supplied {@link TestModel}.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        /**
         * Captured sanity‑checker instance from {@code performCheck}.
         */
        SanityChecker capturedSanityChecker;

        /**
         * Captured model instance from {@code performCheck}.
         */
        Model capturedModel;

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // Record arguments for later assertions.
            this.capturedSanityChecker = sc;
            this.capturedModel = model;
            // Simulate various failure scenarios based on the model content.
            if (model == null) {
                // The contract of the real implementation is to reject null.
                throw new NullPointerException("Model must not be null");
            }
            if (model instanceof TestModel) {
                String id = ((TestModel) model).getId();
                switch(id) {
                    case "triggerException":
                        throw new IllegalStateException("Sanity check failed");
                    case "runtimeException":
                        throw new NullPointerException("Simulated NPE");
                    case "illegalArgument":
                        throw new IllegalArgumentException("Simulated IAE");
                    default:
                }
            }
        }

        // -----------------------------------------------------------------
        // Stub implementations of the remaining abstract members.
        // They are irrelevant for the current tests.
        // -----------------------------------------------------------------
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
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op
        }
    }

    @Test
    void sanityCheck_invokesPerformCheckWithCorrectArguments() {
        // GIVEN
        Model dummyModel = new TestModel("dummy");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN
        configurator.sanityCheck(dummyModel);
        // THEN
        assertNotNull(configurator.capturedSanityChecker, "performCheck should have been called with a non‑null SanityChecker");
        assertTrue(configurator.capturedSanityChecker instanceof AppenderWithinAppenderSanityChecker, "SanityChecker should be an instance of AppenderWithinAppenderSanityChecker");
        assertSame(dummyModel, configurator.capturedModel, "The Model passed to sanityCheck must be forwarded unchanged to performCheck");
    }

    @Test
    @DisplayName("sanityCheck forwards the provided Model and creates the correct SanityChecker")
    void sanityCheck_argumentForwarding() {
        // GIVEN
        TestModel model = new TestModel("valid");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN
        configurator.sanityCheck(model);
        // THEN
        assertNotNull(configurator.capturedSanityChecker, "performCheck should have been called with a non‑null SanityChecker");
        assertTrue(configurator.capturedSanityChecker instanceof AppenderWithinAppenderSanityChecker, "SanityChecker must be an instance of AppenderWithinAppenderSanityChecker");
        // Exact class check – the implementation should not be a different subclass.
        assertEquals(AppenderWithinAppenderSanityChecker.class, configurator.capturedSanityChecker.getClass(), "SanityChecker class should be exactly AppenderWithinAppenderSanityChecker");
        assertSame(model, configurator.capturedModel, "The Model passed to sanityCheck must be forwarded unchanged to performCheck");
    }

    @Test
    @DisplayName("sanityCheck throws NullPointerException when a null Model is supplied")
    void sanityCheck_nullModelHandling() {
        // GIVEN
        TestConfigurator configurator = new TestConfigurator();
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> configurator.sanityCheck(null), "sanityCheck should propagate NullPointerException for null Model");
        assertEquals("Model must not be null", npe.getMessage());
    }

    @Test
    @DisplayName("sanityCheck propagates IllegalStateException from the checker")
    void sanityCheck_exceptionPropagation_illegalState() {
        // GIVEN
        TestModel model = new TestModel("triggerException");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN / THEN
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> configurator.sanityCheck(model), "sanityCheck must let IllegalStateException bubble up unchanged");
        assertEquals("Sanity check failed", ise.getMessage());
    }

    @Test
    @DisplayName("sanityCheck propagates NullPointerException (runtime) from the checker")
    void sanityCheck_exceptionPropagation_runtimeNPE() {
        // GIVEN
        TestModel model = new TestModel("runtimeException");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> configurator.sanityCheck(model), "sanityCheck must let runtime NullPointerException bubble up unchanged");
        assertEquals("Simulated NPE", npe.getMessage());
    }

    @Test
    @DisplayName("sanityCheck propagates IllegalArgumentException from the checker")
    void sanityCheck_exceptionPropagation_illegalArgument() {
        // GIVEN
        TestModel model = new TestModel("illegalArgument");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN / THEN
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> configurator.sanityCheck(model), "sanityCheck must let IllegalArgumentException bubble up unchanged");
        assertEquals("Simulated IAE", iae.getMessage());
    }

    @Test
    @DisplayName("sanityCheck creates exactly an AppenderWithinAppenderSanityChecker instance")
    void sanityCheck_checkerInstanceTypeVerification() {
        // GIVEN
        TestModel model = new TestModel("valid");
        TestConfigurator configurator = new TestConfigurator();
        // WHEN
        configurator.sanityCheck(model);
        // THEN
        assertNotNull(configurator.capturedSanityChecker, "SanityChecker must not be null");
        // The requirement allows subclasses, but we also verify the exact type.
        assertEquals(AppenderWithinAppenderSanityChecker.class, configurator.capturedSanityChecker.getClass(), "The SanityChecker created by sanityCheck should be AppenderWithinAppenderSanityChecker");
    }
}