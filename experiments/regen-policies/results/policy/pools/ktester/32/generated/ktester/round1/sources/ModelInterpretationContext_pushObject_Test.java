package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Stack;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushObject_Test {

    @Mock
    Context // mocked Context – no real behaviour needed
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @Mock
    Model // a mock Model instance for the Model‑specific test
    mockModel;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for every test so that stacks start empty
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Provides the different input objects for the generic pushObject tests.
     *
     * <ul>
     *   <li>Normal non‑null object</li>
     *   <li>Explicit {@code null} value</li>
     *   <li>Two‑step LIFO verification (handled in a separate test)</li>
     *   <li>Simple check that {@code isObjectStackEmpty()} becomes {@code false}</li>
     * </ul>
     */
    private static Stream<Arguments> pushObjectProvider() {
        return Stream.of(arguments("sampleObject", false, "sampleObject"), arguments(null, false, null));
    }

    @Test
    void pushObjectAddsObjectToStack() {
        // given
        String expected = "testObject";
        // when
        mic.pushObject(expected);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Object stack should not be empty after push");
        assertEquals(expected, mic.peekObject(), "Top of the stack should be the pushed object");
    }

    @Test
    void pushObjectMultipleElementsPreserveLifoOrder() {
        // given
        String first = "first";
        String second = "second";
        // when
        mic.pushObject(first);
        mic.pushObject(second);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Object stack should contain elements");
        assertEquals(second, mic.peekObject(), "Top of the stack must be the last pushed object");
        // Verify internal order via the public accessor getObjectStack()
        // Stack extends Vector – index 0 is the bottom of the stack
        Stack<Object> stack = mic.getObjectStack();
        assertEquals(first, stack.get(0), "First element should be at index 0");
        assertEquals(second, stack.get(1), "Second element should be at index 1");
        // Pop to ensure LIFO behaviour
        assertEquals(second, mic.popObject(), "popObject() should return the last pushed element");
        assertEquals(first, mic.popObject(), "popObject() should then return the first element");
        assertTrue(mic.isObjectStackEmpty(), "Stack must be empty after popping all elements");
    }

    @Test
    void pushObjectDoesNotAffectModelStack() {
        // given
        String obj = "someObject";
        // when
        mic.pushObject(obj);
        // then
        assertTrue(mic.isModelStackEmpty(), "Model stack must remain unchanged after pushing an object");
    }

    @Test
    void pushObjectAcceptsNullValue() {
        // when
        mic.pushObject(null);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Stack should contain a null entry after pushing null");
        assertNull(mic.peekObject(), "Top of the stack should be null when a null value was pushed");
    }

    @ParameterizedTest(name = "pushObject({0}) → stack not empty, peek = {2}")
    @MethodSource("pushObjectProvider")
    void pushObjectAddsObjectToStackParameterized(Object input, boolean expectEmpty, Object expectedPeek) {
        // when
        mic.pushObject(input);
        // then
        assertEquals(expectEmpty, mic.isObjectStackEmpty(), "isObjectStackEmpty() should reflect the stack state after push");
        assertEquals(expectedPeek, mic.peekObject(), "peekObject() must return the object that was pushed");
    }

    @Test
    void pushObjectIsObjectStackEmptyReflectsState() {
        // when
        mic.pushObject("any");
        // then
        assertFalse(mic.isObjectStackEmpty(), "isObjectStackEmpty() must return false after a successful push");
    }

    @Test
    void pushObjectThrowsNullPointerExceptionWhenInternalStackIsNull() throws Exception {
        // Force the internal objectStack field to null via reflection
        Field objectStackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        objectStackField.setAccessible(true);
        objectStackField.set(mic, null);
        // Expect a NullPointerException because pushObject invokes push on a null Stack
        assertThrows(NullPointerException.class, () -> mic.pushObject("trigger"), "Calling pushObject when internal objectStack is null should throw NPE");
    }

    @Test
    void pushObjectAcceptsModelImplementation() {
        // when
        mic.pushObject(mockModel);
        // then
        assertFalse(mic.isObjectStackEmpty(), "Object stack must not be empty after pushing a Model instance");
        assertSame(mockModel, mic.peekObject(), "peekObject() should return the exact Model instance that was pushed");
        assertTrue(mic.peekObject() instanceof Model, "The pushed object should be recognised as an instance of Model");
    }
}
