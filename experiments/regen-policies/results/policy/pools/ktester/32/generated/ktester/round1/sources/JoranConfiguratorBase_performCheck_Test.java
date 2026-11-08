package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import org.mockito.InOrder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.isNull;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_performCheck_Test {

    /**
     * The configurator under test – a minimal concrete subclass of the abstract base.
     */
    private JoranConfiguratorBase<Object> configurator;

    /**
     * Mocked Logback context that will be injected into the configurator.
     */
    @Mock
    private Context mockContext;

    /**
     * Mocked model passed to {@code performCheck}.
     */
    @Mock
    private Model mockModel;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        MockitoAnnotations.openMocks(this);
        // Create a minimal concrete implementation of the abstract class.
        configurator = new JoranConfiguratorBase<Object>() {

            @Override
            protected void addElementSelectorAndActionAssociations(RuleStore rs) {
                // No‑op for the test.
            }

            @Override
            protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
                // No‑op for the test.
            }

            @Override
            public void buildModelInterpretationContext() {
                // No‑op for the test.
            }

            @Override
            public SaxEventInterpretationContext getInterpretationContext() {
                // Not needed for this test.
                return null;
            }

            @Override
            protected void sanityCheck(Model topModel) {
                // No‑op for the test.
            }

            @Override
            protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
                // No‑op for the test.
            }
        };
        // Inject the mocked Context so that performCheck can forward it.
        configurator.setContext(mockContext);
    }

    @Test
    void performCheck_callsCheck_whenSanityCheckerDoesNotImplementContextAware() {
        // Arrange: a plain SanityChecker mock (does NOT implement ContextAware)
        SanityChecker sc = mock(SanityChecker.class);
        // Act
        configurator.performCheck(sc, mockModel);
        // Assert: check() is invoked exactly once with the supplied model
        verify(sc, times(1)).check(mockModel);
        // No interaction with ContextAware methods should happen
        verifyNoMoreInteractions(sc);
    }

    @Test
    void performCheck_setsContextAndCallsCheck_whenSanityCheckerIsContextAware() {
        // Arrange: a mock that implements both SanityChecker and ContextAware
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ch.qos.logback.core.spi.ContextAware.class));
        // Act
        configurator.performCheck(sc, mockModel);
        // Assert: the configurator's context is passed to the checker
        verify((ch.qos.logback.core.spi.ContextAware) sc, times(1)).setContext(mockContext);
        // And the check method is still invoked
        verify(sc, times(1)).check(mockModel);
    }

    /* --------------------------------------------------------------
     *  Null handling tests
     * -------------------------------------------------------------- */
    @Test
    void performCheck_nullSanityChecker_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> configurator.performCheck(null, mockModel));
    }

    @Test
    void performCheck_nullModel_callsCheckWithNull() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class);
        // Act
        configurator.performCheck(sc, null);
        // Assert
        verify(sc, times(1)).check(null);
    }

    @Test
    void performCheck_nullSanityCheckerAndModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> configurator.performCheck(null, null));
    }

    /* --------------------------------------------------------------
     *  Exception propagation from ContextAware#setContext
     * -------------------------------------------------------------- */
    @Test
    void performCheck_contextAwareSetContextThrows_propagatesException() {
        // Arrange: mock that implements both interfaces and throws on setContext
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        doThrow(new RuntimeException("boom")).when((ContextAware) sc).setContext(any());
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("boom", ex.getMessage());
        // verify that check() was never called
        verify(sc, never()).check(any());
    }

    @Test
    void performCheck_contextAwareSetContextThrows_AssertionErrorPropagated() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        doThrow(new AssertionError("assert fail")).when((ContextAware) sc).setContext(any());
        // Act & Assert
        AssertionError err = assertThrows(AssertionError.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("assert fail", err.getMessage());
        verify(sc, never()).check(any());
    }

    /* --------------------------------------------------------------
     *  Exception propagation from SanityChecker#check
     * -------------------------------------------------------------- */
    @Test
    void performCheck_checkThrowsIllegalStateException_propagated() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class);
        doThrow(new IllegalStateException("illegal")).when(sc).check(any());
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("illegal", ex.getMessage());
        // setContext should not have been called (sc does not implement ContextAware)
        verifyNoMoreInteractions(sc);
    }

    @Test
    void performCheck_contextAwareCheckThrowsIllegalArgumentException_propagated() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        doThrow(new IllegalArgumentException("bad arg")).when(sc).check(any());
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("bad arg", ex.getMessage());
        // Verify setContext was still called before the exception
        InOrder inOrder = inOrder((ContextAware) sc, sc);
        inOrder.verify((ContextAware) sc).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
    }

    @Test
    void performCheck_checkThrowsOutOfMemoryError_propagated() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class);
        doThrow(new OutOfMemoryError("oom")).when(sc).check(any());
        // Act & Assert
        OutOfMemoryError err = assertThrows(OutOfMemoryError.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("oom", err.getMessage());
    }

    @Test
    void performCheck_contextAwareCheckThrowsStackOverflowError_propagated() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        doThrow(new StackOverflowError("so")).when(sc).check(any());
        // Act & Assert
        StackOverflowError err = assertThrows(StackOverflowError.class, () -> configurator.performCheck(sc, mockModel));
        assertEquals("so", err.getMessage());
        // Verify setContext was called before the error
        InOrder inOrder = inOrder((ContextAware) sc, sc);
        inOrder.verify((ContextAware) sc).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
    }

    /* --------------------------------------------------------------
     *  Call order verification
     * -------------------------------------------------------------- */
    @Test
    void performCheck_contextAwareSetContextCalledBeforeCheck_orderVerified() {
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        // Act
        configurator.performCheck(sc, mockModel);
        // Assert order
        InOrder inOrder = inOrder((ContextAware) sc, sc);
        inOrder.verify((ContextAware) sc).setContext(mockContext);
        inOrder.verify(sc).check(mockModel);
    }

    /* --------------------------------------------------------------
     *  Passing through complex Model instances
     * -------------------------------------------------------------- */
    @Test
    void performCheck_complexModel_isPassedUnmodified() {
        // Create a composite model stub (using a mock is sufficient)
        Model compositeModel = mock(Model.class, "CompositeModel");
        // Arrange
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        // Act
        configurator.performCheck(sc, compositeModel);
        // Assert
        verify((ContextAware) sc, times(1)).setContext(mockContext);
        verify(sc, times(1)).check(compositeModel);
    }

    /* --------------------------------------------------------------
     *  Configurator's internal context is null
     * -------------------------------------------------------------- */
    @Test
    void performCheck_contextAwareSetContextReceivesNullWhenConfiguratorContextIsNull() {
        // Arrange: configurator with null context
        configurator.setContext(null);
        SanityChecker sc = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        // Act
        configurator.performCheck(sc, mockModel);
        // Assert that setContext was called with null
        verify((ContextAware) sc, times(1)).setContext(isNull());
        verify(sc, times(1)).check(mockModel);
    }
}