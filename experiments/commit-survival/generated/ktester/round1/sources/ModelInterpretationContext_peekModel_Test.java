package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_peekModel_Test {

    /**
     * Mocked {@link Context} required by the {@link ModelInterpretationContext} constructor.
     */
    @Mock
    Context mockContext;

    /**
     * Instance under test, created fresh for each test method.
     */
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // a Mockito mock is sufficient.
        mic = new ModelInterpretationContext(mockContext);
        // The constructor under test does not need a real Context implementation,
        // The class has a no‑arg constructor; we then inject the mock Context.
        mic = new ModelInterpretationContext();
        mic.setContext(mockContext);
    }

    @Test
    void peekModel_returnsTopModelWithoutRemovingIt() {
        // GIVEN: two distinct mock models are pushed; the second one becomes the top.
        Model first = mock(Model.class);
        Model second = mock(Model.class);
        mic.pushModel(first);
        mic.pushModel(second);
        // WHEN: we peek at the model stack.
        Model peeked = mic.peekModel();
        // THEN: the returned instance must be the same as the top element.
        assertSame(second, peeked, "peekModel should return the model that is on top of the stack");
        // The stack must still contain both models – a subsequent pop should
        // return the same top model we just peeked at.
        assertSame(second, mic.popModel(), "popModel should still return the same top model after peek");
        assertSame(first, mic.popModel(), "after removing the top, the next model should be the first one pushed");
        assertTrue(mic.isModelStackEmpty(), "modelStack should be empty after popping both models");
    }

    @Test
    void peekModel_throwsEmptyStackException_whenStackIsEmpty() {
        // GIVEN: a freshly created {@link ModelInterpretationContext} has an empty stack.
        // WHEN / THEN: calling peekModel() must propagate EmptyStackException.
        assertThrows(EmptyStackException.class, mic::peekModel, "peekModel should throw EmptyStackException when the model stack is empty");
    }
}
