import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import ch.qos.logback.core.joran.spi.DefaultProcessor;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
// import ch.qos.logback.core.joran.spi.SanityChecker;
import ch.qos.logback.core.model.Model;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_getInterpretationContext_Test {

    /**
     * Mocked interpreter that will be injected into the configurator.
     */
    @Mock
    private SaxEventInterpreter mockInterpreter;

    /**
     * Mocked interpretation context that the interpreter will return.
     */
    @Mock
    private SaxEventInterpretationContext mockInterpretationContext;

    /**
     * Concrete instance JoranConfiguratorBase_getInterpretationContext_Testconfigurator used for the test.
     */
    private TestConfigurator configurator;

//     class CustomContext extends SaxEventInterpretationContext {
    }

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}
     * required for testing. All abstract methods are given no‑op implementations
     * because they are irrelevant for the {@code getInterpretationContext()} test.
     */
//     private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        TestConfigurator(SaxEventInterpreter interpreter) {
            // The field {@code saxEventInterpreter} is protected in the superclass,
            // so we can assign it directly.
//             this.saxEventInterpreter = interpreter;
        }

//         @Override
//         protected void addElementSelectorAndActionAssociations(RuleStore rs) {
//             // No‑op for this test.
//         }

//         @Override
//         protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
//             // No‑op for this test.
//         }

//         @Override
//         public void buildModelInterpretationContext() {
//             // No‑op for this test.
//         }

//         @Override
//         protected void performCheck(SanityChecker sc, Model model) {
//             // No‑op for this test.
//         }

//         @Override
//         protected void sanityCheck(Model topModel) {
//             // No‑op for this test.
//         }

//         @Override
//         protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
//             // No‑op for this test.
//         }
    }

    /**
     * Configurator subclass that deliberately leaves {@code saxEventInterpreter}
     * unset to trigger a {@link NullPointerException} when {@code getInterpretationContext()}
     * is called.
     */
//     private static class NoInterpreterConfigurator extends JoranConfiguratorBase<Object> {

        NoInterpreterConfigurator() {
            // Do NOT assign saxEventInterpreter.
        }

//         @Override
//         protected void addElementSelectorAndActionAssociations(RuleStore rs) {
//             // No‑op.
//         }

//         @Override
//         protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
//             // No‑op.
//         }

//         @Override
//         public void buildModelInterpretationContext() {
//             // No‑op.
//         }

//         @Override
//         protected void performCheck(SanityChecker sc, Model model) {
//             // No‑op.
//         }

//         @Override
//         protected void sanityCheck(Model topModel) {
//             // No‑op.
//         }

//         @Override
//         protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
//             // No‑op.
//         }
    }

