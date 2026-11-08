package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekModel_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // logback Context – no behaviour needed for these tests
    mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test does not depend on any external state besides the Context.
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Verifies that {@code peekModel()} returns the model that is on the top of the
     * internal {@code modelStack} without removing it.
     */
    @Test
    void peekModel_returnsTopModel_withoutRemovingIt() {
        // given
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        // second is now on top of the stack
        mic.pushModel(second);
        // when
        Model peeked = mic.peekModel();
        // then
        assertSame(second, peeked, "peekModel should return the model that was pushed last");
        // the stack size must remain unchanged (still 2)
        assertFalse(mic.isModelStackEmpty(), "modelStack should not be empty after peek");
        // pop to verify order
        assertSame(second, mic.popModel(), "popModel should still return the same top element");
        assertSame(first, mic.popModel(), "next pop should return the previously pushed element");
    }

    /**
     * Verifies that {@code peekModel()} throws {@link EmptyStackException}
     * when the model stack is empty, mirroring the behaviour of {@link java.util.Stack#peek()}.
     */
    @Test
    void peekModel_throwsEmptyStackException_whenStackIsEmpty() {
        // given: a freshly created ModelInterpretationContext has an empty modelStack
        // when / then
        assertThrows(EmptyStackException.class, mic::peekModel, "peekModel should throw EmptyStackException when no model is present");
    }

    /**
     * Null handling – the stack can hold a {@code null} reference.
     */
    @Test
    void peekModel_returnsNull_whenNullModelPushed() {
        // given
        mic.pushModel(null);
        // when
        Model peeked = mic.peekModel();
        // then
        assertNull(peeked, "peekModel should return null when a null model is on top of the stack");
        assertFalse(mic.isModelStackEmpty(), "Stack should not be considered empty after pushing null");
    }

    /**
     * Multiple successive calls to {@code peekModel()} must return the same instance
     * and must not modify the stack.
     */
    @Test
    void peekModel_multipleSuccessiveCalls_returnSameInstance() {
        // given
        Model model = mock(Model.class);
        mic.pushModel(model);
        // when / then
        Model firstPeek = mic.peekModel();
        Model secondPeek = mic.peekModel();
        Model thirdPeek = mic.peekModel();
        assertSame(model, firstPeek, "First peek should return the pushed model");
        assertSame(model, secondPeek, "Second peek should return the same instance");
        assertSame(model, thirdPeek, "Third peek should also return the same instance");
        // ensure stack size unchanged
        assertFalse(mic.isModelStackEmpty(), "Stack must remain non‑empty after multiple peeks");
        assertSame(model, mic.popModel(), "popModel should still return the original model");
    }

    /**
     * After a {@code popModel()} operation, {@code peekModel()} must reflect the new top.
     */
    @Test
    void peekModel_returnsNewTopAfterPop() {
        // given
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        mic.pushModel(second);
        // remove the top element
        assertSame(second, mic.popModel(), "popModel should remove the top element");
        // when
        Model peeked = mic.peekModel();
        // then
        assertSame(first, peeked, "peekModel should now return the remaining model (first)");
    }

    /**
     * Operations on the {@code objectStack} must not affect {@code modelStack}.
     */
    @Test
    void peekModel_ignoresObjectStackManipulations() {
        // given
        Model model = mock(Model.class);
        mic.pushModel(model);
        mic.pushObject("string");
        mic.pushObject(42);
        mic.pushObject(new Object());
        // when
        Model peeked = mic.peekModel();
        // then
        assertSame(model, peeked, "peekModel must still return the model regardless of objectStack changes");
    }

    /**
     * Verify that {@code peekModel()} works correctly when the stack contains
     * a large number of elements (e.g., 1,000).
     */
    @Test
    void peekModel_handlesLargeStackSize() {
        final int COUNT = 1000;
        Model lastModel = null;
        // push COUNT distinct mock models
        for (int i = 0; i < COUNT; i++) {
            Model m = mock(Model.class, "model-" + i);
            mic.pushModel(m);
            lastModel = m;
        }
        // when
        Model peeked = mic.peekModel();
        // then
        assertSame(lastModel, peeked, "peekModel should return the last model pushed (model #1000)");
        // sanity check – stack size should still be COUNT
        for (int i = 0; i < COUNT; i++) {
            assertNotNull(mic.popModel(), "All models should still be present after peek");
        }
        assertTrue(mic.isModelStackEmpty(), "Stack must be empty after popping all elements");
    }

    /**
     * Changing the {@code topModel} field must not influence {@code peekModel()},
     * which relies solely on {@code modelStack}.
     */
    @Test
    void peekModel_ignoresTopModelField() {
        // given
        Model pushed = mock(Model.class);
        Model differentTop = mock(Model.class);
        mic.pushModel(pushed);
        // manipulate the unrelated field
        mic.setTopModel(differentTop);
        // when
        Model peeked = mic.peekModel();
        // then
        assertSame(pushed, peeked, "peekModel should return the model from the stack, not the topModel field");
    }

    /**
     * Simulate a corrupted internal state by setting {@code modelStack} to {@code null}
     * via reflection. The method should then throw a {@link NullPointerException}.
     */
    @Test
    void peekModel_throwsNullPointerException_whenInternalStackIsNull() throws Exception {
        // given: use reflection to nullify the private field
        Field stackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // when / then
        assertThrows(NullPointerException.class, mic::peekModel, "peekModel should throw NullPointerException when internal modelStack is null");
    }
}
