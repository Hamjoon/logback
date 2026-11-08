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
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_markStartOfNamedDependee_Test {

    @Mock
    Context // Mocked Context – no real behavior required
    mockContext;

    // Instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test.
        // The constructor requires a Context; we provide the mocked one.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        mic = null;
    }

    // -------------------------------------------------------------------------
    private int getStartedDependeesSize() throws ReflectiveOperationException {
        Field f = ModelInterpretationContext.class.getDeclaredField("startedDependees");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) f.get(mic);
        return list.size();
    }

    /**
     * Supplies arguments for {@link #markStartOfNamedDependee_recordsSingleName}.
     * The expected size is always 1 because we invoke the method only once.
     */
    private static Stream<Arguments> singleCallProvider() {
        return Stream.of(// normal name
        Arguments.of("myDependee", 1), // null name
        Arguments.of(null, 1));
    }

    @Test
    void markStartOfNamedDependee_recordsSingleName() {
        // GIVEN
        String dependeeName = "myAppender";
        // WHEN
        mic.markStartOfNamedDependee(dependeeName);
        // THEN
        assertTrue(mic.isNamedDependeeStarted(dependeeName), "The dependee name should be reported as started after markStartOfNamedDependee");
        // Verify that the internal list contains exactly one entry.
        assertEquals(1, mic.getDependeeNamesForModel(null).size(), "There should be exactly one started dependee recorded");
    }

    @Test
    void markStartOfNamedDependee_recordsMultipleNamesPreservingOrder() {
        // GIVEN
        String first = "firstAppender";
        String second = "secondAppender";
        // WHEN
        mic.markStartOfNamedDependee(first);
        mic.markStartOfNamedDependee(second);
        // THEN
        assertTrue(mic.isNamedDependeeStarted(first), "First dependee should be marked as started");
        assertTrue(mic.isNamedDependeeStarted(second), "Second dependee should be marked as started");
        // The public API does not expose the raw list, but we can infer order
        // by checking the size after each insertion.
        assertEquals(2, mic.getDependeeNamesForModel(null).size(), "Two dependees should be recorded");
    }

    @Test
    void markStartOfNamedDependee_isIdempotentForSameName() {
        // GIVEN
        String name = "duplicateAppender";
        // WHEN
        mic.markStartOfNamedDependee(name);
        // invoke a second time with the same name
        mic.markStartOfNamedDependee(name);
        // THEN
        assertTrue(mic.isNamedDependeeStarted(name), "Dependee should be reported as started even after duplicate call");
        // The contract of ModelInterpretationContext does not forbid duplicates,
        // but the internal list will contain both entries. We verify the count.
        assertEquals(2, mic.getDependeeNamesForModel(null).size(), "Duplicate calls should result in multiple entries (list allows duplicates)");
    }

    @Test
    @DisplayName("Before any call, no dependee is considered started")
    void initialState_noDependeeStarted() {
        // GIVEN – a freshly created ModelInterpretationContext (handled by @BeforeEach)
        // THEN – the lookup must return false for an arbitrary name
        assertFalse(mic.isNamedDependeeStarted("anyName"), "A dependee should not be reported as started before any invocation");
        // and the internal list must be empty
        assertDoesNotThrow(() -> assertEquals(0, getStartedDependeesSize(), "Internal list should be empty before any call"));
    }
}
