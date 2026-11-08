package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectStack_Test {

    private Context context;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Context implementation (ContextBase) – it has no heavy side‑effects
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        mic = null;
        context = null;
    }

    @Test
    void getObjectStack_shouldReturnNonNullEmptyStackInitially() {
        // given – mic is created in @BeforeEach
        // when
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNotNull(stack, "The object stack must not be null");
        assertTrue(stack.isEmpty(), "The object stack should be empty immediately after construction");
    }

    @Test
    void getObjectStack_shouldReflectObjectsPushedViaAPI() {
        // given
        String expected = "testObject";
        // when
        mic.pushObject(expected);
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertEquals(1, stack.size(), "Stack size should be 1 after one push");
        assertSame(expected, stack.peek(), "The object on top of the stack must be the one that was pushed");
    }

    @Test
    void getObjectStack_modifyingReturnedStackShouldAffectContext() {
        // given
        String obj1 = "first";
        String obj2 = "second";
        // when – manipulate the stack directly
        Stack<Object> stack = mic.getObjectStack();
        stack.push(obj1);
        stack.push(obj2);
        // then – the MIC API should see the same state
        assertEquals(obj2, mic.peekObject(), "peekObject should return the last element pushed via the returned stack");
        assertEquals(obj1, mic.getObject(0), "getObject(0) should return the first element pushed");
        assertEquals(2, mic.getObjectStack().size(), "Stack size reported by MIC should be 2");
    }

    @Test
    void getObjectStack_shouldBeIndependentBetweenDifferentInstances() {
        // given
        ModelInterpretationContext mic2 = new ModelInterpretationContext(new ContextBase());
        // when
        mic.pushObject("A");
        mic2.pushObject("B");
        // then
        assertEquals("A", mic.peekObject(), "First MIC should contain its own object");
        assertEquals("B", mic2.peekObject(), "Second MIC should contain its own object");
        assertNotSame(mic.getObjectStack(), mic2.getObjectStack(), "Each ModelInterpretationContext must maintain its own stack instance");
    }

    /* --------------------------------------------------------------
     *  Test group: Initial state of object stack
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_initiallyShouldReturnNull() {
        // given – a freshly created ModelInterpretationContext (setup in @BeforeEach)
        // when
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNull(stack, "A newly created ModelInterpretationContext must return null for getObjectStack()");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after a single push
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterSinglePushShouldReturnNonNullStackWithOneElement() {
        // given
        String myObject = "myObject";
        // when
        mic.pushObject(myObject);
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNotNull(stack, "Stack must be instantiated after first pushObject");
        assertEquals(1, stack.size(), "Stack size should be 1 after a single push");
        assertSame(myObject, stack.peek(), "Top element must be the object that was pushed");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after multiple pushes
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterMultiplePushesShouldContainAllElementsInOrder() {
        // given
        String first = "first";
        String second = "second";
        String third = "third";
        // when
        mic.pushObject(first);
        mic.pushObject(second);
        mic.pushObject(third);
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNotNull(stack, "Stack must be non‑null after pushes");
        assertEquals(3, stack.size(), "Stack size should reflect three pushes");
        assertSame(third, stack.peek(), "The top element must be the last pushed object");
        // verify insertion order via getObject(int)
        assertSame(first, mic.getObject(0), "Element at index 0 should be the first pushed object");
        assertSame(second, mic.getObject(1), "Element at index 1 should be the second pushed object");
        assertSame(third, mic.getObject(2), "Element at index 2 should be the third pushed object");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after popping to empty
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterPushAndPopShouldBeEmptyButNonNull() {
        // given
        mic.pushObject("onlyOne");
        // when
        // removes the only element
        mic.popObject();
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNotNull(stack, "Stack must remain non‑null after it becomes empty");
        assertTrue(stack.isEmpty(), "Stack should be empty after popping the only element");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack reference integrity
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_externalModificationShouldBeVisibleThroughAPI() {
        // given
        // should be null initially
        Stack<Object> externalView = mic.getObjectStack();
        assertNull(externalView, "Initially the internal stack is null");
        // when – push via API first to create the internal stack
        mic.pushObject("initial");
        // now non‑null
        Stack<Object> stack = mic.getObjectStack();
        // modify directly
        stack.push("external");
        // then – API must see the change
        assertEquals(2, mic.getObjectStack().size(), "Size must reflect the external push");
        assertEquals("external", mic.peekObject(), "peekObject must return the element added externally");
    }

    /* --------------------------------------------------------------
     *  Test group: Consistent Stack instance across calls
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_multipleCallsShouldReturnSameInstance() {
        // given
        mic.pushObject("a");
        // when
        Stack<Object> firstCall = mic.getObjectStack();
        Stack<Object> secondCall = mic.getObjectStack();
        // then
        assertSame(firstCall, secondCall, "Successive calls must return the identical Stack instance");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack remains null when no push performed
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_shouldStayNullWhenOnlyUnrelatedMethodsAreInvoked() {
        // given – call a few unrelated methods
        mic.setTopModel(null);
        mic.addImport("stem", "fqcn");
        // passing null is acceptable for this test
        mic.addDependencyDefinition(null);
        // when
        Stack<Object> stack = mic.getObjectStack();
        // then
        assertNull(stack, "Without any pushObject call the stack must stay null even after other API usage");
    }
}
