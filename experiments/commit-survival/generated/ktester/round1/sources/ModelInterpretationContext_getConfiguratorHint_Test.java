package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getConfiguratorHint_Test {

    @Mock
    Context // mock of the required Context dependency
    mockContext;

    /**
     * Simple custom class used to verify that arbitrary objects can be stored
     * as configurator hints.
     */
    private static final class CustomHint {

        private final String value;

        CustomHint(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomHint))
                return false;
            CustomHint that = (CustomHint) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "CustomHint{" + "value='" + value + '\'' + '}';
        }
    }

    /* -----------------------------------------------------------------
     *  Method source supplying the different hint objects required by the
     *  parameterized test above.
     *  ----------------------------------------------------------------- */
    private static Stream<Arguments> hintProvider() {
        return Stream.of(// Simple String hint
        Arguments.of("simpleStringHint"), // Integer hint
        Arguments.of(Integer.valueOf(42)), // Custom object hint
        Arguments.of(new CustomHint("custom")));
    }

    /**
     * -----------------------------------------------------------------
     *  Test that the default constructor (Context only) yields a null hint.
     *  -----------------------------------------------------------------
     */
    @Test
    void getConfiguratorHint_returnsNull_whenConstructedWithoutHint() {
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        assertNull(mic.getConfiguratorHint(), "Configurator hint should be null when not supplied in the constructor");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that the constructor with an explicit hint stores and returns it.
     *  -----------------------------------------------------------------
     */
    @Test
    void getConfiguratorHint_returnsProvidedHint_whenConstructedWithHint() {
        Object expectedHint = new Object();
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext, expectedHint);
        assertSame(expectedHint, mic.getConfiguratorHint(), "Configurator hint should be the same instance supplied to the constructor");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that the setter overrides the previously stored hint.
     *  -----------------------------------------------------------------
     */
    @Test
    void getConfiguratorHint_reflectsSetterChange() {
        Object initialHint = "initial";
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext, initialHint);
        Object newHint = "newHint";
        mic.setConfiguratorHint(newHint);
        assertSame(newHint, mic.getConfiguratorHint(), "Configurator hint should reflect the value set via setConfiguratorHint()");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that the copy‑constructor copies the configurator hint from the
     *  original instance.
     *  -----------------------------------------------------------------
     */
    @Test
    void copyConstructor_copiesConfiguratorHint() {
        Object hint = Integer.valueOf(42);
        ModelInterpretationContext original = new ModelInterpretationContext(mockContext, hint);
        ModelInterpretationContext copy = new ModelInterpretationContext(original);
        assertSame(hint, copy.getConfiguratorHint(), "Copy constructor should duplicate the configurator hint from the original instance");
    }

    /* -----------------------------------------------------------------
     *  Parameterized tests for the various hint values described in the JSON.
     *  ----------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => hint={0}")
    @MethodSource("hintProvider")
    void getConfiguratorHint_returnsHintAfterSetter(Object hint) {
        // Arrange: create a fresh context without any hint
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        assertNull(mic.getConfiguratorHint(), "Pre‑condition: hint should start as null");
        // Act: set the hint
        mic.setConfiguratorHint(hint);
        // Assert: getter returns the exact same object (or null)
        assertSame(hint, mic.getConfiguratorHint(), "Getter should return the same instance that was set via setConfiguratorHint()");
    }

    /**
     * -----------------------------------------------------------------
     *  After a non‑null hint has been set, calling setConfiguratorHint(null)
     *  must clear the stored value.
     *  -----------------------------------------------------------------
     */
    @Test
    void getConfiguratorHint_returnsNullAfterReset() {
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        mic.setConfiguratorHint("temporary");
        assertNotNull(mic.getConfiguratorHint(), "Pre‑condition: hint should be non‑null after first set");
        mic.setConfiguratorHint(null);
        assertNull(mic.getConfiguratorHint(), "After resetting with null, getConfiguratorHint() should return null");
    }
}
