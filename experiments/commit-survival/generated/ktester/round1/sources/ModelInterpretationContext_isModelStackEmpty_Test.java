package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isModelStackEmpty_Test {

    @Mock
    Context // mock of the required Context dependency
    mockContext;

    @Mock
    Model // a dummy Model used for push/pop tests
    mockModel;

    // the instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh ModelInterpretationContext before each test.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void isModelStackEmpty_ShouldReturnTrue_WhenStackIsInitiallyEmpty() {
        // GIVEN – a newly created ModelInterpretationContext (modelStack is empty)
        // WHEN
        boolean result = mic.isModelStackEmpty();
        // THEN
        assertTrue(result, "Model stack should be reported as empty immediately after construction");
    }

    @Test
    void isModelStackEmpty_ShouldReturnFalse_WhenModelIsPushed() {
        // GIVEN
        // push a model onto the internal stack
        mic.pushModel(mockModel);
        // WHEN
        boolean result = mic.isModelStackEmpty();
        // THEN
        assertFalse(result, "Model stack should not be empty after a model has been pushed");
    }

    @Test
    void isModelStackEmpty_ShouldReturnTrue_AfterPushedModelIsPopped() {
        // GIVEN
        mic.pushModel(mockModel);
        // remove the model, returning the stack to its original state
        mic.popModel();
        // WHEN
        boolean result = mic.isModelStackEmpty();
        // THEN
        assertTrue(result, "Model stack should be empty again after the pushed model is popped");
    }

    @Test
    void isModelStackEmpty_ShouldThrowNPE_WhenInternalStackIsNull() throws Exception {
        // GIVEN – forcibly set the private field 'modelStack' to null via reflection
        Field modelStackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        modelStackField.setAccessible(true);
        modelStackField.set(mic, null);
        // WHEN / THEN – calling the method should result in a NullPointerException
        assertThrows(NullPointerException.class, () -> mic.isModelStackEmpty(), "Calling isModelStackEmpty() with a null internal stack should throw NullPointerException");
    }
}
