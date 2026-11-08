package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushModel_Test {

    @Mock
    Context // mocked Context – no real behavior required
    mockContext;

    @Mock
    Model // first mock Model instance
    mockModel1;

    @Mock
    Model // second mock Model instance
    mockModel2;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Returns the current size of the private {@code modelStack} field using reflection.
     */
    private int getModelStackSize() {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("modelStack");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Stack<Model> stack = (java.util.Stack<Model>) f.get(mic);
            return stack == null ? 0 : stack.size();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access modelStack via reflection", e);
            // unreachable
            return -1;
        }
    }

    /**
     * Sets the private {@code modelStack} field to {@code null} using reflection.
     * Used to provoke a NullPointerException in the {@code pushModel} method.
     */
    private void nullifyModelStack() {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("modelStack");
            f.setAccessible(true);
            f.set(mic, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to nullify modelStack via reflection", e);
        }
    }

    @Test
    @DisplayName("pushModel adds a model to the model stack")
    void pushModelAddsModelToStack() {
        // GIVEN the model stack is initially empty
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty before push");
        // WHEN a model is pushed
        mic.pushModel(mockModel1);
        // THEN the stack is no longer empty and the model can be retrieved
        assertFalse(mic.isModelStackEmpty(), "Model stack should contain an element after push");
        assertSame(mockModel1, mic.peekModel(), "Top of the stack should be the pushed model");
    }

    @Test
    @DisplayName("pushModel preserves order when multiple models are pushed")
    void pushMultipleModelsPreservesOrder() {
        // GIVEN an empty stack
        assertTrue(mic.isModelStackEmpty());
        // WHEN two models are pushed sequentially
        mic.pushModel(mockModel1);
        mic.pushModel(mockModel2);
        // THEN the stack size is 2 and the most recent model is on top
        assertFalse(mic.isModelStackEmpty());
        assertSame(mockModel2, mic.peekModel(), "The last pushed model should be on top");
        // AND popping returns them in LIFO order
        Model poppedFirst = mic.popModel();
        Model poppedSecond = mic.popModel();
        assertSame(mockModel2, poppedFirst, "First pop should return the last pushed model");
        assertSame(mockModel1, poppedSecond, "Second pop should return the first pushed model");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping all models");
    }

    @Test
    @DisplayName("pushModel does not affect object stack")
    void pushModelDoesNotTouchObjectStack() {
        // GIVEN an empty object stack
        assertTrue(mic.isObjectStackEmpty());
        // WHEN a model is pushed
        mic.pushModel(mockModel1);
        // THEN the object stack remains unchanged
        assertTrue(mic.isObjectStackEmpty(), "Object stack should stay empty after pushing a model");
    }

    @Test
    @DisplayName("pushModel with a null model stores null and marks stack non‑empty")
    void pushNullModel() {
        // GIVEN an empty model stack
        assertTrue(mic.isModelStackEmpty());
        // WHEN a null model is pushed
        mic.pushModel(null);
        // THEN the stack is not empty and peekModel returns null
        assertFalse(mic.isModelStackEmpty(), "Stack should be non‑empty after pushing null");
        assertNull(mic.peekModel(), "peekModel() should return null when null was pushed");
    }

    @Test
    @DisplayName("duplicate model instances are treated as separate stack entries")
    void pushDuplicateModelInstances() {
        Model duplicate = new Model();
        mic.pushModel(duplicate);
        // same instance again
        mic.pushModel(duplicate);
        // The top of the stack should still be the same instance
        assertSame(duplicate, mic.peekModel(), "Top should be the duplicate instance");
        // Two pops should both return the same instance
        Model firstPop = mic.popModel();
        Model secondPop = mic.popModel();
        assertSame(duplicate, firstPop, "First pop returns the duplicate");
        assertSame(duplicate, secondPop, "Second pop returns the duplicate again");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after both pops");
    }

    @Test
    @DisplayName("pushModel works correctly after the stack has been fully emptied")
    void pushAfterFullPop() {
        Model first = new Model();
        Model second = new Model();
        Model third = new Model();
        // Push two models and pop them both
        mic.pushModel(first);
        mic.pushModel(second);
        assertSame(second, mic.popModel());
        assertSame(first, mic.popModel());
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping first two models");
        // Now push a third model
        mic.pushModel(third);
        assertFalse(mic.isModelStackEmpty(), "Stack should not be empty after pushing third model");
        assertSame(third, mic.peekModel(), "peekModel should return the third model");
        assertSame(third, mic.popModel(), "popModel should return the third model");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping the third model");
    }

    @Test
    @DisplayName("stress test: pushing and popping 50 models")
    void pushManyModelsStressTest() {
        final int COUNT = 50;
        List<Model> models = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            Model m = new Model();
            models.add(m);
            mic.pushModel(m);
        }
        // Verify size via reflection
        assertEquals(COUNT, getModelStackSize(), "Model stack size should be 50 after pushes");
        // Top should be the last pushed instance
        assertSame(models.get(COUNT - 1), mic.peekModel(), "Top of stack should be the 50th model");
        // Pop all and ensure LIFO order
        for (int i = COUNT - 1; i >= 0; i--) {
            Model popped = mic.popModel();
            assertSame(models.get(i), popped, "Popped model should follow LIFO order");
        }
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping all models");
    }

    @Test
    @DisplayName("pushModel throws NullPointerException when internal modelStack is null")
    void pushModelWhenModelStackIsNullThrows() {
        // Simulate a corrupted MIC where modelStack has not been initialised
        nullifyModelStack();
        // The method should now throw NPE because it attempts to push onto a null stack
        assertThrows(NullPointerException.class, () -> mic.pushModel(new Model()), "pushModel should throw NullPointerException when modelStack is null");
    }

    @Test
    @DisplayName("object stack remains untouched after various pushModel operations")
    void objectStackUnaffectedAcrossOperations() {
        assertTrue(mic.isObjectStackEmpty(), "Pre‑condition: object stack empty");
        // Perform a series of pushModel calls
        mic.pushModel(new Model());
        mic.pushModel(null);
        mic.pushModel(new Model());
        // Verify object stack still empty
        assertTrue(mic.isObjectStackEmpty(), "Object stack must stay empty after pushModel calls");
    }
}
