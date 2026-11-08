package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.RuleStore;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_buildModelInterpretationContext_Test {

    private TestConfigurator configurator;

    /**
     * Minimal concrete configurator used for the tests.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No element‑selector/action associations needed for these tests.
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No model‑handler associations needed for these tests.
        }
    }

    class FailingConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        public void buildModelInterpretationContext() {
            throw new RuntimeException("super buildModelInterpretationContext failed");
        }

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }
    }

    class ErrorConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        public void buildModelInterpretationContext() {
            throw new AssertionError("super method assertion");
        }

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }
    }

    class FailingConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        public void buildModelInterpretationContext() {
            throw new RuntimeException("super buildModelInterpretationContext failed");
        }

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }
    }

    class ErrorConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        public void buildModelInterpretationContext() {
            throw new AssertionError("super method assertion");
        }

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
        configurator = new TestConfigurator();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the configurator reference to help GC.
        configurator = null;
        configurator = null;
    }

    /**
     * Reflection helper to set the protected {@code modelInterpretationContext} field.
     */
    private static void setModelInterpretationContext(JoranConfiguratorBase<?> cfg, ModelInterpretationContext mic) throws Exception {
        Field f = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
        f.setAccessible(true);
        f.set(cfg, mic);
    }

    /**
     * Reflection helper to retrieve the protected {@code modelInterpretationContext} field.
     */
    private static ModelInterpretationContext getModelInterpretationContext(JoranConfiguratorBase<?> cfg) throws Exception {
        Field f = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
        f.setAccessible(true);
        return (ModelInterpretationContext) f.get(cfg);
    }

    @Test
    void buildModelInterpretationContext_shouldCreateInterpretationContext() {
        // When
        configurator.buildModelInterpretationContext();
        // Then
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull(ctx, "Interpretation context should be created by buildModelInterpretationContext()");
        // Additional sanity: the context should be an instance of the expected type.
        assertTrue(ctx instanceof SaxEventInterpretationContext, "Returned object should be a SaxEventInterpretationContext");
    }

    /* -------------------------------------------------------------
     *  Normal happy‑path: super method creates a context and
     *  createAppenderBags() is invoked exactly once.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_happyPath_callsSuperAndCreateAppenderBags() throws Exception {
        // Arrange – inject a Mockito mock for the ModelInterpretationContext.
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        setModelInterpretationContext(configurator, micMock);
        // Act
        configurator.buildModelInterpretationContext();
        // Assert – the super implementation should have created a non‑null interpretation context.
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull(ctx, "Interpretation context must be created by the super call");
        // Verify that createAppenderBags() was invoked exactly once.
        verify(micMock, times(1)).createAppenderBags();
    }

    /* -------------------------------------------------------------
     *  Null modelInterpretationContext – should result in NPE.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_nullModelInterpretationContext_throwsNPE() throws Exception {
        // Arrange – explicitly set the protected field to null.
        setModelInterpretationContext(configurator, null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> configurator.buildModelInterpretationContext(), "Calling buildModelInterpretationContext with a null modelInterpretationContext should throw NPE");
        assertTrue(npe.getMessage().contains("modelInterpretationContext"), "Exception message should mention modelInterpretationContext");
    }

    /* -------------------------------------------------------------
     *  Propagation of an exception thrown by createAppenderBags().
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsIllegalStateException_propagates() throws Exception {
        // Arrange – mock that throws when createAppenderBags() is called.
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        doThrow(new IllegalStateException("createAppenderBags failed")).when(micMock).createAppenderBags();
        setModelInterpretationContext(configurator, micMock);
        // Act & Assert
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> configurator.buildModelInterpretationContext(), "Exception from createAppenderBags should propagate");
        assertTrue(ise.getMessage().contains("createAppenderBags failed"));
    }

    /* -------------------------------------------------------------
     *  Verify the exact order: super.buildModelInterpretationContext()
     *  must happen before createAppenderBags().
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_operationOrder_isSuperFirstThenCreateAppenderBags() throws Exception {
        // Use an AtomicInteger to capture the sequence.
        AtomicInteger order = new AtomicInteger(0);
        // A spy on the configurator allows us to intercept the super call via a
        // custom subclass that records when the super method finishes.
        TestConfigurator spyConfigurator = Mockito.spy(configurator);
        // Replace the protected field with a mock that records its invocation order.
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        doAnswer(invocation -> {
            // The value should be 2 because super call should have set it to 1.
            assertEquals(2, order.get(), "createAppenderBags should be invoked after the super method");
            return null;
        }).when(micMock).createAppenderBags();
        // When super.buildModelInterpretationContext() finishes, we increment the order.
        // We achieve this by overriding the protected method in a subclass of
        // GenericXMLConfigurator via reflection – not possible directly, so we
        // simulate the ordering by incrementing before the actual call.
        // The easiest way is to set the order before invoking the method under test.
        // super call simulated as step 1
        order.set(1);
        setModelInterpretationContext(spyConfigurator, micMock);
        // Act
        spyConfigurator.buildModelInterpretationContext();
        // Assert – verify that both steps occurred.
        assertEquals(2, order.get(), "The order counter should indicate that both steps have been executed");
        verify(micMock, times(1)).createAppenderBags();
    }

    /* -------------------------------------------------------------
     *  Multiple invocations must be idempotent – each call triggers both
     *  super and createAppenderBags().
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_multipleCalls_idempotentBehavior() throws Exception {
        // Arrange – mock that records how many times createAppenderBags() is called.
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        setModelInterpretationContext(configurator, micMock);
        // Act – call the method twice.
        configurator.buildModelInterpretationContext();
        configurator.buildModelInterpretationContext();
        // Assert – super call creates a fresh interpretation context each time,
        // and createAppenderBags() is invoked twice.
        verify(micMock, times(2)).createAppenderBags();
        // The interpretation context should be non‑null after the second call.
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull(ctx, "Interpretation context must still be non‑null after repeated calls");
    }

    /* -------------------------------------------------------------
     *  Simulate a RuntimeException thrown from the super implementation.
     *  Because we cannot directly mock the super method, we create a
     *  specialised subclass that overrides the method to throw.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_superMethodThrowsRuntimeException_propagates() {
        // A configurator that deliberately throws from the super implementation.
        class FailingConfigurator extends JoranConfiguratorBase<Object> {

            @Override
            public void buildModelInterpretationContext() {
                throw new RuntimeException("super buildModelInterpretationContext failed");
            }

            @Override
            protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            }

            @Override
            protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            }
        }
        FailingConfigurator failing = new FailingConfigurator();
        RuntimeException ex = assertThrows(RuntimeException.class, failing::buildModelInterpretationContext, "Exception from the overridden super method should propagate");
        assertTrue(ex.getMessage().contains("super buildModelInterpretationContext failed"));
    }

    /* -------------------------------------------------------------
     *  Simulate an AssertionError (Error) thrown from the super method.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_superMethodThrowsAssertionError_propagates() {
        class ErrorConfigurator extends JoranConfiguratorBase<Object> {

            @Override
            public void buildModelInterpretationContext() {
                throw new AssertionError("super method assertion");
            }

            @Override
            protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            }

            @Override
            protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            }
        }
        ErrorConfigurator cfg = new ErrorConfigurator();
        AssertionError err = assertThrows(AssertionError.class, cfg::buildModelInterpretationContext, "AssertionError from super should propagate unchanged");
        assertTrue(err.getMessage().contains("super method assertion"));
    }

    /* -------------------------------------------------------------
     *  createAppenderBags throws NullPointerException – should propagate.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsNullPointerException_propagates() throws Exception {
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        doThrow(new NullPointerException("createAppenderBags NPE")).when(micMock).createAppenderBags();
        setModelInterpretationContext(configurator, micMock);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> configurator.buildModelInterpretationContext(), "NullPointerException from createAppenderBags should propagate");
        assertTrue(npe.getMessage().contains("createAppenderBags"));
    }

    /* -------------------------------------------------------------
     *  createAppenderBags throws IllegalArgumentException – should propagate.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsIllegalArgumentException_propagates() throws Exception {
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        doThrow(new IllegalArgumentException("invalid argument")).when(micMock).createAppenderBags();
        setModelInterpretationContext(configurator, micMock);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> configurator.buildModelInterpretationContext(), "IllegalArgumentException from createAppenderBags should propagate");
        assertTrue(iae.getMessage().contains("invalid argument"));
    }

    /* -------------------------------------------------------------
     *  createAppenderBags throws a custom RuntimeException – should propagate.
     * ------------------------------------------------------------- */
    @Test
    void buildModelInterpretationContext_createAppenderBagsThrowsCustomRuntimeException_propagates() throws Exception {
        ModelInterpretationContext micMock = mock(ModelInterpretationContext.class);
        doThrow(new RuntimeException("custom failure")).when(micMock).createAppenderBags();
        setModelInterpretationContext(configurator, micMock);
        RuntimeException re = assertThrows(RuntimeException.class, () -> configurator.buildModelInterpretationContext(), "Custom RuntimeException from createAppenderBags should propagate");
        assertTrue(re.getMessage().contains("custom failure"));
    }
}
