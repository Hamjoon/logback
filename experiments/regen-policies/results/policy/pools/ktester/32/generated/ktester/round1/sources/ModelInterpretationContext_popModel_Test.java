package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popModel_Test {

    @Mock
    Context // mocked Context required by the constructor
    mockContext;

    @Mock
    Model // a dummy Model to push onto the stack
    mockModel;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    @DisplayName("popModel() returns the model that was pushed and empties the stack")
    void testPopModelReturnsPushedModel() {
        // given: a model is pushed onto the MIC's model stack
        mic.pushModel(mockModel);
        assertFalse(mic.isModelStackEmpty(), "pre‑condition: model stack should not be empty");
        // when: popModel() is invoked
        Model popped = mic.popModel();
        // then: the returned model is the same instance that was pushed
        assertSame(mockModel, popped, "popModel should return the model that was on top of the stack");
        // and: the model stack is now empty
        assertTrue(mic.isModelStackEmpty(), "model stack should be empty after popping the only element");
    }

    @Test
    @DisplayName("popModel() throws EmptyStackException when the model stack is empty")
    void testPopModelWhenEmptyThrowsException() {
        // given: the model stack is empty (default state)
        assertTrue(mic.isModelStackEmpty(), "pre‑condition: model stack should be empty");
        // when / then: invoking popModel() results in EmptyStackException
        assertThrows(EmptyStackException.class, mic::popModel, "popModel should throw EmptyStackException when the stack is empty");
    }

    /* -------------------------------------------------------------
     *  Normal behaviour – multiple elements (LIFO)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("popModel() returns Model instances in LIFO order for multiple pushes")
    void testPopModelLifoOrderMultiplePushes() {
        // given: three distinct models are pushed in order A, B, C
        Model modelA = mock(Model.class, "modelA");
        Model modelB = mock(Model.class, "modelB");
        Model modelC = mock(Model.class, "modelC");
        mic.pushModel(modelA);
        mic.pushModel(modelB);
        mic.pushModel(modelC);
        assertFalse(mic.isModelStackEmpty());
        // when & then: pop three times and verify LIFO order
        assertSame(modelC, mic.popModel(), "first pop should return the last pushed model (C)");
        assertSame(modelB, mic.popModel(), "second pop should return model B");
        assertSame(modelA, mic.popModel(), "third pop should return model A");
        assertTrue(mic.isModelStackEmpty(), "stack should be empty after popping all elements");
    }

    @Test
    @DisplayName("popModel() throws EmptyStackException after a previous pop emptied the stack")
    void testPopModelAfterPreviousPopEmptiesStack() {
        // given: push a model and pop it, leaving the stack empty
        mic.pushModel(mockModel);
        mic.popModel();
        assertTrue(mic.isModelStackEmpty());
        // when / then: a second pop should fail
        assertThrows(EmptyStackException.class, mic::popModel, "popModel should throw EmptyStackException when called on an already empty stack");
    }

    /* -------------------------------------------------------------
     *  Null handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("popModel() returns null when a null reference was pushed")
    void testPopModelReturnsNullWhenPushedNull() {
        // given: a null model is pushed onto the stack
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty());
        // when: popModel() is called
        Model result = mic.popModel();
        // then: the result is null and the stack becomes empty
        assertNull(result, "popModel should return null when null was pushed");
        assertTrue(mic.isModelStackEmpty(), "stack should be empty after popping the null element");
    }

    /* -------------------------------------------------------------
     *  Interaction with peekModel()
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("peekModel returns the same Model instance without removing it")
    void testPeekModelDoesNotRemoveElement() {
        // given: a single model is on the stack
        mic.pushModel(mockModel);
        assertFalse(mic.isModelStackEmpty());
        // when: peekModel() is called
        Model peeked = mic.peekModel();
        // then: peeked model is the same and stack size unchanged
        assertSame(mockModel, peeked, "peekModel should return the top model without removing it");
        assertFalse(mic.isModelStackEmpty(), "stack should remain non‑empty after peekModel");
        // and when: popModel() is subsequently called
        Model popped = mic.popModel();
        // then: popModel returns the same instance and empties the stack
        assertSame(mockModel, popped, "popModel should return the model that was peeked earlier");
        assertTrue(mic.isModelStackEmpty(), "stack should be empty after popping the only element");
    }

    @Test
    @DisplayName("popModel returns the Model that was peeked earlier in a multi‑push scenario")
    void testPeekThenPopWithMultipleModels() {
        // given: two models are pushed (model1 then model2)
        Model model1 = mock(Model.class, "model1");
        Model model2 = mock(Model.class, "model2");
        mic.pushModel(model1);
        mic.pushModel(model2);
        // when: peekModel() is called, it should return model2
        Model peeked = mic.peekModel();
        assertSame(model2, peeked, "peekModel should return the most recently pushed model (model2)");
        // when: popModel() is called, it should also return model2
        Model popped = mic.popModel();
        assertSame(model2, popped, "popModel should return the same model that was peeked");
        // then: the remaining model on the stack is model1
        assertSame(model1, mic.peekModel(), "after popping model2, peekModel should return model1");
    }

    /* -------------------------------------------------------------
     *  Stack state verification after pop
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isModelStackEmpty() returns true after popModel removes the last element")
    void testIsModelStackEmptyAfterPoppingLastElement() {
        mic.pushModel(mockModel);
        assertFalse(mic.isModelStackEmpty());
        mic.popModel();
        assertTrue(mic.isModelStackEmpty(), "isModelStackEmpty() should return true after the final element is popped");
    }

    @Test
    @DisplayName("isModelStackEmpty() remains false after popModel when more than one Model remains")
    void testIsModelStackEmptyWhenElementsRemain() {
        // given: three models on the stack
        mic.pushModel(mock(Model.class));
        mic.pushModel(mock(Model.class));
        mic.pushModel(mock(Model.class));
        assertFalse(mic.isModelStackEmpty());
        // when: one pop is performed
        mic.popModel();
        // then: stack is not empty
        assertFalse(mic.isModelStackEmpty(), "isModelStackEmpty() should be false while there are still models on the stack");
    }

    /* -------------------------------------------------------------
     *  Independence from objectStack
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("popModel returns the correct Model regardless of objects pushed onto objectStack")
    void testPopModelIndependentFromObjectStack() {
        // given: a model on the modelStack
        mic.pushModel(mockModel);
        // and: several arbitrary objects on the objectStack
        mic.pushObject("stringObject");
        mic.pushObject(42);
        mic.pushObject(new Object());
        // when: popModel() is invoked
        Model result = mic.popModel();
        // then: the correct model is returned and the objectStack is untouched by popModel
        assertSame(mockModel, result, "popModel should return the model irrespective of objectStack contents");
        // verify that objectStack still contains the three objects (size check)
        assertEquals(3, mic.getObjectStack().size(), "objectStack size should remain unchanged after popModel");
    }
}
