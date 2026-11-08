package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isObjectStackEmpty_Test {

    /**
     * The object under test.
     */
    private ModelInterpretationContext mic;

    /**
     * A mocked Logback context – the real implementation is not needed for this test.
     */
    @Mock
    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor we want to test creates its own internal stacks, so a simple mock
        // context is sufficient.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference to help the garbage collector and avoid accidental reuse.
        mic = null;
    }

    @Test
    void whenObjectStackIsEmpty_thenIsObjectStackEmptyReturnsTrue() {
        // Given: a freshly constructed ModelInterpretationContext (objectStack is empty)
        // When:
        boolean result = mic.isObjectStackEmpty();
        // Then:
        assertTrue(result, "Object stack should be reported as empty for a new context");
    }

    @Test
    void whenObjectStackHasElements_thenIsObjectStackEmptyReturnsFalse() {
        // Given: push a dummy object onto the stack
        Object dummy = new Object();
        mic.pushObject(dummy);
        // When:
        boolean result = mic.isObjectStackEmpty();
        // Then:
        assertFalse(result, "Object stack should not be reported as empty after a push");
    }

    @Test
    void afterPoppingAllElements_stackBecomesEmptyAgain() {
        // Given: push two objects
        mic.pushObject("first");
        mic.pushObject("second");
        // When: pop both objects
        mic.popObject();
        mic.popObject();
        // Then: the stack should be empty
        assertTrue(mic.isObjectStackEmpty(), "Object stack should be empty after all elements have been popped");
    }

    @Test
    void whenObjectStackHasOneElement_thenIsObjectStackEmptyReturnsFalse() {
        // Given: push a dummy object onto the stack
        Object dummy = new Object();
        mic.pushObject(dummy);
        // When:
        boolean result = mic.isObjectStackEmpty();
        // Then:
        assertFalse(result, "Object stack should not be reported as empty after a single push");
    }

    @Test
    void whenObjectStackHasMultipleElements_thenIsObjectStackEmptyReturnsFalse() {
        // Given: push three distinct objects onto the stack
        mic.pushObject("first");
        mic.pushObject(42);
        mic.pushObject(new Object());
        // When:
        boolean result = mic.isObjectStackEmpty();
        // Then:
        assertFalse(result, "Object stack should remain non‑empty after pushing several objects");
    }
}
