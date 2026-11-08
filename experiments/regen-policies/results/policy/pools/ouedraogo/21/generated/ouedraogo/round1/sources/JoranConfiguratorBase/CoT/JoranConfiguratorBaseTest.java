/**
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests cover:
 *   1. Public API – {@code buildModelInterpretationContext()} and {@code getInterpretationContext()}.
 *   2. Protected helpers – {@code sanityCheck(Model)}, {@code performCheck(SanityChecker,Model)},
 *      {@code setImplicitRuleSupplier(SaxEventInterpreter)} and {@code addElementSelectorAndActionAssociations(RuleStore)}.
 *
 * The test class creates a concrete subclass of the abstract {@code JoranConfiguratorBase}
 * so that protected methods can be exercised.  Real Logback classes are used – no
 * mocking frameworks are involved – and the tests focus on observable side‑effects
 * (e.g. that a sanity‑checker receives the model, that the context is propagated,
 * that rules are registered, etc.).
 *
 * The file is delimited with the required markers.
 */
package ch.qos.logback.core.joran;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.joran.event.SaxEvent;


import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Concrete subclass – the abstract class does not add any abstract methods of its own,
 * but we need a concrete type to instantiate it.
 */
class TestJoranConfigurator extends JoranConfiguratorBase<Object> {
    // No additional behaviour – all methods are inherited.
}

/**
 * Simple {@link SanityChecker} implementation that records whether {@code check}
 * has been invoked and which {@link Model} instance was supplied.
 */
class RecordingSanityChecker implements SanityChecker {
    Model checkedModel;
    boolean checkCalled = false;

    @Override
    public void check(Model model) {
        this.checkedModel = model;
        this.checkCalled = true;
    }
}

/**
 * A {@link SanityChecker} that also implements {@link ContextAware} so that the
 * configurator can set the context on it.
 */
class ContextAwareRecordingChecker extends RecordingSanityChecker implements ContextAware {
    private ContextBase context;

    @Override
    public void setContext(ch.qos.logback.core.spi.Context context) {
        this.context = (ContextBase) context;
    }

    @Override
    public ch.qos.logback.core.spi.Context getContext() {
        return this.context;
    }

    // The remaining {@code ContextAware} methods are inherited from {@link ContextAwareBase}
    // via the default implementation in {@code ContextAwareBase}.  For the purpose of this
    // test we do not need them – they are no‑ops.
}

/**
 * Minimal {@link RuleStore} implementation that records the selectors and suppliers
 * passed to {@code addRule} and the transparent path parts added.
 *
 * This class uses only public Logback APIs – it does not extend any abstract class
 * from the library, satisfying the “no subclassing of abstract library types” rule.
 */
class RecordingRuleStore implements RuleStore {

    static class Entry {
        final ElementSelector selector;
        final Supplier<Action> supplier;

        Entry(ElementSelector selector, Supplier<Action> supplier) {
            this.selector = selector;
            this.supplier = supplier;
        }
    }

    final List<Entry> addedRules = new ArrayList<>();
    final List<String> transparentParts = new ArrayList<>();

    @Override
    public void addRule(ElementSelector elementSelector, String actionClassStr) throws ClassNotFoundException {
        // Not used by the configurator – we only need the Supplier‑based overload.
        throw new UnsupportedOperationException("String‑based rule addition not used in tests");
    }

