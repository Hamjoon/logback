package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObject_Test {

    @Mock
    Context // mocked dependency required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void getObject_returnsPreviouslyPushedObject() {
        // given
        String first = "firstObject";
        Integer second = 42;
        mic.pushObject(first);
        mic.pushObject(second);
        // when
        Object result0 = mic.getObject(0);
        Object result1 = mic.getObject(1);
        // then
        assertEquals(first, result0, "Object at index 0 should be the first pushed value");
        assertEquals(second, result1, "Object at index 1 should be the second pushed value");
    }

    @Test
    void getObject_withInvalidIndex_throwsIndexOutOfBoundsException() {
        // given – an empty stack (no push performed)
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(0), "Calling getObject on an empty stack should throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_afterPushAndPop_behavesAccordingToStackState() {
        // given
        String a = "A";
        String b = "B";
        mic.pushObject(a);
        mic.pushObject(b);
        // pop the top element (b)
        Object popped = mic.popObject();
        assertEquals(b, popped, "Popped element should be the last pushed value");
        // when
        Object remaining = mic.getObject(0);
        // then
        assertEquals(a, remaining, "After popping, the remaining element should be the first pushed value");
    }

    @Test
    void getObject_returnsFirstPushedObject() {
        // given
        String first = "first";
        mic.pushObject(first);
        // when
        Object result = mic.getObject(0);
        // then
        assertEquals(first, result, "getObject(0) should return the first object that was pushed");
    }

    @ParameterizedTest(name = "[{index}] getObject({0}) returns expected value")
    @CsvSource({ // index, expected value (as string) – the third object is an Integer
    "2,42" })
    void getObject_returnsObjectAtGivenIndex(int index, String expectedAsString) {
        // given – push three objects in order
        mic.pushObject("a");
        mic.pushObject("b");
        // third object is an Integer
        mic.pushObject(42);
        // when
        Object result = mic.getObject(index);
        // then
        // Convert expected string to the appropriate type for comparison
        Object expected = index == 2 ? Integer.valueOf(expectedAsString) : expectedAsString;
        assertEquals(expected, result, () -> "getObject(" + index + ") should return the object pushed at that position");
    }

    @ParameterizedTest(name = "[{index}] getObject({0}) throws IndexOutOfBoundsException")
    @CsvSource({ // negative index on non‑empty stack
    "-1", // index larger than stack size
    "5" })
    void getObject_invalidIndex_throwsException(int invalidIndex) {
        // given – three objects on the stack
        mic.pushObject("x");
        mic.pushObject("y");
        mic.pushObject("z");
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(invalidIndex), () -> "Calling getObject(" + invalidIndex + ") should throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_negativeIndexOnEmptyStack_throwsException() {
        // given – empty stack, no pushes
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(-1), "Calling getObject(-1) on an empty stack must throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_indexOnEmptyStack_throwsException() {
        // given – empty stack, no pushes
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(0), "Calling getObject(0) on an empty stack must throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_afterPop_returnsRemainingElement() {
        // given – push two objects and pop the top one
        mic.pushObject("first");
        mic.pushObject("second");
        // should be "second"
        Object popped = mic.popObject();
        assertEquals("second", popped, "popObject() should return the last pushed value");
        // when
        Object remaining = mic.getObject(0);
        // then
        assertEquals("first", remaining, "After popping, getObject(0) should return the element that is now at the bottom of the stack");
    }

    @Test
    void getObject_afterPop_invalidHigherIndex_throwsException() {
        // given – same setup as previous test
        mic.pushObject("first");
        mic.pushObject("second");
        // removes "second"
        mic.popObject();
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(1), "After popping one element, index 1 is out of bounds and must throw");
    }

    @Test
    void getObject_returnsNullWhenNullWasPushed() {
        // given – push a null reference
        mic.pushObject(null);
        // when
        Object result = mic.getObject(0);
        // then
        assertNull(result, "getObject(0) should return null when a null reference was pushed");
    }

    @Test
    void getObject_doesNotModifyStackState() {
        // given – push three distinct objects
        List<Object> pushed = new ArrayList<>();
        pushed.add("one");
        pushed.add(2);
        pushed.add(3.0);
        pushed.forEach(mic::pushObject);
        // when – retrieve element at index 1
        Object retrieved = mic.getObject(1);
        // then – the retrieved value matches the pushed one and the stack size stays unchanged
        assertEquals(2, retrieved, "Retrieved value must match the object at the given index");
        assertEquals(3, mic.getObjectStack().size(), "Calling getObject must not alter the size of the internal object stack");
    }
}
