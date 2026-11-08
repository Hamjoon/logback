package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_markStartOfNamedDependee_Test {

    private ModelInterpretationContext mic;

    private Context mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // The real ModelInterpretationContext expects a Context instance.
        // A simple mock is sufficient because the method under test does not use it.
        mockContext = org.mockito.Mockito.mock(Context.class);
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void markStartOfNamedDependee_AddsNameToStartedDependees() {
        // given
        String dependeeName = "myDep";
        // pre‑condition: the dependee has not been started yet
        assertFalse(mic.isNamedDependeeStarted(dependeeName), "Dependee should not be marked as started before the call");
        // when
        mic.markStartOfNamedDependee(dependeeName);
        // then
        assertTrue(mic.isNamedDependeeStarted(dependeeName), "Dependee should be marked as started after the call");
        assertEquals(1, getStartedDependeesList().size(), "Internal list size should be 1 after a single addition");
    }

    @Test
    void markStartOfNamedDependee_AllowsMultipleCalls() {
        // given
        String first = "firstDep";
        String second = "secondDep";
        // when
        mic.markStartOfNamedDependee(first);
        mic.markStartOfNamedDependee(second);
        // then
        assertTrue(mic.isNamedDependeeStarted(first), "First dependee should be started");
        assertTrue(mic.isNamedDependeeStarted(second), "Second dependee should be started");
        assertEquals(2, getStartedDependeesList().size(), "Internal list size should reflect both additions");
    }

    @Test
    void markStartOfNamedDependee_AllowsNullName() {
        // when
        mic.markStartOfNamedDependee(null);
        // then
        assertTrue(mic.isNamedDependeeStarted(null), "Null name should be recorded as a started dependee");
        assertEquals(1, getStartedDependeesList().size(), "Internal list size should be 1 after adding null");
        assertNull(getStartedDependeesList().get(0), "The stored entry should be null");
    }

    /**
     * Helper that uses reflection to obtain the private {@code startedDependees} list.
     */
    @SuppressWarnings("unchecked")
    private List<String> getStartedDependeesList() {
        try {
            Field field = ModelInterpretationContext.class.getDeclaredField("startedDependees");
            field.setAccessible(true);
            return (List<String>) field.get(mic);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to access startedDependees field", e);
        }
    }

    @Test
    void markStartOfNamedDependee_AllowsEmptyString() {
        // given
        String empty = "";
        // when
        mic.markStartOfNamedDependee(empty);
        // then
        assertTrue(mic.isNamedDependeeStarted(empty), "Empty string should be recorded as a started dependee");
        assertEquals(1, getStartedDependeesList().size(), "Internal list size should be 1 after adding empty string");
        assertEquals(empty, getStartedDependeesList().get(0), "The stored entry should be the empty string");
    }

    @Test
    void markStartOfNamedDependee_AllowsDuplicateNames() {
        // given
        String dup = "serviceA";
        // when
        mic.markStartOfNamedDependee(dup);
        mic.markStartOfNamedDependee(dup);
        // then
        assertTrue(mic.isNamedDependeeStarted(dup), "Dependee should be reported as started after duplicate calls");
        List<String> list = getStartedDependeesList();
        assertEquals(2, list.size(), "Duplicate entries should both be stored, resulting in size 2");
        assertEquals(dup, list.get(0), "First entry should be the first call");
        assertEquals(dup, list.get(1), "Second entry should be the second call");
    }

    @Test
    void markStartOfNamedDependee_PreservesInsertionOrder() {
        // given
        String first = "first";
        String second = "second";
        String third = "third";
        // when
        mic.markStartOfNamedDependee(first);
        mic.markStartOfNamedDependee(second);
        mic.markStartOfNamedDependee(third);
        // then
        List<String> list = getStartedDependeesList();
        assertEquals(3, list.size(), "List should contain three entries");
        assertEquals(first, list.get(0), "First inserted name should be at index 0");
        assertEquals(second, list.get(1), "Second inserted name should be at index 1");
        assertEquals(third, list.get(2), "Third inserted name should be at index 2");
    }
}
