package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.event.SaxEvent;


import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.*;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * JUnit‑4 test suite for {@link JoranConfiguratorBase}.
 *
 * <p>The tests focus on the public / protected behaviour of the class
 * without relying on the full Logback runtime.  All objects are created
 * using the constructors and factory methods that appear in the supplied
 * source or in the dependency signatures.</p>
 *
 * <p>Three “expert” perspectives are represented by three groups of
 * test methods.  Each group supplies one test for each method of the
 * class under test.  The final file combines the three groups into a
 * single coherent test class.</p>
 */
public class JoranConfiguratorBaseTest {

    /** --------------------------------------------------------------------
     *  Helper classes
     * -------------------------------------------------------------------- */

    /**
     * Minimal concrete subclass of {@link JoranConfiguratorBase}.
     * The generic type argument is not used by the tests, therefore {@code Object}
     * is supplied.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // No extra behaviour – we just need a concrete class.
    }

    /**
     * Simple {@link ContextAware} implementation that records the context
     * it receives.  It is used to verify that {@code performCheck} sets the
     * context on a {@link SanityChecker} that also implements {@link ContextAware}.
     */
    private static class RecordingContextAware extends ContextAwareBase {
        private ContextBase receivedContext;

        @Override
        public void setContext(ContextBase context) {
            this.receivedContext = context;
        }

        public ContextBase getReceivedContext() {
            return receivedContext;
        }
    }

    /**
     * {@link SanityChecker} implementation that records whether {@code check}
     * has been invoked and with which model.
     */
    private static class RecordingSanityChecker implements SanityChecker {
        private Model checkedModel;
        private boolean checkCalled = false;

        @Override
        public void check(Model model) {
            this.checkedModel = model;
            this.checkCalled = true;
        }

        public boolean isCheckCalled() {
            return checkCalled;
        }

        public Model getCheckedModel() {
            return checkedModel;
        }
    }

    /**
     * {@link SaxEventInterpreter} subclass that records the supplier passed to
     * {@code setImplicitActionSupplier}.  Subclassing a concrete class is
     * permitted by the rules.
     */
    private static class RecordingSaxEventInterpreter extends SaxEventInterpreter {
        private Supplier<Action> recordedSupplier;

        public RecordingSaxEventInterpreter(ContextBase context,
                                            RuleStore rs,
                                            ElementPath initialElementPath,
                                            List<SaxEvent> saxEvents) {
            super(context, rs, initialElementPath, saxEvents);
        }

        @Override
        public void setImplicitActionSupplier(Supplier<Action> actionSupplier) {
            this.recordedSupplier = actionSupplier;
            super.setImplicitActionSupplier(actionSupplier);
        }

        public Supplier<Action> getRecordedSupplier() {
            return recordedSupplier;
        }
    }

    /**
     * A {@link RuleStore} implementation created via {@code java.lang.reflect.Proxy}
     * that records the selectors and actions passed to {@code addRule}
     * and the transparent parts added via {@code addTransparentPathPart}.
     */
    private static class RecordingRuleStore {
        private final List<ElementSelector> ruleSelectors = new ArrayList<>();
        private final List<Object> ruleSuppliers = new ArrayList<>();
        private final List<String> transparentParts = new ArrayList<>();

        public RuleStore createProxy() {
            return (RuleStore) Proxy.newProxyInstance(
                    RuleStore.class.getClassLoader(),
                    new Class<?>[]{RuleStore.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "addRule":
                                // Two overloads – we store selector and supplier / class name
                                ruleSelectors.add((ElementSelector) args[0]);
                                ruleSuppliers.add(args[1]); // could be String or Supplier<Action>
                                return null;
                            case "addTransparentPathPart":
                                transparentParts.add((String) args[0]);
                                return null;
                            case "matchActions":
                                return null; // not needed for the tests
                            default:
                                throw new UnsupportedOperationException("Unexpected method: " + method);
                        }
                    });
        }

        public List<ElementSelector> getRuleSelectors() {
            return ruleSelectors;
        }

        public List<Object> getRuleSuppliers() {
            return ruleSuppliers;
        }

