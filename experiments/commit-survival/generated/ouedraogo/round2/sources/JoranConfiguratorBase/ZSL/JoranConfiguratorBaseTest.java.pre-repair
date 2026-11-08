/**
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests exercise the protected behaviour of the configurator without using any
 * mocking framework – only concrete implementations that are part of the public
 * API are instantiated.
 */
package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Concrete subclass that makes the protected members of {@link JoranConfiguratorBase}
 * visible for testing.
 */
class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

    /* --------------------------------------------------------------------- */
    /* expose protected methods for the test suite                           */
    /* --------------------------------------------------------------------- */

    void exposeAddElementSelectorAndActionAssociations(RuleStore rs) {
        super.addElementSelectorAndActionAssociations(rs);
    }

    void exposePerformCheck(SanityChecker sc, Model model) {
        super.performCheck(sc, model);
    }

    void exposeAddModelHandlerAssociations(DefaultProcessor dp) {
        super.addModelHandlerAssociations(dp);
    }

    void exposeSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        super.setImplicitRuleSupplier(interpreter);
    }

    void exposeBuildModelInterpretationContext() {
        super.buildModelInterpretationContext();
    }

    /* --------------------------------------------------------------------- */
    /* helper to inject a SaxEventInterpreter (the field is protected in the
       parent class)                                                            */
    /* --------------------------------------------------------------------- */
    void setInterpreter(SaxEventInterpreter interpreter) {
        this.saxEventInterpreter = interpreter;
    }
}

/* ------------------------------------------------------------------------- */
/* Minimal implementations of Logback SPI classes used by the tests          */
/* ------------------------------------------------------------------------- */

/**
 * Very small {@link RuleStore} implementation that only records the
 * selectors and transparent path parts that were added.
 */
class RecordingRuleStore implements RuleStore {

    final List<String> selectors = new ArrayList<>();
    final List<String> transparentParts = new ArrayList<>();
    final Map<String, Supplier<Action>> actions = new HashMap<>();

    @Override
    public void addRule(ElementSelector elementSelector, String actionClassStr) {
        // not used by the configurator – keep for completeness
        selectors.add(elementSelector.toString() + "->" + actionClassStr);
    }

    @Override
    public void addRule(ElementSelector elementSelector,
                        Supplier<Action> actionSupplier) {
        selectors.add(elementSelector.toString());
        actions.put(elementSelector.toString(), actionSupplier);
    }

    @Override
    public Supplier<Action> matchActions(ElementPath elementPath) {
        // not needed for the tests
        return null;
    }

    @Override
    public void addTransparentPathPart(String pathPart) {
        transparentParts.add(pathPart);
    }
}

/**
 * Simple {@link Context} implementation – only the methods required by
 * {@link ContextAwareBase} are needed.
 */
class DummyContext implements Context {
    private final Map<String, String> propertyMap = new HashMap<>();

    @Override public String getName() { return "dummy"; }
    @Override public void setName(String name) {}
    @Override public Object getObject(String key) { return null; }
    @Override public void putObject(String key, Object value) {}
    @Override public void removeObject(String key) {}
    @Override public void addStatusListener(ch.qos.logback.core.status.StatusListener sl) {}
    @Override public void addInfo(String msg) {}
    @Override public void addInfo(String msg, Throwable ex) {}
    @Override public void addWarn(String msg) {}
    @Override public void addWarn(String msg, Throwable ex) {}
    @Override public void addError(String msg) {}
    @Override public void addError(String msg, Throwable ex) {}
    @Override public void addStatus(ch.qos.logback.core.status.Status status) {}
    @Override public void reset() {}
    @Override public ch.qos.logback.core.status.StatusManager getStatusManager() { return null; }
    @Override public void setPackagingDataEnabled(boolean packagingDataEnabled) {}
    @Override public boolean isPackagingDataEnabled() { return false; }
    @Override public void setMaximumCallerDataDepth(int depth) {}
    @Override public int getMaximumCallerDataDepth() { return 0; }
    @Override public void putProperty(String key, String value) { propertyMap.put(key, value); }
    @Override public String getProperty(String key) { return propertyMap.get(key); }
    @Override public Map<String, String> getCopyOfPropertyMap() { return new HashMap<>(propertyMap); }
}

/**
 * {@link SanityChecker} that records whether {@code check(..)} has been
 * invoked and, if it also implements {@link ContextAware}, whether the context
 * was injected.
 */
class RecordingSanityChecker extends ContextAwareBase implements SanityChecker {

    Model checkedModel = null;
    boolean contextInjected = false;

    @Override
    public void check(Model model) {
        this.checkedModel = model;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        this.contextInjected = true;
    }
}

/**
 * A very small {@link DefaultProcessor} that only records the model classes
 * for which a handler has been registered.
 */
class RecordingProcessor extends DefaultProcessor {

    final List<Class<? extends Model>> registeredModelClasses = new ArrayList<>();

    public RecordingProcessor() {
        super(new DummyContext(), null);
    }

    @Override
    public void addHandler(Class<? extends Model> modelClass,
                           ModelHandlerFactoryMethod modelFactoryMethod) {
        registeredModelClasses.add(modelClass);
    }
}

/**
 * Minimal {@link SaxEventInterpreter} that supplies a real
 * {@link SaxEventInterpretationContext}.  All arguments required by the
 * constructor are supplied with dummy objects.
 */
class SimpleSaxEventInterpreter extends SaxEventInterpreter {

    public SimpleSaxEventInterpreter(Context ctx) {
        super(ctx,
              new RecordingRuleStore(),
              null,                     // ElementPath – not needed for the test
              Collections.emptyList()); // no events
    }
}

