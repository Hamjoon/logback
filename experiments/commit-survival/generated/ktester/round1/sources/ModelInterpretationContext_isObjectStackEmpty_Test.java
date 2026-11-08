package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_isObjectStackEmpty_Test {

    @Mock
    Context // Context has no behavior needed for these tests
    mockContext;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        mic = null;
    }

    @Test
    void isObjectStackEmpty_ShouldReturnTrue_WhenStackIsInitiallyEmpty() {
        // GIVEN: a newly created ModelInterpretationContext (objectStack is empty)
        // WHEN
        boolean empty = mic.isObjectStackEmpty();
        // THEN
        assertTrue(empty, "Object stack should be reported as empty immediately after construction");
    }

    @Test
    void isObjectStackEmpty_ShouldReturnFalse_AfterPushingAnObject() {
        // GIVEN: a ModelInterpretationContext with one object pushed onto the stack
        Object dummy = new Object();
        mic.pushObject(dummy);
        // WHEN
        boolean empty = mic.isObjectStackEmpty();
        // THEN
        assertFalse(empty, "Object stack should not be empty after an object has been pushed");
    }

    @Test
    void isObjectStackEmpty_ShouldReturnTrue_AfterPushingAndPoppingAnObject() {
        // GIVEN: push then pop an object, returning the stack to its original state
        Object dummy = new Object();
        mic.pushObject(dummy);
        // should be the same dummy
        Object popped = mic.popObject();
        assertEquals(dummy, popped, "Popped object should be the one that was pushed");
        // WHEN
        boolean empty = mic.isObjectStackEmpty();
        // THEN
        assertTrue(empty, "Object stack should be empty after the only element has been popped");
    }
}
