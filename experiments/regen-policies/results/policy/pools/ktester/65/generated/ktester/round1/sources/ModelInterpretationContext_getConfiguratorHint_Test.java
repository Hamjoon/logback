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
class ModelInterpretationContext_getConfiguratorHint_Test {

    @Mock
    Context // mock of the required Context dependency
    mockContext;

    // instance under test
    private ModelInterpretationContext mic;

    /**
     * Simple POJO used to verify that custom objects are stored and returned
     * without alteration.
     */
    static class TestHint {

        private final String value;

        TestHint(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestHint))
                return false;
            TestHint testHint = (TestHint) o;
            return Objects.equals(value, testHint.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Container for a single parameterized test scenario.
     */
    private static class HintScenario {

        final String description;

        final Object hint;

        final Object expected;

        HintScenario(String description, Object hint, Object expected) {
            this.description = description;
            this.hint = hint;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default create a MIC with a null configurator hint
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Supplies a stream of hint objects together with a description.
     * The expected value is the same object (or, for arrays, an equal array).
     */
    private static Stream<HintScenario> hintScenarios() {
        // Immutable String
        String immutableString = "myHint";
        // Mutable StringBuilder (modifications after setting should be visible)
        StringBuilder mutableBuilder = new StringBuilder("initial");
        // final content "initialModified"
        mutableBuilder.append("Modified");
        // Explicit null (handled separately, not part of this stream)
        // Integer (immutable wrapper)
        Integer integerHint = 42;
        // Custom user‑defined object
        TestHint customHint = new TestHint("payload");
        // Primitive array (int[])
        int[] intArray = new int[] { 1, 2, 3 };
        // Mutable List
        List<String> mutableList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        return Stream.of(new HintScenario("Immutable String", immutableString, immutableString), new HintScenario("Mutable StringBuilder", mutableBuilder, mutableBuilder), new HintScenario("Integer", integerHint, integerHint), new HintScenario("Custom object", customHint, customHint), new HintScenario("int[] array", intArray, intArray), new HintScenario("Mutable List", mutableList, mutableList));
    }

    @Test
    void getConfiguratorHint_returnsNullWhenConstructedWithoutHint() {
        // Given a ModelInterpretationContext created via the (Context) ctor
        // When we query the configurator hint
        Object hint = mic.getConfiguratorHint();
        // Then it should be null
        assertNull(hint, "Configurator hint should be null when not supplied in the constructor");
    }

    @Test
    void getConfiguratorHint_returnsHintProvidedInConstructor() {
        // Given a specific hint object
        String expectedHint = "myHint";
        // When we create a MIC with that hint
        ModelInterpretationContext micWithHint = new ModelInterpretationContext(mockContext, expectedHint);
        // Then getConfiguratorHint should return the same object
        assertSame(expectedHint, micWithHint.getConfiguratorHint(), "Configurator hint should be the same instance supplied to the constructor");
    }

    @Test
    void getConfiguratorHint_reflectsValueSetViaSetter() {
        // Given a MIC initially without a hint
        assertNull(mic.getConfiguratorHint());
        // When we set a new hint
        Integer newHint = 42;
        mic.setConfiguratorHint(newHint);
        // Then getConfiguratorHint should return the newly set value
        assertSame(newHint, mic.getConfiguratorHint(), "Configurator hint should reflect the value set by setConfiguratorHint()");
    }

    @Test
    void copyConstructor_copiesConfiguratorHint() {
        // Given an original MIC with a non‑null hint
        String originalHint = "originalHint";
        ModelInterpretationContext original = new ModelInterpretationContext(mockContext, originalHint);
        // When we create a copy via the copy‑constructor
        ModelInterpretationContext copy = new ModelInterpretationContext(original);
        // Then the configurator hint of the copy should be equal to the original's hint
        assertSame(originalHint, copy.getConfiguratorHint(), "Copy constructor should duplicate the configurator hint from the source instance");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hintScenarios")
    void getConfiguratorHint_returnsHintSetViaSetter(HintScenario scenario) {
        // Given a fresh MIC (created in @BeforeEach)
        assertNull(mic.getConfiguratorHint(), "Pre‑condition: hint should be null before setting");
        // When we set the hint using the setter
        mic.setConfiguratorHint(scenario.hint);
        // Then getConfiguratorHint should return the same reference (or equal array)
        Object actual = mic.getConfiguratorHint();
        if (scenario.hint instanceof int[]) {
            // For primitive arrays we compare content with Arrays.equals
            assertTrue(Arrays.equals((int[]) scenario.expected, (int[]) actual), "Returned int[] should have the same content as the one set");
        } else {
            assertSame(scenario.expected, actual, "Returned hint should be the exact same instance that was set");
        }
    }

    @Test
    void setConfiguratorHint_withExplicitNull_returnsNull() {
        // When we explicitly set a null hint
        mic.setConfiguratorHint(null);
        // Then the getter must return null
        assertNull(mic.getConfiguratorHint(), "Explicitly setting a null hint should result in a null return value");
    }

    @Test
    void mutableStringBuilder_modificationsAreReflected() {
        // Given a mutable StringBuilder
        StringBuilder sb = new StringBuilder("base");
        mic.setConfiguratorHint(sb);
        // When we modify the builder after setting it
        sb.append("-changed");
        // Then getConfiguratorHint should reflect the modification
        Object result = mic.getConfiguratorHint();
        assertSame(sb, result, "Getter should return the same StringBuilder instance");
        assertEquals("base-changed", result.toString(), "Modifications to the mutable hint must be visible through the getter");
    }

    @Test
    void mutableList_modificationsAreReflected() {
        // Given a mutable List
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        mic.setConfiguratorHint(list);
        // When we add an element after setting the hint
        list.add("d");
        // Then the getter should see the added element
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) mic.getConfiguratorHint();
        assertSame(list, result, "Getter should return the same List instance");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result, "List modifications after setting must be reflected");
    }

    @Test
    void repeatedCalls_returnStableReference() {
        // Given a stable hint
        String stableHint = "stableHint";
        mic.setConfiguratorHint(stableHint);
        // When we call the getter multiple times
        Object first = mic.getConfiguratorHint();
        Object second = mic.getConfiguratorHint();
        Object third = mic.getConfiguratorHint();
        // Then all calls must return the same instance
        assertSame(stableHint, first);
        assertSame(first, second);
        assertSame(second, third);
    }

    @Test
    void configuratorHint_isolatedFromObjectStackChanges() {
        // Given a hint and an arbitrary object pushed onto the object stack
        String isolatedHint = "isolatedHint";
        mic.setConfiguratorHint(isolatedHint);
        // manipulate stack
        mic.pushObject(new Object());
        // When we retrieve the hint
        Object result = mic.getConfiguratorHint();
        // Then the hint must be unchanged
        assertSame(isolatedHint, result, "Object stack operations must not affect the configurator hint");
    }
}
