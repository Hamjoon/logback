package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushModel_Test {

    /**
     * Mocked {@link Context} required by the {@link ModelInterpretationContext} constructor.
     */
    @Mock
    Context mockContext;

    /**
     * The instance under test.
     */
    ModelInterpretationContext mic;

    /**
     * A simple mock {@link Model} used in the tests.
     */
    @Mock
    Model mockModel;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case.
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * The production class does not expose the size of {@code modelStack},
     * but for testing we can infer it via a safe public operation:
     * repeatedly pop until empty, counting the pops, then restore the state.
     *
     * <p>This helper is deliberately package‑private so that it does not become
     * part of the public API of the test suite.</p>
     *
     * @return the current number of models on the stack
     */
    private int getModelStackSize() {
        // Guard against a null stack (should never happen with a proper MIC)
        if (mic.isModelStackEmpty()) {
            return 0;
        }
        int count = 0;
        // Temporarily pop all elements to count them
        java.util.ArrayDeque<Model> temp = new java.util.ArrayDeque<>();
        while (!mic.isModelStackEmpty()) {
            Model m = mic.popModel();
            temp.push(m);
            count++;
        }
        // Restore the original order
        while (!temp.isEmpty()) {
            mic.pushModel(temp.pop());
        }
        return count;
    }

    @Test
    @DisplayName("pushModel adds a model to an empty stack")
    void pushModelAddsToEmptyStack() {
        // pre‑condition: the model stack must be empty
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty before push");
        // when
        mic.pushModel(mockModel);
        // then
        assertFalse(mic.isModelStackEmpty(), "Model stack should contain an element after push");
        assertSame(mockModel, mic.peekModel(), "The model on top of the stack must be the one that was pushed");
        assertEquals(1, mic.getModelStackSize(), "Stack size should be 1 after a single push");
    }

    @Test
    @DisplayName("pushModel preserves LIFO order for multiple pushes")
    void pushModelPreservesLifoOrder() {
        // given two distinct mock models
        // first mock injected by Mockito
        Model first = mockModel;
        Model second = org.mockito.Mockito.mock(Model.class);
        // when
        mic.pushModel(first);
        mic.pushModel(second);
        // then
        // The most recently pushed model should be on top
        assertSame(second, mic.peekModel(), "Top of stack must be the last pushed model");
        // Popping should return models in reverse order of insertion
        Model poppedFirst = mic.popModel();
        assertSame(second, poppedFirst, "First pop should return the last pushed model");
        Model poppedSecond = mic.popModel();
        assertSame(first, poppedSecond, "Second pop should return the first pushed model");
        // After popping both, the stack must be empty again
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty after popping all elements");
    }

    @Test
    @DisplayName("pushModel does not affect object stack")
    void pushModelDoesNotTouchObjectStack() {
        // The object stack should stay untouched by pushModel
        assertTrue(mic.isObjectStackEmpty(), "Object stack should be empty initially");
        mic.pushModel(mockModel);
        // object stack must still be empty
        assertTrue(mic.isObjectStackEmpty(), "Object stack must remain empty after pushModel");
    }

    @Test
    @DisplayName("Push a single non‑null Model onto an empty stack")
    void pushModelSingleNonNull() {
        // pre‑condition: stack must be empty
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty before push");
        // when
        mic.pushModel(mockModel);
        // then
        assertFalse(mic.isModelStackEmpty(), "Model stack should contain an element after push");
        assertSame(mockModel, mic.peekModel(), "peekModel() must return the pushed model");
        assertEquals(1, getModelStackSize(), "Stack size should be 1 after a single push");
    }

    @Test
    @DisplayName("Push a null Model – stack accepts null")
    void pushModelNullModel() {
        assertTrue(mic.isModelStackEmpty(), "Model stack should start empty");
        // push null – Stack permits null entries
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty(), "Stack must not be empty after pushing null");
        assertNull(mic.peekModel(), "peekModel() should return null when null was pushed");
        assertEquals(1, getModelStackSize(), "Stack size should be 1 even when null is stored");
    }

    @Test
    @DisplayName("Push two models and verify LIFO order")
    void pushModelTwoModelsLifo() {
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        mic.pushModel(second);
        // most recent on top
        assertSame(second, mic.peekModel(), "Top of stack must be the last pushed model");
        // pop order must be reverse of push order
        Model poppedFirst = mic.popModel();
        assertSame(second, poppedFirst, "First pop returns the last pushed model");
        Model poppedSecond = mic.popModel();
        assertSame(first, poppedSecond, "Second pop returns the first pushed model");
        // stack empty again
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty after popping all elements");
    }

    @Test
    @DisplayName("Push a model and immediately pop it")
    void pushThenPopModel() {
        Model only = mock(Model.class);
        mic.pushModel(only);
        Model popped = mic.popModel();
        assertSame(only, popped, "popModel() must return the same instance that was pushed");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after pop");
    }

    @Test
    @DisplayName("pushModel does not affect object stack (model first, object second)")
    void pushModelDoesNotTouchObjectStack_ModelFirst() {
        // push a model
        Model model = mock(Model.class);
        mic.pushModel(model);
        // push an object onto the separate object stack
        String obj = "someString";
        mic.pushObject(obj);
        // verify both stacks are independent
        assertSame(model, mic.peekModel(), "peekModel() should return the model");
        assertSame(obj, mic.peekObject(), "peekObject() should return the object");
        assertFalse(mic.isModelStackEmpty(), "Model stack must not be empty");
        assertFalse(mic.isObjectStackEmpty(), "Object stack must not be empty");
    }

    @Test
    @DisplayName("pushModel does not affect object stack (object first, model second)")
    void pushModelDoesNotTouchObjectStack_ObjectFirst() {
        // push an object first
        String obj = "firstObject";
        mic.pushObject(obj);
        // then push a model
        Model model = mock(Model.class);
        mic.pushModel(model);
        // both stacks retain their elements
        assertSame(obj, mic.peekObject(), "peekObject() must still return the previously pushed object");
        assertSame(model, mic.peekModel(), "peekModel() must return the newly pushed model");
        assertFalse(mic.isObjectStackEmpty(), "Object stack must contain the object");
        assertFalse(mic.isModelStackEmpty(), "Model stack must contain the model");
    }

    @Test
    @DisplayName("pushModel throws NullPointerException when internal modelStack is null")
    void pushModelWhenModelStackIsNull() throws Exception {
        // Use reflection to set the private field modelStack to null
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        modelStackField.set(mic, null);
        // Expect NPE because mic.pushModel will try to invoke push on a null Stack
        assertThrows(NullPointerException.class, () -> mic.pushModel(mock(Model.class)), "pushModel should throw NullPointerException when modelStack is null");
    }
}
