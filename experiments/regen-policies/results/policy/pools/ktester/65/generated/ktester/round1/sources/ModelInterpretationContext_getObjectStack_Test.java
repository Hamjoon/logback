package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.*;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObjectStack_Test {

    private ModelInterpretationContext mic;

    private Context context;

    @BeforeEach
    void setupBeforeEach() {
        // Use the concrete ContextBase implementation provided by Logback
        context = new ContextBase();
        mic = new ModelInterpretationContext(context);
    }

    @Test
    @DisplayName("getObjectStack() returns a non‑null, initially empty stack")
    void testGetObjectStackInitiallyEmpty() {
        Stack<Object> stack = mic.getObjectStack();
        assertNotNull(stack, "The object stack should never be null");
        assertTrue(stack.isEmpty(), "The object stack should be empty right after construction");
    }

    @Test
    @DisplayName("getObjectStack() reflects objects pushed via pushObject()")
    void testGetObjectStackAfterPush() {
        // Push a few distinct objects
        String first = "first";
        Integer second = 42;
        Object third = new Object();
        mic.pushObject(first);
        mic.pushObject(second);
        mic.pushObject(third);
        Stack<Object> stack = mic.getObjectStack();
        // The stack should contain exactly three elements in LIFO order
        assertEquals(3, stack.size(), "Stack size should match number of pushed objects");
        assertSame(third, stack.peek(), "Top of the stack should be the last pushed object");
        assertEquals(third, mic.peekObject(), "peekObject() should return the same element as stack.peek()");
    }

    @Test
    @DisplayName("Modifying the returned stack directly affects the MIC internal state")
    void testReturnedStackIsLiveReference() {
        Stack<Object> stack = mic.getObjectStack();
        // Directly manipulate the returned stack
        String element = "direct";
        stack.push(element);
        // The MIC should now see this element via its public API
        assertFalse(mic.isObjectStackEmpty(), "Object stack should no longer be empty after direct push");
        assertEquals(element, mic.peekObject(), "peekObject() should reflect the directly pushed element");
    }

    /**
     * Verify that a freshly created {@link ModelInterpretationContext}
     * returns {@code null} from {@code getObjectStack()} because the internal
     * {@code objectStack} field is not initialised by the constructor.
     */
    @Test
    @DisplayName("getObjectStack() returns null immediately after construction")
    void testGetObjectStackReturnsNullWhenUninitialised() {
        Stack<Object> stack = mic.getObjectStack();
        assertNull(stack, "Expected objectStack to be null before any explicit initialisation");
    }

    /**
     * Using reflection, inject a non‑empty {@link Stack} containing a single
     * element into the private {@code objectStack} field. The getter must
     * return the exact same instance (reference equality).
     */
    @Test
    @DisplayName("getObjectStack() returns the injected non‑empty stack")
    void testGetObjectStackReturnsInjectedNonEmptyStack() throws Exception {
        // Prepare a stack with one element.
        Stack<Object> injectedStack = new Stack<>();
        String element = "singleElement";
        injectedStack.push(element);
        // Inject the stack via reflection.
        Field field = ModelInterpretationContext.class.getDeclaredField("objectStack");
        field.setAccessible(true);
        field.set(mic, injectedStack);
        // Retrieve via the public getter.
        Stack<Object> returnedStack = mic.getObjectStack();
        // Verify reference equality and content.
        assertSame(injectedStack, returnedStack, "The getter should return the exact Stack instance that was injected");
        assertEquals(1, returnedStack.size(), "Injected stack should contain one element");
        assertEquals(element, returnedStack.peek(), "The element on top of the injected stack should be preserved");
    }

    /**
     * Inject an empty {@link Stack} via reflection and ensure the getter returns
     * this empty instance unchanged.
     */
    @Test
    @DisplayName("getObjectStack() returns the injected empty stack")
    void testGetObjectStackReturnsInjectedEmptyStack() throws Exception {
        // Prepare an empty stack.
        Stack<Object> emptyStack = new Stack<>();
        // Inject the empty stack via reflection.
        Field field = ModelInterpretationContext.class.getDeclaredField("objectStack");
        field.setAccessible(true);
        field.set(mic, emptyStack);
        // Retrieve via the public getter.
        Stack<Object> returnedStack = mic.getObjectStack();
        // Verify reference equality and emptiness.
        assertSame(emptyStack, returnedStack, "The getter should return the exact empty Stack instance that was injected");
        assertTrue(returnedStack.isEmpty(), "Returned stack should be empty");
    }
}
