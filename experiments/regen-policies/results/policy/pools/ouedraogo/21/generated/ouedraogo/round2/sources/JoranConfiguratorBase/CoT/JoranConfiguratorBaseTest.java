/**
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests cover:
 *   • All public (or effectively public) methods of JoranConfiguratorBase.
 *   • Basic functional behaviour.
 *   • Edge cases such as null arguments and context handling.
 *
 * The test class creates a concrete subclass of the abstract
 * {@code JoranConfiguratorBase} so that protected members can be exercised
 * without using any mocking frameworks – only real objects from the Logback
 * core library are instantiated.
 */
package ch.qos.logback.core.joran;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Concrete subclass used only for testing. All protected methods that need to be
 * called from the test code are exposed through public delegating methods.
 */
class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

    /** expose protected {@code performCheck} */
    public void callPerformCheck(SanityChecker sc, Model model) {
        performCheck(sc, model);
    }

    /** expose protected {@code sanityCheck} */
    public void callSanityCheck(Model topModel) {
        sanityCheck(topModel);
    }

    /** expose protected {@code addModelHandlerAssociations} */
    public void callAddModelHandlerAssociations(DefaultProcessor dp) {
        addModelHandlerAssociations(dp);
    }

    /** expose protected {@code setImplicitRuleSupplier} */
    public void callSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        setImplicitRuleSupplier(interpreter);
    }

    /** expose protected {@code addElementSelectorAndActionAssociations} */
    public void callAddElementSelectorAndActionAssociations(RuleStore rs) {
        addElementSelectorAndActionAssociations(rs);
    }
}

/**
 * A simple {@link SanityChecker} implementation that also implements
 * {@link ContextAware}. It records whether {@code check(..)} has been called
 * and which {@link ch.qos.logback.core.spi.ContextAware#setContext(Context)}
 * was invoked.
 */
class RecordingChecker implements SanityChecker, ContextAware {

    private boolean checkInvoked = false;
    private ContextBase context;

    @Override
    public void check(Model model) {
        checkInvoked = true;
    }

    public boolean isCheckInvoked() {
        return checkInvoked;
    }

    // ----------------------------------------------------------------------
    // ContextAware implementation
    // ----------------------------------------------------------------------
    @Override
    public void setContext(ch.qos.logback.core.spi.Context context) {
        this.context = (ContextBase) context;
    }

    @Override
    public ch.qos.logback.core.spi.Context getContext() {
        return context;
    }

    @Override
    public void addStatus(ch.qos.logback.core.status.Status status) { /* no‑op */ }

    @Override
    public void addInfo(String msg) { /* no‑op */ }

    @Override
    public void addInfo(String msg, Throwable ex) { /* no‑op */ }

    @Override
    public void addWarn(String msg) { /* no‑op */ }

    @Override
    public void addWarn(String msg, Throwable ex) { /* no‑op */ }

    @Override
    public void addError(String msg) { /* no‑op */ }

    @Override
    public void addError(String msg, Throwable ex) { /* no‑op */ }

    @Override
    public ch.qos.logback.core.spi.StatusManager getStatusManager() {
        return null;
    }
}

/**
 * Test suite.
 */
public class JoranConfiguratorBaseTest {

