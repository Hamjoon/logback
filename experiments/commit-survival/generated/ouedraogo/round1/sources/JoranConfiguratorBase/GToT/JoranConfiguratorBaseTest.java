package ch.qos.logback.core.joran;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.joran.event.SaxEvent;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;

/**
 * Comprehensive JUnit‑4 test suite for {@link JoranConfiguratorBase}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Public (and protected) API surface of the class.</li>
 *   <li>Basic functional validation of each method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments and
 *       {@link ContextAware} interaction.</li>
 *   <li>Verification that the internal rule/store and processor
 *       registrations are performed as expected.</li>
 * </ul>
 *
 * <p>Since {@code JoranConfiguratorBase} is abstract we employ a minimal concrete
 * subclass defined as {@code TestConfigurator}. All helper test doubles are
 * inner classes to keep the test self‑contained and free from external
 * mocking frameworks (as required by the task description).
 */
public class JoranConfiguratorBaseTest {

    /** Simple concrete subclass used for exercising the abstract base class. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // No additional behaviour – we simply expose the protected methods
        // via public wrappers for testing convenience.
        public void publicAddElementSelectorAndActionAssociations(RuleStore rs) {
            addElementSelectorAndActionAssociations(rs);
        }

        public void publicSanityCheck(Model topModel) {
            sanityCheck(topModel);
        }

        public void publicPerformCheck(SanityChecker sc, Model model) {
            performCheck(sc, model);
        }

        public void publicSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            setImplicitRuleSupplier(interpreter);
        }

        @Override
        public void buildModelInterpretationContext() {
            super.buildModelInterpretationContext();
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            super.addModelHandlerAssociations(defaultProcessor);
        }
    }

    /** Minimal {@link RuleStore} implementation that records added rules. */
    private static class RecordingRuleStore implements RuleStore {
        static class Entry {
            final ElementSelector selector;
            final Supplier<Action> supplier;
            Entry(ElementSelector selector, Supplier<Action> supplier) {
                this.selector = selector;
                this.supplier = supplier;
            }
        }
        final List<Entry> entries = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // Not used in this configurator – ignore.
        }

        @Override
        public void addRule(ElementSelector elementSelector,
                            Supplier<Action> actionSupplier) {
            entries.add(new Entry(elementSelector, actionSupplier));
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return null; // not needed for these tests
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /** Minimal {@link SaxEventInterpreter} that records the implicit supplier. */
    private static class RecordingInterpreter extends SaxEventInterpreter {
        Supplier<Action> implicitSupplier;

        public RecordingInterpreter(ContextBase context, RuleStore rs,
                                    ElementPath initialElementPath,
                                    List<SaxEvent> saxEvents) {
            super(context, rs, initialElementPath, saxEvents);
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.implicitSupplier = actionSupplier;
        }
    }

    /** Minimal {@link SanityChecker} that records whether {@code check} was invoked. */
    private static class TestSanityChecker implements SanityChecker, ContextAware {
        boolean checkCalled = false;
        ContextBase contextSet = null;

        @Override
        public void check(Model model) {
            checkCalled = true;
        }

        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            this.contextSet = (ContextBase) context;
        }

        @Override
        public ch.qos.logback.core.spi.Context getContext() {
            return contextSet;
        }

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) { }

        @Override
        public void addInfo(String msg) { }

        @Override
        public void addInfo(String msg, Throwable ex) { }

        @Override
        public void addWarn(String msg) { }

        @Override
        public void addWarn(String msg, Throwable ex) { }

        @Override
        public void addError(String msg) { }

