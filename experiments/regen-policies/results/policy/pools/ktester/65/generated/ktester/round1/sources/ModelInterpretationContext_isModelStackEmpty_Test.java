package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Stack;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isModelStackEmpty_Test {

    private Context context;

    private ModelInterpretationContext mic;

    /**
     * Simple concrete {@link Model} used in the tests.
     */
    private static class DummyModel implements Model {

        private final String name;

        DummyModel(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "DummyModel{" + name + '}';
        }
    }

    class ThrowingStack extends Stack<Model> {

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("isEmpty not supported");
        }
    }

    class ThrowingStack extends Stack<Model> {

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("isEmpty not supported");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ContextBase is a concrete implementation of the Context interface
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        mic = null;
        context = null;
    }

    @Test
    void isModelStackEmpty_ShouldReturnTrue_WhenStackIsInitiallyEmpty() {
        // Given – mic is freshly created, its modelStack is empty
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertTrue(result, "Model stack should be reported as empty right after construction");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_WhenModelIsPushedOntoStack() {
        // Given
        Model dummyModel = new Model() {
        };
        // push a model to make the stack non‑empty
        mic.pushModel(dummyModel);
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertFalse(result, "Model stack should not be reported as empty after a model has been pushed");
    }

    @Test
    void isModelStackEmpty_ShouldReturnTrue_AfterPoppingOnlyElement() {
        // Given
        Model dummyModel = new Model() {
        };
        mic.pushModel(dummyModel);
        // Ensure stack is non‑empty before pop
        assertFalse(mic.isModelStackEmpty(), "Precondition failed: stack should not be empty after push");
        // When
        // remove the only element
        Model popped = mic.popModel();
        assertNotNull(popped, "Popped model should not be null");
        // Then
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty after popping the only element");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_AfterPushingSingleModel() {
        // Given
        mic.pushModel(new DummyModel("first"));
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertFalse(result, "Model stack should not be empty after a model has been pushed");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_AfterPushingTwoModels() {
        // Given
        mic.pushModel(new DummyModel("first"));
        mic.pushModel(new DummyModel("second"));
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertFalse(result, "Model stack should remain non‑empty after pushing two models");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_AfterPushingTwoAndPoppingOne() {
        // Given
        mic.pushModel(new DummyModel("first"));
        mic.pushModel(new DummyModel("second"));
        // remove the top model
        mic.popModel();
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertFalse(result, "Stack still contains one model after popping one of two, so should be non‑empty");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_WhenObjectStackIsManipulated() {
        // Given: push a model (so modelStack is non‑empty)
        mic.pushModel(new DummyModel("only"));
        // Also push an unrelated object onto the objectStack
        mic.pushObject("some unrelated object");
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertFalse(result, "Manipulating objectStack must not affect modelStack emptiness");
    }

    @Test
    void isModelStackEmpty_ShouldReturnTrue_AfterClearingModelStackViaReflection() throws Exception {
        // Given: push a model so the stack is non‑empty
        mic.pushModel(new DummyModel("temp"));
        assertFalse(mic.isModelStackEmpty(), "Precondition failed: stack should be non‑empty before clear");
        // Use reflection to obtain the private field and clear it
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Stack<Model> stack = (Stack<Model>) modelStackField.get(mic);
        // clear the internal stack
        stack.clear();
        // When
        boolean result = mic.isModelStackEmpty();
        // Then
        assertTrue(result, "After clearing the internal modelStack, isModelStackEmpty() must return true");
    }

    @Test
    void isModelStackEmpty_ShouldThrowNullPointerException_WhenModelStackIsNull() throws Exception {
        // Using reflection to set the internal modelStack field to null
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        modelStackField.set(mic, null);
        // When / Then
        assertThrows(NullPointerException.class, () -> mic.isModelStackEmpty(), "Calling isModelStackEmpty() with a null internal modelStack must throw NPE");
    }

    @Test
    void isModelStackEmpty_ShouldPropagateUnsupportedOperationException_FromCustomStack() throws Exception {
        // Create a custom Stack subclass whose isEmpty() throws UnsupportedOperationException
        class ThrowingStack extends Stack<Model> {

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException("isEmpty not supported");
            }
        }
        // Replace the internal modelStack with the throwing implementation
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        modelStackField.set(mic, new ThrowingStack());
        // When / Then
        assertThrows(UnsupportedOperationException.class, () -> mic.isModelStackEmpty(), "The exception from the custom stack's isEmpty() should propagate");
    }
}