    private TestJoranConfigurator configurator;
    private ContextBase context;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
        context = new ContextBase();
        configurator.setContext(context);
    }

    // ----------------------------------------------------------------------
    // 1. Test getInterpretationContext()
    // ----------------------------------------------------------------------
    @Test
    public void testGetInterpretationContext_returnsNonNullAfterImplicitRuleSupplierSet()
            throws Exception {

        // a simple RuleStore implementation provided by Logback
        RuleStore ruleStore = new ch.qos.logback.core.joran.spi.SimpleRuleStore();

        // an empty element path – the concrete value does not matter for this test
        ElementPath elementPath = new ElementPath();

        // create interpreter with empty SaxEvent list
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                context, ruleStore, elementPath, new ArrayList<>());

        // invoke the method that registers the implicit action supplier
        configurator.callSetImplicitRuleSupplier(interpreter);

        // now the interpreter must have a non‑null SaxEventInterpretationContext
        SaxEventInterpretationContext ic = configurator.getInterpretationContext();
        assertNotNull("Interpretation context should not be null after setting implicit rule supplier",
                ic);
    }

    // ----------------------------------------------------------------------
    // 2. Test buildModelInterpretationContext()
    // ----------------------------------------------------------------------
    @Test
    public void testBuildModelInterpretationContext_initialisesModelInterpretationContext()
            throws Exception {
        // the super implementation creates a ModelInterpretationContext instance.
        configurator.buildModelInterpretationContext();

        assertNotNull("ModelInterpretationContext must be created",
                configurator.getModelInterpretationContext());

        // The concrete implementation adds an "appender bag" – we simply verify
        // that no exception is thrown and that the context object is still the one we set.
        assertSame("Context must be the one we injected", context, configurator.getContext());
    }

    // ----------------------------------------------------------------------
    // 3. Test sanityCheck() – normal flow
    // ----------------------------------------------------------------------
    @Test
    public void testSanityCheck_executesCheckerWithoutException() {
        Model model = new Model(); // a plain model instance
        // The method internally uses AppenderWithinAppenderSanityChecker which
        // does not throw for a simple Model.
        configurator.callSanityCheck(model);
        // No exception → test passes
    }

    // ----------------------------------------------------------------------
    // 4. Test sanityCheck() – edge case: null argument
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testSanityCheck_nullModel_throwsNPE() {
        configurator.callSanityCheck(null);
    }

    // ----------------------------------------------------------------------
    // 5. Test performCheck() – normal flow with a custom checker that also
    //    implements ContextAware
    // ----------------------------------------------------------------------
    @Test
    public void testPerformCheck_setsContextAndInvokesCheck() {
        Model model = new Model();
        RecordingChecker checker = new RecordingChecker();

        configurator.callPerformCheck(checker, model);

        assertTrue("Checker.check(..) must have been invoked", checker.isCheckInvoked());
        assertSame("Checker must have received the configurator's context",
                context, checker.getContext());
    }

    // ----------------------------------------------------------------------
    // 6. Test performCheck() – edge case: null SanityChecker
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testPerformCheck_nullChecker_throwsNPE() {
        Model model = new Model();
        configurator.callPerformCheck(null, model);
    }

    // ----------------------------------------------------------------------
    // 7. Test addModelHandlerAssociations() – ensure no exception is thrown.
    //    The method registers many handler factories; we simply verify that the
    //    DefaultProcessor receives them without blowing up.
    // ----------------------------------------------------------------------
    @Test
    public void testAddModelHandlerAssociations_registersHandlers() {
        DefaultProcessor processor = new DefaultProcessor(context,
                configurator.getModelInterpretationContext());

        // The method is protected; we expose it via the test subclass.
        configurator.callAddModelHandlerAssociations(processor);

        // The DefaultProcessor does not expose the internal registry, but we can
        // at least assert that the processor instance is still usable (e.g. process a dummy model)
        Model dummy = new Model();
        // processing should not throw
        processor.process(dummy);
    }

    // ----------------------------------------------------------------------
    // 8. Test that the implicit rule supplier is correctly set to
    //    ImplicitModelAction::new
    // ----------------------------------------------------------------------
    @Test
    public void testSetImplicitRuleSupplier_registersImplicitAction() throws Exception {
        RuleStore ruleStore = new ch.qos.logback.core.joran.spi.SimpleRuleStore();
        ElementPath elementPath = new ElementPath();
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                context, ruleStore, elementPath, new ArrayList<>());

        configurator.callSetImplicitRuleSupplier(interpreter);

        // The interpreter now holds a Supplier<Action> that creates
        // ImplicitModelAction instances. We verify that invoking the supplier
        // yields a non‑null Action of the expected type.
        java.util.function.Supplier<ch.qos.logback.core.joran.action.Action> supplier =
                interpreter.getImplicitActionSupplier();

        assertNotNull("Implicit action supplier must be set", supplier);
        ch.qos.logback.core.joran.action.Action action = supplier.get();
        assertTrue("Implicit action must be an instance of ImplicitModelAction",
                action instanceof ch.qos.logback.core.joran.action.ImplicitModelAction);
    }

}
