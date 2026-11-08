package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Stack;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekObject_Test {

    @Mock
    private Context // mocked dependency
    mockContext;

    // instance under test
    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test to guarantee isolation
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        mic = null;
    }

    @Test
    void peekObject_returnsTopObjectWithoutRemovingIt() {
        // given
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        // second is now on top of the stack
        mic.pushObject(second);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(second, peeked, "peekObject should return the object most recently pushed");
        // Ensure the stack size is unchanged
        assertEquals(2, mic.getObjectStack().size(), "peekObject must not pop the object");
        // Ensure the top of the stack is still the same after peeking
        assertSame(second, mic.getObjectStack().peek(), "top of stack should remain unchanged after peek");
    }

    @Test
    void peekObject_onEmptyStack_throwsEmptyStackException() {
        // given: a freshly created MIC with an empty object stack
        // when / then
        assertThrows(EmptyStackException.class, mic::peekObject, "peekObject should throw EmptyStackException when the stack is empty");
    }

    @Test
    void peekObject_returnsSinglePushedObjectWithoutRemovingIt() {
        // given
        String first = "first";
        mic.pushObject(first);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(first, peeked, "peekObject should return the only object pushed");
        assertEquals(1, mic.getObjectStack().size(), "Stack size must stay unchanged after peek");
    }

    @Test
    void peekObject_returnsMostRecentObjectPreservingLIFOOrder() {
        // given
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        // second is now on top
        mic.pushObject(second);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(second, peeked, "peekObject should return the most recently pushed object");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must stay unchanged after peek");
        assertSame(second, mic.getObjectStack().peek(), "Top of stack must remain the same after peek");
    }

    @Test
    void peekObject_afterPop_returnsRemainingTopElement() {
        // given
        String first = "first";
        String second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        // removes "second"
        Object popped = mic.popObject();
        assertSame(second, popped, "popObject should have removed the second element");
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(first, peeked, "After popping, peekObject should return the new top element");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should reflect the pop");
    }

    @Test
    void peekObject_whenTopIsNull_returnsNullWithoutAlteringStack() {
        // given
        mic.pushObject(null);
        // ensure stack depth > 1
        mic.pushObject("not-null");
        // remove "not-null", now null is on top
        mic.popObject();
        // when
        Object peeked = mic.peekObject();
        // then
        assertNull(peeked, "peekObject should return null when the top element is null");
        assertEquals(1, mic.getObjectStack().size(), "Stack size must stay unchanged after peek");
    }

    @Test
    void peekObject_afterMultiplePushesAndOnePop_returnsCorrectElement() {
        // given
        mic.pushObject("a");
        mic.pushObject("b");
        mic.pushObject("c");
        // removes "c"
        Object popped = mic.popObject();
        assertSame("c", popped, "popObject should remove the last pushed element");
        // when
        Object peeked = mic.peekObject();
        // then
        assertEquals("b", peeked, "After popping \"c\", peekObject should return \"b\"");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must reflect the pop");
    }

    @Test
    void peekObject_doesNotChangeStackSize() {
        // given
        mic.pushObject("x");
        // y is on top
        mic.pushObject("y");
        // when
        Object peeked = mic.peekObject();
        // then
        assertEquals("y", peeked, "peekObject should return the top element \"y\"");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must remain 2 after peeking (no pop should occur)");
    }

    @Test
    void peekObject_whenInternalStackIsNull_throwsNullPointerException() throws Exception {
        // given: force the private field `objectStack` to null via reflection
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // when / then
        assertThrows(NullPointerException.class, mic::peekObject, "If objectStack is null, peekObject should throw NullPointerException");
    }
}
