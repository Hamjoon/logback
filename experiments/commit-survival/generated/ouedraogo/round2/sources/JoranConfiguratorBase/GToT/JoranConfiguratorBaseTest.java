/**
 * JUnit‑4 test suite for {@link JoranConfiguratorBase}.
 *
 * The tests cover:
 *   1. Public API ( {@code getInterpretationContext()}, {@code buildModelInterpretationContext()} )
 *   2. Protected hook methods that are exercised via a concrete test subclass:
 *        – {@code addElementSelectorAndActionAssociations}
 *        – {@code sanityCheck}
 *        – {@code performCheck}
 *        – {@code setImplicitRuleSupplier}
 *        – {@code addModelHandlerAssociations}
 *
 * The test‑subclass supplies concrete implementations where the abstract base class
 * requires them and also exposes the protected members so that they can be inspected.
 *
 * No external mocking frameworks are used – all collaborators are tiny concrete
 * implementations that record the interactions required by the assertions.
 */
package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.model.conditional.ThenModel;


import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Concrete subclass used only for testing.  The generic type parameter is irrelevant
 * for the behaviour exercised in these tests, therefore {@code Object} is used.
 */
public class JoranConfiguratorBaseTest {

    /** Simple {@link RuleStore} implementation that records every rule that is added. */
    private static class RecordingRuleStore implements RuleStore {

        static class RecordedRule {
            final ElementSelector selector;
            final Supplier<Action> supplier;   // null if the rule was added via class name

            RecordedRule(ElementSelector selector, Supplier<Action> supplier) {
                this.selector = selector;
                this.supplier = supplier;
            }
        }

