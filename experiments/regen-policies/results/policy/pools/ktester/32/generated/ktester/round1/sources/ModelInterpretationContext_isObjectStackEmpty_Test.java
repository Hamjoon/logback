package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isObjectStackEmpty_Test {

    private static Context context;

    private ModelInterpretationContext mic;

    @BeforeAll
    static void setupBeforeAll() {
        // A concrete Context implementation required by ModelInterpretationContext
        context = new ContextBase();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to avoid state leakage
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no leftover state interferes with other tests
        mic = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but clear the static reference
        context = null;
    }

    @Test
    void shouldReturnTrueWhenObjectStackIsEmpty() {
        // Given: a newly created ModelInterpretationContext (objectStack is empty)
        // When
        boolean result = mic.isObjectStackEmpty();
        // Then
        assertTrue(result, "Object stack should be reported as empty");
    }

    @Test
    void shouldReturnFalseWhenObjectStackIsNotEmpty() {
        // Given: push an object onto the stack
        mic.pushObject(new Object());
        // When
        boolean result = mic.isObjectStackEmpty();
        // Then
        assertFalse(result, "Object stack should be reported as not empty after a push");
    }

    /**
     * Scenario: the internal {@code objectStack} field is forcibly set to {@code null}
     * (using reflection). The method does not guard against a null stack, so a
     * {@link NullPointerException} is expected.
     */
    @Test
    void shouldThrowNullPointerExceptionWhenObjectStackIsNull() throws Exception {
        // Use reflection to set the private field `objectStack` to null
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // Verify that calling the method now throws NPE
        assertThrows(NullPointerException.class, mic::isObjectStackEmpty, "Calling isObjectStackEmpty() with a null internal stack should throw NullPointerException");
    }
}
