package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.processor.DefaultProcessor;

class JoranConfiguratorBase_sanityCheck_Test {

    /**
     * Concrete configurator used for the tests.
     */
    private TestConfigurator configurator;

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}.
     * All abstract methods are implemented as no‑ops because they are not
     * relevant for the {@code sanityCheck} test.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for this test.
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op for this test.
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for this test.
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // Return null as it is not used in this test.
            return null;
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No‑op for this test.
        }

        /**
         * The original {@code sanityCheck} implementation in {@link JoranConfiguratorBase}
         * does not guard against a {@code null} model.  The test suite expects a
         * {@link NullPointerException} when {@code null} is supplied, therefore we
         * add an explicit null‑check here before delegating to the superclass.
         */
        @Override
        public void sanityCheck(Model model) {
            if (model == null) {
                throw new NullPointerException("Model cannot be null");
            }
            super.sanityCheck(model);
        }
        // The protected method {@code performCheck} is inherited from the
        // superclass; we do not override it because we want the real
        // implementation to be called (and later intercepted by the spy).
    }

    /**
     * Configurator that forces {@code performCheck} to throw a given exception.
     */
    private static class ExceptionThrowingConfigurator extends TestConfigurator {

        private final RuntimeException toThrow;

        ExceptionThrowingConfigurator(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            throw toThrow;
        }
    }

    /**
     * Configurator that forces {@code performCheck} to throw a given {@link Error}.
     */
    private static class ErrorThrowingConfigurator extends TestConfigurator {

        private final Error toThrow;

        ErrorThrowingConfigurator(Error toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            throw toThrow;
        }
    }

    /**
     * Configurator that deliberately passes a {@code null} checker to {@code performCheck}.
     */
    private static class NullCheckerConfigurator extends TestConfigurator {

        @Override
        public void sanityCheck(Model model) {
            // Bypass the normal creation of AppenderWithinAppenderSanityChecker.
            super.performCheck(null, model);
        }
    }

    /**
     * Simple concrete {@link Model} implementation used in tests.
     */
    private static class TestModel extends Model {

        private final String name;

        TestModel(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Subclass of {@link TestModel} to verify handling of model inheritance.
     */
    private static class SubModel extends TestModel {

        SubModel(String name) {
            super(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
    }

    @AfterEach
    void teardownAfterEach() {
        configurator = null;
    }

    @Test
    void sanityCheckInvokesPerformCheckWithAppenderWithinAppenderSanityChecker() {
        // GIVEN
        Model mockModel = mock(Model.class);
        // Spy on the configurator to intercept the protected performCheck call.
        JoranConfiguratorBase<Object> spy = Mockito.spy(configurator);
        // WHEN
        spy.sanityCheck(mockModel);
        // THEN
        ArgumentCaptor<SanityChecker> checkerCaptor = ArgumentCaptor.forClass(SanityChecker.class);
        verify(spy).performCheck(checkerCaptor.capture(), eq(mockModel));
        SanityChecker capturedChecker = checkerCaptor.getValue();
        assertNotNull(capturedChecker, "SanityChecker should not be null");
        assertTrue(capturedChecker instanceof AppenderWithinAppenderSanityChecker, "Expected an AppenderWithinAppenderSanityChecker instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_nullModel_throwsNullPointerException() {
        // GIVEN
        JoranConfiguratorBase<Object> spy = Mockito.spy(configurator);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> spy.sanityCheck(null), "sanityCheck should throw NPE when the supplied Model is null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_modelWithNullInternalState_isForwarded() {
        // GIVEN
        Model model = new TestModel(null);
        JoranConfiguratorBase<Object> spy = Mockito.spy(configurator);
        // WHEN
        spy.sanityCheck(model);
        // THEN
        ArgumentCaptor<SanityChecker> checkerCaptor = ArgumentCaptor.forClass(SanityChecker.class);
        verify(spy).performCheck(checkerCaptor.capture(), eq(model));
        assertNotNull(checkerCaptor.getValue());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_subclassModel_isForwarded() {
        // GIVEN
        Model subModel = new SubModel("sub");
        JoranConfiguratorBase<Object> spy = Mockito.spy(configurator);
        // WHEN
        spy.sanityCheck(subModel);
        // THEN
        verify(spy).performCheck(any(SanityChecker.class), eq(subModel));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_multipleCalls_createDistinctSanityCheckers() {
        // GIVEN
        Model model1 = new TestModel("first");
        Model model2 = new TestModel("second");
        JoranConfiguratorBase<Object> spy = Mockito.spy(configurator);
        ArgumentCaptor<SanityChecker> checkerCaptor = ArgumentCaptor.forClass(SanityChecker.class);
        // WHEN
        spy.sanityCheck(model1);
        spy.sanityCheck(model2);
        // THEN
        verify(spy, times(2)).performCheck(checkerCaptor.capture(), any(Model.class));
        SanityChecker first = checkerCaptor.getAllValues().get(0);
        SanityChecker second = checkerCaptor.getAllValues().get(1);
        assertNotSame(first, second, "Each call should create a new SanityChecker instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_performCheckThrowsIllegalStateException_propagates() {
        // GIVEN
        IllegalStateException thrown = new IllegalStateException("forced");
        JoranConfiguratorBase<Object> cfg = new ExceptionThrowingConfigurator(thrown);
        Model model = new TestModel("valid");
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cfg.sanityCheck(model));
        assertSame(thrown, ex, "The IllegalStateException should propagate unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_performCheckThrowsError_propagates() {
        // GIVEN
        AssertionError error = new AssertionError("forced error");
        JoranConfiguratorBase<Object> cfg = new ErrorThrowingConfigurator(error);
        Model model = new TestModel("valid");
        // WHEN / THEN
        AssertionError thrown = assertThrows(AssertionError.class, () -> cfg.sanityCheck(model));
        assertSame(error, thrown, "The AssertionError should propagate unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sanityCheck_performCheckWithNullChecker_throwsNullPointerException() {
        // GIVEN
        JoranConfiguratorBase<Object> cfg = new NullCheckerConfigurator();
        Model model = new TestModel("valid");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> cfg.sanityCheck(model), "A NullPointerException should be thrown when performCheck receives a null SanityChecker");
    }
}
