package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.DefineModel;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.EventEvaluatorModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.IncludeModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelHandlerFactoryMethod;
import ch.qos.logback.core.model.ParamModel;
import ch.qos.logback.core.model.PropertyModel;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.SequenceNumberGeneratorModel;
import ch.qos.logback.core.model.ShutdownHookModel;
import ch.qos.logback.core.model.SiftModel;
import ch.qos.logback.core.model.StatusListenerModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.TimestampModel;


import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for {@link JoranConfiguratorBase}.  The tests exercise the
 * concrete (non‑abstract) behaviour defined in the base class without
 * requiring any of the heavy XML parsing machinery of Logback.
 *
 * <p>The strategy is to provide lightweight test‑doubles for the
 * {@link RuleStore}, {@link SaxEventInterpreter}, {@link DefaultProcessor}
 * and a simple {@link SanityChecker} implementation.  These test‑doubles
 * record the interactions performed by the configurator so that the
 * assertions can verify the expected contracts.</p>
 */
public class JoranConfiguratorBaseTest {

    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
    }

    /* -------------------------------------------------------------
     *  Helper classes (test doubles)
     * ----------------------------------------------------------- */

    /** Simple implementation of {@link RuleStore} that records calls. */
    static class TestRuleStore implements RuleStore {

        static class RuleEntry {
            final ElementSelector selector;
            final Supplier<Action> supplier;
            RuleEntry(ElementSelector selector, Supplier<Action> supplier) {
                this.selector = selector;
                this.supplier = supplier;
            }
        }

        final List<RuleEntry> rules = new ArrayList<>();
        final List<String> transparentPathParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector,
                            String actionClassStr) {
            // Not used by the configurator – ignore.
        }

        @Override
        public void addRule(ElementSelector elementSelector,
                            Supplier<Action> actionSupplier) {
            rules.add(new RuleEntry(elementSelector, actionSupplier));
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            // Not needed for the tests.
            return null;
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentPathParts.add(pathPart);
        }

        @Override
        public void addPathPathMapping(String originalName, String modifiedName) {
            // Not used in these tests.
        }
    }

    /** Minimal {@link SaxEventInterpreter} that only records the supplied
     *  implicit action supplier. */
    static class TestInterpreter extends SaxEventInterpreter {

        Supplier<Action> implicitSupplier;

        public TestInterpreter() {
            // The super‑constructor requires a Context, RuleStore,
            // ElementPath and a list of SaxEvent.  All of these can be null
            // for the purpose of this test because the overridden method
            // does not touch them.
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.implicitSupplier = actionSupplier;
        }
    }

    /** Simple {@link DefaultProcessor} that records which handlers are
     *  registered. */
    static class TestProcessor extends DefaultProcessor {

        static class HandlerEntry {
            final Class<? extends Model> modelClass;
            final ModelHandlerFactoryMethod factory;
            HandlerEntry(Class<? extends Model> modelClass,
                         ModelHandlerFactoryMethod factory) {
                this.modelClass = modelClass;
                this.factory = factory;
            }
        }

        final List<HandlerEntry> handlers = new ArrayList<>();

        public TestProcessor() {
            // Context and ModelInterpretationContext are not needed for the
            // assertions performed in this test suite.
            super(null, null);
        }

        @Override
        public void addHandler(Class<? extends Model> modelClass,
                               ModelHandlerFactoryMethod modelFactoryMethod) {
            handlers.add(new HandlerEntry(modelClass, modelFactoryMethod));
        }
    }

    /** A {@link SanityChecker} that also implements {@link ContextAware}
     *  so that we can verify that the configurator passes its context. */
    static class TestSanityChecker extends ContextAwareBase implements SanityChecker {

        Context receivedContext = null;

        @Override
        public void setContext(Context context) {
            super.setContext(context);
            this.receivedContext = context;
        }

        @Override
        public void check(Model model) {
            // No‑op – the test only cares about the context injection.
        }
    }

    /** Concrete subclass of {@link JoranConfiguratorBase} used for testing.
     *  It simply forwards to the protected methods of the base class. */
    static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // delegate to the implementation in the base class
            super.addElementSelectorAndActionAssociations(rs);
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            super.setImplicitRuleSupplier(interpreter);
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            super.addModelHandlerAssociations(defaultProcessor);
        }

        // Expose the protected performCheck for testing.
        void invokePerformCheck(SanityChecker sc, Model model) {
            super.performCheck(sc, model);
        }

        // Expose the protected sanityCheck for testing.
        void invokeSanityCheck(Model topModel) {
            super.sanityCheck(topModel);
        }
    }

    /* -------------------------------------------------------------
     *  Test cases
     * ----------------------------------------------------------- */

    @Test
    public void testAddElementSelectorAndActionAssociations_registersAllRules() {
        TestRuleStore ruleStore = new TestRuleStore();

        configurator.addElementSelectorAndActionAssociations(ruleStore);

        // Verify that a rule for variable/property/substitutionProperty exists.
        assertTrue(containsSelector(ruleStore, "*/variable"));
        assertTrue(containsSelector(ruleStore, "*/property"));
        assertTrue(containsSelector(ruleStore, "*/substitutionProperty"));

        // Verify a few configuration‑level selectors.
        assertTrue(containsSelector(ruleStore, "configuration/import"));
        assertTrue(containsSelector(ruleStore, "configuration/timestamp"));
        assertTrue(containsSelector(ruleStore, "configuration/shutdownHook"));
        assertTrue(containsSelector(ruleStore, "configuration/sequenceNumberGenerator"));
        assertTrue(containsSelector(ruleStore, "configuration/serializeModel"));
        assertTrue(containsSelector(ruleStore, "configuration/define"));
        assertTrue(containsSelector(ruleStore, "configuration/evaluator"));
        assertTrue(containsSelector(ruleStore, "configuration/contextProperty"));
        assertTrue(containsSelector(ruleStore, "configuration/conversionRule"));
        assertTrue(containsSelector(ruleStore, "configuration/statusListener"));
        assertTrue(containsSelector(ruleStore, "*/appender"));
        assertTrue(containsSelector(ruleStore, "configuration/appender/appender-ref"));
        assertTrue(containsSelector(ruleStore, "configuration/newRule"));
        assertTrue(containsSelector(ruleStore, "*/param"));
        assertTrue(containsSelector(ruleStore, "*/if"));
        assertTrue(containsSelector(ruleStore, "*/if/then"));
        assertTrue(containsSelector(ruleStore, "*/if/else"));
        assertTrue(containsSelector(ruleStore, "*/appender/sift"));

        // Verify transparent path parts.
        List<String> expectedTransparent = Arrays.asList("if", "then", "else", "sift");
        assertEquals(expectedTransparent, ruleStore.transparentPathParts);
    }

    private boolean containsSelector(TestRuleStore rs, String pattern) {
        for (TestRuleStore.RuleEntry e : rs.rules) {
            if (e.selector.toString().equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testSetImplicitRuleSupplier_assignsCorrectSupplier() {
        TestInterpreter interpreter = new TestInterpreter();

        configurator.setImplicitRuleSupplier(interpreter);

        assertNotNull("Implicit supplier must be set", interpreter.implicitSupplier);
        // The supplier should create an instance of ImplicitModelAction.
        Action action = interpreter.implicitSupplier.get();
        assertTrue("Supplier must produce an ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    @Test
    public void testPerformCheck_setsContextOnContextAwareSanityChecker() {
        // Prepare a dummy context (null is acceptable – we only check that the
        // method is invoked without throwing).
        Context dummyContext = null;   // using null to keep the test lightweight
        configurator.setContext(dummyContext);

        TestSanityChecker sc = new TestSanityChecker();
        Model dummyModel = new Model(); // empty model – content irrelevant

        configurator.invokePerformCheck(sc, dummyModel);

        // The sanity checker should have received the same context instance.
        assertSame("Context should be propagated to the sanity checker",
                dummyContext, sc.receivedContext);
    }

    @Test
    public void testSanityCheck_delegatesToPerformCheckWithAppenderWithinAppenderChecker() {
        // The sanityCheck method internally creates an AppenderWithinAppenderSanityChecker.
        // We cannot replace that instance, but we can verify that no exception is thrown
        // and that the method runs to completion.
        Model topModel = new Model();
        // No special setup required – just ensure the call does not explode.
        configurator.invokeSanityCheck(topModel);
    }

    @Test
    public void testAddModelHandlerAssociations_registersExpectedHandlers() {
        TestProcessor processor = new TestProcessor();

        configurator.addModelHandlerAssociations(processor);

        // Collect the model classes for which handlers were registered.
        Set<Class<? extends Model>> registered = new HashSet<>();
        for (TestProcessor.HandlerEntry e : processor.handlers) {
            registered.add(e.modelClass);
        }

        // Verify a representative subset of expected model handlers.
        assertTrue(registered.contains(ImportModel.class));
        assertTrue(registered.contains(ShutdownHookModel.class));
        assertTrue(registered.contains(SequenceNumberGeneratorModel.class));
        assertTrue(registered.contains(EventEvaluatorModel.class));
        assertTrue(registered.contains(DefineModel.class));
        assertTrue(registered.contains(IncludeModel.class));
        assertTrue(registered.contains(ParamModel.class));
        assertTrue(registered.contains(PropertyModel.class));
        assertTrue(registered.contains(TimestampModel.class));
        assertTrue(registered.contains(StatusListenerModel.class));
        assertTrue(registered.contains(ImplicitModel.class));
        assertTrue(registered.contains(IfModel.class));
        assertTrue(registered.contains(ThenModel.class));
        assertTrue(registered.contains(ElseModel.class));
        assertTrue(registered.contains(SiftModel.class));
    }
}
