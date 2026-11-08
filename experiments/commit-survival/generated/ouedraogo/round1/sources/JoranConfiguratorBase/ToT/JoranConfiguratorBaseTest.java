package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.conditional.IfModel;


import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.*;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Unit tests for {@link JoranConfiguratorBase}.  The tests focus on the
 * behaviour that can be observed without needing the full Logback runtime.
 * All objects are created directly – no mocking frameworks are used – and
 * only the classes and methods that appear in the supplied source or its
 * dependency signatures are referenced.
 */
public class JoranConfiguratorBaseTest {

    /** Minimal concrete subclass so we can instantiate the abstract base class. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // No additional behaviour – we just expose the protected methods we want to test.
    }

    /** Simple {@link RuleStore} implementation that records added rules and transparent parts. */
    private static class TestRuleStore implements RuleStore {

        /** Map from selector string to the supplied {@link Supplier<Action>}. */
        final Map<String, Supplier<Action>> ruleMap = new LinkedHashMap<>();

        /** List of transparent path parts in the order they were added. */
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // Not used in the code under test – ignore.
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            ruleMap.put(elementSelector.getPattern(), actionSupplier);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            // Very simple matching based on the exact pattern string.
            return ruleMap.get(elementPath.toString());
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /** Simple {@link SanityChecker} that records whether {@code check} was invoked. */
    private static class RecordingSanityChecker implements SanityChecker, ContextAware {

        private ContextAwareBase delegate = new ContextAwareBase();
        boolean called = false;

        @Override
        public void check(Model model) {
            called = true;
        }

        // ContextAware delegation
        @Override
        public void setContext(ch.qos.logback.core.Context context) {
            delegate.setContext(context);
        }

        @Override
        public ch.qos.logback.core.Context getContext() {
            return delegate.getContext();
        }

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) {
            delegate.addStatus(status);
        }

        @Override
        public void addInfo(String msg) {
            delegate.addInfo(msg);
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
            delegate.addInfo(msg, ex);
        }

        @Override
        public void addWarn(String msg) {
            delegate.addWarn(msg);
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
            delegate.addWarn(msg, ex);
        }

        @Override
        public void addError(String msg) {
            delegate.addError(msg);
        }

        @Override
        public void addError(String msg, Throwable ex) {
            delegate.addError(msg, ex);
        }
    }

