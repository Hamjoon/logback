package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectStack_Test {

    @Mock
    Context // mocked dependency for the MIC constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise @Mock fields automatically
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Supplies different push sequences for the parameterised test above.
     */
    private static Stream<String[]> pushSequences() {
        return Stream.of(new String[] { "onlyOne" }, new String[] { "first", "second" }, new String[] { "a", "b", "c" });
    }

    @Test
    void getObjectStack_returnsNonNullEmptyStackImmediatelyAfterConstruction() {
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "The object stack should never be null");
        assertTrue(stack.isEmpty(), "The object stack should be empty right after construction");
    }

    @Test
    void getObjectStack_reflectsObjectsPushedViaPublicApi() {
        Object first = new Object();
        Object second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        Stack<Object> stack = mic.getObjectStack();
        assertEquals(2, stack.size(), "Stack size should match number of pushed objects");
        assertSame(second, stack.peek(), "Top of the stack should be the last pushed object");
        assertSame(first, stack.get(0), "Bottom of the stack should be the first pushed object");
    }

    @Test
    void getObjectStack_returnsLiveReference_notACopy() {
        mic.pushObject("initial");
        Stack<Object> stackRef = mic.getObjectStack();
        // Mutate the returned stack directly
        stackRef.push("directlyAdded");
        // The MIC instance must see the same mutation
        assertEquals(2, mic.getObjectStack().size(), "The returned stack should be the actual internal stack, not a copy");
        assertEquals("directlyAdded", mic.peekObject(), "Object added via the returned reference should be visible through MIC API");
    }

    @Test
    void copyConstructor_createsNewInstanceWithIndependentObjectStack() {
        // Populate the original MIC's stack
        mic.pushObject("original");
        // Create a copy using the copy‑constructor
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // The copy must have its own empty object stack
        Stack<Object> copyStack = copy.getObjectStack();
        assertNotNull(copyStack, "Copy's object stack must not be null");
        assertTrue(copyStack.isEmpty(), "Copy's object stack should be empty (not cloned from original)");
        // Mutating the original must not affect the copy
        mic.pushObject("another");
        assertEquals(2, mic.getObjectStack().size(), "Original should now contain two elements");
        assertTrue(copy.getObjectStack().isEmpty(), "Copy should remain empty after original is mutated");
    }

    /* --------------------------------------------------------------
     *  Test group: Initial state of object stack
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_initiallyReturnsNull() {
        // According to the specification the internal stack is not created
        // until the first pushObject() call.
        assertNull(mic.getObjectStack(), "When a new ModelInterpretationContext is created, getObjectStack() should return null");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after pushing a single object
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterSinglePush_returnsStackWithOneElement() {
        String testObj = "testObj";
        // initialise the internal stack
        mic.pushObject(testObj);
        // should now be non‑null
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "Stack must be instantiated after first push");
        assertEquals(1, stack.size(), "Stack size should be 1 after a single push");
        assertSame(testObj, stack.peek(), "Top of the stack must be the pushed object");
        assertSame(testObj, stack.get(0), "Bottom (and only) element must be the pushed object");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after pushing multiple objects
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterMultiplePushes_reflectsLifoOrder() {
        String first = "first";
        String second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "Stack must be instantiated after pushes");
        assertEquals(2, stack.size(), "Stack size should match number of pushed objects");
        assertSame(second, stack.peek(), "Top of the stack must be the last pushed object");
        assertSame(first, stack.get(0), "Bottom of the stack must be the first pushed object");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after pushing and popping to empty
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterPushAndPop_returnsEmptyStack() {
        String payload = "payload";
        mic.pushObject(payload);
        Object popped = mic.popObject();
        assertEquals(payload, popped, "popObject() must return the previously pushed value");
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "Even after popping the last element the stack reference must stay non‑null");
        assertTrue(stack.isEmpty(), "Stack should be empty after pushing and then popping the only element");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack after pushing null
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_afterPushingNull_containsNullEntry() {
        // null is a legitimate stack entry
        mic.pushObject(null);
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "Stack must be instantiated even when null is pushed");
        assertEquals(1, stack.size(), "Stack size should be 1 after pushing null");
        assertNull(stack.peek(), "Top element must be null");
    }

    /* --------------------------------------------------------------
     *  Test group: Object stack reference stability
     * -------------------------------------------------------------- */
    @Test
    void getObjectStack_multipleCalls_returnSameInstance() {
        // force lazy initialisation
        mic.pushObject("x");
        Stack<Object> firstCall = mic.getObjectStack();
        Stack<Object> secondCall = mic.getObjectStack();
        assertSame(firstCall, secondCall, "Successive calls to getObjectStack() must return the identical Stack instance");
    }

    /**
     * Parameterised test that exercises the same assertions for 1, 2 and 3 pushes.
     * Demonstrates the use of {@code @MethodSource} while still covering the
     * JSON‑specified scenarios.
     */
    @ParameterizedTest(name = "[{index}] pushes = {0}")
    @MethodSource("pushSequences")
    void getObjectStack_parameterisedPushes(String[] values) {
        // push each value (null values are allowed)
        for (String v : values) {
            mic.pushObject(v);
        }
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "Stack must be instantiated after pushes");
        assertEquals(values.length, stack.size(), "Stack size must equal number of pushObject calls");
        // Verify LIFO order
        for (int i = values.length - 1; i >= 0; i--) {
            assertSame(values[i], stack.pop(), "Elements must be popped in reverse order of pushes");
        }
        assertTrue(stack.isEmpty(), "Stack should be empty after popping all elements");
    }
}
