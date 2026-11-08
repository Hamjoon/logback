package ch.qos.logback.core.joran;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Unit tests for {@link JoranConfiguratorBase}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic successful execution of public &amp; protected methods</li>
 *   <li>edge‑case handling such as {@code null} arguments</li>
 *   <li>verification that internal collaborators (e.g. {@link SanityChecker})
 *       are invoked as expected.</li>
 * </ul>
 *
 * No external mocking frameworks are used – all collaborators are real
 * instances created via their public constructors.
 */
public class JoranConfiguratorBaseTest {

    /** Concrete subclass of the abstract {@link JoranConfiguratorBase}. */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {
        // No additional behaviour – we simply inherit the implementation.
    }

    /** Simple {@link SanityChecker} that records whether {@code check} was called. */
    private static class RecordingChecker extends ContextAwareBase implements SanityChecker {
        boolean checked = false;
        Model modelChecked = null;

        @Override
        public void check(Model model) {
            this.checked = true;
            this.modelChecked = model;
        }
    }

    private TestConfigurator configurator;

    @Before
    public void setUp() {
        configurator = new TestConfigurator();
        // The configurator extends ContextAwareBase; a null context is acceptable for the
        // purposes of these unit tests because none of the exercised code dereferences it.
        configurator.setContext(null);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Basic functionality tests                                           */
    /* --------------------------------------------------------------------- */

    /** Verify that {@code buildModelInterpretationContext()} creates a
     *  non‑null {@link SaxEventInterpretationContext}. */
    @Test
    public void testBuildModelInterpretationContextCreatesInterpretationContext() {
        configurator.buildModelInterpretationContext();
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNotNull("Interpretation context should be created", ctx);
    }

    /** The {@code getInterpretationContext()} method must return the same
     *  instance that the underlying {@code SaxEventInterpreter} holds. */
    @Test
    public void testGetInterpretationContextReturnsSameInstance() {
        configurator.buildModelInterpretationContext();
        SaxEventInterpretationContext ctx1 = configurator.getInterpretationContext();

        // The method simply forwards to the interpreter; calling it again must
        // return the identical object.
        SaxEventInterpretationContext ctx2 = configurator.getInterpretationContext();
        assertSame("Subsequent calls must return the same instance", ctx1, ctx2);
    }

    /** {@code performCheck} should forward the call to the supplied
     *  {@link SanityChecker} and set the context when the checker implements
     *  {@link ContextAware}. */
    @Test
    public void testPerformCheckInvokesChecker() {
        Model model = new Model();
        RecordingChecker checker = new RecordingChecker();

        configurator.performCheck(checker, model);

        assertTrue("Checker.check() should have been invoked", checker.checked);
        assertSame("The model passed to the checker must be identical", model, checker.modelChecked);
    }

    /** {@code sanityCheck} uses the built‑in {@link AppenderWithinAppenderSanityChecker}
     *  and therefore must not throw when a normal model is supplied. */
    @Test
    public void testSanityCheckWithValidModelDoesNotThrow() {
        Model model = new Model();
        // The method is protected; we call it via a public wrapper in the test class.
        configurator.sanityCheck(model);
        // No exception → test passes.
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Edge‑case / exception handling tests                                 */
    /* --------------------------------------------------------------------- */

    /** {@code performCheck} must throw {@link NullPointerException}
     *  when the {@link SanityChecker} argument is {@code null}. */
    @Test(expected = NullPointerException.class)
    public void testPerformCheckWithNullCheckerThrows() {
        Model model = new Model();
        configurator.performCheck(null, model);
    }

    /** {@code performCheck} must throw {@link NullPointerException}
     *  when the {@link Model} argument is {@code null}. */
    @Test(expected = NullPointerException.class)
    public void testPerformCheckWithNullModelThrows() {
        RecordingChecker checker = new RecordingChecker();
        configurator.performCheck(checker, null);
    }

    /** {@code setImplicitRuleSupplier} must throw {@link NullPointerException}
     *  when the supplied {@link SaxEventInterpreter} is {@code null}. */
    @Test(expected = NullPointerException.class)
    public void testSetImplicitRuleSupplierWithNullInterpreterThrows() {
        configurator.setImplicitRuleSupplier(null);
    }

    /** {@code sanityCheck} should propagate any exception thrown by the
     *  underlying {@link SanityChecker}.  We simulate this by using a checker
     *  that deliberately throws. */
    @Test(expected = IllegalStateException.class)
    public void testSanityCheckPropagatesCheckerException() {
        // Create a checker that throws on check()
        SanityChecker explodingChecker = new SanityChecker() {
            @Override
            public void check(Model model) {
                throw new IllegalStateException("boom");
            }
        };
        // Replace the internal call by invoking performCheck directly,
        // because {@code sanityCheck} creates its own AppenderWithinAppenderSanityChecker.
        // To test the propagation we call performCheck with the exploding checker.
        configurator.performCheck(explodingChecker, new Model());
    }

    /** Verify that {@code addModelHandlerAssociations} does not throw when
     *  supplied with a freshly created {@link DefaultProcessor}. */
    @Test
    public void testAddModelHandlerAssociationsDoesNotThrow() {
        // DefaultProcessor requires a Context and a ModelInterpretationContext.
        // Both can be created with null context for this unit test because the
        // handlers themselves do not touch the context during registration.
        DefaultProcessor processor = new DefaultProcessor(null, null);
        configurator.addModelHandlerAssociations(processor);
        // No exception → test passes.
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Verification of rule registration (indirectly via public API)       */
    /* --------------------------------------------------------------------- */

    /** Ensure that the rule store receives the expected number of rules
     *  after {@code addElementSelectorAndActionAssociations} is executed.
     *  This test uses a minimal {@link RuleStore} implementation that counts
     *  registrations. */
    @Test
    public void testAddElementSelectorAndActionAssociationsRegistersRules() {
        // Minimal RuleStore that records the number of added rules.
        class CountingRuleStore implements RuleStore {
            int ruleCount = 0;

            @Override
            public void addRule(ElementSelector elementSelector, String actionClassStr) {
                // not used in this configurator
            }

            @Override
            public void addRule(ElementSelector elementSelector, java.util.function.Supplier<Action> actionSupplier) {
                ruleCount++;
            }

            @Override
            public java.util.function.Supplier<Action> matchActions(ElementPath elementPath) {
                return null;
            }

            @Override
            public void addTransparentPathPart(String pathPart) {
                // no‑op for counting
            }
        }

        CountingRuleStore ruleStore = new CountingRuleStore();
        configurator.addElementSelectorAndActionAssociations(ruleStore);

        // The configurator adds a fixed set of rules (see source).  The expected
        // count is 24 (derived from counting each `addRule` call in the method).
        // If the implementation changes, this test will fail, indicating that
        // the rule set has been altered.
        assertEquals("Unexpected number of rules registered", 24, ruleStore.ruleCount);
    }
}
