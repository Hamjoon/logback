package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import java.lang.reflect.Field;
import java.util.Stack;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekObject_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // mocked dependency required by the constructor
    mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the stacks to avoid side‑effects (not strictly necessary because a new instance is created each time)
        mic.getObjectStack().clear();
        mic.getModelStack().clear();
    }

    @Test
    void peekObject_returnsTopObjectWithoutRemovingIt() {
        // given
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        mic.pushObject(second);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(second, peeked, "peekObject should return the most recently pushed object");
        // verify that the stack size is unchanged
        assertEquals(2, mic.getObjectStack().size(), "peekObject must not remove the element from the stack");
    }

    @Test
    void peekObject_multiplePushes_behavesAsLIFO() {
        // given
        String a = "A";
        String b = "B";
        String c = "C";
        mic.pushObject(a);
        mic.pushObject(b);
        mic.pushObject(c);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(c, peeked, "The last pushed object should be on top of the stack");
        // ensure the order is preserved after another peek
        assertSame(c, mic.peekObject(), "Subsequent peeks should still return the same top element");
    }

    @Test
    void peekObject_onEmptyStack_throwsEmptyStackException() {
        // given: a freshly created ModelInterpretationContext has an empty objectStack
        // when / then
        assertThrows(EmptyStackException.class, () -> mic.peekObject(), "peekObject should throw EmptyStackException when the stack is empty");
    }

    @Test
    void peekObject_returnsNullWhenTopIsNull() {
        // given
        mic.pushObject(null);
        // when
        Object peeked = mic.peekObject();
        // then
        assertNull(peeked, "peekObject should return null when the top element is null");
        assertEquals(1, mic.getObjectStack().size(), "Stack size must remain unchanged after peek");
    }

    @Test
    void peekObject_afterPop_returnsUnderlyingElement() {
        // given
        String first = "first";
        String second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        // remove the top element
        assertEquals(second, mic.popObject(), "popObject should return the last pushed element");
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(first, peeked, "After popping, peekObject should return the element now on top of the stack");
    }

    @Test
    void peekObject_nullTopAndUnderlyingElement_behavior() {
        // given: push a non‑null element followed by a null
        String base = "base";
        mic.pushObject(base);
        mic.pushObject(null);
        // when: peek while null is on top
        Object topPeek = mic.peekObject();
        // then
        assertNull(topPeek, "peekObject should return null when the current top is null");
        assertEquals(2, mic.getObjectStack().size(), "Stack size must stay the same after peeking a null top");
        // when: pop the null and peek again
        assertNull(mic.popObject(), "popObject should return null for the null top element");
        Object afterPopPeek = mic.peekObject();
        // then
        assertSame(base, afterPopPeek, "After removing the null top, peekObject should return the next element");
    }

    @Test
    void peekObject_consecutiveCalls_areIdempotent() {
        // given
        String singleton = "singleton";
        mic.pushObject(singleton);
        // when
        Object firstPeek = mic.peekObject();
        Object secondPeek = mic.peekObject();
        // then
        assertSame(singleton, firstPeek, "First peek should return the pushed object");
        assertSame(singleton, secondPeek, "Second peek should return the same object");
        assertEquals(1, mic.getObjectStack().size(), "Stack size must remain unchanged after multiple peeks");
    }

    @Test
    void peekObject_duplicateReference_returnsSameInstance() {
        // given
        Object shared = new Object();
        mic.pushObject(shared);
        // push the same reference again
        mic.pushObject(shared);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(shared, peeked, "peekObject should return the exact same reference that was pushed last");
    }

    @Test
    void peekObject_withModelInstance_returnsSameModel() {
        // given
        Model mockModel = mock(Model.class);
        mic.pushObject(mockModel);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(mockModel, peeked, "peekObject must return the same Model instance that was pushed");
    }

    @Test
    void peekObject_withMutableObject_preservesIdentity() {
        // given
        StringBuilder sb = new StringBuilder("mutable");
        mic.pushObject(sb);
        // when
        Object peeked = mic.peekObject();
        // then
        assertSame(sb, peeked, "The returned object should be the exact same mutable instance (==) that was pushed");
    }

    @Test
    void peekObject_nullObjectStack_throwsNullPointerException() throws Exception {
        // given: use reflection to set the internal objectStack to null
        Field field = ModelInterpretationContext.class.getDeclaredField("objectStack");
        field.setAccessible(true);
        field.set(mic, null);
        // when / then
        assertThrows(NullPointerException.class, () -> mic.peekObject(), "If objectStack is null, peekObject should throw NullPointerException");
    }
}
