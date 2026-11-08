package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.status.StatusManager;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.StatusManager;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Unit tests for {@link JoranConfiguratorBase}.  The tests use concrete
 * subclasses that expose the protected API and simple stub implementations of
 * the library interfaces (RuleStore, Context, etc.) so that no external
 * Logback dependencies are required.
 */
public class JoranConfiguratorBaseTest {

    /** Simple stub for {@link Context}. */
    private static class DummyContext implements Context {
        @Override public String getName() { return "dummy"; }
        @Override public void setName(String name) {}
        @Override public StatusManager getStatusManager() { return null; }
        @Override public void addStatusListener(ch.qos.logback.core.status.StatusListener listener) {}
        @Override public void removeStatusListener(ch.qos.logback.core.status.StatusListener listener) {}
        @Override public void reset() {}
        @Override public void putObject(String key, Object value) {}
        @Override public Object getObject(String key) { return null; }
        @Override public void putProperty(String key, String value) {}
        @Override public String getProperty(String key) { return null; }
        @Override public void addInstanceListener(ch.qos.logback.core.spi.LifeCycleListener listener) {}
        @Override public void removeInstanceListener(ch.qos.logback.core.spi.LifeCycleListener listener) {}
        @Override public void start() {}
        @Override public void stop() {}
        @Override public boolean isStarted() { return false; }
        @Override public void register(Object obj) {}
    }

    /** Stub implementation of {@link RuleStore} that records added rules. */
    private static class RecordingRuleStore implements RuleStore {
        static class RuleEntry {
            final ElementSelector selector;
            final Supplier<Action> supplier;
            RuleEntry(ElementSelector s, Supplier<Action> sup) {
                this.selector = s;
                this.supplier = sup;
            }
        }

        final List<RuleEntry> rules = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // Not used in the code under test.
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            rules.add(new RuleEntry(elementSelector, actionSupplier));
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return null;
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /** Minimal {@link Model} implementation used for sanity‑check tests. */
    private static class DummyModel extends Model {
        private static final long serialVersionUID = 1L;
    }

    /** A {@link SanityChecker} that also implements {@link ContextAware}. */
    private static class DummyChecker implements SanityChecker, ContextAware {
        boolean contextSet = false;
        boolean checkCalled = false;
        Model receivedModel = null;
        Context ctx = null;

        @Override
        public void setContext(Context context) {
            this.contextSet = true;
            this.ctx = context;
        }

        @Override
        public Context getContext() {
            return ctx;
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

        @Override
        public void check(Model model) {
            this.checkCalled = true;
            this.receivedModel = model;
        }
    }

    /** A {@link SaxEventInterpreter} that records the supplied implicit action supplier. */
    private static class RecordingInterpreter extends SaxEventInterpreter {
        Supplier<Action> recordedSupplier = null;

        public RecordingInterpreter(Context ctx, RuleStore rs, ElementPath ep, List<SaxEvent> events) {
            super(ctx, rs, ep, events);
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.recordedSupplier = actionSupplier;
        }

        public Supplier<Action> getRecordedSupplier() {
            return recordedSupplier;
        }
    }

    /** Concrete subclass exposing protected methods for testing. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        // expose the protected method for direct invocation
        public void exposeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }

        // expose sanityCheck – we keep the original implementation
        public void exposeSanityCheck(Model topModel) {
            super.sanityCheck(topModel);
        }

        // expose performCheck – we keep the original implementation
        public void exposePerformCheck(SanityChecker sc, Model model) {
            super.performCheck(sc, model);
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op – not needed for these tests
        }
    }

    /** Subclass that overrides {@code performCheck} to capture arguments. */
    private static class CapturePerformCheckConfigurator extends JoranConfiguratorBase<Object> {
        SanityChecker capturedChecker;
        Model capturedModel;

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            this.capturedChecker = sc;
            this.capturedModel = model;
            // delegate to super to keep behaviour (not required for the test)
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op
        }
    }

    private TestConfigurator configurator;
    private DummyContext dummyContext;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        dummyContext = new DummyContext();
        configurator.setContext(dummyContext);
    }

    @Test
    public void testAddElementSelectorAndActionAssociations() {
        RecordingRuleStore rs = new RecordingRuleStore();

        configurator.exposeAddElementSelectorAndActionAssociations(rs);

        // Verify that a few known selectors are present
        assertTrue("selector */variable must be registered",
                rs.rules.stream().anyMatch(e -> e.selector.getPath().equals("*/variable")));

        assertTrue("selector configuration/appender/appender-ref must be registered",
                rs.rules.stream().anyMatch(e -> e.selector.getPath().equals("configuration/appender/appender-ref")));

        // Verify transparent path parts
        List<String> expectedTransparent = List.of("if", "then", "else", "sift");
        assertEquals("Transparent parts must match expected list", expectedTransparent, rs.transparentParts);
    }

    @Test
    public void testSanityCheckInvokesPerformCheckWithCorrectChecker() {
        CapturePerformCheckConfigurator cfg = new CapturePerformCheckConfigurator();
        cfg.setContext(dummyContext);
        DummyModel model = new DummyModel();

        cfg.exposeSanityCheck(model);

        assertNotNull("performCheck should have been called with a checker", cfg.capturedChecker);
        assertTrue("Checker must be an instance of AppenderWithinAppenderSanityChecker",
                cfg.capturedChecker instanceof AppenderWithinAppenderSanityChecker);
        assertSame("Model passed to performCheck must be the same as supplied", model, cfg.capturedModel);
    }

    @Test
    public void testPerformCheckSetsContextWhenCheckerIsContextAware() {
        DummyChecker checker = new DummyChecker();
        DummyModel model = new DummyModel();

        configurator.exposePerformCheck(checker, model);

        assertTrue("Context should be set on the checker", checker.contextSet);
        assertSame("The context set on the checker must be the configurator's context",
                dummyContext, checker.getContext());
        assertTrue("Checker.check() must have been invoked", checker.checkCalled);
        assertSame("The model passed to check() must be the same as supplied", model, checker.receivedModel);
    }

    @Test
    public void testSetImplicitRuleSupplier() {
        RecordingInterpreter interpreter = new RecordingInterpreter(
                dummyContext,
                new RecordingRuleStore(),
                new ElementPath(),
                Collections.emptyList());

        configurator.setImplicitRuleSupplier(interpreter);

        Supplier<Action> supplier = interpreter.getRecordedSupplier();
        assertNotNull("Implicit action supplier must be recorded", supplier);

        // The supplier should produce an instance of ImplicitModelAction
        Action action = supplier.get();
        assertTrue("Supplier must create an ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    @Test
    public void testGetInterpretationContextReturnsInterpreterContext() {
        // Build a configurator that holds a known interpreter instance
        RecordingInterpreter interpreter = new RecordingInterpreter(
                dummyContext,
                new RecordingRuleStore(),
                new ElementPath(),
                Collections.emptyList());

        // Use reflection to set the protected field `saxEventInterpreter`
        try {
            java.lang.reflect.Field f = GenericXMLConfigurator.class.getDeclaredField("saxEventInterpreter");
            f.setAccessible(true);
            f.set(configurator, interpreter);
        } catch (Exception e) {
            fail("Failed to inject interpreter via reflection: " + e);
        }

        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull("Interpretation context must not be null", ctx);
        assertSame("Interpretation context must be the one held by the interpreter",
                interpreter.getSaxEventInterpretationContext(), ctx);
    }
}
