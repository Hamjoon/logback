/**
 * JUnit‑4 test suite for {@link JoranConfiguratorBase}.
 *
 * The tests cover every public (or protected) method declared in
 * {@code JoranConfiguratorBase}.  For each method a basic functionality test is
 * provided together with edge‑case / exception tests.
 *
 * The test code uses only the classes, constructors and methods that appear in
 * the supplied source code or in the dependency signatures that were given.
 * No external mocking frameworks are used – simple concrete helper classes are
 * implemented inside the test file.
 */

package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;


import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.*;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.*;
import ch.qos.logback.core.model.processor.conditional.*;
import ch.qos.logback.core.sift.*;
import ch.qos.logback.core.spi.*;


/**
 * Concrete subclass of {@link JoranConfiguratorBase} that does not add any
 * extra behaviour – it simply inherits the implementations that are already
 * provided by the abstract base class.
 */
class TestJoranConfigurator extends JoranConfiguratorBase<Object> {
    // no additional code needed – all abstract methods are already implemented
}

/* --------------------------------------------------------------------- *
 * Helper implementations – they are tiny concrete classes that satisfy
 * the library interfaces without pulling in any additional Logback code.
 * --------------------------------------------------------------------- */

/**
 * Very small {@link RuleStore} implementation that records the rules added
 * via {@code addRule(...)} and the transparent path parts added via
 * {@code addTransparentPathPart(...)}.
 */
class SimpleRuleStore implements RuleStore {

    static class Rule {
        final ElementSelector selector;
        final Supplier<Action> supplier; // may be null when a class name is used
        Rule(ElementSelector selector, Supplier<Action> supplier) {
            this.selector = selector;
            this.supplier = supplier;
        }
    }

    final List<Rule> rules = new ArrayList<>();
    final List<String> transparentParts = new ArrayList<>();

    @Override
    public void addRule(ElementSelector elementSelector,
                        String actionClassStr) throws ClassNotFoundException {
        // Not used in the class under test – keep for completeness
        rules.add(new Rule(elementSelector, null));
    }

    @Override
    public void addRule(ElementSelector elementSelector,
                        Supplier<Action> actionSupplier) {
        rules.add(new Rule(elementSelector, actionSupplier));
    }

    @Override
    public Supplier<Action> matchActions(ElementPath elementPath) {
        // Very naive matcher – not needed for our tests
        for (Rule r : rules) {
            if (r.selector.getPath().equals(elementPath.toString())) {
                return r.supplier;
            }
        }
        return null;
    }

    @Override
    public void addTransparentPathPart(String pathPart) {
        transparentParts.add(pathPart);
    }

    @Override
    public void addPathPathMapping(String originalName, String modifiedName) {
        // not required for the current test suite
    }
}

/**
 * Simple {@link SanityChecker} that also implements {@link ContextAware}
 * so that we can verify that {@code performCheck(...)} correctly injects the
 * configurator's context.
 */
class DummySanityChecker extends ContextAwareBase implements SanityChecker {

    boolean checked = false;
    Context receivedContext = null;

    @Override
    public void check(Model model) {
        checked = true;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        receivedContext = context;
    }
}

/**
 * Minimal {@link DefaultProcessor} subclass exposing the handlers that have
 * been registered via {@code addHandler(...)}.
 */
class RecordingDefaultProcessor extends DefaultProcessor {

    final Set<Class<? extends Model>> registeredHandlers = new HashSet<>();

    public RecordingDefaultProcessor(Context context,
                                     ModelInterpretationContext mic) {
        super(context, mic);
    }

    @Override
    public void addHandler(Class<? extends Model> modelClass,
                           ModelHandlerFactoryMethod modelFactoryMethod) {
        registeredHandlers.add(modelClass);
        super.addHandler(modelClass, modelFactoryMethod);
    }
}

/* --------------------------------------------------------------------- *
 * The test class itself.
 * --------------------------------------------------------------------- */
public class JoranConfiguratorBaseTest {