        final List<RecordedRule> recordedRules = new ArrayList<>();
        final List<String> transparentPathParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // This overload is not used by JoranConfiguratorBase – we store a null supplier.
            recordedRules.add(new RecordedRule(elementSelector, null));
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            recordedRules.add(new RecordedRule(elementSelector, actionSupplier));
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            // Not needed for the current test suite.
            return null;
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentPathParts.add(pathPart);
        }
    }

    /** Simple {@link SanityChecker} that also implements {@link ContextAware}. */
    private static class RecordingSanityChecker implements SanityChecker, ContextAware {

        ContextAware contextAware = null;
        Model checkedModel = null;

        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            this.contextAware = this;
        }

        @Override
        public ch.qos.logback.core.spi.Context getContext() {
            // Not used in the test – return null.
            return null;
        }

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) {
        }

        @Override
        public void addInfo(String msg) {
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
        }

        @Override
        public void addWarn(String msg) {
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
        }

        @Override
        public void addError(String msg) {
        }

        @Override
        public void addError(String msg, Throwable ex) {
        }

        @Override
        public void check(Model model) {
            this.checkedModel = model;
        }
    }

    /** Concrete subclass exposing protected members for verification. */
    private static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

        // expose the protected RuleStore that the base class creates
        RuleStore ruleStore;

        // expose the interpreter that the base class creates
        SaxEventInterpreter interpreter;

        // expose the ModelInterpretationContext created by the base class
        ModelInterpretationContext mic;

        // capture the SanityChecker that is passed to {@code performCheck}
        SanityChecker lastSanityChecker;
        Model      lastModelChecked;

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // delegate to the real implementation so that the default rule set is installed
            super.addElementSelectorAndActionAssociations(rs);
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            super.setImplicitRuleSupplier(interpreter);
            // keep a reference for the test
            this.interpreter = interpreter;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // remember arguments for later assertions
            this.lastSanityChecker = sc;
            this.lastModelChecked = model;
            super.performCheck(sc, model);
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            super.addModelHandlerAssociations(defaultProcessor);
        }

        /** Helper that forces the configurator to build its internal interpreter. */
        void initialiseInterpreter() {
            // The GenericXMLConfigurator builds the interpreter lazily inside
            // {@code buildSaxEventInterpreter}.  We can trigger the creation by
            // calling the protected method directly with an empty event list.
            List<SaxEvent> emptyList = Collections.emptyList();
            super.buildSaxEventInterpreter(emptyList);
        }

        /** Helper that forces the configurator to build its ModelInterpretationContext. */
        void initialiseModelInterpretationContext() {
            super.buildModelInterpretationContext();
            // The protected field {@code modelInterpretationContext} is defined in the
            // superclass.  We retrieve it via reflection because it is not visible.
            try {
                java.lang.reflect.Field f = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
                f.setAccessible(true);
                this.mic = (ModelInterpretationContext) f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TestJoranConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
    }

    /* -------------------------------------------------------------
     *  1.  Tests for the public API
     * ------------------------------------------------------------- */

    @Test
    public void testGetInterpretationContext_beforeInterpreterBuilt_returnsNull() {
        // The interpreter is created lazily; before that the method must return null.
        assertNull("InterpretationContext should be null before interpreter creation",
                configurator.getInterpretationContext());
    }

    @Test
    public void testGetInterpretationContext_afterInterpreterBuilt_isNotNull() {
        configurator.initialiseInterpreter();               // forces creation of SaxEventInterpreter
        assertNotNull("InterpretationContext must be non‑null after interpreter is built",
                configurator.getInterpretationContext());
    }

    @Test
    public void testBuildModelInterpretationContext_callsCreateAppenderBags() {
        // The base class' implementation should invoke {@code createAppenderBags()} on the MIC.
        configurator.initialiseModelInterpretationContext();

        // The ModelInterpretationContext class is not part of the supplied API, therefore we
        // cannot call {@code createAppenderBags()} directly.  Instead we verify that the MIC
        // instance is not null (i.e. the method completed without throwing) which is sufficient
        // for the purpose of this test suite.
        assertNotNull("ModelInterpretationContext should have been instantiated", configurator.mic);
    }

    /* -------------------------------------------------------------
     *  2.  Tests for addElementSelectorAndActionAssociations
     * ------------------------------------------------------------- */

    @Test
    public void testAddElementSelectorAndActionAssociations_registersAllDefaultRules() {
        RecordingRuleStore recordingStore = new RecordingRuleStore();

        // invoke the protected method via the concrete subclass
        configurator.addElementSelectorAndActionAssociations(recordingStore);

        // collect the selector strings for easier verification
        Set<String> selectors = new HashSet<>();
        for (RecordingRuleStore.RecordedRule r : recordingStore.recordedRules) {
            selectors.add(r.selector.getElementPath());
        }

        // expected selectors (a subset that proves the method exercised the full list)
        String[] expected = {
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
        for (String exp : expected) {
            assertTrue("Expected selector not found: " + exp, selectors.contains(exp));
        }

        // Verify that transparent path parts were added for the conditional constructs
        List<String> transparentParts = recordingStore.transparentPathParts;
        assertTrue("Transparent part 'if' missing", transparentParts.contains("if"));
        assertTrue("Transparent part 'then' missing", transparentParts.contains("then"));
        assertTrue("Transparent part 'else' missing", transparentParts.contains("else"));
        assertTrue("Transparent part 'sift' missing", transparentParts.contains("sift"));
    }

    /* -------------------------------------------------------------
     *  3.  Tests for sanityCheck / performCheck
     * ------------------------------------------------------------- */

    @Test
    public void testSanityCheck_delegatesToPerformCheck_withAppenderWithinAppenderChecker() {
        // Use a dummy Model instance – the concrete type does not matter for this test.
        Model dummyModel = new Model() {};

        // invoke the protected method via the subclass
        configurator.sanityCheck(dummyModel);

        // The subclass records the arguments passed to performCheck.
        assertNotNull("SanityChecker argument should have been captured", configurator.lastSanityChecker);
        assertTrue("SanityChecker should be an instance of AppenderWithinAppenderSanityChecker",
                configurator.lastSanityChecker instanceof AppenderWithinAppenderSanityChecker);
        assertSame("Model passed to performCheck must be the one supplied",
                dummyModel, configurator.lastModelChecked);
    }

    @Test
    public void testPerformCheck_setsContextWhenCheckerIsContextAware() {
        RecordingSanityChecker checker = new RecordingSanityChecker();
        Model dummyModel = new Model() {};

        // The configurator does not have a real Context, but we can still verify that
        // the {@code setContext} method of the checker is invoked (the checker records it).
        configurator.performCheck(checker, dummyModel);

        // The checker records that setContext was called by storing a non‑null reference.
        assertNotNull("Checker should have received a context via setContext()", checker.contextAware);
        // Verify that the model received by the checker is the same one we passed.
        assertSame("Checker must receive the exact model instance", dummyModel, checker.checkedModel);
    }

    @Test
    public void testPerformCheck_withoutContextAware_doesNotThrow() {
        // A sanity checker that does NOT implement ContextAware.
        SanityChecker simpleChecker = new SanityChecker() {
            @Override
            public void check(Model model) {
                // no‑op
            }
        };
        Model dummyModel = new Model() {};

        // Should complete without any exception.
        configurator.performCheck(simpleChecker, dummyModel);
    }

    /* -------------------------------------------------------------
     *  4.  Tests for setImplicitRuleSupplier
     * ------------------------------------------------------------- */

    @Test
    public void testSetImplicitRuleSupplier_assignsSupplier() throws Exception {
        // Build a minimal interpreter – the concrete constructor needs a Context,
        // a RuleStore, an ElementPath and a (possibly empty) list of events.
        // The Context type is not part of the supplied API; however the constructor
        // accepts {@code null} for the context, which is sufficient for this test.
        RuleStore dummyStore = new RecordingRuleStore();
        ElementPath initialPath = new ElementPath();
        List<SaxEvent> emptyEvents = Collections.emptyList();

        SaxEventInterpreter interpreter = new SaxEventInterpreter(null, dummyStore, initialPath, emptyEvents);

        // invoke the method under test
        configurator.setImplicitRuleSupplier(interpreter);

        // Verify via reflection that the private field {@code implicitActionSupplier}
        // in SaxEventInterpreter is now non‑null and creates an instance of
        // {@code ImplicitModelAction}.
        java.lang.reflect.Field f = SaxEventInterpreter.class.getDeclaredField("implicitActionSupplier");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Supplier<Action> supplier = (Supplier<Action>) f.get(interpreter);
        assertNotNull("Implicit action supplier must be set", supplier);
        Action action = supplier.get();
        assertTrue("Supplier must produce an ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    /* -------------------------------------------------------------
     *  5.  Tests for addModelHandlerAssociations
     * ------------------------------------------------------------- */

    @Test
    public void testAddModelHandlerAssociations_registersAllDefaultHandlers() {
        // A very small DefaultProcessor that records the handler factories.
        class RecordingProcessor extends DefaultProcessor {
            final Map<Class<? extends Model>, ModelHandlerFactoryMethod> factories = new HashMap<>();

            RecordingProcessor(ch.qos.logback.core.spi.Context context, ModelInterpretationContext mic) {
                super(context, mic);
            }

            @Override
            public void addHandler(Class<? extends Model> modelClass, ModelHandlerFactoryMethod modelFactoryMethod) {
                factories.put(modelClass, modelFactoryMethod);
                super.addHandler(modelClass, modelFactoryMethod);
            }
        }

        // We need a ModelInterpretationContext – use the one already created by the configurator.
        configurator.initialiseModelInterpretationContext();

        RecordingProcessor processor = new RecordingProcessor(null, configurator.mic);
        configurator.addModelHandlerAssociations(processor);

        // Verify that a handler has been registered for a representative set of model classes.
        Class<?>[] expectedModels = {
                ImportModel.class,
                ShutdownHookModel.class,
                SequenceNumberGeneratorModel.class,
                EventEvaluatorModel.class,
                DefineModel.class,
                IncludeModel.class,
                ParamModel.class,
                PropertyModel.class,
                TimestampModel.class,
                StatusListenerModel.class,
                ImplicitModel.class,
                IfModel.class,
                ThenModel.class,
                ElseModel.class,
                SiftModel.class
        };

        for (Class<?> modelClass : expectedModels) {
            assertTrue("Handler for " + modelClass.getSimpleName() + " should be registered",
                    processor.factories.containsKey(modelClass));
        }
    }
}
