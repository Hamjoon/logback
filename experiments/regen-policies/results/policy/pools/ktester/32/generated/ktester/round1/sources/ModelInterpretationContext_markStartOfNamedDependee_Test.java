package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_markStartOfNamedDependee_Test {

    @Mock
    Context // the Context required by the constructor
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test needs a Context; we provide a Mockito mock.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the instance reference to help GC; not strictly required.
        mic = null;
    }

    /* --------------------------------------------------------------------- *
     * Parameterized tests for the first group of JSON cases                *
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> basicCasesProvider() {
        return Stream.of(// case 1: single regular name
        Arguments.of(new String[] { "serviceA" }, List.of("serviceA")), // case 2: second distinct name after first
        Arguments.of(new String[] { "serviceA", "serviceB" }, List.of("serviceA", "serviceB")), // case 3: null name
        Arguments.of(new String[] { null }, List.of((String) null)), // case 4: empty string
        Arguments.of(new String[] { "" }, List.of("")), // case 5: duplicate name
        Arguments.of(new String[] { "serviceA", "serviceA" }, List.of("serviceA", "serviceA")));
    }

    /* --------------------------------------------------------------------- *
     * Parameterized tests for the additional JSON cases                     *
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> additionalCasesProvider() {
        return Stream.of(// very long string
        Arguments.of(new String[] { "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" }, List.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")), // leading/trailing whitespace
        Arguments.of(new String[] { "  service X  " }, List.of("  service X  ")), // special characters
        Arguments.of(new String[] { "!@#$%^&*()_+" }, List.of("!@#$%^&*()_+")), // Unicode characters
        Arguments.of(new String[] { "服务" }, List.of("服务")), // unknown query after adding two distinct dependees
        Arguments.of(new String[] { "serviceC", "serviceD" }, // expected list
        List.of("serviceC", "serviceD")), // mixed null among other entries
        Arguments.of(new String[] { "serviceE", null, "serviceF" }, List.of("serviceE", null, "serviceF")));
    }

    @Test
    void markStartOfNamedDependee_addsNameToStartedDependees() {
        // given
        String dependeeName = "myDependee";
        // when
        mic.markStartOfNamedDependee(dependeeName);
        // then
        assertTrue(mic.isNamedDependeeStarted(dependeeName), "The dependee name should be recorded as started after markStartOfNamedDependee");
    }

    @Test
    void markStartOfNamedDependee_multipleCalls_accumulateNamesInOrder() {
        // given
        String first = "firstDep";
        String second = "secondDep";
        // when
        mic.markStartOfNamedDependee(first);
        mic.markStartOfNamedDependee(second);
        // then
        assertTrue(mic.isNamedDependeeStarted(first), "First dependee should be marked as started");
        assertTrue(mic.isNamedDependeeStarted(second), "Second dependee should be marked as started");
    }

    @Test
    void markStartOfNamedDependee_idempotentForSameName() {
        // given
        String name = "duplicateDep";
        // when
        mic.markStartOfNamedDependee(name);
        // call again with the same name
        mic.markStartOfNamedDependee(name);
        // then
        // The internal list may contain duplicates, but the public query should still return true.
        assertTrue(mic.isNamedDependeeStarted(name), "Dependee should be reported as started even after duplicate calls");
    }

    @Test
    void markStartOfNamedDependee_doesNotAffectOtherState() {
        // given
        String name = "someDep";
        // Capture initial state of a few other properties that should stay unchanged.
        int initialObjectStackSize = mic.getObjectStack().size();
        // using objectStack for illustration
        int initialModelStackSize = mic.getObjectStack().size();
        // when
        mic.markStartOfNamedDependee(name);
        // then
        assertEquals(initialObjectStackSize, mic.getObjectStack().size(), "Object stack size must remain unchanged after marking a dependee");
        assertEquals(initialModelStackSize, mic.getObjectStack().size(), "Model stack size must remain unchanged after marking a dependee");
        assertTrue(mic.isNamedDependeeStarted(name));
    }

    /* --------------------------------------------------------------------- *
     * Helper to access the private startedDependees list via reflection      *
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private List<String> getStartedDependeesList() {
        try {
            Field f = ModelInterpretationContext.class.getDeclaredField("startedDependees");
            f.setAccessible(true);
            return (List<String>) f.get(mic);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access startedDependees field via reflection", e);
            // unreachable
            return new ArrayList<>();
        }
    }

    @ParameterizedTest(name = "basic case #{index}")
    @MethodSource("basicCasesProvider")
    void markStartOfNamedDependee_basicScenarios(String[] namesToAdd, List<String> expectedList) {
        // Add each name in order
        for (String name : namesToAdd) {
            mic.markStartOfNamedDependee(name);
        }
        // Verify the internal list reflects exactly what we expect
        List<String> actual = getStartedDependeesList();
        assertEquals(expectedList.size(), actual.size(), "startedDependees list size should match expected");
        assertIterableEquals(expectedList, actual, "startedDependees list contents and order should match expected");
        // Verify isNamedDependeeStarted returns true for each added name (including null)
        for (String name : namesToAdd) {
            assertTrue(mic.isNamedDependeeStarted(name), "isNamedDependeeStarted should be true for added name: " + name);
        }
    }

    @ParameterizedTest(name = "additional case #{index}")
    @MethodSource("additionalCasesProvider")
    void markStartOfNamedDependee_additionalScenarios(String[] namesToAdd, List<String> expectedList) {
        // Add each name in order
        for (String name : namesToAdd) {
            mic.markStartOfNamedDependee(name);
        }
        // Verify internal list size and order
        List<String> actual = getStartedDependeesList();
        assertEquals(expectedList.size(), actual.size(), "startedDependees list size should match expected");
        assertIterableEquals(expectedList, actual, "startedDependees list contents and order should match expected");
        // Verify isNamedDependeeStarted for each added name
        for (String name : namesToAdd) {
            assertTrue(mic.isNamedDependeeStarted(name), "isNamedDependeeStarted should be true for added name: " + name);
        }
        // Additional verification for the "unknown query" scenario (case index 4)
        if (expectedList.equals(List.of("serviceC", "serviceD"))) {
            assertFalse(mic.isNamedDependeeStarted("serviceX"), "isNamedDependeeStarted should return false for a name never added");
        }
    }
}
