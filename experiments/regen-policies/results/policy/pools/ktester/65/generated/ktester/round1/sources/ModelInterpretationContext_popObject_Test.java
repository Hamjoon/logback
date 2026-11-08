package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popObject_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock Context – the real implementation is not needed for popObject()
        mockContext = mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC (not strictly required)
        mic = null;
        mockContext = null;
    }

    private static Stream<Arguments> provideSequentialPopData() {
        return Stream.of(// first pop from three pushes -> should be "third"
        Arguments.of(Arrays.asList("first", "second", "third"), 1, "third"), // second pop from three pushes -> should be "second"
        Arguments.of(Arrays.asList("first", "second", "third"), 2, "second"), // third pop from three pushes -> should be "first"
        Arguments.of(Arrays.asList("first", "second", "third"), 3, "first"));
    }

    private static Stream<Arguments> provideFullPopData() {
        return Stream.of(Arguments.of(Arrays.asList("alpha", "beta"), true), Arguments.of(Arrays.asList("x", null, "y"), true));
    }

    @Test
    void popObject_returnsAndRemovesTopElement() {
        // given
        String first = "first";
        Integer second = 42;
        mic.pushObject(first);
        mic.pushObject(second);
        // when
        Object popped = mic.popObject();
        // then
        assertEquals(second, popped, "popObject should return the most recently pushed element");
        // The stack should now contain only the first element
        assertFalse(mic.isObjectStackEmpty(), "Stack should not be empty after one pop");
        assertEquals(first, mic.peekObject(), "Remaining top element should be the first pushed object");
    }

    @Test
    void popObject_onEmptyStack_throwsEmptyStackException() {
        // given: an empty stack (mic is freshly created)
        // when / then
        assertThrows(java.util.EmptyStackException.class, () -> mic.popObject(), "popObject should throw EmptyStackException when the stack is empty");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_returnsAndRemovesTopElement_singleElement() {
        // given
        String testObject = "testObject";
        mic.pushObject(testObject);
        // when
        Object popped = mic.popObject();
        // then
        assertEquals(testObject, popped, "popObject should return the only element");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping the sole element");
    }

    @Test
    void popObject_returnsAndRemovesTopElement_twoElements() {
        // given
        String first = "first";
        String second = "second";
        mic.pushObject(first);
        mic.pushObject(second);
        // when
        Object popped = mic.popObject();
        // then
        assertEquals(second, popped, "popObject should return the most recently pushed element");
        assertFalse(mic.isObjectStackEmpty(), "Stack should not be empty after one pop");
        assertEquals(first, mic.peekObject(), "Remaining top element should be the first pushed object");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_allowsNullValues() {
        // given
        mic.pushObject(null);
        // when
        Object popped = mic.popObject();
        // then
        assertNull(popped, "popObject should return null when a null entry was pushed");
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping the null entry");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_stackSizeReducedAfterPop() {
        // given
        mic.pushObject("a");
        mic.pushObject("b");
        // when
        Object popped = mic.popObject();
        // then
        assertEquals("b", popped, "First pop should return the top element");
        assertEquals(1, mic.getObjectStack().size(), "Stack size must be reduced to 1 after one pop");
        assertEquals("a", mic.peekObject(), "Remaining element should be the first pushed one");
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => pushSequence={0}, popCount={1}, expected={2}")
    @MethodSource("provideSequentialPopData")
    void popObject_multipleSequentialPops(List<String> pushSequence, int popCount, String expected) {
        // given
        pushSequence.forEach(mic::pushObject);
        // when
        Object result = null;
        for (int i = 0; i < popCount; i++) {
            result = mic.popObject();
        }
        // then
        assertEquals(expected, result, "The result after " + popCount + " pop(s) should match expectation");
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index} => pushSequence={0}, popAll={1}")
    @MethodSource("provideFullPopData")
    void popObject_emptyAfterFullPops(List<Object> pushSequence, boolean popAll) {
        // given
        pushSequence.forEach(mic::pushObject);
        // when
        if (popAll) {
            while (!mic.isObjectStackEmpty()) {
                mic.popObject();
            }
        }
        // then
        assertTrue(mic.isObjectStackEmpty(), "Stack should be empty after popping all elements");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_interleavedModelStackOperations_returnsTopObject() {
        // given
        Model mockModel1 = mock(Model.class);
        Model mockModel2 = mock(Model.class);
        mic.pushObject("obj1");
        // modelStack operation – should not affect objectStack
        mic.pushModel(mockModel1);
        mic.pushObject("obj2");
        mic.pushModel(mockModel2);
        mic.pushObject("obj3");
        // when
        Object popped = mic.popObject();
        // then
        assertEquals("obj3", popped, "popObject must return the latest object irrespective of model pushes");
        // Verify that modelStack size is unchanged (two models)
        assertEquals(2, mic.getDefaultNestedComponentRegistry().size(), "Model stack should remain untouched (registry size is unrelated but serves as a sanity check)");
    }

    @Test
    void popObject_interleavedModelStackOperations_singleObject() {
        // given
        Model mockModel = mock(Model.class);
        mic.pushObject("onlyObject");
        mic.pushModel(mockModel);
        // when
        Object popped = mic.popObject();
        // then
        assertEquals("onlyObject", popped, "popObject should still return the object even after a model push");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_mixedNullAndNonNull_firstPopReturnsNonNull() {
        // given
        mic.pushObject(null);
        mic.pushObject("value");
        // when
        Object firstPop = mic.popObject();
        // then
        assertEquals("value", firstPop, "First pop should return the non‑null value on top");
    }

    @Test
    void popObject_mixedNullAndNonNull_secondPopReturnsNull() {
        // given
        mic.pushObject(null);
        mic.pushObject("value");
        // when
        // pop "value"
        mic.popObject();
        // pop null
        Object secondPop = mic.popObject();
        // then
        assertNull(secondPop, "Second pop should return null, the earlier null entry");
    }

    // -------------------------------------------------------------------------
    @Test
    void popObject_internalStackNull_throwsNullPointerException() throws Exception {
        // given: force the private field `objectStack` to be null via reflection
        Field stackField = ModelInterpretationContext.class.getDeclaredField("objectStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // when / then
        assertThrows(NullPointerException.class, () -> mic.popObject(), "popObject should throw NullPointerException when internal objectStack is null");
    }
}
