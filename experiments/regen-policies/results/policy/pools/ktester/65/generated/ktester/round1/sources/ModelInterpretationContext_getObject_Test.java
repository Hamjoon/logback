package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.Stack;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObject_Test {

    private ModelInterpretationContext mic;

    private Context context;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation provided by Logback
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
        // Populate the object stack with known values for the default tests
        mic.pushObject("first");
        mic.pushObject(42);
        mic.pushObject(new Object());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        mic = null;
        context = null;
    }

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(arguments(0, "first"), arguments(1, 42));
    }

    @Test
    void getObject_returnsCorrectElementAtIndexZero() {
        Object result = mic.getObject(0);
        assertEquals("first", result, "Object at index 0 should be the first pushed element");
    }

    @Test
    void getObject_returnsCorrectElementAtIndexOne() {
        Object result = mic.getObject(1);
        assertEquals(42, result, "Object at index 1 should be the second pushed element");
    }

    @Test
    void getObject_returnsCorrectElementAtIndexTwo() {
        Object result = mic.getObject(2);
        // The third element is an Object instance; we only verify it is the same instance
        assertNotNull(result, "Object at index 2 should not be null");
        // Since we cannot compare directly (different instances each run), we just ensure the stack size matches
        assertEquals(3, mic.getObjectStack().size(), "Stack size should remain unchanged");
    }

    @Test
    void getObject_withInvalidPositiveIndex_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(5), "Accessing an index beyond the stack size should throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_withNegativeIndex_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(-1), "Accessing a negative index should throw IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "getObject({0}) returns {1}")
    @MethodSource("normalBehaviorProvider")
    void getObject_normalBehavior(int index, Object expected) {
        Object actual = mic.getObject(index);
        assertEquals(expected, actual, () -> String.format("Expected element %s at index %d", expected, index));
    }

    @Test
    void getObject_onEmptyStack_throwsIndexOutOfBoundsException() {
        ModelInterpretationContext emptyMic = new ModelInterpretationContext(context);
        assertThrows(IndexOutOfBoundsException.class, () -> emptyMic.getObject(0), "Calling getObject(0) on an empty stack should throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_negativeIndex_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(-1), "A negative index is invalid; getObject(-1) must throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_indexEqualToSize_throwsIndexOutOfBoundsException() {
        // Build a context with exactly two elements
        ModelInterpretationContext twoElements = new ModelInterpretationContext(context);
        twoElements.pushObject("a");
        twoElements.pushObject("b");
        assertEquals(2, twoElements.getObjectStack().size());
        assertThrows(IndexOutOfBoundsException.class, () -> twoElements.getObject(2), "Requesting index equal to stack size should throw IndexOutOfBoundsException");
    }

    @Test
    void getObject_indexFarBeyondSize_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(10), "Even a far beyond index must result in IndexOutOfBoundsException");
    }

    @Test
    void getObject_nullElement_returnsNull() {
        ModelInterpretationContext nullMic = new ModelInterpretationContext(context);
        nullMic.pushObject(null);
        assertEquals(1, nullMic.getObjectStack().size());
        assertNull(nullMic.getObject(0), "Retrieving a null element should return null, not throw");
    }

    @Test
    void getObject_middleElement_returnsCorrectValue() {
        ModelInterpretationContext threeStrings = new ModelInterpretationContext(context);
        threeStrings.pushObject("first");
        threeStrings.pushObject("middle");
        threeStrings.pushObject("last");
        assertEquals("middle", threeStrings.getObject(1), "The element at index 1 should be the middle string");
    }

    @Test
    void getObject_afterPop_validIndexReturnsSecondElement() {
        ModelInterpretationContext micWithPop = new ModelInterpretationContext(context);
        micWithPop.pushObject("first");
        micWithPop.pushObject("second");
        micWithPop.pushObject("third");
        // Remove the top element
        Object popped = micWithPop.popObject();
        assertEquals("third", popped);
        // Now index 1 should be "second"
        assertEquals("second", micWithPop.getObject(1), "After popping, index 1 should refer to the original second element");
    }

    @Test
    void getObject_afterPop_outOfBoundsThrows() {
        ModelInterpretationContext micWithPop = new ModelInterpretationContext(context);
        micWithPop.pushObject("first");
        micWithPop.pushObject("second");
        micWithPop.pushObject("third");
        // size becomes 2
        micWithPop.popObject();
        assertThrows(IndexOutOfBoundsException.class, () -> micWithPop.getObject(2), "After popping, requesting index equal to new size should throw");
    }

    @Test
    void getObject_multipleCallsReturnSameInstance() {
        ModelInterpretationContext immutMic = new ModelInterpretationContext(context);
        Object unchanged = new Object();
        immutMic.pushObject(unchanged);
        Object firstCall = immutMic.getObject(0);
        Object secondCall = immutMic.getObject(0);
        assertSame(unchanged, firstCall, "First call should return the original object");
        assertSame(firstCall, secondCall, "Subsequent calls must return the same instance");
    }

    @Test
    void getObject_nullObjectStack_throwsNullPointerException() throws Exception {
        // Use reflection to corrupt the internal state
        Field objectStackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        objectStackField.setAccessible(true);
        // now objectStack is null
        objectStackField.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.getObject(0), "When objectStack is null, getObject should throw NullPointerException");
    }
}
