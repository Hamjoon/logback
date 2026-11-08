package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setConfiguratorHint_Test {

    @Mock
    Context // mock Context – no real behaviour required for these tests
    mockContext;

    private ModelInterpretationContext mic;

    /**
     * Simple POJO describing the expected outcome of a test case.
     */
    private static class ExpectedOutcome {

        enum Type {

            NULL, SAME_REFERENCE, EQUALS
        }

        final Type type;

        // used for SAME_REFERENCE and EQUALS
        final Object reference;

        private ExpectedOutcome(Type type, Object reference) {
            this.type = type;
            this.reference = reference;
        }

        static ExpectedOutcome nullOutcome() {
            return new ExpectedOutcome(Type.NULL, null);
        }

        static ExpectedOutcome sameReference(Object ref) {
            return new ExpectedOutcome(Type.SAME_REFERENCE, ref);
        }

        static ExpectedOutcome equalsTo(Object ref) {
            return new ExpectedOutcome(Type.EQUALS, ref);
        }
    }

    /**
     * A tiny user‑defined class used to verify that arbitrary objects are stored
     * by reference.
     */
    private static class CustomHint {
        // No behaviour needed – the class exists solely for identity testing.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test; start with a null hint
        mic = new ModelInterpretationContext(mockContext);
    }

    private static Stream<Arguments> provideSetConfiguratorHintCases() {
        // Helper objects used in several test cases
        Object newObject = new Object();
        String sampleHint = "SampleHint";
        String firstHint = "FirstHint";
        String secondHint = "SecondHint";
        Integer intValue = 123;
        Boolean boolValue = true;
        CustomHint customHint = new CustomHint();
        List<Integer> listHint = Arrays.asList(1, 2, 3);
        String sameHint = "SameHint";
        return Stream.of(// ----- group: setConfiguratorHint - basic assignments -----
        Arguments.of("Setting the configurator hint to null should result in getConfiguratorHint() returning null.", Collections.singletonList(null), ExpectedOutcome.nullOutcome()), Arguments.of("Setting the configurator hint to a newly created Object should store that exact reference.", Collections.singletonList(newObject), ExpectedOutcome.sameReference(newObject)), Arguments.of("Setting the configurator hint to a String should store the string.", Collections.singletonList(sampleHint), ExpectedOutcome.equalsTo(sampleHint)), Arguments.of("When the hint is set twice, the second value should overwrite the first.", Arrays.asList(firstHint, secondHint), ExpectedOutcome.equalsTo(secondHint)), // ----- group: setConfiguratorHint - diverse types -----
        Arguments.of("Setting the configurator hint to an Integer should store that integer.", Collections.singletonList(intValue), ExpectedOutcome.equalsTo(intValue)), Arguments.of("Setting the configurator hint to a Boolean should store that boolean.", Collections.singletonList(boolValue), ExpectedOutcome.equalsTo(boolValue)), Arguments.of("Setting the configurator hint to an instance of a user‑defined class should store that exact reference.", Collections.singletonList(customHint), ExpectedOutcome.sameReference(customHint)), Arguments.of("Setting the hint twice to the identical string should leave the stored value unchanged.", Arrays.asList(sameHint, sameHint), // same reference because the literal is interned
        ExpectedOutcome.sameReference(sameHint)), Arguments.of("Setting the configurator hint to a collection object should store that exact collection reference.", Collections.singletonList(listHint), ExpectedOutcome.sameReference(listHint)));
    }

    @Test
    void setConfiguratorHint_assigns_given_object() {
        // GIVEN
        Object hint = new Object();
        // WHEN
        mic.setConfiguratorHint(hint);
        // THEN
        assertSame(hint, mic.getConfiguratorHint(), "The configurator hint returned by getConfiguratorHint() should be the same instance that was set.");
    }

    @Test
    void setConfiguratorHint_overwrites_existing_hint() {
        // GIVEN
        Object initialHint = "initial";
        ModelInterpretationContext micWithInitial = new ModelInterpretationContext(mockContext, initialHint);
        Object newHint = "new";
        // WHEN
        micWithInitial.setConfiguratorHint(newHint);
        // THEN
        assertSame(newHint, micWithInitial.getConfiguratorHint(), "The configurator hint should be replaced by the new value passed to setConfiguratorHint.");
    }

    @Test
    void setConfiguratorHint_accepts_null() {
        // GIVEN
        Object nonNullHint = "not-null";
        mic.setConfiguratorHint(nonNullHint);
        assertNotNull(mic.getConfiguratorHint());
        // WHEN
        mic.setConfiguratorHint(null);
        // THEN
        assertNull(mic.getConfiguratorHint(), "Passing null to setConfiguratorHint should result in a null hint being stored.");
    }

    /**
     * Parameterized test that drives all cases described in the JSON test‑case document.
     *
     * <p>Each {@code TestCase} contains:
     * <ul>
     *   <li>a list of values that will be passed to {@code setConfiguratorHint} sequentially,</li>
     *   <li>the expected final value (or reference) stored in the context,</li>
     *   <li>a human‑readable description.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSetConfiguratorHintCases")
    void setConfiguratorHint_variousScenarios(String description, List<Object> inputs, ExpectedOutcome expected) {
        // Apply each input value in order
        Object lastInput = null;
        for (Object input : inputs) {
            mic.setConfiguratorHint(input);
            lastInput = input;
        }
        // Verify the final state according to the expectation
        switch(expected.type) {
            case NULL:
                assertNull(mic.getConfiguratorHint(), "Expected null after applying inputs: " + inputs);
                break;
            case SAME_REFERENCE:
                assertSame(expected.reference, mic.getConfiguratorHint(), "Expected the same reference as the last input.");
                break;
            case EQUALS:
                assertEquals(expected.reference, mic.getConfiguratorHint(), "Expected stored hint to be equal to the expected value.");
                break;
            default:
                fail("Unsupported expectation type: " + expected.type);
        }
    }
}
