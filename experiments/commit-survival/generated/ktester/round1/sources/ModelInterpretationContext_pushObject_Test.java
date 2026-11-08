package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushObject_Test {

    private Context mockContext;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // A simple mock Context is sufficient – the class under test only stores the reference.
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary but keeps the pattern consistent.
        mic = null;
        mockContext = null;
    }

    private static Stream<Arguments> basicPushProvider() {
        return Stream.of(Arguments.of("testString"), Arguments.of(null));
    }

    private static Stream<Arguments> extendedPushProvider() {
        return Stream.of(// Integer payload
        Arguments.of(42), // Generic Object instance
        Arguments.of(new Object()), // Duplicate string values – we only push once here; LIFO is verified in a dedicated test
        Arguments.of("duplicate"));
    }

    @Test
    void pushObject_shouldAddObjectToStack() {
        // GIVEN
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty before push");
        Object payload = new Object();
        // WHEN
        mic.pushObject(payload);
        // THEN
        assertFalse(mic.isObjectStackEmpty(), "Stack should not be empty after push");
        assertEquals(payload, mic.peekObject(), "Top of the stack should be the pushed object");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be 1");
    }

    @Test
    void pushObject_multiplePushesMaintainLIFOOrder() {
        // GIVEN
        Object first = "first";
        Object second = "second";
        // WHEN
        mic.pushObject(first);
        mic.pushObject(second);
        // THEN
        assertEquals(2, mic.getObjectStack().size(), "Stack size should be 2 after two pushes");
        assertEquals(second, mic.peekObject(), "Top of the stack should be the most recently pushed object");
        // Pop and verify order
        Object poppedSecond = mic.popObject();
        Object poppedFirst = mic.popObject();
        assertEquals(second, poppedSecond, "First pop should return the second object pushed");
        assertEquals(first, poppedFirst, "Second pop should return the first object pushed");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping all elements");
    }

    @Test
    void pushObject_nullShouldBeAccepted() {
        // GIVEN
        assertTrue(mic.isObjectStackEmpty(), "Stack should start empty");
        // WHEN
        mic.pushObject(null);
        // THEN
        assertFalse(mic.isObjectStackEmpty(), "Stack should contain an element after pushing null");
        assertNull(mic.peekObject(), "Top of the stack should be null when null is pushed");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be 1 after pushing null");
    }

    @ParameterizedTest(name = "[{index}] pushObject({0})")
    @MethodSource("basicPushProvider")
    void pushObject_basic(Object payload) {
        // GIVEN
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty before push");
        // WHEN
        mic.pushObject(payload);
        // THEN
        assertFalse(mic.isObjectStackEmpty(), "Stack should not be empty after push");
        assertEquals(payload, mic.peekObject(), "Top of the stack should be the same object that was pushed");
        assertEquals(1, mic.getObjectStack().size(), "Stack size should be exactly 1 after a single push");
    }

    @Test
    void pushObject_twoPushesMaintainLIFOOrder() {
        // GIVEN
        Object first = "first";
        Object second = "second";
        // WHEN
        mic.pushObject(first);
        mic.pushObject(second);
        // THEN – verify stack size and top element
        assertEquals(2, mic.getObjectStack().size(), "Stack size should be 2 after two pushes");
        assertEquals(second, mic.peekObject(), "Top of the stack must be the most recently pushed object");
        // POP and verify LIFO order
        Object poppedSecond = mic.popObject();
        Object poppedFirst = mic.popObject();
        assertEquals(second, poppedSecond, "First pop should return the second object pushed");
        assertEquals(first, poppedFirst, "Second pop should return the first object pushed");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping all elements");
    }

    @ParameterizedTest(name = "[{index}] pushObject with payload: {0}")
    @MethodSource("extendedPushProvider")
    void pushObject_extended(Object payload) {
        // GIVEN
        assertTrue(mic.isObjectStackEmpty(), "Pre‑condition: stack must be empty");
        // WHEN
        mic.pushObject(payload);
        // THEN
        assertFalse(mic.isObjectStackEmpty(), "Stack must contain the pushed element");
        assertEquals(payload, mic.peekObject(), "peekObject() should return the exact instance that was pushed");
        assertEquals(1, mic.getObjectStack().size(), "After a single push the size must be 1");
        // POP and ensure the same reference/value is returned
        Object popped = mic.popObject();
        assertEquals(payload, popped, "popObject() must return the same object/value that was pushed");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after the pop");
    }

    @Test
    void pushObject_duplicateValuesMaintainDistinctEntries() {
        // GIVEN
        String duplicate = "duplicate";
        // WHEN – push the same value twice
        mic.pushObject(duplicate);
        mic.pushObject(duplicate);
        // THEN – the stack should contain two entries
        assertEquals(2, mic.getObjectStack().size(), "Stack size must be 2 after pushing the same value twice");
        assertEquals(duplicate, mic.peekObject(), "Top of the stack must be the duplicate value from the second push");
        // POP twice and verify both returns are equal to the original value
        Object firstPop = mic.popObject();
        Object secondPop = mic.popObject();
        assertEquals(duplicate, firstPop, "First pop should return the value from the second push");
        assertEquals(duplicate, secondPop, "Second pop should return the value from the first push");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after both pops");
    }

    @Test
    void pushObject_stressTestWithHundredElements() {
        // GIVEN – push 100 distinct strings
        int count = 100;
        for (int i = 0; i < count; i++) {
            mic.pushObject("element-" + i);
        }
        // THEN – size must be 100 and top must be the last element
        assertEquals(count, mic.getObjectStack().size(), "Stack size should be 100 after pushing 100 elements");
        assertEquals("element-99", mic.peekObject(), "Top of the stack must be the last pushed element");
        // POP all elements and verify reverse order
        for (int i = count - 1; i >= 0; i--) {
            Object popped = mic.popObject();
            assertEquals("element-" + i, popped, "Popped element should follow reverse insertion order");
        }
        // Finally the stack must be empty
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping all 100 elements");
    }

    @Test
    void pushObject_whenInternalStackIsNull_throwsNullPointerException() throws Exception {
        // GIVEN – force the private field objectStack to null via reflection
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        // Explicitly set to null to simulate a broken internal state
        stackField.set(mic, null);
        // WHEN / THEN – invoking pushObject must throw NPE because it dereferences a null stack
        assertThrows(NullPointerException.class, () -> mic.pushObject("anyValue"), "pushObject should throw NullPointerException when internal objectStack is null");
    }
}
