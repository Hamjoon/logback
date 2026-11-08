package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Stack;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekModel_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Context – the real implementation is not needed for peekModel()
        mockContext = mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        mic = null;
        mockContext = null;
    }

    /* ----------------------------------------------------------------------
     * Helper method added only for test readability.
     * The original class does not expose the stack size, so we use reflection
     * safely within the test scope to verify that peekModel does not mutate the stack.
     * ---------------------------------------------------------------------- */
    private int getModelStackSize() {
        // Using reflection to access the private field `modelStack` for verification only.
        try {
            java.lang.reflect.Field field = ModelInterpretationContext.class.getDeclaredField("modelStack");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Stack<Model> stack = (java.util.Stack<Model>) field.get(mic);
            return stack.size();
        } catch (Exception e) {
            fail("Unable to access modelStack for verification", e);
            // unreachable
            return -1;
        }
    }

    @Test
    void peekModel_returnsTopModelWhenStackIsNotEmpty() {
        // GIVEN
        Model topModel = mock(Model.class);
        Model secondModel = mock(Model.class);
        // push first
        mic.pushModel(secondModel);
        // push second – this should be on top
        mic.pushModel(topModel);
        // WHEN
        Model result = mic.peekModel();
        // THEN
        assertSame(topModel, result, "peekModel should return the model that is on the top of the stack");
        // Ensure the stack size has not changed
        assertEquals(2, mic.getModelStackSize(), "peekModel must not remove the model from the stack");
    }

    @Test
    void peekModel_throwsEmptyStackExceptionWhenStackIsEmpty() {
        // GIVEN – a fresh ModelInterpretationContext (modelStack is empty)
        // WHEN / THEN
        assertThrows(EmptyStackException.class, mic::peekModel, "peekModel should throw EmptyStackException when the model stack is empty");
    }

    @Test
    void peekModel_returnsNullWhenNullModelPushed() {
        // GIVEN – push a null reference onto the stack
        mic.pushModel(null);
        // WHEN
        Model result = mic.peekModel();
        // THEN
        assertNull(result, "peekModel should return null when the top element of the stack is null");
        assertEquals(1, getModelStackSize(), "The stack size should still be 1 after peeking a null element");
    }

    @Test
    void peekModel_returnsSecondModelAfterTwoPushes() {
        // GIVEN – push two different models
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        mic.pushModel(second);
        // WHEN
        Model result = mic.peekModel();
        // THEN
        assertSame(second, result, "After pushing two models, peekModel must return the second (top) model");
    }

    @Test
    void peekModel_reflectsNewTopAfterPop() {
        // GIVEN – push two models and then pop the top one
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        mic.pushModel(second);
        // removes 'second'
        mic.popModel();
        // WHEN
        Model result = mic.peekModel();
        // THEN
        assertSame(first, result, "After popping the top model, peekModel should return the new top (the first model)");
    }

    @Test
    void peekModel_throwsNullPointerExceptionWhenModelStackIsNull() throws Exception {
        // GIVEN – force the internal `modelStack` field to be null via reflection
        var field = ModelInterpretationContext.class.getDeclaredField("modelStack");
        field.setAccessible(true);
        field.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, mic::peekModel, "If the internal modelStack is null, peekModel should throw NullPointerException");
    }
}
