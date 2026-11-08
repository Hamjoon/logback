package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import org.mockito.InOrder;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.mockito.MockitoAnnotations;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_performCheck_Test {

    private TestConfigurator configurator;

    @Mock
    private Context mockContext;

    @Mock
    private Model mockModel;

    private AutoCloseable mocks;

    /**
     * Minimal concrete subclass that makes the protected method public.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        // The abstract methods are not needed for these tests – they can stay empty.
        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }

        @Override
        public void buildModelInterpretationContext() {
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return null;
        }

        @Override
        protected void sanityCheck(Model topModel) {
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        }

        /**
         * Exposes the protected {@code performCheck} for testing.
         */
        public void callPerformCheck(SanityChecker sc, Model model) {
            performCheck(sc, model);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
        configurator = new TestConfigurator();
        configurator.setContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        Mockito.reset(mockContext, mockModel);
        // Close the Mockito openMocks resource.
        mocks.close();
    }

    @Test
    void performCheck_withoutContextAware_callsOnlyCheck() {
        // Arrange: a plain SanityChecker mock (does NOT implement ContextAware)
        SanityChecker sc = mock(SanityChecker.class);
        // Act
        configurator.callPerformCheck(sc, mockModel);
        // Assert: only check(model) should be invoked; setContext must never be called.
        verify(sc, times(1)).check(mockModel);
        verifyNoMoreInteractions(sc);
    }

    @Test
    void performCheck_withContextAware_invokesSetContextThenCheck() {
        // Arrange: a mock that implements both SanityChecker and ContextAware
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        // Act
        configurator.callPerformCheck(sc, mockModel);
        // Assert: setContext should be called with the configurator's context,
        // then check should be called.
        InOrder inOrder = inOrder(ca, sc);
        inOrder.verify(ca).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void performCheck_withoutContextAware_andNullModel_callsCheckWithNull() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class);
        // Act
        configurator.callPerformCheck(sc, null);
        // Assert
        verify(sc, times(1)).check(null);
        verifyNoMoreInteractions(sc);
    }

    @Test
    void performCheck_withContextAware_andNullModel() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        // Act
        configurator.callPerformCheck(sc, null);
        // Assert
        InOrder inOrder = inOrder(ca, sc);
        inOrder.verify(ca).setContext(mockContext);
        inOrder.verify(sc).check(null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void performCheck_withContextAware_checkThrowsRuntimeException() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(sc).check(any());
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.callPerformCheck(sc, mockModel));
        assertSame(boom, thrown, "Exception from check() should propagate unchanged");
        // Verify setContext was still called before the exception.
        InOrder inOrder = inOrder(ca, sc);
        inOrder.verify(ca).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void performCheck_withContextAware_setContextThrowsException() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        IllegalStateException ise = new IllegalStateException("context error");
        doThrow(ise).when(ca).setContext(any());
        // Act & Assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> configurator.callPerformCheck(sc, mockModel));
        assertSame(ise, thrown, "Exception from setContext() should propagate");
        // Verify that check() is never invoked.
        verify(sc, never()).check(any());
    }

    @Test
    void performCheck_withContextAware_orderVerification() {
        // Arrange: same as the first ContextAware test but we emphasise order.
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        // Act
        configurator.callPerformCheck(sc, mockModel);
        // Assert
        InOrder inOrder = inOrder(ca, sc);
        inOrder.verify(ca).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
        inOrder.verifyNoMoreInteractions();
    }

    /* --------------------------------------------------------------
     * Group: Null SanityChecker argument
     * -------------------------------------------------------------- */
    @Test
    void performCheck_nullSanityChecker_throwsNullPointerException() {
        // Arrange
        SanityChecker sc = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> configurator.callPerformCheck(sc, mockModel));
    }

    /* --------------------------------------------------------------
     * Group: Configurator context is null – ContextAware receives null
     * -------------------------------------------------------------- */
    @Test
    void performCheck_contextIsNull_ContextAwareReceivesNull() {
        // Arrange: configurator with a null internal context
        configurator.setContext(null);
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) sc;
        // Act
        configurator.callPerformCheck(sc, mockModel);
        // Assert: setContext(null) should be called, then check().
        InOrder inOrder = inOrder(ca, sc);
        inOrder.verify(ca).setContext(null);
        inOrder.verify(sc).check(mockModel);
        inOrder.verifyNoMoreInteractions();
    }
}