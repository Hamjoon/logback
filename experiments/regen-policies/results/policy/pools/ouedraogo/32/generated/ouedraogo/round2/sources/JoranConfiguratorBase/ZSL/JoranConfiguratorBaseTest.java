/**
 * JUnit4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests exercise all public / protected behaviour of the class without
 * using any external mocking framework – only lightweight concrete test doubles
 * are created.
 */
package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.conditional.IfModel;


import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.joran.conditional.IfAction;
import ch.qos.logback.core.joran.conditional.ThenAction;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Concrete subclass used only for testing the protected behaviour of the
 * abstract {@link JoranConfiguratorBase}.
 */
public class JoranConfiguratorBaseTest {

    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
    }

    /**
     * -----------------------------------------------------------------------
     *  Test that the element‑selector/action associations added by the base class
     *  are exactly those we expect and that transparent path parts are recorded.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testAddElementSelectorAndActionAssociations() {
        TestRuleStore ruleStore = new TestRuleStore();
        configurator.addElementSelectorAndActionAssociations(ruleStore);

        // Verify a few representative selectors are present
        assertTrue("selector */variable must be registered",
                ruleStore.hasRule("*/variable"));
        assertTrue("selector configuration/appender/appender-ref must be registered",
                ruleStore.hasRule("configuration/appender/appender-ref"));
        assertTrue("selector */if/else must be registered",
                ruleStore.hasRule("*/if/else"));

        // Verify that the transparent parts are exactly the ones added in the source
        List<String> expectedTransparent = Arrays.asList("if", "then", "else", "sift");
        assertEquals("transparent path parts", expectedTransparent, ruleStore.getTransparentParts());
    }

    /**
     * -----------------------------------------------------------------------
     *  Test {@code setImplicitRuleSupplier}. The supplied interpreter must store
     *  a supplier that creates an {@link ImplicitModelAction} instance.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testSetImplicitRuleSupplier() {
        TestInterpreter interpreter = new TestInterpreter();
        configurator.setImplicitRuleSupplier(interpreter);

        Supplier<Action> stored = interpreter.getImplicitActionSupplier();
        assertNotNull("Implicit action supplier must be stored", stored);
        Action action = stored.get();
        assertTrue("Supplier must create ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    /**
     * -----------------------------------------------------------------------
     *  Test {@code performCheck}. The sanity checker receives the configurator's
     *  context (which may be {@code null} in the test) and its {@code check}
     *  method is invoked.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testPerformCheck() {
        TestSanityChecker checker = new TestSanityChecker();
        Model dummyModel = new Model() {
        };
        configurator.performCheck(checker, dummyModel);

        assertTrue("ContextAware#setContext must have been called", checker.contextSet);
        assertTrue("SanityChecker#check must have been called", checker.checkCalled);
        assertSame("The model passed to check must be the dummy model",
                dummyModel, checker.passedModel);
    }

    /**
     * -----------------------------------------------------------------------
     *  Test {@code sanityCheck}. It should delegate to {@code performCheck}
     *  using an {@link AppenderWithinAppenderSanityChecker}.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testSanityCheckUsesAppenderWithinAppenderSanityChecker() {
        // Replace the configurator with a spy that records the checker passed to
        // performCheck.
        TestConfigurator spy = new TestConfigurator() {
            SanityChecker receivedChecker = null;

            @Override
            protected void performCheck(SanityChecker sc, Model model) {
                receivedChecker = sc;
                // invoke super to keep normal behaviour (no‑op for the test)
                super.performCheck(sc, model);
            }
        };

        Model top = new Model() {
        };
        spy.sanityCheck(top);
        assertNotNull("sanityCheck must have called performCheck",
                ((TestConfigurator) spy).receivedChecker);
        assertTrue("The checker must be an AppenderWithinAppenderSanityChecker",
                spy.receivedChecker instanceof AppenderWithinAppenderSanityChecker);
    }

    /**
     * -----------------------------------------------------------------------
     *  Test {@code addModelHandlerAssociations}. The processor must receive a
     *  handler registration for each model class listed in the source.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testAddModelHandlerAssociations() {
        TestDefaultProcessor processor = new TestDefaultProcessor();
        configurator.addModelHandlerAssociations(processor);

        // Verify a sample of the expected model‑handler mappings
        assertTrue("ImportModel must have a handler", processor.hasHandler(ImportModel.class));
        assertTrue("IfModel must have a handler", processor.hasHandler(IfModel.class));
        assertTrue("SiftModel must have a handler", processor.hasHandler(SiftModel.class));
    }

    /**
     * -----------------------------------------------------------------------
     *  Test {@code getInterpretationContext}. The method should simply forward
     *  the call to the internal {@link SaxEventInterpreter}.
     * -----------------------------------------------------------------------
     */
    @Test
    public void testGetInterpretationContext() {
        // Build a dummy interpreter that returns a known context object.
        ContextAwareBase dummyContext = new ContextAwareBase();
        TestInterpreter interpreter = new TestInterpreter(dummyContext);
        configurator.setSaxEventInterpreter(interpreter); // expose for test

        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertSame("Returned interpretation context must be that of the interpreter",
                interpreter.getSaxEventInterpretationContext(), ctx);
    }

    // -----------------------------------------------------------------------
    //  Helper / test double implementations
    // -----------------------------------------------------------------------

    /**
     * Concrete subclass exposing the protected members needed for testing.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // expose the saxEventInterpreter for the getInterpretationContext test
        void setSaxEventInterpreter(SaxEventInterpreter interpreter) {
            this.saxEventInterpreter = interpreter;
        }

        // expose the received checker for the sanityCheck test
        SanityChecker receivedChecker = null;

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            super.setImplicitRuleSupplier(interpreter);
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            this.receivedChecker = sc;
            super.performCheck(sc, model);
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            super.addModelHandlerAssociations(defaultProcessor);
        }
    }

    /**
     * Minimal {@link RuleStore} implementation that records added rules and
     * transparent path parts.
     */
    private static class TestRuleStore implements RuleStore {
        private final Map<String, Supplier<Action>> rules = new LinkedHashMap<>();
        private final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector,
                            String actionClassStr) throws ClassNotFoundException {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public void addRule(ElementSelector elementSelector,
                            Supplier<Action> actionSupplier) {
            rules.put(elementSelector.toString(), actionSupplier);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }

        boolean hasRule(String selectorPattern) {
            return rules.containsKey(selectorPattern);
        }

        List<String> getTransparentParts() {
            return new ArrayList<>(transparentParts);
        }
    }

    /**
     * Test double for {@link SaxEventInterpreter} that records the supplier
     * passed to {@code setImplicitActionSupplier}.
     */
    private static class TestInterpreter extends SaxEventInterpreter {
        private Supplier<Action> implicitSupplier;

        TestInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        TestInterpreter(ContextAwareBase context) {
            super(context, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.implicitSupplier = actionSupplier;
        }

        Supplier<Action> getImplicitActionSupplier() {
            return implicitSupplier;
        }
    }

    /**
     * Simple {@link SanityChecker} that also implements {@link ContextAware}
     * so that we can verify {@code setContext} is invoked.
     */
    private static class TestSanityChecker implements SanityChecker, ContextAware {
        boolean contextSet = false;
        boolean checkCalled = false;
        Model passedModel = null;
        Context context = null;

        @Override
        public void check(Model model) {
            this.checkCalled = true;
            this.passedModel = model;
        }

        @Override
        public void setContext(Context context) {
            this.contextSet = true;
            this.context = context;
        }

        @Override
        public Context getContext() {
            return context;
        }

        @Override
        public void addStatus(Status status) {
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
    }

    /**
     * Test double for {@link DefaultProcessor} that records handler registrations.
     */
    private static class TestDefaultProcessor extends DefaultProcessor {
        private final Map<Class<? extends Model>, ModelHandlerFactoryMethod> handlers = new HashMap<>();

        TestDefaultProcessor() {
            super(null, null);
        }

        @Override
        public void addHandler(Class<? extends Model> modelClass,
                               ModelHandlerFactoryMethod modelFactoryMethod) {
            handlers.put(modelClass, modelFactoryMethod);
        }

        boolean hasHandler(Class<? extends Model> modelClass) {
            return handlers.containsKey(modelClass);
        }
    }
}
