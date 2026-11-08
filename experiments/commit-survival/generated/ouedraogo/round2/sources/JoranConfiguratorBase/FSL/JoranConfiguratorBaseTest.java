package ch.qos.logback.core.joran;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.joran.conditional.IfAction;
import ch.qos.logback.core.joran.conditional.ThenAction;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.joran.action.ImplicitModelAction;

/**
 * Unit tests for {@link JoranConfiguratorBase}.
 * <p>
 * The tests focus on the protected methods that constitute the
 * public contract of the class.  A concrete test subclass is used to
 * expose those methods and to capture internal interactions.
 */
public class JoranConfiguratorBaseTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}
     * that does not add any extra behaviour.
     */
    private static class ConcreteJoranConfigurator extends JoranConfiguratorBase<Object> {
        // No extra implementation – all behaviour comes from the base class.
    }

    /**
     * Test double for {@link RuleStore} that records the selectors that are
     * added as well as the transparent path parts.
     */
    private static class RecordingRuleStore implements RuleStore {

        final List<ElementSelector> selectors = new ArrayList<>();
        final List<String> actionDescriptions = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            selectors.add(elementSelector);
            actionDescriptions.add(actionClassStr);
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            selectors.add(elementSelector);
            // Record the class name of the supplied Action for verification.
            Action a = actionSupplier.get();
            actionDescriptions.add(a != null ? a.getClass().getName() : "null");
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return null; // not needed for the tests
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /**
     * A {@link SanityChecker} that also implements {@link ContextAware}
     * so that we can verify that {@code setContext} is invoked.
     */
    private static class DummySanityChecker implements SanityChecker, ContextAware {

        private ContextAwareBase contextHolder = new ContextAwareBase();
        boolean checkInvoked = false;

        @Override
        public void check(Model model) {
            checkInvoked = true;
        }

        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            contextHolder.setContext(context);
        }

        @Override
        public ch.qos.logback.core.spi.Context getContext() {
            return contextHolder.getContext();
        }

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) {
            // no‑op
        }

        @Override
        public void addInfo(String msg) {
            // no‑op
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
            // no‑op
        }

        @Override
        public void addWarn(String msg) {
            // no‑op
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
            // no‑op
        }

        @Override
        public void addError(String msg) {
            // no‑op
        }

        @Override
        public void addError(String msg, Throwable ex) {
            // no‑op
        }
    }

    /**
     * Subclass that overrides {@code performCheck} so that the test can
     * capture the arguments passed by {@code sanityCheck}.
     */
    private static class SanityCheckingJoranConfigurator extends JoranConfiguratorBase<Object> {

        SanityChecker capturedChecker;
        Model capturedModel;

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            this.capturedChecker = sc;
            this.capturedModel = model;
        }
    }

    /**
     * Exposes the protected {@code performCheck} method for testing.
     */
    private static class ExposedPerformCheckConfigurator extends JoranConfiguratorBase<Object> {
        void invokePerformCheck(SanityChecker sc, Model model) {
            super.performCheck(sc, model);
        }
    }

    /**
     * Simple {@link SaxEventInterpreter} subclass that returns a pre‑created
     * {@link SaxEventInterpretationContext}.  This avoids the need for a real
     * {@link ElementPath} implementation.
     */
    private static class TestSaxEventInterpreter extends SaxEventInterpreter {
        private final SaxEventInterpretationContext testContext;

        TestSaxEventInterpreter(SaxEventInterpretationContext ctx) {
            super(null, null, null, Collections.emptyList());
            this.testContext = ctx;
        }

        @Override
        public SaxEventInterpretationContext getSaxEventInterpretationContext() {
            return testContext;
        }
    }

    /* --------------------------------------------------------------------- */
    /* Tests                                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddElementSelectorAndActionAssociations() throws Exception {
        ConcreteJoranConfigurator cfg = new ConcreteJoranConfigurator();
        RecordingRuleStore rs = new RecordingRuleStore();

        // invoke the protected method via reflection
        Field methodField = JoranConfiguratorBase.class.getDeclaredMethod(
                "addElementSelectorAndActionAssociations", RuleStore.class).getDeclaringClass()
                .getDeclaredMethod("addElementSelectorAndActionAssociations", RuleStore.class)
                .getClass();
        cfg.addElementSelectorAndActionAssociations(rs);

        // Expected number of rules (see source code)
        assertEquals("Number of element selectors added", 21, rs.selectors.size());

        // Verify a few representative selectors are present
        assertTrue(rs.selectors.stream()
                .anyMatch(es -> "*/variable".equals(es.getPattern())));
        assertTrue(rs.selectors.stream()
                .anyMatch(es -> "configuration/import".equals(es.getPattern())));
        assertTrue(rs.selectors.stream()
                .anyMatch(es -> "*/appender/sift".equals(es.getPattern())));

        // Transparent path parts
        assertEquals("Transparent path parts count", 4, rs.transparentParts.size());
        assertTrue(rs.transparentParts.contains("if"));
        assertTrue(rs.transparentParts.contains("then"));
        assertTrue(rs.transparentParts.contains("else"));
        assertTrue(rs.transparentParts.contains("sift"));
    }

    @Test
    public void testSetImplicitRuleSupplierAssignsCorrectSupplier() throws Exception {
        ConcreteJoranConfigurator cfg = new ConcreteJoranConfigurator();

        // Create a minimal interpreter (the constructor allows nulls for the
        // parameters that are not needed for this test)
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                null, null, null, Collections.emptyList());

        // Call the method under test
        cfg.setImplicitRuleSupplier(interpreter);

        // Use reflection to obtain the private field that holds the supplier
        Field supplierField = SaxEventInterpreter.class.getDeclaredField("implicitActionSupplier");
        supplierField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Supplier<Action> supplier = (Supplier<Action>) supplierField.get(interpreter);

        assertNotNull("Implicit action supplier must not be null", supplier);
        Action action = supplier.get();
        assertNotNull("Supplier must produce a non‑null Action", action);
        assertTrue("Supplier must produce an ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    @Test
    public void testSanityCheckDelegatesToPerformCheck() {
        SanityCheckingJoranConfigurator cfg = new SanityCheckingJoranConfigurator();
        Model dummyModel = new Model(); // Model has a public no‑arg constructor

        cfg.sanityCheck(dummyModel);

        // The overridden performCheck should have captured the arguments
        assertNotNull("Checker should have been captured", cfg.capturedChecker);
        assertNotNull("Model should have been captured", cfg.capturedModel);
        assertSame("Captured model must be the one passed in", dummyModel, cfg.capturedModel);
        // The checker used by the base class is an AppenderWithinAppenderSanityChecker
        assertTrue("Checker must be an instance of AppenderWithinAppenderSanityChecker",
                cfg.capturedChecker instanceof AppenderWithinAppenderSanityChecker);
    }

    @Test
    public void testPerformCheckSetsContextWhenCheckerIsContextAware() {
        ExposedPerformCheckConfigurator cfg = new ExposedPerformCheckConfigurator();

        // Prepare a dummy context (null is acceptable for the purpose of this test)
        cfg.setContext(null);

        DummySanityChecker checker = new DummySanityChecker();
        Model dummyModel = new Model();

        // Invoke the protected method via the public wrapper
        cfg.invokePerformCheck(checker, dummyModel);

        // The checker should have received the configurator's context (null in this case)
        assertSame("Context should have been set on the checker", cfg.getContext(), checker.getContext());

        // Ensure that the check() method was indeed called
        assertTrue("check() on the SanityChecker must have been invoked", checker.checkInvoked);
    }

    @Test
    public void testGetInterpretationContextReturnsInterpreterContext() throws Exception {
        ConcreteJoranConfigurator cfg = new ConcreteJoranConfigurator();

        // Create a SaxEventInterpretationContext (constructor allows nulls)
        SaxEventInterpretationContext ctx = new SaxEventInterpretationContext(null, null);
        TestSaxEventInterpreter interpreter = new TestSaxEventInterpreter(ctx);

        // Inject the interpreter into the configurator via reflection
        Field interpreterField = GenericXMLConfigurator.class.getDeclaredField("saxEventInterpreter");
        interpreterField.setAccessible(true);
        interpreterField.set(cfg, interpreter);

        // Verify that getInterpretationContext returns the same instance
        SaxEventInterpretationContext returned = cfg.getInterpretationContext();
        assertSame("Returned interpretation context must be the one supplied by the interpreter", ctx, returned);
    }

    @Test
    public void testBuildModelInterpretationContextExecutesWithoutException() {
        ConcreteJoranConfigurator cfg = new ConcreteJoranConfigurator();

        // The method should complete without throwing an exception.
        // No further state verification is possible without accessing
        // internal members of GenericXMLConfigurator.
        cfg.buildModelInterpretationContext();
    }

    @Test
    public void testAddModelHandlerAssociationsExecutesWithoutException() {
        ConcreteJoranConfigurator cfg = new ConcreteJoranConfigurator();

        // DefaultProcessor can be instantiated with null arguments for the purpose
        // of this test because the method only registers handler factories.
        DefaultProcessor dp = new DefaultProcessor(null, null);
        cfg.addModelHandlerAssociations(dp);
        // No exception means the registration succeeded.
    }
}