    private TestJoranConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
        // The configurator inherits ContextAwareBase – we can safely set a null
        // context because the code under test never dereferences it.
        configurator.setContext(null);
    }

    /* --------------------------------------------------------------- *
     * 1. addElementSelectorAndActionAssociations
     * --------------------------------------------------------------- */

    @Test
    public void testAddElementSelectorAndActionAssociations_basic() {
        SimpleRuleStore rs = new SimpleRuleStore();

        configurator.addElementSelectorAndActionAssociations(rs);

        // 22 distinct addRule calls are made in the source code
        assertEquals("Number of rules added", 22, rs.rules.size());

        // 4 transparent path parts are added: if, then, else, sift
        List<String> expectedTransparent = Arrays.asList("if", "then", "else", "sift");
        assertEquals("Transparent path parts", expectedTransparent, rs.transparentParts);
    }

    @Test(expected = NullPointerException.class)
    public void testAddElementSelectorAndActionAssociations_nullRuleStore() {
        configurator.addElementSelectorAndActionAssociations(null);
    }

    /* --------------------------------------------------------------- *
     * 2. performCheck & sanityCheck
     * --------------------------------------------------------------- */

    @Test
    public void testPerformCheck_withContextAwareChecker() {
        DummySanityChecker checker = new DummySanityChecker();
        Model model = new Model();

        configurator.performCheck(checker, model);

        assertTrue("Checker.check(...) should have been invoked", checker.checked);
        assertNull("Context injected should be null (we set null in setUp)", checker.receivedContext);
    }

    @Test
    public void testPerformCheck_withoutContextAware() {
        // A SanityChecker that does NOT implement ContextAware
        SanityChecker plainChecker = new SanityChecker() {
            boolean called = false;
            @Override
            public void check(Model model) {
                called = true;
            }
        };
        Model model = new Model();

        configurator.performCheck(plainChecker, model);
        // No exception – just ensure the method runs
        // (cannot inspect internal state of the anonymous checker)
    }

    @Test(expected = NullPointerException.class)
    public void testPerformCheck_nullChecker() {
        configurator.performCheck(null, new Model());
    }

    @Test
    public void testSanityCheck_executesWithoutException() {
        Model top = new Model();
        // The real checker (AppenderWithinAppenderSanityChecker) is part of the
        // library; we only verify that the call does not throw.
        configurator.sanityCheck(top);
    }

    /* --------------------------------------------------------------- *
     * 3. setImplicitRuleSupplier
     * --------------------------------------------------------------- */

    @Test
    public void testSetImplicitRuleSupplier_setsSupplier() {
        // Minimal interpreter – all constructor arguments may be null or empty
        SimpleRuleStore rs = new SimpleRuleStore();
        SaxEventInterpreter interpreter = new SaxEventInterpreter(
                null, rs, null, Collections.emptyList());

        configurator.setImplicitRuleSupplier(interpreter);

        // The interpreter now holds a non‑null supplier; we cannot access it
        // directly, but we can verify that calling the supplier does not throw.
        Supplier<Action> supplier = interpreter.getImplicitActionSupplier();
        assertNotNull("Implicit action supplier must be set", supplier);
        // The supplier should create an ImplicitModelAction instance.
        Action action = supplier.get();
        assertNotNull("Supplier must return a non‑null Action", action);
    }

    @Test(expected = NullPointerException.class)
    public void testSetImplicitRuleSupplier_nullInterpreter() {
        configurator.setImplicitRuleSupplier(null);
    }

    /* --------------------------------------------------------------- *
     * 4. buildModelInterpretationContext & getInterpretationContext
     * --------------------------------------------------------------- */

    @Test
    public void testBuildModelInterpretationContext_initialisesContext() {
        configurator.buildModelInterpretationContext();

        SaxEventInterpretationContext ic = configurator.getInterpretationContext();
        assertNotNull("Interpretation context must be created", ic);
        // The newly created context should have an empty model stack.
        assertTrue("Model stack must be empty after construction",
                ic.isModelStackEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuildModelInterpretationContext_twiceThrows() {
        // The base implementation does not forbid a second call, but many
        // configurators assume a single use.  We simulate the illegal‑reuse
        // scenario by invoking the method a second time after the first call
        // and expecting an IllegalStateException (the real Logback code throws
        // this when the context is already built).  If the implementation does
        // not throw, the test will fail – this documents the expected contract.
        configurator.buildModelInterpretationContext();
        configurator.buildModelInterpretationContext(); // second call
    }

    /* --------------------------------------------------------------- *
     * 5. addModelHandlerAssociations
     * --------------------------------------------------------------- */

    @Test
    public void testAddModelHandlerAssociations_registersAllHandlers() {
        // Use a RecordingDefaultProcessor so we can inspect the registered keys
        RecordingDefaultProcessor processor = new RecordingDefaultProcessor(null, null);
        configurator.addModelHandlerAssociations(processor);

        // The source registers handlers for the following model classes
        Class<?>[] expected = new Class<?>[] {
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

        for (Class<?> c : expected) {
            assertTrue("Handler for " + c.getSimpleName() + " must be registered",
                    processor.registeredHandlers.contains(c));
        }
        // Ensure no unexpected handlers were added
        assertEquals("Exactly the expected number of handlers should be registered",
                expected.length, processor.registeredHandlers.size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddModelHandlerAssociations_nullProcessor() {
        configurator.addModelHandlerAssociations(null);
    }
}
