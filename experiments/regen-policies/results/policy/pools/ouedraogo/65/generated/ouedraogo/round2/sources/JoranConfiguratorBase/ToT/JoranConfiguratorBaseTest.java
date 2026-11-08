package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.event.SaxEvent;


import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;

/**
 * Unit tests for {@link JoranConfiguratorBase}.
 *
 * The tests focus on the *behaviour* that can be verified without
 * needing the full Logback XML configuration machinery.
 *
 * <p>We use a concrete anonymous subclass of {@code JoranConfiguratorBase}
 * that exposes the protected methods we want to test and, where needed,
 * overrides {@code performCheck} to capture its invocation.</p>
 */
public class JoranConfiguratorBaseTest {

    /** a simple concrete configurator used for all tests */
    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        // give the configurator a dummy context – ContextBase is the standard
        // concrete implementation shipped with Logback.
        configurator.setContext(new ContextBase());
    }

    /**
     * Test that {@code getInterpretationContext()} returns a non‑null
     * {@link SaxEventInterpretationContext} after the model‑interpretation
     * context has been built.
     */
    @Test
    public void testGetInterpretationContext_NotNull() {
        // Build the model interpretation context – this also creates the
        // underlying SaxEventInterpreter.
        configurator.buildModelInterpretationContext();

        SaxEventInterpretationContext ic = configurator.getInterpretationContext();
        assertNotNull("Interpretation context should not be null after building", ic);
        // The context inside the interpretation context must be the same as
        // the configurator's context.
        assertSame("Interpretation context must hold the configurator's context",
                configurator.getContext(), ic.getContext());
    }

    /**
     * Verify that {@code sanityCheck(Model)} delegates to {@code performCheck}
     * with an {@link AppenderWithinAppenderSanityChecker}.
     *
     * The concrete {@code TestConfigurator} overrides {@code performCheck}
     * to capture the arguments it receives.
     */
    @Test
    public void testSanityCheck_DelegatesToPerformCheck() {
        // a minimal model – the concrete content does not matter for this
        // test because the overridden performCheck does not inspect it.
        Model dummyModel = new Model();

        configurator.resetPerformCheckCapture();
        configurator.sanityCheck(dummyModel);

        // verify that performCheck was called exactly once
        assertTrue("performCheck should have been invoked", configurator.wasPerformCheckInvoked());

        // verify that the checker supplied is an instance of the expected class
        assertTrue("Checker should be an AppenderWithinAppenderSanityChecker",
                configurator.getLastChecker() instanceof AppenderWithinAppenderSanityChecker);
        // and that the model passed through unchanged
        assertSame("Model passed to performCheck must be the same instance",
                dummyModel, configurator.getLastModel());
    }

    /**
     * Exercise {@code addElementSelectorAndActionAssociations(RuleStore)}.
     *
     * The test creates a real {@link RuleStore} implementation provided by
     * Logback ( {@code ch.qos.logback.core.joran.spi.DefaultRuleStore} ).
     * After invoking the method we verify that a few well‑known selectors
     * are associated with the expected action factories.
     */
    @Test
    public void testAddElementSelectorAndActionAssociations() throws Exception {
        // DefaultRuleStore is the standard concrete implementation of RuleStore.
        RuleStore rs = new ch.qos.logback.core.joran.spi.DefaultRuleStore();

        // expose the protected method via the test subclass
        configurator.publicAddElementSelectorAndActionAssociations(rs);

        // Verify a couple of selectors – we use the RuleStore's matcher to
        // obtain the supplier and then instantiate the Action to check its type.
        Supplier<Action> variableSupplier = rs.matchActions(
                new ElementPath("some/parent/variable"));
        assertNotNull("Variable selector must have a supplier", variableSupplier);
        assertTrue("Variable selector must produce a PropertyAction",
                variableSupplier.get() instanceof PropertyAction);

        Supplier<Action> ifSupplier = rs.matchActions(
                new ElementPath("any/if"));
        assertNotNull("If selector must have a supplier", ifSupplier);
        assertTrue("If selector must produce an IfAction",
                ifSupplier.get() instanceof IfAction);

        Supplier<Action> siftSupplier = rs.matchActions(
                new ElementPath("foo/appender/sift"));
        assertNotNull("Sift selector must have a supplier", siftSupplier);
        assertTrue("Sift selector must produce a SiftAction",
                siftSupplier.get() instanceof SiftAction);
    }

    /**
     * Verify that {@code setImplicitRuleSupplier} correctly registers the
     * {@code ImplicitModelAction} supplier on the interpreter.
     */
    @Test
    public void testSetImplicitRuleSupplier_RegistersImplicitAction() {
        // Build the minimal objects required by SaxEventInterpreter.
        ContextBase ctx = new ContextBase();
        RuleStore rs = new ch.qos.logback.core.joran.spi.DefaultRuleStore();
        ElementPath initialPath = new ElementPath(); // empty path
        List<SaxEvent> emptyEvents = Collections.emptyList();

        SaxEventInterpreter interpreter = new SaxEventInterpreter(ctx, rs, initialPath, emptyEvents);
        // invoke the method under test
        configurator.setImplicitRuleSupplier(interpreter);

        // The interpreter does not expose the supplier directly, but we can
        // verify that it is used when the interpreter needs an implicit
        // action. The easiest way is to ask the interpreter for the
        // supplier (via reflection) and invoke it.
        try {
            java.lang.reflect.Field f = SaxEventInterpreter.class
                    .getDeclaredField("implicitActionSupplier");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Supplier<Action> supplier = (Supplier<Action>) f.get(interpreter);
            assertNotNull("Implicit action supplier must be set", supplier);
            assertTrue("Supplier must produce an ImplicitModelAction",
                    supplier.get() instanceof ImplicitModelAction);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while checking implicit action supplier: " + e);
        }
    }

    /**
     * Concrete subclass used for testing. It exposes the protected methods
     * we need to call and overrides {@code performCheck} so that we can
     * assert that {@code sanityCheck} delegates correctly.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        private boolean performCheckInvoked = false;
        private SanityChecker lastChecker = null;
        private Model lastModel = null;

        /** reset the capture flags before each test */
        void resetPerformCheckCapture() {
            performCheckInvoked = false;
            lastChecker = null;
            lastModel = null;
        }

        boolean wasPerformCheckInvoked() {
            return performCheckInvoked;
        }

        SanityChecker getLastChecker() {
            return lastChecker;
        }

        Model getLastModel() {
            return lastModel;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // capture the arguments for verification
            performCheckInvoked = true;
            lastChecker = sc;
            lastModel = model;
            // delegate to the real implementation to keep behaviour unchanged
            super.performCheck(sc, model);
        }

        /** expose the protected method for the test */
        void publicAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }
    }
}