/* ------------------------------------------------------------------------- */
/* The actual test class                                                     */
/* ------------------------------------------------------------------------- */
public class JoranConfiguratorBaseTest {

    private TestJoranConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
        configurator.setContext(new DummyContext());
    }

    /* --------------------------------------------------------------------- */
    /* 1. addElementSelectorAndActionAssociations                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddElementSelectorAndActionAssociations() {
        RecordingRuleStore rs = new RecordingRuleStore();

        configurator.exposeAddElementSelectorAndActionAssociations(rs);

        // verify a few representative selectors
        assertTrue(rs.selectors.contains("*/variable"));
        assertTrue(rs.selectors.contains("*/property"));
        assertTrue(rs.selectors.contains("configuration/import"));
        assertTrue(rs.selectors.contains("*/appender"));
        assertTrue(rs.selectors.contains("*/if"));
        assertTrue(rs.selectors.contains("*/if/then"));
        assertTrue(rs.selectors.contains("*/if/else"));
        assertTrue(rs.selectors.contains("*/appender/sift"));

        // verify transparent path parts
        List<String> tp = rs.transparentParts;
        assertTrue(tp.contains("if"));
        assertTrue(tp.contains("then"));
        assertTrue(tp.contains("else"));
        assertTrue(tp.contains("sift"));
    }

    /* --------------------------------------------------------------------- */
    /* 2. performCheck – context injection and check invocation                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPerformCheckContextInjection() {
        RecordingSanityChecker checker = new RecordingSanityChecker();
        Model dummyModel = new Model();

        configurator.exposePerformCheck(checker, dummyModel);

        // the checker should have received the model
        assertSame(dummyModel, checker.checkedModel);
        // because the checker implements ContextAware, the context must have been set
        assertTrue(checker.contextInjected);
        assertSame(configurator.getContext(), checker.getContext());
    }

    /* --------------------------------------------------------------------- */
    /* 3. sanityCheck – uses the real AppenderWithinAppenderSanityChecker   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSanityCheckDoesNotThrow() {
        // the real checker does not throw; we just make sure the method runs
        Model top = new Model();
        configurator.sanityCheck(top);
        // no exception → test passes
    }

    /* --------------------------------------------------------------------- */
    /* 4. setImplicitRuleSupplier – the supplier must be set on the interpreter */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSetImplicitRuleSupplier() {
        SimpleSaxEventInterpreter interpreter = new SimpleSaxEventInterpreter(new DummyContext());

        configurator.exposeSetImplicitRuleSupplier(interpreter);

        // there is no public getter for the supplier, but we can verify that the
        // interpreter does not reject the call (i.e. no exception) and that it
        // holds a non‑null supplier after the call.
        // The interpreter's internal field is package‑private, therefore we use
        // reflection to read it.
        try {
            java.lang.reflect.Field f = SaxEventInterpreter.class.getDeclaredField("implicitActionSupplier");
            f.setAccessible(true);
            Object supplier = f.get(interpreter);
            assertNotNull("Implicit action supplier must be set", supplier);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 5. buildModelInterpretationContext – ensure createAppenderBags() is called */
    /* --------------------------------------------------------------------- */
    @Test
    public void testBuildModelInterpretationContextCreatesAppenderBags() {
        // The base class stores the ModelInterpretationContext in a protected field.
        // We expose the method and then check that the context is not null
        // (the real implementation creates the object and calls createAppenderBags()).
        configurator.exposeBuildModelInterpretationContext();

        // Access the protected field using reflection
        try {
            java.lang.reflect.Field f = GenericXMLConfigurator.class
                    .getDeclaredField("modelInterpretationContext");
            f.setAccessible(true);
            Object mic = f.get(configurator);
            assertNotNull("ModelInterpretationContext should have been created", mic);
        } catch (Exception e) {
            fail("Unable to access modelInterpretationContext: " + e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 6. getInterpretationContext – returns the context created by the interpreter */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetInterpretationContext() {
        SimpleSaxEventInterpreter interpreter = new SimpleSaxEventInterpreter(new DummyContext());
        configurator.setInterpreter(interpreter);

        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull("Interpretation context must not be null", ctx);
        // the returned context must be the one belonging to the interpreter
        assertSame(interpreter.getSaxEventInterpretationContext(), ctx);
    }

    /* --------------------------------------------------------------------- */
    /* 7. addModelHandlerAssociations – all expected model classes are registered */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddModelHandlerAssociations() {
        RecordingProcessor processor = new RecordingProcessor();

        configurator.exposeAddModelHandlerAssociations(processor);

        // Verify a subset of the expected model classes
        assertTrue(processor.registeredModelClasses.contains(ImportModel.class));
        assertTrue(processor.registeredModelClasses.contains(ShutdownHookModel.class));
        assertTrue(processor.registeredModelClasses.contains(SequenceNumberGeneratorModel.class));
        assertTrue(processor.registeredModelClasses.contains(EventEvaluatorModel.class));
        assertTrue(processor.registeredModelClasses.contains(DefineModel.class));
        assertTrue(processor.registeredModelClasses.contains(ParamModel.class));
        assertTrue(processor.registeredModelClasses.contains(PropertyModel.class));
        assertTrue(processor.registeredModelClasses.contains(TimestampModel.class));
        assertTrue(processor.registeredModelClasses.contains(StatusListenerModel.class));
        assertTrue(processor.registeredModelClasses.contains(ImplicitModel.class));
        assertTrue(processor.registeredModelClasses.contains(IfModel.class));
        assertTrue(processor.registeredModelClasses.contains(ThenModel.class));
        assertTrue(processor.registeredModelClasses.contains(ElseModel.class));
        assertTrue(processor.registeredModelClasses.contains(SiftModel.class));
    }
}
