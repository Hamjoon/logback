package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Stack;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popModel_Test {

    private ModelInterpretationContext mic;

    private Context context;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation; ContextBase is the standard concrete class
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary
        mic = null;
        context = null;
    }

    @Test
    void popModel_returnsPushedModel_andRemovesItFromStack() {
        // GIVEN
        // a simple, empty model instance
        Model firstModel = new Model();
        Model secondModel = new Model();
        mic.pushModel(firstModel);
        mic.pushModel(secondModel);
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(secondModel, popped, "popModel should return the most recently pushed model");
        // The stack should now contain only the first model
        assertFalse(mic.isModelStackEmpty(), "Model stack should not be empty after one pop");
        assertSame(firstModel, mic.peekModel(), "The remaining model on the stack should be the first one pushed");
    }

    @Test
    void popModel_onEmptyStack_throwsEmptyStackException() {
        // GIVEN
        assertTrue(mic.isModelStackEmpty(), "Precondition: model stack must be empty");
        // WHEN / THEN
        assertThrows(EmptyStackException.class, mic::popModel, "popModel should throw EmptyStackException when the model stack is empty");
    }

    @Test
    void popModel_multiplePops_behaveCorrectly() {
        // GIVEN
        Model a = new Model();
        Model b = new Model();
        Model c = new Model();
        mic.pushModel(a);
        mic.pushModel(b);
        mic.pushModel(c);
        // WHEN & THEN
        assertSame(c, mic.popModel(), "First pop should return the last pushed model (c)");
        assertSame(b, mic.popModel(), "Second pop should return the next model (b)");
        assertSame(a, mic.popModel(), "Third pop should return the first model (a)");
        assertTrue(mic.isModelStackEmpty(), "After popping all models the stack should be empty");
    }

    @Test
    void popModel_nullModel_returnsNullWithoutException() {
        // GIVEN: push a null reference onto the model stack
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty(), "Stack should contain the null element");
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertNull(popped, "popModel should return null when a null model was pushed");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping the null element");
    }

    @Test
    void popModel_stateConsistency_objectStackUnaffected() {
        // GIVEN: populate objectStack with distinct objects
        String obj1 = "first";
        Integer obj2 = 42;
        mic.pushObject(obj1);
        mic.pushObject(obj2);
        Stack<Object> beforeObjectStack = new Stack<>();
        beforeObjectStack.addAll(mic.getObjectStack());
        // Push a model and pop it
        Model model = new Model();
        mic.pushModel(model);
        mic.popModel();
        // THEN: objectStack must be unchanged (same size and same elements in order)
        Stack<Object> afterObjectStack = mic.getObjectStack();
        assertEquals(beforeObjectStack.size(), afterObjectStack.size(), "objectStack size must remain unchanged after popModel");
        assertIterableEquals(beforeObjectStack, afterObjectStack, "Contents of objectStack must remain unchanged after popModel");
    }

    @Test
    void popModel_multiplePops_thenFourthPopThrowsException() {
        // GIVEN: three models on the stack
        Model modelA = new Model();
        Model modelB = new Model();
        Model modelC = new Model();
        mic.pushModel(modelA);
        mic.pushModel(modelB);
        mic.pushModel(modelC);
        // WHEN: pop three times (valid)
        assertSame(modelC, mic.popModel(), "First pop should return modelC");
        assertSame(modelB, mic.popModel(), "Second pop should return modelB");
        assertSame(modelA, mic.popModel(), "Third pop should return modelA");
        // THEN: fourth pop must throw EmptyStackException
        assertThrows(EmptyStackException.class, mic::popModel, "Fourth pop on an empty stack should throw EmptyStackException");
    }

    @Test
    void popModel_singleElement_stackBecomesEmpty() {
        // GIVEN: a single model on the stack
        Model only = new Model();
        mic.pushModel(only);
        assertFalse(mic.isModelStackEmpty(), "Precondition: stack contains one element");
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(only, popped, "popModel should return the only element");
        assertTrue(mic.isModelStackEmpty(), "After popping the sole element the stack must be empty");
        assertTrue(mic.isModelStackEmpty(), "isModelStackEmpty() must report true");
    }

    @Test
    void popModel_internalModelStackIsNull_throwsNullPointerException() {
        // GIVEN: deliberately nullify the internal modelStack (package‑private field)
        // accessible because test class is in the same package
        mic.modelStack = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, mic::popModel, "Calling popModel when internal modelStack is null should throw NullPointerException");
    }
}