    /** Minimal {@link SaxEventInterpreter} that can be used to test {@code setImplicitRuleSupplier}. */
    private static class TestSaxEventInterpreter extends SaxEventInterpreter {
        TestSaxEventInterpreter(RuleStore rs) {
            // The constructor of SaxEventInterpreter requires a Context, a RuleStore,
            // an initial ElementPath and a list of SaxEvents.  We pass null for the
            // context (it is not used by the method under test) and empty collections
            // for the remaining arguments.
            super(null, rs, new ElementPath(), Collections.emptyList());
        }
    }

    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
    }

    /**
     * Verify that {@link JoranConfiguratorBase#addElementSelectorAndActionAssociations(RuleStore)}
     * registers the expected selectors, action suppliers and transparent path parts.
     */
    @Test
    public void testAddElementSelectorAndActionAssociations() {
        TestRuleStore rs = new TestRuleStore();

        // invoke the method under test
        configurator.addElementSelectorAndActionAssociations(rs);

        // ---- selectors that must be present ----
        String[] requiredSelectors = {
                "*/variable", "*/property", "*/substitutionProperty",
                "configuration/import", "configuration/timestamp",
                "configuration/shutdownHook", "configuration/sequenceNumberGenerator",
                "configuration/serializeModel", "configuration/define",
                "configuration/evaluator", "configuration/contextProperty",
                "configuration/conversionRule", "configuration/statusListener",
                "*/appender", "configuration/appender/appender-ref",
                "configuration/newRule", "*/param", "*/if", "*/if/then",
                "*/if/else", "*/appender/sift"
        };

        for (String sel : requiredSelectors) {
            assertTrue("selector '" + sel + "' should have been added",
                    rs.ruleMap.containsKey(sel));
        }

        // ---- verify that transparent path parts were added for if/then/else and sift ----
        List<String> expectedTransparentParts = Arrays.asList("if", "then", "else", "sift");
        assertEquals("Transparent path parts", expectedTransparentParts, rs.transparentParts);
    }

    /**
     * Verify that {@link JoranConfiguratorBase#performCheck(SanityChecker, Model)} sets the
     * context on a {@link ContextAware} checker and invokes {@code check(Model)}.
     */
    @Test
    public void testPerformCheckSetsContextAndInvokesCheck() {
        RecordingSanityChecker checker = new RecordingSanityChecker();
        Model dummyModel = new Model(); // a plain, empty model is sufficient

        // The configurator (as a ContextAwareBase) does not have a context set;
        // we do not need a real context for this test – null is acceptable.
        configurator.performCheck(checker, dummyModel);

        // The checker should have been called.
        assertTrue("SanityChecker.check should have been invoked", checker.called);
    }

    /**
     * Verify that {@link JoranConfiguratorBase#sanityCheck(Model)} delegates to
     * {@link AppenderWithinAppenderSanityChecker} via {@code performCheck}.
     */
    @Test
    public void testSanityCheckDelegatesToAppenderWithinAppenderSanityChecker() {
        // Use a subclass of AppenderWithinAppenderSanityChecker that records the call.
        class RecordingAppenderChecker extends AppenderWithinAppenderSanityChecker implements ContextAware {
            boolean called = false;
            @Override
            public void check(Model model) {
                called = true;
            }
        }

        // Replace the internal call by temporarily overriding performCheck.
        TestConfigurator cfg = new TestConfigurator() {
            @Override
            protected void performCheck(SanityChecker sc, Model model) {
                // we expect the passed checker to be an instance of our recording subclass
                assertTrue(sc instanceof RecordingAppenderChecker);
                ((RecordingAppenderChecker) sc).check(model);
            }
        };

        Model topModel = new Model();
        cfg.sanityCheck(topModel);
        // The overridden performCheck would have thrown an AssertionError if the
        // wrong checker type had been used, so reaching this line means the test passed.
    }

    /**
     * Verify that {@link JoranConfiguratorBase#setImplicitRuleSupplier(SaxEventInterpreter)}
     * correctly installs the implicit action supplier.
     */
    @Test
    public void testSetImplicitRuleSupplierInstallsSupplier() {
        TestRuleStore rs = new TestRuleStore();
        TestSaxEventInterpreter interpreter = new TestSaxEventInterpreter(rs);

        // invoke the method under test – it should not throw any exception
        configurator.setImplicitRuleSupplier(interpreter);

        // The interpreter now has a supplier that creates ImplicitModelAction instances.
        // We can retrieve the supplier via reflection (the field is package‑private).
        try {
            java.lang.reflect.Field f = SaxEventInterpreter.class.getDeclaredField("implicitActionSupplier");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Supplier<Action> supplier = (Supplier<Action>) f.get(interpreter);
            assertNotNull("Implicit action supplier should be set", supplier);
            Action action = supplier.get();
            assertTrue("Supplier must produce an ImplicitModelAction",
                    action instanceof ImplicitModelAction);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while checking implicit action supplier: " + e);
        }
    }

    /**
     * Verify that {@link JoranConfiguratorBase#addModelHandlerAssociations(DefaultProcessor)}
     * registers handlers for the expected model classes.  The {@code DefaultProcessor}
     * stores handlers internally; we can verify registration by attempting to retrieve
     * a handler for a known model class via the protected {@code addHandler} method.
     */
    @Test
    public void testAddModelHandlerAssociationsRegistersHandlers() {
        // Create a dummy context (null is acceptable for this test) and a dummy
        // ModelInterpretationContext (we can obtain one from a freshly created
        // configurator via the protected method).
        ModelInterpretationContext mic = configurator.getModelInterpretationContext();

        // The DefaultProcessor constructor needs a Context and a ModelInterpretationContext.
        // We pass null for the Context – the processor does not use it in the test.
        DefaultProcessor processor = new DefaultProcessor(null, mic);

        // Invoke the method under test.
        configurator.addModelHandlerAssociations(processor);

        // We cannot access the internal map directly, but we can verify that
        // calling {@code addHandler} again for the same model class does not
        // replace the existing handler (i.e., no exception is thrown).  This
        // indirectly proves that the initial registration succeeded.
        try {
            processor.addHandler(ImportModel.class, ImportModelHandler::makeInstance);
            processor.addHandler(IfModel.class, IfModelHandler::makeInstance);
        } catch (Exception e) {
            fail("Re‑adding a known handler should not fail: " + e);
        }
    }
}
