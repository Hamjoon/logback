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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setConfiguratorHint_Test {

    @Mock
    Context // mock of the required Context dependency
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    class CustomHint {

        final String value;

        CustomHint(String v) {
            this.value = v;
        }
    }

    class CustomHint {

        final String value;

        CustomHint(String v) {
            this.value = v;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test.
        // Use the constructor that does NOT set a configurator hint initially.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        mic = null;
    }

    private static Stream<Arguments> simpleHintProvider() {
        return Stream.of(// String
        Arguments.of("simpleString"), // Integer
        Arguments.of(12345), // mutable Date
        Arguments.of(new Date()), // null
        Arguments.of(null));
    }

    private static Stream<Arguments> additionalHintProvider() {
        // Custom user‑defined class
        class CustomHint {

            final String value;

            CustomHint(String v) {
                this.value = v;
            }
        }
        // Lambda (Runnable)
        Runnable runnable = () -> System.out.println("run");
        return Stream.of(// custom class
        Arguments.of(new CustomHint("alpha")), // array
        Arguments.of(new String[] { "a", "b", "c" }), // List implementation
        Arguments.of(Arrays.asList(1, 2, 3)), // lambda / functional interface
        Arguments.of(runnable), // Boolean wrapper
        Arguments.of(Boolean.TRUE));
    }

    @Test
    void setConfiguratorHint_assigns_given_object() {
        // GIVEN
        Object hint = new Object();
        // WHEN
        mic.setConfiguratorHint(hint);
        // THEN
        assertSame(hint, mic.getConfiguratorHint(), "The configurator hint returned by getConfiguratorHint() should be the same object that was set.");
    }

    @Test
    void constructor_with_configuratorHint_initialises_field_correctly() {
        // GIVEN
        Object initialHint = "initialHint";
        // WHEN
        ModelInterpretationContext micWithHint = new ModelInterpretationContext(mockContext, initialHint);
        // THEN
        assertSame(initialHint, micWithHint.getConfiguratorHint(), "The configurator hint supplied to the constructor should be retrievable via getConfiguratorHint().");
    }

    @Test
    void setConfiguratorHint_overwrites_previous_hint() {
        // GIVEN first hint
        Object firstHint = "firstValue";
        mic.setConfiguratorHint(firstHint);
        assertSame(firstHint, mic.getConfiguratorHint(), "Pre‑condition: first hint should be stored.");
        // WHEN second hint is set
        Object secondHint = "secondValue";
        mic.setConfiguratorHint(secondHint);
        // THEN the second hint replaces the first
        assertSame(secondHint, mic.getConfiguratorHint(), "Calling setConfiguratorHint a second time should replace the previous hint.");
    }

    @Test
    void setConfiguratorHint_overwrite_with_same_value_is_idempotent() {
        // GIVEN a hint
        String hint = "repeat";
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
        // WHEN the same hint is set again
        mic.setConfiguratorHint(hint);
        // THEN the stored reference is unchanged (still the same object)
        assertSame(hint, mic.getConfiguratorHint(), "Setting the same hint again must leave the stored reference unchanged.");
    }

    @Test
    void setConfiguratorHint_overwrite_with_null_clears_hint() {
        // GIVEN a non‑null hint
        mic.setConfiguratorHint("temporary");
        assertNotNull(mic.getConfiguratorHint());
        // WHEN we set null
        mic.setConfiguratorHint(null);
        // THEN the hint becomes null
        assertNull(mic.getConfiguratorHint(), "Overwriting a non‑null hint with null should result in a null configurator hint.");
    }

    /* -------------------------------------------------
     *  Additional object types (custom class, array, list, lambda, Boolean, self‑reference)
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index}: setConfiguratorHint({0}) → same reference")
    @MethodSource("additionalHintProvider")
    void setConfiguratorHint_stores_various_object_types(Object hint) {
        // WHEN
        mic.setConfiguratorHint(hint);
        // THEN
        assertSame(hint, mic.getConfiguratorHint(), "The stored configurator hint must be the exact same instance that was supplied.");
    }

    @Test
    void setConfiguratorHint_can_store_self_reference() {
        // GIVEN the ModelInterpretationContext instance itself
        Object self = mic;
        // WHEN
        mic.setConfiguratorHint(self);
        // THEN
        assertSame(self, mic.getConfiguratorHint(), "The context should be able to store a reference to itself without alteration.");
    }

    /* -------------------------------------------------
     *  Interaction test – unrelated operations must not affect the hint
     * ------------------------------------------------- */
    @Test
    void configuratorHint_is_unchanged_by_unrelated_context_operations() {
        // GIVEN a hint
        String hint = "stable";
        mic.setConfiguratorHint(hint);
        assertSame(hint, mic.getConfiguratorHint());
        // Perform some unrelated operations
        mic.pushObject("someObject");
        // push/pop should be a no‑op for the hint
        mic.popObject();
        mic.addDependencyDefinition(mock(DependencyDefinition.class));
        // THEN the hint is still the same instance
        assertSame(hint, mic.getConfiguratorHint(), "Unrelated context modifications must not modify the stored configurator hint.");
    }
}
