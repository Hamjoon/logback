package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_sanityCheck_Test {

    @InjectMocks
    private TestJoranConfigurator // will be spied in @BeforeEach
    configurator;

    /**
     * Concrete implementation of the abstract class used for testing.
     */
    private static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for test
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op for test
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for test
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // not needed for this test
            return null;
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No‑op for test
        }
    }

    /**
     * Minimal concrete {@link Model} implementation used as a test double.
     */
    private static class MockModel extends Model {

        private final String name;

        MockModel(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "MockModel{" + name + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        MockitoAnnotations.openMocks(this);
        // Create a fresh spy for each test to verify interactions on the protected method.
        configurator = spy(new TestJoranConfigurator());
    }

//     private static Stream<Arguments> normalFlowProvider() {
//         return Stream.of(Arguments.of(new MockModel("valid"), "non‑null model"), Arguments.of(null, "null model"));
//     }

    @Test
    void sanityCheck_delegatesToPerformCheckWithCorrectCheckerAndModel() {
        // GIVEN: a mock Model instance
        Model mockModel = mock(Model.class);
        // Stub performCheck so that the real implementation is not executed.
        // We only want to capture the arguments.
        doNothing().when(configurator).performCheck(any(SanityChecker.class), any(Model.class));
        // WHEN: invoking the method under test
        configurator.sanityCheck(mockModel);
        // THEN: verify that performCheck was called exactly once with the expected arguments
        ArgumentCaptor<SanityChecker> checkerCaptor = ArgumentCaptor.forClass(SanityChecker.class);
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(configurator, times(1)).performCheck(checkerCaptor.capture(), modelCaptor.capture());
        // The checker must be an instance of AppenderWithinAppenderSanityChecker
        assertTrue(checkerCaptor.getValue() instanceof AppenderWithinAppenderSanityChecker, "Expected an AppenderWithinAppenderSanityChecker instance");
        // The model passed to performCheck must be the same instance supplied to sanityCheck
        assertSame(mockModel, modelCaptor.getValue(), "The Model passed to performCheck should be the same as the one supplied to sanityCheck");
    }

    @Test
    void sanityCheck_propagatesRuntimeExceptionFromPerformCheck() {
        Model model = new MockModel("exception");
        RuntimeException expected = new RuntimeException("boom");
        // make performCheck throw
        doThrow(expected).when(configurator).performCheck(any(SanityChecker.class), eq(model));
        RuntimeException actual = assertThrows(RuntimeException.class, () -> configurator.sanityCheck(model), "sanityCheck should let RuntimeException from performCheck propagate");
        assertSame(expected, actual, "The thrown exception must be the same instance");
    }

    @Test
    void sanityCheck_propagatesIllegalArgumentExceptionFromPerformCheck() {
        Model model = new MockModel("illegalArg");
        IllegalArgumentException expected = new IllegalArgumentException("bad arg");
        doThrow(expected).when(configurator).performCheck(any(SanityChecker.class), eq(model));
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> configurator.sanityCheck(model), "sanityCheck should let IllegalArgumentException from performCheck propagate");
        assertSame(expected, actual);
    }

    @Test
    void sanityCheck_propagatesErrorFromPerformCheck() {
        Model model = new MockModel("error");
        AssertionError expected = new AssertionError("assertion failed");
        doThrow(expected).when(configurator).performCheck(any(SanityChecker.class), eq(model));
        AssertionError actual = assertThrows(AssertionError.class, () -> configurator.sanityCheck(model), "sanityCheck should let Errors from performCheck propagate");
        assertSame(expected, actual);
    }

    @Test
    void sanityCheck_createsDistinctSanityCheckerInstancesOnConsecutiveCalls() {
        Model firstModel = new MockModel("first");
        Model secondModel = new MockModel("second");
        // Stub performCheck to do nothing but allow argument capture
        doNothing().when(configurator).performCheck(any(SanityChecker.class), any(Model.class));
        // First call
        configurator.sanityCheck(firstModel);
        // Second call
        configurator.sanityCheck(secondModel);
        // Capture both calls
        ArgumentCaptor<SanityChecker> checkerCaptor = ArgumentCaptor.forClass(SanityChecker.class);
        verify(configurator, times(2)).performCheck(checkerCaptor.capture(), any(Model.class));
        SanityChecker firstChecker = checkerCaptor.getAllValues().get(0);
        SanityChecker secondChecker = checkerCaptor.getAllValues().get(1);
        assertTrue(firstChecker instanceof AppenderWithinAppenderSanityChecker, "First checker should be an AppenderWithinAppenderSanityChecker");
        assertTrue(secondChecker instanceof AppenderWithinAppenderSanityChecker, "Second checker should be an AppenderWithinAppenderSanityChecker");
        assertNotSame(firstChecker, secondChecker, "Each call must receive a distinct SanityChecker instance");
    }

    @Test
    @Disabled("Requires mockito-inline to mock constructor; skipped in pure Mockito environment")
    void sanityCheck_propagatesExceptionFromSanityCheckerConstructor() {
        // This test demonstrates the intended behaviour using Mockito's mockConstruction
        // (requires the mockito-inline artifact). If the environment does not support it,
        // the test is disabled.
        Model model = new MockModel("any");
        IllegalStateException ctorException = new IllegalStateException("ctor failed");
        try (MockedConstruction<AppenderWithinAppenderSanityChecker> mocked = mockConstruction(AppenderWithinAppenderSanityChecker.class, (mock, context) -> doThrow(ctorException).when(mock).check(any(Model.class)))) {
            // Stub performCheck to invoke the mock's check method, which will throw.
            doAnswer(invocation -> {
                SanityChecker sc = invocation.getArgument(0);
                Model m = invocation.getArgument(1);
                ((AppenderWithinAppenderSanityChecker) sc).check(m);
                return null;
            }).when(configurator).performCheck(any(SanityChecker.class), any(Model.class));
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> configurator.sanityCheck(model), "Exception from SanityChecker construction should propagate");
            assertSame(ctorException, thrown);
        }
    }
}