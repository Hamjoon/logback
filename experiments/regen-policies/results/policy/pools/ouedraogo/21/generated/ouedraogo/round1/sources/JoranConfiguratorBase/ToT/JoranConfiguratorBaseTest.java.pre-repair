/****************************************************************************************
 * JUnit‑4 test suite for {@link ch.qos.logback.core.joran.JoranConfiguratorBase}.
 *
 * The suite follows the “three expert” scenario described in the prompt.
 * Each expert contributes one focused test case that exercises a distinct
 * public‑ or protected‑method of the class under test.
 *
 * The tests use only the APIs listed in the problem statement – no external
 * Logback classes, no mocking frameworks and no subclassing of library
 * abstract types.  Simple concrete helper classes are defined inside the test
 * source where a library interface must be implemented (e.g. {@link SanityChecker}).
 *
 * Author:  OpenAI ChatGPT
 * Date:    2026‑11‑08
 ****************************************************************************************/

package ch.qos.logback.core.joran;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.joran.sanity.AppenderWithinAppenderSanityChecker;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Concrete (non‑abstract) subclass of {@link JoranConfiguratorBase} used only
 * for testing.  All abstract behaviour is already supplied by the base class,
 * therefore the subclass can be empty.
 *
 * @param <E> dummy generic type – the configurator does not use it
 */
class TestJoranConfigurator<E> extends JoranConfiguratorBase<E> {
    // No extra implementation required – all abstract methods are already
    // implemented in the base class.
}

/**
 * Simple {@link SanityChecker} implementation that records whether its
 * {@code check(..)} method has been invoked.  It also implements {@link ContextAware}
 * so that the {@code performCheck(..)} logic that sets the context can be exercised.
 */
class RecordingSanityChecker implements SanityChecker, ContextAware {

    private boolean checkInvoked = false;
    private Object context;   // we only need a placeholder object

    @Override
    public void check(Model model) {
        checkInvoked = true;
    }

    public boolean wasCheckInvoked() {
        return checkInvoked;
    }

    /* ---------- ContextAware contract (minimal stubs) ---------- */
    @Override public void setContext(ch.qos.logback.core.spi.ContextAware context) { /* not used */ }

    @Override public ch.qos.logback.core.spi.ContextAware getContext() { return null; }

    @Override public void addStatus(ch.qos.logback.core.spi.Status status) { }

    @Override public void addInfo(String msg) { }

    @Override public void addInfo(String msg, Throwable ex) { }

    @Override public void addWarn(String msg) { }

    @Override public void addWarn(String msg, Throwable ex) { }

    @Override public void addError(String msg) { }

    @Override public void addError(String msg, Throwable ex) { }
}

/**
 * Test suite.
 */
public class JoranConfiguratorBaseTest {

    private TestJoranConfigurator<Object> configurator;

    @Before
    public void setUp() {
        configurator = new TestJoranConfigurator<>();
    }

    /**
     * Expert 1 – sanityCheck:
     *
     * Verifies that {@code sanityCheck(Model)} executes without throwing an
     * exception.  The method internally delegates to {@code performCheck(..)} with
     * an {@link AppenderWithinAppenderSanityChecker}.  The checker does not
     * modify the supplied model, therefore the only observable effect is the
     * absence of an exception.
     */
    @Test
    public void expert1_sanityCheckRunsWithoutException() {
        Model top = new Model();                 // empty model is sufficient
        // The call should complete normally
        configurator.sanityCheck(top);
        // No assertions needed – the test will fail if an exception propagates
    }

    /**
     * Expert 2 – performCheck:
     *
     * Uses a custom {@link RecordingSanityChecker} to ensure that
     * {@code performCheck(..)} forwards the model to the checker and that the
     * check method is really executed.
     */
    @Test
    public void expert2_performCheckInvokesChecker() {
        Model model = new Model();
        RecordingSanityChecker checker = new RecordingSanityChecker();

        // invoke the protected method via reflection
        try {
            java.lang.reflect.Method m = JoranConfiguratorBase.class
                    .getDeclaredMethod("performCheck", SanityChecker.class, Model.class);
            m.setAccessible(true);
            m.invoke(configurator, checker, model);
        } catch (Exception e) {
            fail("Reflection invocation of performCheck failed: " + e);
        }

        assertTrue("SanityChecker.check(..) should have been called", checker.wasCheckInvoked());
    }

    /**
     * Expert 3 – buildModelInterpretationContext:
     *
     * Calls the protected {@code buildModelInterpretationContext()} method and
     * verifies that it does not throw.  The method first delegates to the
     * superclass implementation and then creates the internal “appender bags”.
     * Because those bags are private, the test validates the observable contract
     * (no exception) only.
     */
    @Test
    public void expert3_buildModelInterpretationContextDoesNotThrow() {
        // invoke the protected method via reflection
        try {
            java.lang.reflect.Method m = JoranConfiguratorBase.class
                    .getDeclaredMethod("buildModelInterpretationContext");
            m.setAccessible(true);
            m.invoke(configurator);
        } catch (Exception e) {
            fail("buildModelInterpretationContext should not throw, but got: " + e);
        }
        // Success – if we reach here the method behaved as expected.
    }

    /**
     * Additional sanity test – getInterpretationContext():
     *
     * The interpreter is lazily created by {@code doConfigure(..)}.  When no
     * configuration has been performed yet the method should simply return
     * {@code null}.  This behaviour is verified to avoid NPEs in client code.
     */
    @Test
    public void getInterpretationContextReturnsNullBeforeConfiguration() {
        SaxEventInterpretationContext ctx = configurator.getInterpretationContext();
        assertNull("InterpretationContext should be null before any configuration", ctx);
    }
}
