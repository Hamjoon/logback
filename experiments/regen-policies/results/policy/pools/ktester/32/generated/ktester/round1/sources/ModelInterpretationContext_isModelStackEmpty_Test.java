package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Stack;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isModelStackEmpty_Test {

    @Mock
    private Context // a mock Context is sufficient for the constructor
    mockContext;

    @Mock
    private Model // a dummy Model used for push/pop operations
    mockModel;

    // the object under test
    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required when using @Mock without a runner)
        MockitoAnnotations.openMocks(this);
        // Create a fresh instance for each test to avoid state leakage.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    @DisplayName("isModelStackEmpty() returns true when no model has been pushed")
    void shouldReturnTrueWhenStackIsEmpty() {
        // GIVEN – a freshly constructed ModelInterpretationContext (handled in @BeforeEach)
        // WHEN
        boolean empty = mic.isModelStackEmpty();
        // THEN
        assertTrue(empty, "The model stack should be reported as empty immediately after construction");
    }

    @Test
    @DisplayName("isModelStackEmpty() returns false after a model is pushed")
    void shouldReturnFalseWhenStackContainsModel() {
        // GIVEN
        // push a model onto the internal stack
        mic.pushModel(mockModel);
        // WHEN
        boolean empty = mic.isModelStackEmpty();
        // THEN
        assertFalse(empty, "The model stack should not be reported as empty after a model has been pushed");
    }

    @Test
    @DisplayName("isModelStackEmpty() returns true after pushing and then popping a model")
    void shouldReturnTrueAfterPushAndPop() {
        // GIVEN
        mic.pushModel(mockModel);
        // Ensure the stack is not empty before the pop
        assertFalse(mic.isModelStackEmpty(), "Pre‑condition check: stack must be non‑empty after push");
        // WHEN
        // remove the previously pushed model
        mic.popModel();
        // THEN
        assertTrue(mic.isModelStackEmpty(), "The model stack should be empty again after the model is popped");
    }

    @Test
    @DisplayName("isModelStackEmpty() returns false after pushing null, true after popping it")
    void shouldHandleNullModelPushAndPop() {
        // GIVEN: push a null reference – the API permits this
        mic.pushModel(null);
        // WHEN: check emptiness
        boolean afterPush = mic.isModelStackEmpty();
        // THEN: stack is not empty even though the element is null
        assertFalse(afterPush, "Stack should be non‑empty after pushing a null Model");
        // WHEN: pop the null element
        Model popped = mic.popModel();
        // THEN: popped value is null and the stack is empty again
        assertNull(popped, "Popped element should be the null Model that was pushed");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping the null Model");
    }

    @Test
    @DisplayName("isModelStackEmpty() reflects correct state during a multiple push/pop sequence")
    void shouldTrackStateDuringMultiplePushPop() {
        // GIVEN: three distinct mock models
        // reuse the same mock for simplicity
        Model m1 = mockModel;
        Model m2 = mock(Model.class);
        Model m3 = mock(Model.class);
        // PUSH three models
        mic.pushModel(m1);
        mic.pushModel(m2);
        mic.pushModel(m3);
        assertFalse(mic.isModelStackEmpty(), "After three pushes the stack must be non‑empty");
        // POP one -> two remain
        mic.popModel();
        assertFalse(mic.isModelStackEmpty(), "After popping one element (two remain) the stack must still be non‑empty");
        // POP another -> one remains
        mic.popModel();
        assertFalse(mic.isModelStackEmpty(), "After popping a second element (one remains) the stack must still be non‑empty");
        // POP last -> empty
        mic.popModel();
        assertTrue(mic.isModelStackEmpty(), "After popping the final element the stack must be empty");
    }

    @Test
    @DisplayName("isModelStackEmpty() calls are idempotent and do not modify the stack")
    void shouldBeIdempotentWithoutSideEffects() {
        // GIVEN: a single model on the stack
        mic.pushModel(mockModel);
        assertFalse(mic.isModelStackEmpty(), "Pre‑condition: stack must be non‑empty after push");
        // WHEN: call the method twice
        boolean firstCall = mic.isModelStackEmpty();
        boolean secondCall = mic.isModelStackEmpty();
        // THEN: both calls return false and the stack size stays 1
        assertFalse(firstCall, "First call should report non‑empty stack");
        assertFalse(secondCall, "Second call should also report non‑empty stack");
        // Verify stack size via reflection (since no public size getter exists)
        Stack<Model> internalStack = getModelStackViaReflection();
        assertEquals(1, internalStack.size(), "Stack size must remain unchanged after repeated isModelStackEmpty() calls");
    }

    @Test
    @DisplayName("isModelStackEmpty() throws NullPointerException when internal modelStack is null")
    void shouldThrowNPEWhenInternalStackIsNull() throws Exception {
        // GIVEN: corrupt the internal field using reflection
        Field field = ModelInterpretationContext.class.getDeclaredField("modelStack");
        field.setAccessible(true);
        // deliberately set to null
        field.set(mic, null);
        // WHEN / THEN: invoking the method must throw NPE
        assertThrows(NullPointerException.class, mic::isModelStackEmpty, "Calling isModelStackEmpty() with a null internal stack should throw NullPointerException");
    }

    /**
     * Retrieves the private {@code modelStack} field via reflection.
     *
     * @return the internal {@link Stack} of {@link Model}s
     */
    @SuppressWarnings("unchecked")
    private Stack<Model> getModelStackViaReflection() {
        try {
            Field field = ModelInterpretationContext.class.getDeclaredField("modelStack");
            field.setAccessible(true);
            return (Stack<Model>) field.get(mic);
        } catch (ReflectiveOperationException e) {
            fail("Unable to access modelStack via reflection", e);
            // unreachable
            return null;
        }
    }
}
