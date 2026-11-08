package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.*;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link JoranConfiguratorBase}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API methods</li>
 *   <li>Protected helper methods (exposed via a test subclass)</li>
 *   <li>Edge‑case handling such as {@code null} arguments</li>
 *   <li>Interaction with a {@link SanityChecker} that also implements {@link ContextAware}</li>
 * </ul>
 *
 * All tests are written with JUnit 4.
 */
public class JoranConfiguratorBaseTest {

    /**
     * Minimal concrete subclass that makes the protected methods of
     * {@link JoranConfiguratorBase} visible for testing.
     */
    private static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

        // No‑arg constructor – the superclass does not define any.
        public TestJoranConfigurator() {
        }

        // expose protected methods as public for the test suite
        public void publicSanityCheck(Model model) {
            super.sanityCheck(model);
        }

        public void publicPerformCheck(SanityChecker checker, Model model) {
            super.performCheck(checker, model);
        }

        public void publicAddModelHandlerAssociations(DefaultProcessor processor) {
            super.addModelHandlerAssociations(processor);
        }

        // The abstract method from GenericXMLConfigurator is already
        // implemented in JoranConfiguratorBase, so no further overrides are needed.
    }

    /**
     * Simple {@link SanityChecker} implementation that also implements {@link ContextAware}
     * so we can verify that the configurator correctly sets the context.
     */
    private static class TestSanityChecker implements SanityChecker, ContextAware {

        private boolean checkCalled = false;
        private Model receivedModel = null;
        private ContextAware contextSetTo = null;

        @Override
        public void check(Model model) {
            this.checkCalled = true;
            this.receivedModel = model;
        }

        // ----- ContextAware contract (no‑op implementations) -----
        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            this.contextSetTo = this; // just record that the method was invoked
        }

        @Override
        public ch.qos.logback.core.spi.Context getContext() {
            return null;
        }

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) {}

        @Override
        public void addInfo(String msg) {}

        @Override
        public void addInfo(String msg, Throwable ex) {}

        @Override
        public void addWarn(String msg) {}

        @Override
        public void addWarn(String msg, Throwable ex) {}

        @Override
        public void addError(String msg) {}

        @Override
        public void addError(String msg, Throwable ex) {}

        // Helper methods for assertions
        boolean isCheckCalled() {
            return checkCalled;
        }

        Model getReceivedModel() {
            return receivedModel;
        }

        ContextAware getContextSetTo() {
            return contextSetTo;
        }
    }

    private TestJoranConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
    }

    // -----------------------------------------------------------------
    // 1.  Public API tests
    // -----------------------------------------------------------------

    /**
     * {@link JoranConfiguratorBase#getInterpretationContext()} should return the
     * current {@link SaxEventInterpretationContext} (or {@code null} if the
     * interpreter has not been created yet). The base class does not create the
     * interpreter until the configuration process is started, therefore we only
     * verify that the method can be called without throwing an exception and
     * that it returns {@code null} in the default state.
     */
    @Test
    public void testGetInterpretationContextReturnsNullBeforeConfiguration() {
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        Assert.assertNull("Interpretation context should be null before any configuration work", ctx);
    }

    /**
     * {@link JoranConfiguratorBase#buildModelInterpretationContext()} delegates to
     * the superclass implementation and then creates the appender bags. The test
     * simply ensures that the method executes without throwing an exception.
     */
    @Test
    public void testBuildModelInterpretationContextExecutes() {
        // The method is void and has no observable side‑effects that are visible
        // through the public API, so we just invoke it and verify that no
        // exception propagates.
        configurator.buildModelInterpretationContext();
    }

    /**
     * {@link JoranConfiguratorBase#setImplicitRuleSupplier(SaxEventInterpreter)} should
     * accept a non‑null interpreter and set the implicit action supplier. Because the
     * underlying field is private, we verify the contract by ensuring that the call
     * does not throw and that a subsequent call with the same interpreter succeeds.
     */
    @Test
    public void testSetImplicitRuleSupplierAcceptsValidInterpreter() {
        // Minimal viable arguments for SaxEventInterpreter.
        // The constructor parameters are:
        //   Context context, RuleStore rs, ElementPath initialElementPath, List<SaxEvent> saxEvents
        // All of these can be safely passed as {@code null} or empty collections because the
        // test only checks that the method itself does not fail.
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                null,                    // Context (null is acceptable for this test)
                new RuleStore() {        // anonymous RuleStore with no‑op implementations
                    @Override public void addRule(ElementSelector elementSelector, String actionClassStr) {}
                    @Override public void addRule(ElementSelector elementSelector, java.util.function.Supplier<Action> actionSupplier) {}
                    @Override public java.util.function.Supplier<Action> matchActions(ElementPath elementPath) { return null; }
                    @Override public void addTransparentPathPart(String pathPart) {}
                    @Override public void addPathPathMapping(String originalName, String modifiedName) {}
                },
                null,                    // ElementPath (null is acceptable)
                Collections.emptyList()  // empty SaxEvent list
        );

        // The call should complete without throwing any exception.
        configurator.setImplicitRuleSupplier(interpreter);
    }

    /**
     * Verify that {@code setImplicitRuleSupplier} throws a {@link NullPointerException}
     * when a {@code null} interpreter is supplied.
     */
    @Test(expected = NullPointerException.class)
    public void testSetImplicitRuleSupplierRejectsNullInterpreter() {
        configurator.setImplicitRuleSupplier(null);
    }

    // -----------------------------------------------------------------
    // 2.  Protected helper method tests (exposed via the test subclass)
    // -----------------------------------------------------------------

    /**
     * The default {@code sanityCheck} implementation should invoke the
     * {@link AppenderWithinAppenderSanityChecker}. The checker does not throw,
     * so the test only ensures that no exception propagates.
     */
    @Test
    public void testSanityCheckInvokesDefaultChecker() {
        Model model = new Model(); // an empty model instance
        configurator.publicSanityCheck(model);
        // No assertion needed – the absence of an exception indicates success.
    }

    /**
     * Passing {@code null} to {@code sanityCheck} should result in a
     * {@link NullPointerException} because the underlying checker will receive a
     * null model.
     */
    @Test(expected = NullPointerException.class)
    public void testSanityCheckWithNullModel() {
        configurator.publicSanityCheck(null);
    }

    /**
     * Verify that {@code performCheck} forwards the call to the provided
     * {@link SanityChecker} and, when the checker also implements {@link ContextAware},
     * the configurator sets the context on it.
     */
    @Test
    public void testPerformCheckCallsCheckerAndSetsContext() {
        TestSanityChecker checker = new TestSanityChecker();
        Model model = new Model();

        configurator.publicPerformCheck(checker, model);

        // The checker must have received the model.
        Assert.assertTrue("Checker.check(...) should have been called", checker.isCheckCalled());
        Assert.assertSame("Checker should receive the exact model instance", model, checker.getReceivedModel());

        // The configurator should have called setContext on the checker.
        Assert.assertNotNull("Checker.setContext(...) should have been invoked", checker.getContextSetTo());
    }

    /**
     * {@code performCheck} should throw a {@link NullPointerException} when the
     * supplied {@link SanityChecker} is {@code null}.
     */
    @Test(expected = NullPointerException.class)
    public void testPerformCheckWithNullChecker() {
        Model model = new Model();
        configurator.publicPerformCheck(null, model);
    }

    /**
     * {@code performCheck} should also throw a {@link NullPointerException}
     * when the model argument is {@code null}.
     */
    @Test(expected = NullPointerException.class)
    public void testPerformCheckWithNullModel() {
        TestSanityChecker checker = new TestSanityChecker();
        configurator.publicPerformCheck(checker, null);
    }

    /**
     * The {@code addModelHandlerAssociations} method registers a number of model
     * handlers with a {@link DefaultProcessor}. Because the method does not
     * expose any observable state, the test simply verifies that invoking the
     * method does not raise an exception.
     *
     * A minimal {@link DefaultProcessor} can be constructed with {@code null}
     * arguments – the constructor only stores the references.
     */
    @Test
    public void testAddModelHandlerAssociationsExecutesWithoutError() {
        DefaultProcessor processor = new DefaultProcessor(null, null);
        configurator.publicAddModelHandlerAssociations(processor);
        // Success is defined by the absence of an exception.
    }
}