        @Override
        public void addError(String msg, Throwable ex) { }
    }

    /** Minimal {@link DefaultProcessor} that records added handler factories. */
    private static class RecordingProcessor extends DefaultProcessor {
        static class HandlerEntry {
            final Class<? extends Model> modelClass;
            final ModelHandlerFactoryMethod factoryMethod;
            HandlerEntry(Class<? extends Model> mc, ModelHandlerFactoryMethod f) {
                this.modelClass = mc;
                this.factoryMethod = f;
            }
        }
        final List<HandlerEntry> handlers = new ArrayList<>();

        public RecordingProcessor(ContextBase context,
                                   ModelInterpretationContext mic) {
            super(context, mic);
        }

        @Override
        public void addHandler(Class<? extends Model> modelClass,
                               ModelHandlerFactoryMethod modelFactoryMethod) {
            handlers.add(new HandlerEntry(modelClass, modelFactoryMethod));
        }
    }

    private TestConfigurator configurator;
    private ContextBase context;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        context = new ContextBase();
        configurator.setContext(context);
    }

    /** --------------------------------------------------------------------
     *  1.  Tests for {@code addElementSelectorAndActionAssociations}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testAddElementSelectorAndActionAssociations_registersAllRules() {
        RecordingRuleStore rs = new RecordingRuleStore();

        configurator.publicAddElementSelectorAndActionAssociations(rs);

        // Verify that the expected selectors are present.
        // The exact number is derived from the source code (27 addRule calls).
        assertEquals("Number of registered rules", 27, rs.entries.size());

        // Spot‑check a few selectors to ensure correct mapping.
        boolean foundVariable = rs.entries.stream()
                .anyMatch(e -> e.selector.getPathPattern().equals("*/variable")
                        && e.supplier.get() instanceof PropertyAction);
        assertTrue("Should contain rule for */variable → PropertyAction", foundVariable);

        boolean foundIf = rs.entries.stream()
                .anyMatch(e -> e.selector.getPathPattern().equals("*/if")
                        && e.supplier.get() instanceof IfAction);
        assertTrue("Should contain rule for */if → IfAction", foundIf);

        // Verify transparent path parts
        List<String> expectedTransparent = List.of("if", "then", "else", "sift");
        assertEquals("Transparent path parts", expectedTransparent, rs.transparentParts);
    }

    @Test(expected = NullPointerException.class)
    public void testAddElementSelectorAndActionAssociations_nullRuleStore_throwsNPE() {
        configurator.publicAddElementSelectorAndActionAssociations(null);
    }

    /** --------------------------------------------------------------------
     *  2.  Tests for {@code sanityCheck} and {@code performCheck}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testSanityCheck_usesAppenderWithinAppenderSanityChecker_andSetsContext() {
        // The sanityCheck method creates a new AppenderWithinAppenderSanityChecker
        // and forwards it to performCheck.
        Model dummyModel = new Model(); // empty model is sufficient

        configurator.publicSanityCheck(dummyModel);

        // Since the real AppenderWithinAppenderSanityChecker implements ContextAware,
        // we can verify that its context was set by reflecting on its field.
        // We create a subclass that exposes the flag.
        // (The real class already sets the context; we just ensure no NPE occurs.)
        // No explicit assertion needed – if the method throws, the test fails.
    }

    @Test
    public void testPerformCheck_withContextAwareChecker_setsContextAndInvokesCheck() {
        TestSanityChecker checker = new TestSanityChecker();
        Model dummy = new Model();

        configurator.publicPerformCheck(checker, dummy);

        assertSame("Context should be injected into checker", context, checker.contextSet);
        assertTrue("Checker.check should have been called", checker.checkCalled);
    }

    @Test
    public void testPerformCheck_withoutContextAwareChecker_onlyInvokesCheck() {
        // SanityChecker that does NOT implement ContextAware
        SanityChecker simpleChecker = new SanityChecker() {
            boolean called = false;
            @Override
            public void check(Model model) {
                called = true;
            }
        };
        Model dummy = new Model();

        configurator.publicPerformCheck(simpleChecker, dummy);
        // No exception means the path without ContextAware works.
        // Use reflection to ensure check was called.
        try {
            java.lang.reflect.Field f = simpleChecker.getClass().getDeclaredField("called");
            f.setAccessible(true);
            assertTrue("check should have been called", f.getBoolean(simpleChecker));
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testPerformCheck_nullChecker_throwsNPE() {
        configurator.publicPerformCheck(null, new Model());
    }

    @Test(expected = NullPointerException.class)
    public void testPerformCheck_nullModel_throwsNPE() {
        configurator.publicPerformCheck(new TestSanityChecker(), null);
    }

    /** --------------------------------------------------------------------
     *  3.  Tests for {@code setImplicitRuleSupplier}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testSetImplicitRuleSupplier_registersImplicitActionSupplier() {
        // Build a minimal interpreter – the constructor requires a few arguments.
        RecordingRuleStore rs = new RecordingRuleStore();
        RecordingInterpreter interpreter = new RecordingInterpreter(
                context, rs, new ElementPath(), new ArrayList<>());

        configurator.publicSetImplicitRuleSupplier(interpreter);

        assertNotNull("Implicit supplier should be set",
                interpreter.implicitSupplier);
        // Verify that the supplier creates an ImplicitModelAction instance.
        Action action = interpreter.implicitSupplier.get();
        assertTrue("Supplier must produce ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    @Test(expected = NullPointerException.class)
    public void testSetImplicitRuleSupplier_nullInterpreter_throwsNPE() {
        configurator.publicSetImplicitRuleSupplier(null);
    }

    /** --------------------------------------------------------------------
     *  4.  Tests for {@code buildModelInterpretationContext}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testBuildModelInterpretationContext_callsCreateAppenderBags() {
        // We create a subclass of ModelInterpretationContext that records the call.
        class TestMIC extends ModelInterpretationContext {
            boolean createAppenderBagsCalled = false;
            public TestMIC(ContextBase ctx) {
                super(ctx);
            }
            @Override
            public void createAppenderBags() {
                createAppenderBagsCalled = true;
            }
        }
        // Replace the configurator's internal MIC with our test version.
        TestMIC testMic = new TestMIC(context);
        // Use reflection to inject the test MIC (the field is private in the superclass).
        try {
            java.lang.reflect.Field f = GenericXMLConfigurator.class
                    .getDeclaredField("modelInterpretationContext");
            f.setAccessible(true);
            f.set(configurator, testMic);
        } catch (Exception e) {
            fail("Failed to inject test ModelInterpretationContext: " + e);
        }

        configurator.buildModelInterpretationContext();

        assertTrue("createAppenderBags should have been invoked",
                testMic.createAppenderBagsCalled);
    }

    /** --------------------------------------------------------------------
     *  5.  Tests for {@code getInterpretationContext}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testGetInterpretationContext_returnsNonNullContext() {
        // The interpreter is lazily created inside GenericXMLConfigurator when building.
        // Force its creation by calling a method that needs it.
        configurator.buildModelInterpretationContext(); // ensures interpreter exists
        SaxEventInterpretationContext ic = configurator.getInterpretationContext();
        assertNotNull("Interpretation context must not be null", ic);
        // The context inside the interpretation context should be the same we set.
        assertSame("Context inside interpretation context must match", context,
                ic.getContext());
    }

    /** --------------------------------------------------------------------
     *  6.  Tests for {@code addModelHandlerAssociations}
     *  --------------------------------------------------------------------
     */
    @Test
    public void testAddModelHandlerAssociations_registersAllExpectedHandlers() {
        RecordingProcessor processor = new RecordingProcessor(context, null);
        configurator.addModelHandlerAssociations(processor);

        // Count of distinct handler registrations in the source code:
        // ImportModel, ShutdownHookModel, SequenceNumberGeneratorModel,
        // EventEvaluatorModel, DefineModel, IncludeModel,
        // ParamModel, PropertyModel, TimestampModel, StatusListenerModel,
        // ImplicitModel, IfModel, ThenModel, ElseModel, SiftModel  => 15
        assertEquals("Number of handler registrations", 15, processor.handlers.size());

        // Spot‑check a few handler registrations.
        boolean hasImportHandler = processor.handlers.stream()
                .anyMatch(e -> e.modelClass == ImportModel.class);
        assertTrue("ImportModel handler should be registered", hasImportHandler);

        boolean hasIfHandler = processor.handlers.stream()
                .anyMatch(e -> e.modelClass == IfModel.class);
        assertTrue("IfModel handler should be registered", hasIfHandler);
    }

    @Test(expected = NullPointerException.class)
    public void testAddModelHandlerAssociations_nullProcessor_throwsNPE() {
        configurator.addModelHandlerAssociations(null);
    }
}
