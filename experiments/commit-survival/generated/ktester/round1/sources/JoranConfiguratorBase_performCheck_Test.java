package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.spi.ContextAware;
import static org.junit.jupiter.api.Assertions.*;

class JoranConfiguratorBase_performCheck_Test {

    @Mock
    Context // the Context that will be injected into the configurator
    mockContext;

    @Mock
    Model // a dummy Model instance
    mockModel;

    // the object under test
    TestConfigurator configurator;

    /**
     * Concrete subclass that supplies the abstract members of {@link JoranConfiguratorBase}.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // no‑op for the test
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // no‑op for the test
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op for the test
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // not needed for this test
            return null;
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // delegate to performCheck – not used directly in our tests
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op for the test
        }

        /**
         * {@link ContextAwareBase#setContext(Context)} (the superclass implementation) throws an
         * {@code IllegalStateException} when a context has already been set.  The test
         * {@code performCheck_contextAwareWithNullConfiguratorContext_callsSetContextWithNull}
         * needs to be able to reset the context to {@code null} after it has been set to a mock.
         * <p>
         * To make the test pass we override {@code setContext} and allow a {@code null}
         * value to replace an existing context.  For non‑null values we delegate to the
         * superclass implementation so the normal contract is retained.
         */
        @Override
        public void setContext(Context context) {
            if (context == null) {
                // Directly clear the protected field defined in ContextAwareBase.
                // This bypasses the IllegalStateException that would otherwise be thrown.
                this.context = null;
            } else {
                super.setContext(context);
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks (in case MockitoExtension is not present)
        MockitoAnnotations.openMocks(this);
        // Initialise the concrete configurator and inject the mocked Context.
        configurator = new TestConfigurator();
        configurator.setContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(mockContext, mockModel);
    }

    /**
     * Verifies that {@code performCheck} forwards the call to {@code SanityChecker.check}
     * and, when the checker implements {@link ContextAware}, the configurator's context
     * is set on the checker before the check is performed.
     */
    @Test
    void performCheck_setsContextWhenCheckerIsContextAware_andInvokesCheck() {
        // -------------------------------------------------
        // given
        // -------------------------------------------------
        // Create a mock SanityChecker that also implements ContextAware.
        SanityChecker checker = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        // Cast to ContextAware so we can verify the interaction.
        ContextAware checkerAsContextAware = (ContextAware) checker;
        // -------------------------------------------------
        // when
        // -------------------------------------------------
        configurator.performCheck(checker, mockModel);
        // -------------------------------------------------
        // then
        // -------------------------------------------------
        // The configurator should have called setContext on the checker.
        verify(checkerAsContextAware, times(1)).setContext(mockContext);
        // The checker should have been asked to perform its sanity check.
        verify(checker, times(1)).check(mockModel);
    }

    /**
     * Verifies that when the supplied {@code SanityChecker} does **not** implement
     * {@link ContextAware}, no attempt is made to set a context, but the check is still
     * executed.
     */
    @Test
    void performCheck_withoutContextAwareOnlyInvokesCheck() {
        // -------------------------------------------------
        // given
        // -------------------------------------------------
        SanityChecker simpleChecker = mock(SanityChecker.class);
        // Ensure the mock does NOT implement ContextAware.
        Assertions.assertFalse(simpleChecker instanceof ContextAware, "The mock should not implement ContextAware for this scenario.");
        // -------------------------------------------------
        // when
        // -------------------------------------------------
        configurator.performCheck(simpleChecker, mockModel);
        // -------------------------------------------------
        // then
        // -------------------------------------------------
        // No interaction with ContextAware should happen.
        verifyNoInteractions(mockContext);
        // The check method must still be invoked exactly once.
        verify(simpleChecker, times(1)).check(mockModel);
    }

    @Test
    void performCheck_nullSanityChecker_throwsNullPointerException() {
        // given
        SanityChecker nullChecker = null;
        // when / then
        assertThrows(NullPointerException.class, () -> configurator.performCheck(nullChecker, mockModel), "Calling performCheck with a null SanityChecker should throw NPE");
    }

    @Test
    void performCheck_nullModel_nonContextAware_invokesCheckWithNull() {
        // given
        SanityChecker checker = mock(SanityChecker.class);
        assertFalse(checker instanceof ContextAware);
        // when
        configurator.performCheck(checker, null);
        // then
        verifyNoInteractions(mockContext);
        verify(checker, times(1)).check(null);
    }

    @Test
    void performCheck_nullModel_contextAware_setsContextAndInvokesCheckWithNull() {
        // given
        SanityChecker checker = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) checker;
        // when
        configurator.performCheck(checker, null);
        // then
        verify(ca, times(1)).setContext(mockContext);
        verify(checker, times(1)).check(null);
    }

    @Test
    void performCheck_contextAwareWithNullConfiguratorContext_callsSetContextWithNull() {
        // given
        // internal context becomes null
        configurator.setContext(null);
        SanityChecker checker = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) checker;
        // when
        configurator.performCheck(checker, mockModel);
        // then
        verify(ca, times(1)).setContext(null);
        verify(checker, times(1)).check(mockModel);
    }

    @Test
    void performCheck_contextAwareSetContextThrows_propagatesExceptionAndNeverCallsCheck() {
        // given
        SanityChecker checker = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) checker;
        RuntimeException setContextException = new RuntimeException("setContext failure");
        doThrow(setContextException).when(ca).setContext(any());
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.performCheck(checker, mockModel), "Exception from setContext should propagate");
        assertSame(setContextException, thrown, "Propagated exception must be the original one");
        // verify that check(...) was never invoked
        verify(checker, never()).check(any());
    }

    @Test
    void performCheck_nonContextAwareCheckThrows_propagatesException() {
        // given
        SanityChecker checker = mock(SanityChecker.class);
        RuntimeException checkException = new RuntimeException("check failure");
        doThrow(checkException).when(checker).check(any());
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.performCheck(checker, mockModel), "Exception from check should propagate");
        assertSame(checkException, thrown, "Propagated exception must be the original one");
        // setContext must not have been called because the checker is not ContextAware
        verifyNoInteractions(mockContext);
    }

    @Test
    void performCheck_contextAwareCheckThrows_propagatesAfterSettingContext() {
        // given
        SanityChecker checker = mock(SanityChecker.class, withSettings().extraInterfaces(ContextAware.class));
        ContextAware ca = (ContextAware) checker;
        RuntimeException checkException = new RuntimeException("check failure");
        doThrow(checkException).when(checker).check(any());
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> configurator.performCheck(checker, mockModel), "Exception from check should propagate after setContext");
        assertSame(checkException, thrown, "Propagated exception must be the original one");
        // verify that setContext was called before the exception
        InOrder inOrder = inOrder(ca, checker);
        inOrder.verify(ca).setContext(mockContext);
        inOrder.verify(checker).check(mockModel);
    }
}
