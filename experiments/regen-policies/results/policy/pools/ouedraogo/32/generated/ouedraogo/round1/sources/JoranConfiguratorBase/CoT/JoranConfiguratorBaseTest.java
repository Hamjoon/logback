package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.ElementSelector;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.Context;

/**
 * Unit tests for {@link JoranConfiguratorBase}.  The class under test is
 * abstract, therefore a concrete test subclass is created that simply
 * inherits the behaviour of {@code JoranConfiguratorBase}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API methods: {@code buildModelInterpretationContext()} and
 *       {@code getInterpretationContext()}.</li>
 *   <li>Protected helper methods: {@code sanityCheck(Model)},
 *       {@code performCheck(SanityChecker, Model)}.</li>
 *   <li>Edge‑case handling such as {@code null} arguments and the
 *       interaction with {@link ContextAware} implementations.</li>
 * </ul>
 */
public class JoranConfiguratorBaseTest {

    /** Simple {@link Context} implementation used only for the tests. */
    private static class SimpleContext implements Context {
        // The Context interface has many methods; for the purpose of these
        // tests we only need a stub implementation. All methods are no‑ops.
        @Override public String getName() { return "test"; }
        @Override public void setName(String name) {}
        @Override public void start() {}
        @Override public void stop() {}
        @Override public boolean isStarted() { return true; }
        @Override public boolean isStopped() { return false; }
        @Override public void addListener(ch.qos.logback.core.spi.LifeCycleListener listener) {}
        @Override public void removeListener(ch.qos.logback.core.spi.LifeCycleListener listener) {}
        @Override public List<ch.qos.logback.core.spi.LifeCycleListener> getLifeCycleListeners() { return Collections.emptyList(); }
        @Override public void addStatus(ch.qos.logback.core.status.Status status) {}
        @Override public void addInfo(String msg) {}
        @Override public void addInfo(String msg, Throwable ex) {}
        @Override public void addWarn(String msg) {}
        @Override public void addWarn(String msg, Throwable ex) {}
        @Override public void addError(String msg) {}
        @Override public void addError(String msg, Throwable ex) {}
        @Override public ch.qos.logback.core.status.StatusManager getStatusManager() { return null; }
        @Override public void setStatusManager(ch.qos.logback.core.status.StatusManager sm) {}
        @Override public void reset() {}
    }

    /** Minimal {@link RuleStore} implementation – it records nothing. */
    private static class DummyRuleStore implements RuleStore {
        @Override public void addRule(ElementSelector elementSelector, String actionClassStr) {}
        @Override public void addRule(ElementSelector elementSelector, java.util.function.Supplier<Action> actionSupplier) {}
        @Override public java.util.function.Supplier<Action> matchActions(ElementPath elementPath) { return null; }
        @Override public void addTransparentPathPart(String pathPart) {}
    }

    /** Minimal {@link SaxEventInterpreter} that only creates a real
     *  {@link SaxEventInterpretationContext}. */
    private static class DummySaxEventInterpreter extends SaxEventInterpreter {
        public DummySaxEventInterpreter(Context context) {
            super(context, new DummyRuleStore(), new ElementPath(), Collections.emptyList());
        }
    }

    /** Concrete subclass of {@link JoranConfiguratorBase} exposing the
     *  protected methods for testing. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        /** expose {@code sanityCheck} as public for the test */
        @Override
        public void sanityCheck(Model topModel) {
            super.sanityCheck(topModel);
        }

        /** expose {@code performCheck} as public for the test */
        @Override
        public void performCheck(SanityChecker sc, Model model) {
            super.performCheck(sc, model);
        }

