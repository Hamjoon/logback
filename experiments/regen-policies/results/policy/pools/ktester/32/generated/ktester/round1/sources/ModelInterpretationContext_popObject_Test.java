package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popObject_Test {

    @Mock
    Context // mocked dependency for the MIC constructor
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
        // Clear references – not strictly required but keeps the pattern consistent
        mic = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> normalBehaviorProvider() {
        return Stream.of(arguments("Push a single element and pop it", List.of("firstElement"), "firstElement"), arguments("Push two elements and pop the last one", List.of("firstElement", "secondElement"), "secondElement"), arguments("Push three elements and pop the last one", List.of("firstElement", "secondElement", "thirdElement"), "thirdElement"));
    }

    @Test
    void popObject_returnsTopElement_andRemovesItFromStack() {
        // given: push two distinct objects onto the stack
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        mic.pushObject(second);
        // when: pop the top object
        Object popped = mic.popObject();
        // then: the popped object is the last one pushed
        assertEquals(second, popped, "popObject should return the element that was on top of the stack");
        // and: the stack now contains only the first element
        assertFalse(mic.isObjectStackEmpty(), "stack should not be empty after popping one of two elements");
        assertEquals(first, mic.peekObject(), "the remaining top element should be the first one pushed");
    }

    @Test
    void popObject_onEmptyStack_throwsEmptyStackException() {
        // given: an empty object stack (new MIC is already empty)
        // when / then: popping should throw EmptyStackException
        assertThrows(EmptyStackException.class, () -> mic.popObject(), "popObject should throw EmptyStackException when the stack is empty");
    }

    @Test
    void popObject_multiplePops_behaveCorrectly() {
        // given: push three objects
        String a = "A";
        String b = "B";
        String c = "C";
        mic.pushObject(a);
        mic.pushObject(b);
        mic.pushObject(c);
        // when: pop three times
        assertEquals(c, mic.popObject(), "first pop should return C");
        assertEquals(b, mic.popObject(), "second pop should return B");
        assertEquals(a, mic.popObject(), "third pop should return A");
        // then: stack is empty
        assertTrue(mic.isObjectStackEmpty(), "stack should be empty after popping all elements");
    }

    /* -------------------------------------------------------------
     *  Normal LIFO behavior (single, double and triple pushes)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => description={0}")
    @MethodSource("normalBehaviorProvider")
    void popObject_normalBehavior(String description, List<Object> toPush, Object expectedTop) {
        // given: push the supplied elements in order
        toPush.forEach(mic::pushObject);
        // when: pop the top element
        Object popped = mic.popObject();
        // then: the popped element matches expectation
        assertEquals(expectedTop, popped, description);
        // and: the remaining stack reflects correct LIFO order
        if (toPush.size() > 1) {
            assertEquals(toPush.get(toPush.size() - 2), mic.peekObject(), "after one pop, the new top should be the previously second‑to‑last element");
        } else {
            assertTrue(mic.isObjectStackEmpty(), "stack should be empty after popping the only element");
        }
    }

    /* -------------------------------------------------------------
     *  Null element handling
     * ------------------------------------------------------------- */
    @Test
    void popObject_nullElement_returnsNullAndEmptiesStack() {
        // given: a null value on the stack
        mic.pushObject(null);
        // when
        Object popped = mic.popObject();
        // then
        assertNull(popped, "popObject should return null when the top element is null");
        assertTrue(mic.isObjectStackEmpty(), "stack should be empty after popping the null element");
    }

    /* -------------------------------------------------------------
     *  Multiple types (Integer then String)
     * ------------------------------------------------------------- */
    @Test
    void popObject_multipleTypes_preservesIdentity() {
        // given
        Integer intObj = 42;
        String strObj = "test";
        mic.pushObject(intObj);
        mic.pushObject(strObj);
        // when
        // should be the String
        Object firstPop = mic.popObject();
        // should be the Integer
        Object secondPop = mic.popObject();
        // then
        assertEquals(strObj, firstPop, "first pop should return the last pushed String");
        assertEquals(intObj, secondPop, "second pop should return the previously pushed Integer");
    }

    /* -------------------------------------------------------------
     *  Duplicate object reference
     * ------------------------------------------------------------- */
    @Test
    void popObject_duplicateReference_returnsSameInstance() {
        // given: the same object pushed twice
        Object sharedObject = new Object();
        mic.pushObject(sharedObject);
        mic.pushObject(sharedObject);
        // when
        Object firstPop = mic.popObject();
        // then
        assertSame(sharedObject, firstPop, "first pop should return the exact same instance that was pushed");
        // second pop is verified in a separate test to keep each test independent
    }

    @Test
    void popObject_duplicateReference_secondPop_returnsSameInstance() {
        // given
        Object sharedObject = new Object();
        mic.pushObject(sharedObject);
        mic.pushObject(sharedObject);
        // discard first
        mic.popObject();
        // when
        Object secondPop = mic.popObject();
        // then
        assertSame(sharedObject, secondPop, "second pop should also return the same shared instance");
    }

    /* -------------------------------------------------------------
     *  Mutable object reference (StringBuilder)
     * ------------------------------------------------------------- */
    @Test
    void popObject_mutableObjectReference_preservesIdentity() {
        // given
        StringBuilder sb = new StringBuilder("abc");
        mic.pushObject(sb);
        // when
        Object popped = mic.popObject();
        // then
        assertSame(sb, popped, "popObject must return the exact same mutable instance (no copy)");
    }

    /* -------------------------------------------------------------
     *  Large stack pop (0..999)
     * ------------------------------------------------------------- */
    @Test
    void popObject_largeStack_returnsTopElement() {
        // given: push 0..999
        for (int i = 0; i <= 999; i++) {
            mic.pushObject(i);
        }
        // when
        Object top = mic.popObject();
        // then
        assertEquals(999, top, "popObject should return the last pushed integer (999)");
        assertEquals(999, mic.getObjectStack().size(), "after popping one element, the stack size should be 999");
    }

    /* -------------------------------------------------------------
     *  Collection object pop (ArrayList)
     * ------------------------------------------------------------- */
    @Test
    void popObject_collectionObject_returnsSameInstance() {
        // given
        List<String> listInstance = new ArrayList<>();
        listInstance.add("a");
        listInstance.add("b");
        listInstance.add("c");
        mic.pushObject(listInstance);
        // when
        Object popped = mic.popObject();
        // then
        assertSame(listInstance, popped, "popObject should return the exact same collection instance");
    }

    /* -------------------------------------------------------------
     *  Null internal stack -> NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void popObject_nullInternalStack_throwsNullPointerException() throws Exception {
        // given: create a MIC and forcibly set its objectStack field to null via reflection
        ModelInterpretationContext micWithNullStack = new ModelInterpretationContext(mockContext);
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(micWithNullStack, null);
        // when / then
        assertThrows(NullPointerException.class, micWithNullStack::popObject, "popObject should throw NullPointerException when internal objectStack is null");
    }
}
