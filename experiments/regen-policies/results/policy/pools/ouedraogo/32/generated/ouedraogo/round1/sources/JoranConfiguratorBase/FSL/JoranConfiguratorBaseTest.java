package ch.qos.logback.core.joran;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.joran.conditional.IfAction;
import ch.qos.logback.core.joran.conditional.ThenAction;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Tests for {@link JoranConfiguratorBase}.
 */
public class JoranConfiguratorBaseTest {

    /** Concrete subclass used for testing – no extra behaviour required. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // No additional implementation – all abstract methods are already provided
        // by JoranConfiguratorBase.
    }

    /** Simple RuleStore implementation that records what is added. */
    private static class RecordingRuleStore implements RuleStore {

        final List<ElementSelector> selectors = new ArrayList<>();
        final List<Supplier<Action>> suppliers = new ArrayList<>();
        final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector elementSelector, String actionClassStr) {
            // Not used in JoranConfiguratorBase – ignore.
        }

        @Override
        public void addRule(ElementSelector elementSelector, Supplier<Action> actionSupplier) {
            selectors.add(elementSelector);
            suppliers.add(actionSupplier);
        }

        @Override
        public Supplier<Action> matchActions(ch.qos.logback.core.joran.spi.ElementPath elementPath) {
            // Not needed for the tests – return null.
            return null;
        }

        @Override
        public void addTransparentPathPart(String pathPart) {
            transparentParts.add(pathPart);
        }
    }

    /** A dummy sanity checker that also implements {@link ContextAware}. */
    private static class DummyChecker implements SanityChecker, ContextAware {

        private boolean contextSet = false;
        private boolean checked = false;
        private ContextBase context;

        @Override
        public void setContext(ch.qos.logback.core.spi.Context context) {
            this.context = (ContextBase) context;
            this.contextSet = true;
        }

        @Override
        public ch.qos.logback.core.spi.Context getContext() {
            return context;
        }

        @Override
        public void check(Model model) {
            this.checked = true;
        }

        // The remaining ContextAware methods are delegated to a simple ContextBase.
        private final ContextBase delegate = new ContextBase();

        @Override
        public void addStatus(ch.qos.logback.core.status.Status status) {
            delegate.addStatus(status);
        }

        @Override
        public void addInfo(String msg) {
            delegate.addInfo(msg);
        }

        @Override
        public void addInfo(String msg, Throwable ex) {
            delegate.addInfo(msg, ex);
        }

        @Override
        public void addWarn(String msg) {
            delegate.addWarn(msg);
        }

        @Override
        public void addWarn(String msg, Throwable ex) {
            delegate.addWarn(msg, ex);
        }

        @Override
        public void addError(String msg) {
            delegate.addError(msg);
        }

        @Override
        public void addError(String msg, Throwable ex) {
            delegate.addError(msg, ex);
        }

        public boolean isContextSet() {
            return contextSet;
        }

        public boolean isChecked() {
            return checked;
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

    @Test
    public void testAddElementSelectorAndActionAssociations() {
        RecordingRuleStore ruleStore = new RecordingRuleStore();

        // The method under test
        configurator.addElementSelectorAndActionAssociations(ruleStore);

        // Verify that a reasonable number of selectors have been added
        assertTrue("At least 20 selectors should be registered", ruleStore.selectors.size() >= 20);

        // Spot‑check a few expected selectors
        assertTrue(ruleStore.selectors.stream()
                .anyMatch(es -> "*/variable".equals(es.getPattern())));
        assertTrue(ruleStore.selectors.stream()
                .anyMatch(es -> "configuration/import".equals(es.getPattern())));
        assertTrue(ruleStore.selectors.stream()
                .anyMatch(es -> "*/if".equals(es.getPattern())));

        // Verify that transparent path parts have been registered
        assertTrue(ruleStore.transparentParts.contains("if"));
        assertTrue(ruleStore.transparentParts.contains("then"));
        assertTrue(ruleStore.transparentParts.contains("else"));
        assertTrue(ruleStore.transparentParts.contains("sift"));
    }

    @Test
    public void testPerformCheckWithContextAwareChecker() {
        DummyChecker dummy = new DummyChecker();
        Model dummyModel = new Model() {
            // No additional behaviour required for this test
        };

        // The method under test
        configurator.performCheck(dummy, dummyModel);

        // The dummy checker should have received the context from the configurator
        assertTrue("Context should have been set on the checker", dummy.isContextSet());
        assertEquals("The context set on the checker must be the configurator's context",
                context, dummy.getContext());

        // And its check method must have been invoked
        assertTrue("Checker's check method must have been called", dummy.isChecked());
    }

    @Test
    public void testSanityCheckExecutesWithoutException() {
        // Use a simple model – the concrete type does not matter for this test
        Model model = new Model() {
            // empty anonymous subclass
        };

        // The sanityCheck method should delegate to AppenderWithinAppenderSanityChecker.
        // We only verify that no exception propagates.
        configurator.sanityCheck(model);
    }

    @Test
    public void testGetInterpretationContextIsInitialized() {
        // Build the model interpretation context – this also creates the interpreter
        configurator.buildModelInterpretationContext();

        // Retrieve the interpretation context
        SaxEventInterpretationContext ic = configurator.getInterpretationContext();

        assertNotNull("Interpretation context must not be null after building", ic);
        // The context stored inside the interpretation context should be the same
        // as the configurator's context.
        assertEquals("Interpretation context must carry the configurator's context",
                context, ic.getContext());
    }

    @Test
    public void testAddModelHandlerAssociationsDoesNotThrow() {
        // Prepare a DefaultProcessor – it needs a Context and a ModelInterpretationContext
        configurator.buildModelInterpretationContext(); // ensure MIC is ready
        DefaultProcessor processor = new DefaultProcessor(context, configurator.getModelInterpretationContext());

        // The method under test – we only assert that it completes without throwing
        configurator.addModelHandlerAssociations(processor);
    }
}
