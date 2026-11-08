/*
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The tests focus on the public and protected members that can be exercised
 * without resorting to external Logback infrastructure (e.g. a real Context).
 * A minimal concrete subclass is used to expose the protected methods for
 * verification.
 *
 * NOTE: The Logback library heavily relies on a {@code Context} object.
 * Because the `Context` class is not part of the supplied API surface, the
 * tests deliberately pass `null` where a context would normally be required.
 * This is safe for the methods under test because they either ignore the
 * context (e.g. {@code sanityCheck}) or simply forward the call (e.g.
 * {@code performCheck}) without dereferencing it.
 *
 * The test cases cover:
 *   1. Normal execution of {@code sanityCheck}.
 *   2. Edge cases where {@code null} arguments are supplied to
 *      {@code sanityCheck} and {@code performCheck}.
 *   3. Behaviour of {@code getInterpretationContext()} when the internal
 *      interpreter has not been initialised (expected to throw NPE).
 *
 * All tests are written using only the classes, constructors and methods
 * that appear in the provided source code and dependency signatures.
 */

package ch.qos.logback.core.joran;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;


import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;

/**
 * Concrete subclass that makes the protected methods of
 * {@link JoranConfiguratorBase} visible for testing.
 */
class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

    /** expose {@code sanityCheck} */
    public void invokeSanityCheck(Model model) {
        super.sanityCheck(model);
    }

    /** expose {@code performCheck} */
    public void invokePerformCheck(SanityChecker checker, Model model) {
        super.performCheck(checker, model);
    }

    /** expose {@code addModelHandlerAssociations} */
    public void invokeAddModelHandlerAssociations(DefaultProcessor dp) {
        super.addModelHandlerAssociations(dp);
    }

    /** expose {@code setImplicitRuleSupplier} */
    public void invokeSetImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        super.setImplicitRuleSupplier(interpreter);
    }
}

/**
 * Test class for {@link JoranConfiguratorBase}.
 */
public class JoranConfiguratorBaseTest {

    private TestJoranConfigurator configurator;
    private Model simpleModel;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator();
        // a very simple Model instance – no children, no text
        simpleModel = new Model();
        simpleModel.setTag("test");
        simpleModel.setLineNumber(1);
    }

    /**
     * Verify that {@code sanityCheck} runs without throwing when supplied with
     * a non‑null model. Internally it uses {@link AppenderWithinAppenderSanityChecker},
     * which is a concrete implementation of {@link SanityChecker}.
     */
    @Test
    public void sanityCheck_withValidModel_shouldNotThrow() {
        // No exception is expected
        configurator.invokeSanityCheck(simpleModel);
    }

    /**
     * Edge case: passing {@code null} as the model to {@code sanityCheck}
     * should result in a {@link NullPointerException} because the underlying
     * {@code SanityChecker} implementation will try to dereference the model.
     */
    @Test(expected = NullPointerException.class)
    public void sanityCheck_withNullModel_shouldThrowNPE() {
        configurator.invokeSanityCheck(null);
    }

    /**
     * Edge case: invoking {@code performCheck} with a {@code null}
     * {@link SanityChecker} must raise a {@link NullPointerException}
     * when the method attempts to call {@code check(...)} on the null reference.
     */
    @Test(expected = NullPointerException.class)
    public void performCheck_withNullChecker_shouldThrowNPE() {
        configurator.invokePerformCheck(null, simpleModel);
    }

    /**
     * Normal execution of {@code performCheck} with a concrete
     * {@link AppenderWithinAppenderSanityChecker}. The method should complete
     * without throwing; any internal context handling is safe because we set
     * the configurator's context to {@code null}.
     */
    @Test
    public void performCheck_withValidChecker_shouldNotThrow() {
        SanityChecker checker = new AppenderWithinAppenderSanityChecker();
        configurator.invokePerformCheck(checker, simpleModel);
    }

    /**
     * The {@code getInterpretationContext()} method returns the
     * {@link SaxEventInterpretationContext} held by the internal
     * {@link SaxEventInterpreter}. Because the interpreter is lazily created
     * during a full configuration run (which we do not perform here),
     * the field remains {@code null}. The method therefore throws a
     * {@link NullPointerException}. This test asserts that the NPE is indeed
     * thrown, confirming the current lazy‑initialisation contract.
     */
    @Test(expected = NullPointerException.class)
    public void getInterpretationContext_withoutInitialization_shouldThrowNPE() {
        // Direct call – expected to fail with NPE
        configurator.getInterpretationContext();
    }

    /**
     * Sanity check that the public method {@code buildModelInterpretationContext()}
     * does not throw when called. The method is inherited from
     * {@link GenericXMLConfigurator} and its implementation does not require a
     * real {@code Context}. We only verify that the call completes without
     * propagating an exception.
     */
    @Test
    public void buildModelInterpretationContext_shouldCompleteWithoutException() {
        configurator.buildModelInterpretationContext();
        // No assertions – success is the absence of an exception.
    }
}
