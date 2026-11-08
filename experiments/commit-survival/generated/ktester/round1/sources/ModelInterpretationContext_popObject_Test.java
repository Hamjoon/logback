package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popObject_Test {

    @Mock
    Context // Mockito will provide a mock Context instance
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The real constructor of ModelInterpretationContext expects a Context.
        // The mock supplied by Mockito is sufficient for the purpose of these tests.
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void popObject_returnsPushedObject_andStackBecomesEmpty() {
        // given
        String expected = "testObject";
        mic.pushObject(expected);
        assertFalse(mic.isObjectStackEmpty(), "Stack should contain the pushed object before pop");
        // when
        Object actual = mic.popObject();
        // then
        assertEquals(expected, actual, "popObject should return the object that was pushed");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after the object is popped");
    }

    @Test
    void popObject_returnsLastPushedObject_preservesEarlierObjects() {
        // given
        String first = "first";
        String second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        Stack<Object> stackBeforePop = mic.getObjectStack();
        assertEquals(2, stackBeforePop.size(), "Stack should contain two objects before pop");
        // when
        Object popped = mic.popObject();
        // then
        assertEquals(second, popped, "popObject should return the most recently pushed object");
        Stack<Object> stackAfterPop = mic.getObjectStack();
        assertEquals(1, stackAfterPop.size(), "Stack should contain one object after one pop");
        assertEquals(first, stackAfterPop.peek(), "The remaining object should be the one that was pushed first");
    }

    @Test
    void popObject_onEmptyStack_throwsEmptyStackException() {
        // given: an empty stack (default state)
        // when / then
        assertThrows(EmptyStackException.class, () -> mic.popObject(), "popObject should throw EmptyStackException when the stack is empty");
    }
}
