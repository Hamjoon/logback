/**
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests are written from the point of view of three “experts”.  Each
 * expert proposes a single test case for every public / protected method of the
 * class.  After the discussion the final test class contains the union of the
 * proposals – i.e. one thorough test per method that covers the typical use case,
 * edge cases and error handling.
 *
 * The test does **not** use any external mocking framework.  All collaborators
 * are simple concrete test doubles that implement the required interfaces from
 * the library API (RuleStore, DefaultProcessor, SanityChecker, etc.).
 *
 * The concrete configurator used in the tests is an anonymous subclass of the
 * abstract {@code JoranConfiguratorBase}.  No production code is altered.
 */
package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.conditional.IfModel;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Test class for {@link JoranConfiguratorBase}.
 */
public class JoranConfiguratorBaseTest {

    /** the configurator under test – a minimal concrete subclass */
    private JoranConfiguratorBase<Object> configurator;

    @Before
    public void setUp() {
        configurator = new JoranConfiguratorBase<Object>() {
            // no additional behaviour required – we just need a concrete instance
        };
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 1 – addElementSelectorAndActionAssociations
     * -----------------------------------------------------------------------
     *
     * Verifies that the method registers all expected selectors, the correct
     * action suppliers and that the transparent path parts are recorded.
     */
    @Test
    public void testAddElementSelectorAndActionAssociations() {
        RecordingRuleStore ruleStore = new RecordingRuleStore();

        configurator.addElementSelectorAndActionAssociations(ruleStore);

        // ---- check selectors -------------------------------------------------
        List<String> expectedSelectors = new ArrayList<>();
        Collections.addAll(expectedSelectors,
                "*/variable", "*/property", "*/substitutionProperty",
                "configuration/import",
                "configuration/timestamp", "configuration/shutdownHook",
                "configuration/sequenceNumberGenerator", "configuration/serializeModel",
                "configuration/define", "configuration/evaluator",
                "configuration/contextProperty", "configuration/conversionRule",
                "configuration/statusListener",
                "*/appender", "configuration/appender/appender-ref",
                "configuration/newRule", "*/param",
                "*/if", "*/if/then", "*/if/else",
                "*/appender/sift"
        );
        assertEquals("Number of selectors added", expectedSelectors.size(), ruleStore.selectors.size());
        for (String sel : expectedSelectors) {
            assertTrue("Selector missing: " + sel, ruleStore.selectors.contains(sel));
        }

        // ---- check that every selector received a non‑null supplier ----------
        for (String sel : ruleStore.selectors) {
            Supplier<Action> sup = ruleStore.suppliers.get(sel);
            assertNotNull("No supplier for selector " + sel, sup);
            // the supplier must be able to create an Action instance
            Action act = sup.get();
            assertNotNull("Supplier returned null for selector " + sel, act);
        }

        // ---- transparent parts ------------------------------------------------
        List<String> expectedTransparent = List.of("if", "then", "else", "sift");
        assertEquals("Transparent parts count", expectedTransparent.size(),
                ruleStore.transparentParts.size());
        assertTrue("Transparent parts differ", ruleStore.transparentParts.containsAll(expectedTransparent));
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 2 – sanityCheck / performCheck interaction
     * -----------------------------------------------------------------------
     *
     * The {@code sanityCheck} method must delegate to {@code performCheck} with
     * an {@link AppenderWithinAppenderSanityChecker}.  The test overrides
     * {@code performCheck} to capture the arguments.
     */
    @Test
    public void testSanityCheckInvokesPerformCheck() {
        final Model dummyModel = new Model() {
            // Use the default implementation – nothing special is required
        };
        final List<Model> capturedModels = new ArrayList<>();
        final List<SanityChecker> capturedCheckers = new ArrayList<>();

        JoranConfiguratorBase<Object> spyConfigurator = new JoranConfiguratorBase<Object>() {
            @Override
            protected void performCheck(SanityChecker sc, Model model) {
                capturedCheckers.add(sc);
                capturedModels.add(model);
            }
        };

        spyConfigurator.sanityCheck(dummyModel);

        assertEquals("performCheck should have been called exactly once", 1, capturedCheckers.size());
        assertTrue("Checker should be an AppenderWithinAppenderSanityChecker",
                capturedCheckers.get(0) instanceof AppenderWithinAppenderSanityChecker);
        assertSame("Model passed to performCheck must be the one supplied", dummyModel,
                capturedModels.get(0));
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 3 – performCheck with a ContextAware SanityChecker
     * -----------------------------------------------------------------------
     *
     * A {@link SanityChecker} that also implements {@link ContextAware} must
     * receive the configurator's context before {@code check(..)} is invoked.
     */
    @Test
    public void testPerformCheckContextAware() {
        // a very small stub Context – the real interface has many methods but they
        // are not used by the test, therefore an empty implementation is enough.
        class StubContext implements ch.qos.logback.core.spi.Context {
            // all methods are no‑ops
        }

        configurator.setContext(new StubContext());

        RecordingSanityChecker checker = new RecordingSanityChecker();

        Model dummyModel = new Model() {
        };

        configurator.performCheck(checker, dummyModel);

        assertTrue("setContext should have been called on the checker", checker.contextSet);
        assertSame("The context passed to the checker must be the configurator's context",
                configurator.getContext(), checker.receivedContext);
        assertTrue("check(..) must have been invoked", checker.checked);
        assertSame("check(..) must receive the correct model", dummyModel, checker.checkedModel);
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 4 – setImplicitRuleSupplier
     * -----------------------------------------------------------------------
     *
     * After the call the interpreter must hold a supplier that creates an
     * {@link ImplicitModelAction} instance.
     */
    @Test
    public void testSetImplicitRuleSupplier() {
        RecordingInterpreter interpreter = new RecordingInterpreter();

        configurator.setImplicitRuleSupplier(interpreter);

        assertNotNull("Implicit supplier must be set", interpreter.implicitSupplier);
        Action a = interpreter.implicitSupplier.get();
        assertNotNull("Supplier must produce a non‑null Action", a);
        assertEquals("Supplier must create ImplicitModelAction", ImplicitModelAction.class,
                a.getClass());
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 5 – addModelHandlerAssociations
     * -----------------------------------------------------------------------
     *
     * The method should register a handler for every model class listed in the
     * source code.  The test replaces {@link DefaultProcessor} with a recording
     * subclass that captures the calls to {@code addHandler}.
     */
    @Test
    public void testAddModelHandlerAssociations() {
        RecordingDefaultProcessor processor = new RecordingDefaultProcessor();

        configurator.addModelHandlerAssociations(processor);

        // the source registers handlers for 15 different model classes
        assertEquals("Number of handler registrations", 15, processor.registeredHandlers.size());

        // verify a few representative entries
        assertTrue("ImportModel handler must be registered",
                processor.registeredHandlers.containsKey(ImportModel.class));
        assertTrue("IfModel handler must be registered",
                processor.registeredHandlers.containsKey(IfModel.class));
        assertTrue("SiftModel handler must be registered",
                processor.registeredHandlers.containsKey(SiftModel.class));
    }

    /**
     * -----------------------------------------------------------------------
     *  Test 6 – getInterpretationContext
     * -----------------------------------------------------------------------
     *
     * The method simply forwards to the interpreter.  The test guarantees that
     * the returned object is exactly the one supplied by the interpreter.
     */
    @Test
    public void testGetInterpretationContext() {
        RecordingInterpreter interpreter = new RecordingInterpreter();
        configurator.saxEventInterpreter = interpreter; // package‑private field in the super class

        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertSame("Returned context must be the interpreter's context", interpreter.context, ctx);
    }

    // -----------------------------------------------------------------------
    //  Helper test doubles
    // -----------------------------------------------------------------------

    /** Records calls made to {@link RuleStore}. */
    private static class RecordingRuleStore implements RuleStore {
        final List<String> selectors = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();
        final java.util.Map<String, Supplier<Action>> suppliers = new java.util.HashMap<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // not used in the tested code – leave empty
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            selectors.add(elementSelector.getElementPath());
            suppliers.put(elementSelector.getElementPath(), actionSupplier);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return null; // not needed for the test
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /** Simple {@link SanityChecker} that records method invocations. */
    private static class RecordingSanityChecker extends ContextAwareBase implements SanityChecker, ContextAware {
        boolean contextSet = false;
        ch.qos.logback.core.spi.Context receivedContext = null;
        boolean checked = false;
        Model checkedModel = null;

        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            this.contextSet = true;
            this.receivedContext = context;
        }

        @Override
        public void check(Model model) {
            this.checked = true;
            this.checkedModel = model;
        }
    }

    /** Stub interpreter that only records the supplier set via
     *  {@link SaxEventInterpreter#setImplicitActionSupplier}. */
    private static class RecordingInterpreter extends SaxEventInterpreter {
        Supplier<Action> implicitSupplier;
        SaxEventInterpretationContext context = new SaxEventInterpretationContext(null, null);

        RecordingInterpreter() {
            super(null, null, null, Collections.emptyList());
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.implicitSupplier = actionSupplier;
        }

        @Override
        public SaxEventInterpretationContext getSaxEventInterpretationContext() {
            return context;
        }
    }

    /** Recording version of {@link DefaultProcessor}. */
    private static class RecordingDefaultProcessor extends DefaultProcessor {
        final java.util.Map<Class<? extends Model>, ModelHandlerFactoryMethod> registeredHandlers = new java.util.HashMap<>();

        RecordingDefaultProcessor() {
            super(null, null);
        }

        @Override
        public void addHandler(Class<? extends Model> modelClass,
                               ModelHandlerFactoryMethod modelFactoryMethod) {
            registeredHandlers.put(modelClass, modelFactoryMethod);
        }
    }
}
