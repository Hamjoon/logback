package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Stack;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getObject_Test {

    /**
     * Mocked Logback context – the only external dependency required for the MIC ctor.
     */
    @Mock
    Context mockContext;

    /**
     * Instance under test.
     */
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test so stack state does not leak between tests.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC and makes the intent clear.
        mic = null;
    }

    private static Stream<Arguments> validIndicesProvider() {
        // placeholder – will be re‑created in test
        Object firstObject = new Object();
        // We cannot reuse the same instance across arguments because the test creates its own.
        // Instead we pass the index and let the test compare against the object that was
        // pushed at that position.
        return Stream.of(// expected will be compared via assertSame after push
        Arguments.of(0, firstObject), Arguments.of(1, "secondObject"));
    }

    private static Stream<Arguments> invalidIndicesProvider() {
        return Stream.of(Arguments.of(-1), // assuming stack size < 5
        Arguments.of(5));
    }

    /**
     * Verifies that {@code getObject(int)} returns the exact object that was
     * previously pushed onto the internal {@code objectStack} at the same index.
     */
    @Test
    void getObject_returnsCorrectObject() {
        // Arrange – push two distinct objects onto the stack.
        Object first = new Object();
        String second = "second-element";
        // index 0
        mic.pushObject(first);
        // index 1
        mic.pushObject(second);
        // Act & Assert – retrieve by index and compare with the original references.
        assertSame(first, mic.getObject(0), "Object at index 0 should be the first pushed instance");
        assertSame(second, mic.getObject(1), "Object at index 1 should be the second pushed instance");
    }

    /**
     * Verifies that calling {@code getObject(int)} with an index that does not exist
     * in the stack results in an {@link IndexOutOfBoundsException}.
     */
    @Test
    void getObject_invalidIndex_throwsException() {
        // The stack is empty at this point; any index is invalid.
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(0), "Calling getObject on an empty stack should throw IndexOutOfBoundsException");
        // Push a single element and request an out‑of‑range index.
        mic.pushObject("only-element");
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(1), "Index greater than stack size should throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Valid indices – parameterized test covering both index 0 and 1 cases *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] getObject({0}) should return the expected object")
    @MethodSource("validIndicesProvider")
    void getObject_validIndices_returnsCorrectObject(int index, Object expected) {
        // Arrange – push two distinct objects so that both indices are valid.
        Object firstObject = new Object();
        String secondObject = "secondObject";
        // index 0
        mic.pushObject(firstObject);
        // index 1
        mic.pushObject(secondObject);
        // Act
        Object actual = mic.getObject(index);
        // Assert
        assertSame(expected, actual, () -> "Object at index " + index + " should be the same instance that was pushed");
    }

    /* --------------------------------------------------------------------- *
     *  Invalid indices – negative and out‑of‑range indices should throw       *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] getObject({0}) should throw IndexOutOfBoundsException")
    @MethodSource("invalidIndicesProvider")
    void getObject_invalidIndices_throwsException(int index) {
        // Arrange – push a single element so that index 0 is valid, others are not.
        mic.pushObject("onlyElement");
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(index), () -> "Calling getObject with index " + index + " should throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Empty stack – any index is invalid                                    *
     * --------------------------------------------------------------------- */
    @Test
    void getObject_emptyStack_throwsException() {
        // The stack is empty; any index should cause IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> mic.getObject(0), "Calling getObject on an empty stack should throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Null object handling – pushing null should be retrievable as null     *
     * --------------------------------------------------------------------- */
    @Test
    void getObject_nullObject_returnsNull() {
        // Arrange – push a null reference.
        mic.pushObject(null);
        // Act
        Object result = mic.getObject(0);
        // Assert
        assertNull(result, "Retrieving a null element from the stack should return null");
    }

    /* --------------------------------------------------------------------- *
     *  After pop – ensure the remaining element is still accessible           *
     * --------------------------------------------------------------------- */
    @Test
    void getObject_afterPop_returnsRemainingObject() {
        // Arrange – push two objects.
        Object firstObject = new Object();
        String secondObject = "secondObject";
        // index 0
        mic.pushObject(firstObject);
        // index 1
        mic.pushObject(secondObject);
        // Act – pop the top (secondObject).
        Object popped = mic.popObject();
        assertSame(secondObject, popped, "Popped element should be the second object");
        // Now index 0 should refer to the first object.
        Object remaining = mic.getObject(0);
        assertSame(firstObject, remaining, "After popping, index 0 should be the first object");
    }

    /* --------------------------------------------------------------------- *
     *  Null internal stack – force objectStack to null via reflection        *
     * --------------------------------------------------------------------- */
    @Test
    void getObject_nullInternalStack_throwsNullPointerException() throws Exception {
        // Arrange – use reflection to set the private field 'objectStack' to null.
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // Act & Assert – expect NullPointerException because the method dereferences the null field.
        assertThrows(NullPointerException.class, () -> mic.getObject(0), "When internal objectStack is null, getObject should throw NullPointerException");
    }
}