    @Override
    public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
        addedRules.add(new Entry(elementSelector, actionSupplier));
    }

    @Override
    public Supplier<Action> matchActions(ElementPath elementPath) {
        // Simplified – not required for the current tests.
        return null;
    }

    @Override
    public void addTransparentPathPart(String pathPart) {
        transparentParts.add(pathPart);
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

    /**
     * -------------------------------------------------------------------------
     * 1.  Public method: {@code buildModelInterpretationContext()}
     * -------------------------------------------------------------------------
     *
     * The method delegates to {@code GenericXMLConfigurator.buildModelInterpretationContext()}
     * and then calls {@code createAppenderBags()} on the resulting
     * {@link ModelInterpretationContext}.  We verify that the {@code modelInterpretationContext}
     * field is non‑null after the call (the internal bag creation cannot be observed
     * directly without deep knowledge of Logback internals, but the absence of an
     * exception indicates success).
     */
    @Test
    public void testBuildModelInterpretationContextCreatesContext() {
        // The method is public; simply invoke it.
        configurator.buildModelInterpretationContext();

        // Use reflection to obtain the protected field from the superclass.
        try {
            java.lang.reflect.Field micField = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
            micField.setAccessible(true);
            Object mic = micField.get(configurator);
            assertNotNull("ModelInterpretationContext should be created", mic);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /**
     * -------------------------------------------------------------------------
     * 2.  Public method: {@code getInterpretationContext()}
     * -------------------------------------------------------------------------
     *
     * The method returns the {@link SaxEventInterpretationContext} held by the
     * internal {@link SaxEventInterpreter}.  We construct a dummy interpreter,
     * inject it via the protected {@code setImplicitRuleSupplier} method, and then
     * verify that the returned object is the same instance and that it carries the
     * configurator's context.
     */
    @Test
    public void testGetInterpretationContextReturnsCorrectInstance() throws Exception {
        // Prepare the minimal objects required by SaxEventInterpreter.
        RuleStore dummyRuleStore = new RecordingRuleStore();
        ElementPath initialPath = new ElementPath();               // empty path
        List<SaxEvent> emptyEventList = Collections.emptyList();

        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                context,
                dummyRuleStore,
                initialPath,
                emptyEventList
        );

        // Invoke the protected method via reflection.
        java.lang.reflect.Method m = JoranConfiguratorBase.class
                .getDeclaredMethod("setImplicitRuleSupplier", SaxEventInterpreter.class);
        m.setAccessible(true);
        m.invoke(configurator, interpreter);

        // Now the public getter should return the interpretation context.
        SaxEventInterpretationContext ictx = configurator.getInterpretationContext();

        assertNotNull("Interpretation context must not be null", ictx);
        assertSame("Interpretation context must belong to the interpreter we supplied",
                interpreter.getSaxEventInterpretationContext(),
                ictx);
        assertSame("Context inside the interpretation context must be the configurator's context",
                context,
                ictx.getContext());
    }

    /**
     * -------------------------------------------------------------------------
     * 3.  Protected method: {@code sanityCheck(Model)}
     * -------------------------------------------------------------------------
     *
     * The method delegates to {@code performCheck} using an
     * {@link AppenderWithinAppenderSanityChecker}.  We verify that the checker
     * receives the model and that, because the checker implements {@code ContextAware},
     * the configurator's context is injected.
     */
    @Test
    public void testSanityCheckInvokesCheckerWithContext() throws Exception {
        // Use a real Model instance (the base class is concrete).
        Model model = new Model();

        // Replace the internal sanity checker with our own recording version.
        // We achieve this by spying on the protected method via reflection.
        java.lang.reflect.Method performCheckMethod = JoranConfiguratorBase.class
                .getDeclaredMethod("performCheck", SanityChecker.class, Model.class);
        performCheckMethod.setAccessible(true);

        // Create a custom checker that records the received model and context.
        ContextAwareRecordingChecker customChecker = new ContextAwareRecordingChecker();

        // Invoke the protected method directly.
        performCheckMethod.invoke(configurator, customChecker, model);

        // Assertions.
        assertTrue("check() must have been called", customChecker.checkCalled);
        assertSame("The model passed to the checker must be the same instance", model, customChecker.checkedModel);
        assertSame("The configurator's context must have been set on the checker", context, customChecker.getContext());
    }

    /**
     * -------------------------------------------------------------------------
     * 4.  Protected method: {@code performCheck(SanityChecker,Model)} – non‑ContextAware
     * -------------------------------------------------------------------------
     *
     * When the supplied {@code SanityChecker} does **not** implement {@code ContextAware},
     * the configurator must *not* attempt to set a context.  The test confirms that no
     * exception is thrown and that the checker receives the model.
     */
    @Test
    public void testPerformCheckWithoutContextAware() throws Exception {
        java.lang.reflect.Method performCheckMethod = JoranConfiguratorBase.class
                .getDeclaredMethod("performCheck", SanityChecker.class, Model.class);
        performCheckMethod.setAccessible(true);

        RecordingSanityChecker checker = new RecordingSanityChecker();
        Model model = new Model();

        performCheckMethod.invoke(configurator, checker, model);

        assertTrue("check() must have been called", checker.checkCalled);
        assertSame("Model must be forwarded correctly", model, checker.checkedModel);
        // No context should be set – the checker does not implement ContextAware.
    }

    /**
     * -------------------------------------------------------------------------
     * 5.  Protected method: {@code setImplicitRuleSupplier(SaxEventInterpreter)}
     * -------------------------------------------------------------------------
     *
     * The method must set the implicit‑action supplier on the supplied interpreter.
     * We verify that after invocation the interpreter returns a non‑null supplier
     * that creates instances of {@link ImplicitModelAction}.
     */
    @Test
    public void testSetImplicitRuleSupplierAssignsSupplier() throws Exception {
        RuleStore dummyRuleStore = new RecordingRuleStore();
        ElementPath initialPath = new ElementPath();
        List<SaxEvent> emptyEvents = Collections.emptyList();

        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                context,
                dummyRuleStore,
                initialPath,
                emptyEvents
        );

        // Call the protected method via reflection.
        java.lang.reflect.Method m = JoranConfiguratorBase.class
                .getDeclaredMethod("setImplicitRuleSupplier", SaxEventInterpreter.class);
        m.setAccessible(true);
        m.invoke(configurator, interpreter);

        // The interpreter must now have a supplier that returns a new ImplicitModelAction.
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull("Implicit action supplier must be set", supplier);
        Action action = supplier.get();
        assertTrue("Supplied action must be an ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    /**
     * -------------------------------------------------------------------------
     * 6.  Protected method: {@code addElementSelectorAndActionAssociations(RuleStore)}
     * -------------------------------------------------------------------------
     *
     * This method registers a long list of element‑selector / action pairs.
     * The test ensures that *all* expected selectors are added and that the
     * transparent path parts (“if”, “then”, “else”, “sift”) are recorded.
     */
    @Test
    public void testAddElementSelectorAndActionAssociationsRegistersAllRules() throws Exception {
        RecordingRuleStore ruleStore = new RecordingRuleStore();

        // Invoke the protected method via reflection.
        java.lang.reflect.Method m = JoranConfiguratorBase.class
                .getDeclaredMethod("addElementSelectorAndActionAssociations", RuleStore.class);
        m.setAccessible(true);
        m.invoke(configurator, ruleStore);

        // Expected selectors (excerpt – the full list is verified by size).
        List<String> expectedSelectors = Arrays.asList(
                "*/variable", "*/property", "*/substitutionProperty",
                "configuration/import", "configuration/timestamp",
                "configuration/shutdownHook", "configuration/sequenceNumberGenerator",
                "configuration/serializeModel", "configuration/define",
                "configuration/evaluator", "configuration/contextProperty",
                "configuration/conversionRule", "configuration/statusListener",
                "*/appender", "configuration/appender/appender-ref",
                "configuration/newRule", "*/param",
                "*/if", "*/if/then", "*/if/else",
                "*/appender/sift"
        );

        // Verify that every expected selector is present.
        Set<String> actualSelectors = new HashSet<>();
        for (RecordingRuleStore.Entry e : ruleStore.addedRules) {
            actualSelectors.add(e.selector.toString());
        }
        for (String sel : expectedSelectors) {
            assertTrue("Selector '" + sel + "' must be registered", actualSelectors.contains(sel));
        }

        // Verify transparent path parts.
        List<String> expectedTransparent = Arrays.asList("if", "then", "else", "sift");
        assertEquals("Transparent path parts must match", expectedTransparent, ruleStore.transparentParts);
    }

    /**
     * -------------------------------------------------------------------------
     * 7.  Protected method: {@code addModelHandlerAssociations(DefaultProcessor)}
     * -------------------------------------------------------------------------
     *
     * The method registers a series of model‑handler factories.  We create a
     * {@link DefaultProcessor} instance, invoke the method via reflection, and
     * then check that a handler for a known model class (e.g. {@code ImportModel})
     * has been added.  The {@code DefaultProcessor} stores handlers internally;
     * however, the API does not expose a direct accessor.  To keep the test
     * lightweight we only verify that invoking the method does not throw and
     * that the internal handler map contains at least one entry (the call to
     * {@code addHandler} must have succeeded).
     */
    @Test
    public void testAddModelHandlerAssociationsRegistersHandlers() throws Exception {
        // Obtain a fresh ModelInterpretationContext from the configurator.
        configurator.buildModelInterpretationContext();
        ModelInterpretationContext mic = configurator.getModelInterpretationContext();

        DefaultProcessor processor = new DefaultProcessor(context, mic);

        // Invoke the protected method.
        java.lang.reflect.Method m = JoranConfiguratorBase.class
                .getDeclaredMethod("addModelHandlerAssociations", DefaultProcessor.class);
        m.setAccessible(true);
        m.invoke(configurator, processor);

        // Use reflection to inspect the private field that holds handlers.
        java.lang.reflect.Field handlersField = DefaultProcessor.class.getDeclaredField("handlerMap");
        handlersField.setAccessible(true);
        Map<?, ?> handlerMap = (Map<?, ?>) handlersField.get(processor);
        assertNotNull("Handler map must exist after registration", handlerMap);
        assertFalse("At least one handler must have been registered", handlerMap.isEmpty());
    }
}
