package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekObject_Test {

    @Mock
    Context // the Context required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the stacks to avoid side‑effects (defensive, not strictly required)
        mic.getObjectStack().clear();
    }

    @Test
    void peekObject_returnsTopObjectWhenStackIsNotEmpty() {
        // given
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        // this should be on the top of the stack
        mic.pushObject(second);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(second, peeked, "peekObject should return the object most recently pushed");
        // also verify the stack size is unchanged
        assertEquals(2, mic.getObjectStack().size(), "peekObject must not remove the element");
    }

    @Test
    void peekObject_throwsEmptyStackExceptionWhenStackIsEmpty() {
        // given: a freshly created MIC has an empty objectStack
        // when / then
        assertThrows(EmptyStackException.class, mic::peekObject, "peekObject should throw EmptyStackException when the stack is empty");
    }

    @Test
    void peekObject_multipleElements_returnsTopWithoutRemoval() {
        // given
        String firstObject = "firstObject";
        String secondObject = "secondObject";
        mic.pushObject(firstObject);
        mic.pushObject(secondObject);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(secondObject, peeked, "The top element should be returned");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must remain 2 after peekObject");
    }

    @Test
    void peekObject_afterPop_returnsNextElement() {
        // given
        String firstObject = "firstObject";
        String secondObject = "secondObject";
        mic.pushObject(firstObject);
        mic.pushObject(secondObject);
        // remove the top element
        Object popped = mic.popObject();
        assertSame(secondObject, popped, "Pre‑condition: popObject should return the second object");
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(firstObject, peeked, "After popping, peekObject should return the remaining element");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be 1 after pop and peek");
    }

    @Test
    void peekObject_withNullElement_returnsNullWithoutRemoving() {
        // given
        String nonNull = "nonNull";
        mic.pushObject(nonNull);
        // push a null reference
        mic.pushObject(null);
        // when
        Object peeked = mic.peekObject();
        // then
        assertNull(peeked, "peekObject should return null when the top element is null");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must stay unchanged even when top element is null");
    }

    @Test
    void peekObject_throwsNullPointerExceptionWhenInternalStackIsNull() throws Exception {
        // given: forcibly set the private objectStack field to null via reflection
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // when / then
        assertThrows(NullPointerException.class, mic::peekObject, "peekObject should throw NullPointerException when internal objectStack is null");
    }
}
