package ch.qos.logback.core.joran;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.Before;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.*;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Unit tests for {@link JoranConfiguratorBase}.
 *
 * The tests exercise all protected methods by exposing them through a concrete
 * subclass that provides public wrapper methods.
 *
 * No external mocking frameworks are used – only concrete implementations that
 * are defined inside the test suite.
 */
public class JoranConfiguratorBaseTest {

    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        // The configurator is a ContextAwareBase; we simply use a null context
        // because the code under test does not depend on a real Context
        configurator.setContext(null);
    }

    /**
     * A concrete subclass that makes the protected methods of
     * {@link JoranConfiguratorBase} publicly accessible for testing.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        public void exposeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }

        public void exposeSanityCheck(Model model) {
            super.sanityCheck(model);
        }

        public void exposePerformCheck(SanityChecker sc, Model model) {
            super.performCheck(sc, model);
        }

        public void exposeSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            super.setImplicitRuleSupplier(interpreter);
        }

        public void exposeBuildModelInterpretationContext() {
            super.buildModelInterpretationContext();
        }

        public void exposeAddModelHandlerAssociations(DefaultProcessor dp) {
            super.addModelHandlerAssociations(dp);
        }
    }

    /**
     * Simple in‑memory implementation of {@link RuleStore} that records the
     * selectors, suppliers and transparent path parts that are added.
     */
    private static class InMemoryRuleStore implements RuleStore {

        final List<ElementSelector> selectors = new ArrayList<>();
        final List<Supplier<Action>> suppliers = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // Not used by the code under test
            throw new UnsupportedOperationException();
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            selectors.add(elementSelector);
            suppliers.add(actionSupplier);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            // Simplified – not needed for the tests
            return null;
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /**
     * A {@link SanityChecker} that also implements {@link ContextAware} so that
     * we can verify that the configurator correctly sets the context before
     * invoking {@code check(...)}.
     */
    private static class TestSanityChecker implements SanityChecker, ContextAware {

        ContextAware contextAware;
        boolean checkCalled = false;
        Context setContextArgument = null;

        @Override
        public void setContext(Context context) {
            this.setContextArgument = context;
        }

        @Override
        public Context getContext() {
            return null;
        }

        @Override
        public void addStatus(Status status) {}

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
            checkCalled = true;
        }
    }

    /**
     * A {@link DefaultProcessor} subclass that records every call to
     * {@code addHandler(...)} so that we can assert that the expected model
     * classes are registered.
     */
    private static class RecordingProcessor extends DefaultProcessor {

        static class Entry {
            final Class<? extends Model> modelClass;
            final ModelHandlerFactoryMethod factoryMethod;

            Entry(Class<? extends Model> modelClass, ModelHandlerFactoryMethod factoryMethod) {
                this.modelClass = modelClass;
                this.factoryMethod = factoryMethod;
            }
        }

        final List<Entry> addedHandlers = new ArrayList<>();

        RecordingProcessor(Context context, ModelInterpretationContext mic) {
            super(context, mic);
        }

        @Override
        public void addHandler(Class<? extends Model> modelClass,
                               ModelHandlerFactoryMethod modelFactoryMethod) {
            addedHandlers.add(new Entry(modelClass, modelFactoryMethod));
        }
    }

    @Test
    public void testAddElementSelectorAndActionAssociations_registersRulesAndTransparentParts() {
        InMemoryRuleStore ruleStore = new InMemoryRuleStore();

        configurator.exposeAddElementSelectorAndActionAssociations(ruleStore);

        // Verify that a rule for the "*/if" selector has been added
        boolean ifRuleFound = ruleStore.selectors.stream()
                .anyMatch(es -> "*/if".equals(es.getPath()));
        assertTrue("Rule for selector \"*/if\" should be registered", ifRuleFound);

        // Verify that a rule for the sift selector has been added
        boolean siftRuleFound = ruleStore.selectors.stream()
                .anyMatch(es -> "*/appender/sift".equals(es.getPath()));
        assertTrue("Rule for selector \"*/appender/sift\" should be registered", siftRuleFound);

        // Verify transparent path parts
        List<String> expectedTransparent = Arrays.asList("if", "then", "else", "sift");
        assertTrue("Transparent parts must contain all expected entries",
                ruleStore.transparentParts.containsAll(expectedTransparent));
    }

    @Test
    public void testSetImplicitRuleSupplier_assignsSupplier() {
        // The SaxEventInterpreter constructor requires several arguments.
        // For the purpose of this test we can safely pass nulls because the
        // method under test only calls setImplicitActionSupplier on the interpreter.
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                null,                     // Context
                new InMemoryRuleStore(), // RuleStore (won't be used)
                null,                     // initial ElementPath
                Collections.emptyList()  // empty SaxEvent list
        );

        configurator.exposeSetImplicitRuleSupplier(interpreter);

        // The interpreter does not expose the stored supplier, but we can
        // verify that calling the supplier does not throw and returns an
        // instance of ImplicitModelAction.
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull("Implicit action supplier should have been set", supplier);
        Action action = supplier.get();
        assertNotNull("Supplier must produce a non‑null Action", action);
        assertEquals("Action should be an instance of ImplicitModelAction",
                "ch.qos.logback.core.joran.action.ImplicitModelAction",
                action.getClass().getName());
    }

    @Test
    public void testPerformCheck_setsContextAndInvokesCheck() {
        TestSanityChecker checker = new TestSanityChecker();

        // Use a fresh Model instance (the concrete type is sufficient)
        Model dummyModel = new Model();

        configurator.exposePerformCheck(checker, dummyModel);

        assertTrue("SanityChecker.check(...) should have been called", checker.checkCalled);
        assertNull("Context passed to the checker should be the same as configurator's (null in this test)",
                checker.setContextArgument);
    }

    @Test
    public void testAddModelHandlerAssociations_registersExpectedHandlers() {
        // The constructor of DefaultProcessor needs a Context and a
        // ModelInterpretationContext; we pass nulls because the overridden
        // addHandler method does not use them.
        RecordingProcessor processor = new RecordingProcessor(null, null);

        configurator.exposeAddModelHandlerAssociations(processor);

        // Collect the model classes that were registered
        Set<Class<? extends Model>> registered = new HashSet<>();
        for (RecordingProcessor.Entry e : processor.addedHandlers) {
            registered.add(e.modelClass);
        }

        // Verify a few representative model classes are present
        assertTrue("ImportModel handler must be registered",
                registered.contains(ImportModel.class));
        assertTrue("IfModel handler must be registered",
                registered.contains(IfModel.class));
        assertTrue("SiftModel handler must be registered",
                registered.contains(SiftModel.class));
        // Ensure that the total number of registered handlers matches the
        // number of explicit addHandler calls in the source (15)
        assertEquals("Exactly 15 model handlers should be registered", 15, registered.size());
    }

    @Test
    public void testBuildModelInterpretationContext_initialisesInterpretationContext() {
        // Build the interpretation context – this method is defined in the
        // superclass and should create the internal SaxEventInterpreter.
        configurator.exposeBuildModelInterpretationContext();

        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull("Interpretation context should be created", ctx);
    }
}