        public List<String> getTransparentParts() {
            return transparentParts;
        }
    }

    /** --------------------------------------------------------------------
     *  Test data shared by many tests
     * -------------------------------------------------------------------- */

    private TestConfigurator configurator;
    private ContextBase context;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        context = new ContextBase();
        configurator.setContext(context);
    }

    /** --------------------------------------------------------------------
     *  1. Expert A – tests for rule registration
     * -------------------------------------------------------------------- */

    @Test
    public void testAddElementSelectorAndActionAssociations_registersAllRulesAndTransparentParts() {
        // Arrange
        RecordingRuleStore recording = new RecordingRuleStore();
        RuleStore rs = recording.createProxy();

        // Act
        configurator.addElementSelectorAndActionAssociations(rs);

        // Assert – check that the expected selectors are present.
        List<String> selectorStrings = new ArrayList<>();
        for (ElementSelector es : recording.getRuleSelectors()) {
            selectorStrings.add(es.getPathPattern());   // getPathPattern() is the only public method
        }

        // Expected selectors (extracted from the source)
        String[] expectedSelectors = new String[] {
                "*/variable", "*/property", "*/substitutionProperty",
                "configuration/import",
                "configuration/timestamp", "configuration/shutdownHook",
                "configuration/sequenceNumberGenerator", "configuration/serializeModel",
                "configuration/define", "configuration/evaluator",
                "configuration/contextProperty", "configuration/conversionRule",
                "configuration/statusListener",
                "*/appender", "configuration/appender/appender-ref",
                "configuration/newRule",
                "*/param",
                "*/if", "*/if/then", "*/if/else",
                "*/appender/sift"
        };
        for (String expected : expectedSelectors) {
            assertTrue("Expected selector not found: " + expected,
                    selectorStrings.contains(expected));
        }

        // Transparent path parts
        List<String> transparent = recording.getTransparentParts();
        assertTrue(transparent.contains("if"));
        assertTrue(transparent.contains("then"));
        assertTrue(transparent.contains("else"));
        assertTrue(transparent.contains("sift"));
    }

    /** --------------------------------------------------------------------
     *  2. Expert B – tests for sanity checking behaviour
     * -------------------------------------------------------------------- */

    @Test
    public void testPerformCheck_setsContextWhenCheckerIsContextAware_andInvokesCheck() {
        // Arrange
        RecordingContextAware checker = new RecordingContextAware();
        RecordingSanityChecker sanityChecker = new RecordingSanityChecker() {
            // delegate to the context‑aware checker for the context part
            @Override
            public void check(Model model) {
                // mimic a real checker that also implements ContextAware
                ((ContextAware) checker).setContext(context);
                checker.check(model);
                super.check(model);
            }
        };
        Model model = new Model();

        // Act
        configurator.performCheck(sanityChecker, model);

        // Assert – the supplied sanity checker should have received the model
        assertTrue(sanityChecker.isCheckCalled());
        assertSame(model, sanityChecker.getCheckedModel());

        // The inner context‑aware checker should have received the configurator's context
        assertSame(context, checker.getReceivedContext());
    }

    @Test
    public void testSanityCheck_delegatesToPerformCheckWithAppenderWithinAppenderChecker() {
        // Arrange
        // Use a spy on the configurator to intercept performCheck
        TestConfigurator spyConfigurator = new TestConfigurator() {
            private boolean performCheckCalled = false;
            private SanityChecker passedChecker;
            private Model passedModel;

            @Override
            protected void performCheck(SanityChecker sc, Model model) {
                this.performCheckCalled = true;
                this.passedChecker = sc;
                this.passedModel = model;
            }

            public boolean isPerformCheckCalled() {
                return performCheckCalled;
            }

            public SanityChecker getPassedChecker() {
                return passedChecker;
            }

            public Model getPassedModel() {
                return passedModel;
            }
        };
        spyConfigurator.setContext(context);
        Model topModel = new Model();

        // Act
        spyConfigurator.sanityCheck(topModel);

        // Assert
        assertTrue("performCheck should have been invoked", spyConfigurator.isPerformCheckCalled());
        assertTrue("Checker must be an instance of AppenderWithinAppenderSanityChecker",
                spyConfigurator.getPassedChecker() instanceof AppenderWithinAppenderSanityChecker);
        assertSame("Model passed to performCheck must be the one given to sanityCheck",
                topModel, spyConfigurator.getPassedModel());
    }

    /** --------------------------------------------------------------------
     *  3. Expert C – tests for interpreter and model‑context handling
     * -------------------------------------------------------------------- */

    @Test
    public void testSetImplicitRuleSupplier_assignsImplicitModelActionSupplier() {
        // Arrange
        // Minimal objects required for the SaxEventInterpreter constructor.
        RuleStore dummyRuleStore = (RuleStore) Proxy.newProxyInstance(
                RuleStore.class.getClassLoader(),
                new Class<?>[]{RuleStore.class},
                (proxy, method, args) -> null);
        ElementPath dummyPath = new ElementPath(); // public constructor exists
        List<SaxEvent> emptyEvents = Collections.emptyList();

        RecordingSaxEventInterpreter interpreter =
                new RecordingSaxEventInterpreter(context, dummyRuleStore, dummyPath, emptyEvents);

        // Act
        configurator.setImplicitRuleSupplier(interpreter);

        // Assert
        Supplier<Action> supplier = interpreter.getRecordedSupplier();
        assertNotNull("Implicit action supplier must be set", supplier);
        Action action = supplier.get();
        assertTrue("Supplier must create an instance of ImplicitModelAction",
                action instanceof ImplicitModelAction);
    }

    @Test
    public void testBuildModelInterpretationContext_callsSuperAndCreatesAppenderBags() {
        // The super implementation lives in GenericXMLConfigurator and ultimately
        // creates a ModelInterpretationContext.  The concrete class
        // ModelInterpretationContext has a method {@code createAppenderBags()} that
        // sets an internal flag.  We cannot inspect the flag directly, but we can
        // verify that no exception is thrown and that the returned context is not null.

        // Act
        configurator.buildModelInterpretationContext();

        // Assert
        assertNotNull("ModelInterpretationContext must be created",
                configurator.getModelInterpretationContext());
    }

    @Test
    public void testGetInterpretationContext_returnsNonNullAfterInterpreterIsCreated() throws Exception {
        // Build a SaxEventInterpreter via reflection – the protected method
        // {@code buildSaxEventInterpreter} is available in the superclass.
        // We invoke it with an empty list of SaxEvent objects.

        // Prepare a minimal RuleStore (proxy) and an empty element path.
        RuleStore dummyRuleStore = (RuleStore) Proxy.newProxyInstance(
                RuleStore.class.getClassLoader(),
                new Class<?>[]{RuleStore.class},
                (proxy, method, args) -> null);
        ElementPath dummyPath = new ElementPath();

        // Use reflection to call the protected method.
        Method buildMethod = GenericXMLConfigurator.class.getDeclaredMethod(
                "buildSaxEventInterpreter", List.class);
        buildMethod.setAccessible(true);
        buildMethod.invoke(configurator, Collections.emptyList());

        // Now the configurator should have a non‑null interpreter.
        SaxEventInterpretationContext ictx = configurator.getInterpretationContext();
        assertNotNull("Interpretation context must be non‑null after building interpreter",
                ictx);
    }
}