        /** expose {@code addModelHandlerAssociations} – not used directly */
        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            super.addModelHandlerAssociations(defaultProcessor);
        }
    }

    private TestConfigurator configurator;
    private SimpleContext context;

    @Before
    public void setUp() {
        context = new SimpleContext();
        configurator = new TestConfigurator();
        configurator.setContext(context);
    }

    /** --------------------------------------------------------------------
     *  Public method tests
     *  --------------------------------------------------------------------
     */

    @Test
    public void testBuildModelInterpretationContextDoesNotThrow() throws Exception {
        // The method is public; we simply invoke it and verify that the
        // internal modelInterpretationContext is created (non‑null).
        configurator.buildModelInterpretationContext();

        // Use reflection to obtain the protected field from the super class.
        java.lang.reflect.Field micField = GenericXMLConfigurator.class
                .getDeclaredField("modelInterpretationContext");
        micField.setAccessible(true);
        Object mic = micField.get(configurator);
        assertNotNull("modelInterpretationContext should be initialised", mic);
    }

    @Test
    public void testGetInterpretationContextReturnsNonNull() throws Exception {
        // Prepare a dummy interpreter and inject it via reflection.
        DummySaxEventInterpreter dummyInterpreter = new DummySaxEventInterpreter(context);

        java.lang.reflect.Field interpreterField = GenericXMLConfigurator.class
                .getDeclaredField("saxEventInterpreter");
        interpreterField.setAccessible(true);
        interpreterField.set(configurator, dummyInterpreter);

        SaxEventInterpretationContext ic = configurator.getInterpretationContext();
        assertNotNull("InterpretationContext should not be null after interpreter injection", ic);
        // Verify that the context inside the interpretation context is the one we set.
        assertEquals("Context inside InterpretationContext must be the test context",
                context, ic.getContext());
    }

    /** --------------------------------------------------------------------
     *  Protected helper method tests
     *  --------------------------------------------------------------------
     */

    @Test
    public void testSanityCheckInvokesCheckerWithoutException() {
        Model dummyModel = new Model() {
            // No extra behaviour needed – the base class is sufficient.
        };
        // The real checker (AppenderWithinAppenderSanityChecker) should accept the
        // dummy model without throwing.
        configurator.sanityCheck(dummyModel);
        // No exception means success.
    }

    @Test
    public void testPerformCheckCallsSetContextWhenCheckerIsContextAware() {
        // A custom checker that records whether setContext(..) was invoked.
        class RecordingChecker implements SanityChecker, ContextAware {
            boolean contextSet = false;
            @Override public void setContext(Context ctx) { contextSet = true; }
            @Override public Context getContext() { return null; }
            @Override public void addStatus(ch.qos.logback.core.status.Status status) {}
            @Override public void addInfo(String msg) {}
            @Override public void addInfo(String msg, Throwable ex) {}
            @Override public void addWarn(String msg) {}
            @Override public void addWarn(String msg, Throwable ex) {}
            @Override public void addError(String msg) {}
            @Override public void addError(String msg, Throwable ex) {}
            @Override public void check(Model model) {}
        }

        RecordingChecker checker = new RecordingChecker();
        Model dummyModel = new Model() {};

        configurator.performCheck(checker, dummyModel);
        assertTrue("Context should have been set on a ContextAware checker", checker.contextSet);
    }

    @Test
    public void testPerformCheckDoesNotFailWhenCheckerIsNotContextAware() {
        // A checker that does NOT implement ContextAware.
        class SimpleChecker implements SanityChecker {
            boolean checkCalled = false;
            @Override public void check(Model model) { checkCalled = true; }
        }

        SimpleChecker checker = new SimpleChecker();
        Model dummyModel = new Model() {};

        configurator.performCheck(checker, dummyModel);
        assertTrue("check(...) must be invoked", checker.checkCalled);
        // No exception – the method must gracefully handle non‑ContextAware checkers.
    }

    @Test(expected = NullPointerException.class)
    public void testPerformCheckThrowsNPEWhenModelIsNull() {
        // The implementation does not guard against a null model; it will be
        // passed to the checker which will typically cause a NPE. This test
        // documents that behaviour.
        SanityChecker checker = new AppenderWithinAppenderSanityChecker();
        configurator.performCheck(checker, null);
    }

    @Test(expected = NullPointerException.class)
    public void testSanityCheckThrowsNPEWhenModelIsNull() {
        configurator.sanityCheck(null);
    }
}