//     class CustomContext extends SaxEventInterpretationContext {
    }

    // No additional behaviour required for the test.
    @BeforeEach
    void setupBeforeEach() {
        // Define the behaviour of the mocked interpreter.
        when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(mockInterpretationContext);
        // Create a configurator that uses the mocked interpreter.
        configurator = new TestConfigurator(mockInterpreter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid interaction leakage between tests.
        reset(mockInterpreter, mockInterpretationContext);
    }

//     @Test
//     void getInterpretationContext_ShouldReturnContextFromInterpreter() {
//         // WHEN
//         SaxEventInterpretationContext result = configurator.getInterpretationContext();
//         // THEN
//         assertSame(mockInterpretationContext, result, "The returned interpretation context must be the one supplied by the SaxEventInterpreter.");
//         // Verify that the configurator indeed called the interpreter.
//         verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
//     }

    /* --------------------------------------------------------------
    Null handling – interpreter returns null.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_WhenInterpreterReturnsNull_ShouldPropagateNull() {
//         when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(null);
//         SaxEventInterpretationContext result = configurator.getInterpretationContext();
//         assertNull(result, "A null return from the interpreter must be propagated unchanged.");
//         verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
//     }

    /* --------------------------------------------------------------
    Idempotence – repeated calls return the same reference.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_RepeatedCalls_ShouldReturnSameInstance() {
//         SaxEventInterpretationContext first = configurator.getInterpretationContext();
//         SaxEventInterpretationContext second = configurator.getInterpretationContext();
//         assertSame(first, second, "Multiple calls must return the identical context instance.");
//         verify(mockInterpreter, times(2)).getSaxEventInterpretationContext();
//     }

    /* --------------------------------------------------------------
    Interpreter replacement – after swapping the interpreter,
    the new context is returned.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_AfterInterpreterReplacement_ShouldReturnNewContext() {
//         // First call with the original interpreter.
//         SaxEventInterpretationContext first = configurator.getInterpretationContext();
//         assertSame(mockInterpretationContext, first);
//         // Prepare a second mock interpreter returning a different context.
//         SaxEventInterpreter secondInterpreter = mock(SaxEventInterpreter.class);
//         SaxEventInterpretationContext secondContext = mock(SaxEventInterpretationContext.class);
//         when(secondInterpreter.getSaxEventInterpretationContext()).thenReturn(secondContext);
//         // Replace the interpreter inside the configurator.
//         configurator.saxEventInterpreter = secondInterpreter;
//         // Second call should now delegate to the new interpreter.
//         SaxEventInterpretationContext afterSwap = configurator.getInterpretationContext();
//         assertSame(secondContext, afterSwap, "After interpreter replacement the returned context must come from the new interpreter.");
//         verify(secondInterpreter, times(1)).getSaxEventInterpretationContext();
//     }

    /* --------------------------------------------------------------
    RuntimeException propagation.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_WhenInterpreterThrowsRuntimeException_ShouldPropagate() {
//         RuntimeException ex = new RuntimeException("boom");
//         when(mockInterpreter.getSaxEventInterpretationContext()).thenThrow(ex);
//         RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.getInterpretationContext(), "The RuntimeException from the interpreter must propagate unchanged.");
//         assertSame(ex, thrown);
//     }

    /* --------------------------------------------------------------
    IllegalStateException propagation.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_WhenInterpreterThrowsIllegalStateException_ShouldPropagate() {
//         IllegalStateException ex = new IllegalStateException("illegal state");
//         when(mockInterpreter.getSaxEventInterpretationContext()).thenThrow(ex);
//         IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> configurator.getInterpretationContext(), "The IllegalStateException from the interpreter must propagate unchanged.");
//         assertSame(ex, thrown);
//     }

    /* --------------------------------------------------------------
    Subclass return type – ensure the exact subclass instance is returned.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_WhenInterpreterReturnsSubclass_ShouldReturnExactSubclass() {
//         class CustomContext extends SaxEventInterpretationContext {
//         }
//         CustomContext custom = new CustomContext();
//         when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(custom);
//         SaxEventInterpretationContext result = configurator.getInterpretationContext();
//         assertSame(custom, result, "The exact subclass instance must be returned unchanged.");
//         assertTrue(result instanceof CustomContext, "Result should be an instance of the custom subclass.");
//     }

    /* --------------------------------------------------------------
    Configurator without interpreter – expect NullPointerException.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_WithoutSettingInterpreter_ShouldThrowNPE() {
//         // Create a configurator that never receives an interpreter.
//         NoInterpreterConfigurator noInterp = new NoInterpreterConfigurator();
//         assertThrows(NullPointerException.class, () -> noInterp.getInterpretationContext(), "Calling getInterpretationContext() without an interpreter must throw NullPointerException.");
//     }

    /* --------------------------------------------------------------
    Concurrent access – all threads must receive the same reference.
    --------------------------------------------------------------
     */
//     @Test
//     void getInterpretationContext_ConcurrentCalls_ShouldReturnConsistentReference() throws Exception {
//         int threadCount = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//         List<Callable<SaxEventInterpretationContext>> tasks = new ArrayList<>();
//         // All tasks call the same method.
//         IntStream.range(0, threadCount).forEach(i -> tasks.add(() -> configurator.getInterpretationContext()));
//         List<Future<SaxEventInterpretationContext>> futures = executor.invokeAll(tasks);
//         executor.shutdownNow();
//         SaxEventInterpretationContext first = futures.get(0).get();
//         for (Future<SaxEventInterpretationContext> f : futures) {
//             assertSame(first, f.get(), "All concurrent calls must return the identical context instance.");
//         }
//         // Verify that the interpreter was invoked exactly 'threadCount' times.
//         verify(mockInterpreter, times(threadCount)).getSaxEventInterpretationContext();
//     }
}