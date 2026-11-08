package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushObject_Test {

    @Mock
    Context // mocked Context – no real behavior required for pushObject tests
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test to guarantee isolation
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the pattern consistent
        mic = null;
    }

    private static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(// push a non‑null String onto an initially empty stack
        Arguments.of("Push a String", "firstObject", 1, "firstObject"), // push an Integer after a previous push (LIFO)
        Arguments.of("Push an Integer after a String", 123, 2, 123));
    }

    @Test
    @DisplayName("pushObject should add a non‑null object to the stack")
    void pushObjectAddsElement() {
        // given
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty before push");
        String obj = "sampleObject";
        // when
        mic.pushObject(obj);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Stack should not be empty after push");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be 1");
        assertSame(obj, mic.peekObject(), "Top of stack should be the pushed object");
    }

    @Test
    @DisplayName("pushObject should preserve LIFO order for multiple pushes")
    void pushObjectMultipleElementsLifoOrder() {
        // given
        String first = "first";
        String second = "second";
        // when
        mic.pushObject(first);
        mic.pushObject(second);
        // then
        Stack<Object> stack = mic.getObjectStack();
        assertEquals(2, stack.size(), "Stack should contain two elements");
        assertSame(second, mic.peekObject(), "Top of stack should be the last pushed element");
        // pop to verify order without using the private popObject method
        Object popped = stack.pop();
        assertSame(second, popped, "Popped element should be the second one");
        assertSame(first, stack.peek(), "Remaining element should be the first one");
    }

    @Test
    @DisplayName("pushObject should accept null values")
    void pushObjectAcceptsNull() {
        // given
        assertTrue(mic.isObjectStackEmpty());
        // when
        mic.pushObject(null);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Stack should contain an element even if it is null");
        assertEquals(1, mic.getObjectStack().size());
        assertNull(mic.peekObject(), "Top of stack should be null");
    }

    @ParameterizedTest(name = "[{index}] pushObject normal: {0}")
    @MethodSource("normalBehaviourProvider")
    @DisplayName("pushObject – normal behaviour")
    void pushObjectNormalBehaviour(String description, Object toPush, int expectedSize, Object expectedTop) {
        // Ensure the stack starts empty
        assertTrue(mic.isObjectStackEmpty(), "Pre‑condition: stack must be empty");
        // First push (if any previous pushes are required they are performed in the provider)
        mic.pushObject(toPush);
        // Verify size and top element
        assertEquals(expectedSize, mic.getObjectStack().size(), "Stack size after push should match expected");
        assertSame(expectedTop, mic.peekObject(), "Top of stack should be the last pushed object");
    }

    @Test
    @DisplayName("pushObject – duplicate object pushes create distinct entries")
    void pushObjectDuplicateEntries() {
        String dup = "duplicateObject";
        mic.pushObject(dup);
        // second push of the same reference
        mic.pushObject(dup);
        assertEquals(2, mic.getObjectStack().size(), "Stack size should be 2 after pushing the same object twice");
        assertSame(dup, mic.peekObject(), "Top of stack should be the most recent duplicate push");
    }

    @Test
    @DisplayName("pushObject – pop then push updates stack correctly")
    void pushObjectPopThenPush() {
        mic.pushObject("first");
        mic.pushObject("second");
        // pop the top element using the public API
        Object popped = mic.popObject();
        assertEquals("second", popped, "Popped element should be 'second'");
        // now push a third element
        mic.pushObject("third");
        assertEquals(2, mic.getObjectStack().size(), "Stack size should be 2 after pop‑push sequence");
        assertEquals("third", mic.peekObject(), "Top of stack should be the newly pushed 'third' object");
    }

    @Test
    @DisplayName("pushObject – handling a large number of elements")
    void pushObjectLargeVolume() {
        final int COUNT = 1000;
        for (int i = 0; i < COUNT; i++) {
            mic.pushObject(i);
        }
        assertEquals(COUNT, mic.getObjectStack().size(), "Stack should contain 1000 elements after the loop");
        assertEquals(COUNT - 1, mic.peekObject(), "Top of stack should be the last pushed integer (999)");
    }

    @Test
    @DisplayName("pushObject – accepts a Model instance")
    void pushObjectAcceptsModelInstance() {
        // assuming Model is concrete; otherwise use a subclass/mock
        Model model = new Model();
        mic.pushObject(model);
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be 1 after pushing a Model");
        assertSame(model, mic.peekObject(), "Top of stack should be the Model instance that was pushed");
    }

    @Test
    @DisplayName("pushObject – throws NullPointerException when internal stack is null")
    void pushObjectThrowsWhenStackIsNull() throws Exception {
        // Use reflection to set the private field 'objectStack' to null
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        // now the internal stack is null
        stackField.set(mic, null);
        // Verify that invoking pushObject results in NPE
        assertThrows(NullPointerException.class, () -> mic.pushObject(123), "Calling pushObject with a null internal stack should throw NullPointerException");
    }
}
