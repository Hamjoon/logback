package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getDependeeNamesForModel_Test {

    /**
     * Mocked Logback {@link Context} required by the MIC constructor.
     */
    @Mock
    Context mockContext;

    /**
     * Instance under test.
     */
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test so that the internal dependency list is empty.
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Helper that creates a {@link DependencyDefinition} mock that returns the supplied
     * {@code depender} and {@code dependeeName}.
     *
     * The real {@code DependencyDefinition} class is part of Logback; using a mock avoids
     * dependence on its concrete implementation.
     */
    private DependencyDefinition mockDependency(Model depender, String dependeeName) {
        DependencyDefinition dd = mock(DependencyDefinition.class);
        when(dd.getDepender()).thenReturn(depender);
        when(dd.getDependee()).thenReturn(dependeeName);
        return dd;
    }

    @Test
    void shouldReturnEmptyListWhenNoDependenciesDefined() {
        // GIVEN
        Model model = mock(Model.class);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(model);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Expected no dependee names when none have been added");
    }

    @Test
    void shouldReturnSingleDependeeNameForMatchingModel() {
        // GIVEN
        Model modelA = mock(Model.class);
        DependencyDefinition dd = mockDependency(modelA, "depA");
        mic.addDependencyDefinition(dd);
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelA);
        // THEN
        assertEquals(1, result.size(), "Exactly one dependee name should be returned");
        assertEquals("depA", result.get(0), "Returned dependee name does not match the one added");
    }

    @Test
    void shouldReturnMultipleDependeeNamesForSameModel() {
        // GIVEN
        Model modelB = mock(Model.class);
        mic.addDependencyDefinition(mockDependency(modelB, "first"));
        mic.addDependencyDefinition(mockDependency(modelB, "second"));
        mic.addDependencyDefinition(mockDependency(modelB, "third"));
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelB);
        // THEN
        assertEquals(3, result.size(), "All three dependee names should be returned");
        assertTrue(result.containsAll(List.of("first", "second", "third")), "Result list must contain all added dependee names");
    }

    @Test
    void shouldIgnoreDefinitionsForOtherModels() {
        // GIVEN
        Model modelX = mock(Model.class);
        Model modelY = mock(Model.class);
        mic.addDependencyDefinition(mockDependency(modelX, "onlyForX"));
        mic.addDependencyDefinition(mockDependency(modelY, "onlyForY"));
        // WHEN
        List<String> resultForX = mic.getDependeeNamesForModel(modelX);
        List<String> resultForY = mic.getDependeeNamesForModel(modelY);
        // THEN
        assertEquals(1, resultForX.size());
        assertEquals("onlyForX", resultForX.get(0));
        assertEquals(1, resultForY.size());
        assertEquals("onlyForY", resultForY.get(0));
    }

    @Test
    void shouldReturnEmptyListWhenModelHasNoRegisteredDependees() {
        // GIVEN
        Model modelWithNoDeps = mock(Model.class);
        // Add a dependency for a *different* model
        Model otherModel = mock(Model.class);
        mic.addDependencyDefinition(mockDependency(otherModel, "someDep"));
        // WHEN
        List<String> result = mic.getDependeeNamesForModel(modelWithNoDeps);
        // THEN
        assertTrue(result.isEmpty(), "Expected empty list when the model has no dependee definitions");
    }

    @Test
    void nullModelWithEmptyDependencyListReturnsEmptyList() {
        // Dependency list is empty by default.
        List<String> result = mic.getDependeeNamesForModel(null);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Null model with empty list should yield empty result");
    }

    @Test
    void nullModelMatchesNullDependerEntry() {
        // Add a definition whose depender is null and dependee is "nullDependee".
        DependencyDefinition dd = mockDependency(null, "nullDependee");
        mic.addDependencyDefinition(dd);
        List<String> result = mic.getDependeeNamesForModel(null);
        assertEquals(List.of("nullDependee"), result, "Null depender should match null model argument");
    }

    @Test
    void nullDependeeIsReturnedInResult() {
        Model modelA = mock(Model.class);
        DependencyDefinition dd = mockDependency(modelA, null);
        mic.addDependencyDefinition(dd);
        List<String> result = mic.getDependeeNamesForModel(modelA);
        assertEquals(1, result.size());
        assertNull(result.get(0), "Dependee name null should be present in the result list");
    }

    @Test
    void duplicateDependeeNamesArePreservedInOrder() {
        Model modelA = mock(Model.class);
        mic.addDependencyDefinition(mockDependency(modelA, "dep1"));
        mic.addDependencyDefinition(mockDependency(modelA, "dep1"));
        List<String> result = mic.getDependeeNamesForModel(modelA);
        assertEquals(List.of("dep1", "dep1"), result, "Both duplicate entries must be present preserving insertion order");
    }

    @Test
    void referenceEqualityVsEquals_NoMatchWhenDifferentInstances() {
        // Two distinct Model instances with identical logical state (mocked equally).
        Model modelA = mock(Model.class);
        Model modelB = mock(Model.class);
        // Ensure equals() returns true to highlight that the method uses '=='.
        when(modelA.equals(modelB)).thenReturn(true);
        when(modelB.equals(modelA)).thenReturn(true);
        mic.addDependencyDefinition(mockDependency(modelA, "dep"));
        List<String> result = mic.getDependeeNamesForModel(modelB);
        assertTrue(result.isEmpty(), "Method uses reference equality; different instances should not match even if equals() is true");
    }

    @Test
    void returnedListIsMutableAndDoesNotAffectSubsequentCalls() {
        Model modelA = mock(Model.class);
        mic.addDependencyDefinition(mockDependency(modelA, "dep1"));
        List<String> firstCall = mic.getDependeeNamesForModel(modelA);
        // Mutate the returned list
        firstCall.add("extra");
        // Second call should not see the mutation
        List<String> secondCall = mic.getDependeeNamesForModel(modelA);
        assertEquals(List.of("dep1"), secondCall, "Each invocation must return a fresh list; modifications must not leak");
    }

    @Test
    void largeNumberOfDependenciesReturnedInInsertionOrder() {
        Model modelA = mock(Model.class);
        for (int i = 0; i < 10; i++) {
            mic.addDependencyDefinition(mockDependency(modelA, "dep" + i));
        }
        List<String> result = mic.getDependeeNamesForModel(modelA);
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add("dep" + i);
        }
        assertEquals(expected, result, "All ten dependee names must be returned in insertion order");
    }

    @Test
    void nullElementInDependencyDefinitionListCausesNullPointerException() {
        // Directly add a null entry via the public API.
        mic.addDependencyDefinition(null);
        Model modelA = mock(Model.class);
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(modelA), "Iterating over a list containing null should throw NPE when dd.getDepender() is invoked");
    }

    @Test
    void internalDependencyListSetToNullCausesNullPointerException() throws Exception {
        // Use reflection to set the private field to null.
        Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
        listField.setAccessible(true);
        listField.set(mic, null);
        assertThrows(NullPointerException.class, () -> mic.getDependeeNamesForModel(null), "If the internal list is null, a NPE should be thrown when the method attempts to iterate");
    }

    @Test
    void concurrentModificationDuringIterationThrowsConcurrentModificationException() throws Exception {
        Model modelA = mock(Model.class);
        // Populate with a few entries so that iteration takes a little time.
        for (int i = 0; i < 5; i++) {
            mic.addDependencyDefinition(mockDependency(modelA, "initial" + i));
        }
        // Latch to make sure the main thread is inside the iteration.
        CountDownLatch startIterating = new CountDownLatch(1);
        CountDownLatch modifyDone = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Wrap the original method call to signal when iteration begins.
        Callable<List<String>> task = () -> {
            // Use reflection to replace the list with a subclass that signals on iterator creation.
            Field listField = ModelInterpretationContext.class.getDeclaredField("dependencyDefinitionList");
            listField.setAccessible(true);
            List<DependencyDefinition> original = (List<DependencyDefinition>) listField.get(mic);
            List<DependencyDefinition> signallingList = new AbstractList<>() {

                @Override
                public DependencyDefinition get(int index) {
                    return original.get(index);
                }

                @Override
                public int size() {
                    return original.size();
                }

                @Override
                public Iterator<DependencyDefinition> iterator() {
                    // signal that iteration is about to start
                    startIterating.countDown();
                    return original.iterator();
                }
            };
            listField.set(mic, signallingList);
            // This call should block until the other thread modifies the list.
            return mic.getDependeeNamesForModel(modelA);
        };
        Future<List<String>> future = executor.submit(task);
        // Wait until the iteration has started.
        assertTrue(startIterating.await(2, TimeUnit.SECONDS), "Iteration did not start in time");
        // Modify the underlying list concurrently.
        mic.addDependencyDefinition(mockDependency(modelA, "concurrent"));
        modifyDone.countDown();
        // Expect a CME.
        assertThrows(ExecutionException.class, () -> {
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException ee) {
                // Unwrap the cause to verify it is a ConcurrentModificationException.
                assertTrue(ee.getCause() instanceof ConcurrentModificationException, "Expected ConcurrentModificationException, got " + ee.getCause());
                throw ee;
            }
        });
        executor.shutdownNow();
    }

    @Test
    void dependencyDefinitionThrowsRuntimeExceptionPropagates() {
        Model modelA = mock(Model.class);
        DependencyDefinition badDd = mock(DependencyDefinition.class);
        when(badDd.getDepender()).thenThrow(new RuntimeException("boom"));
        // The dependee method is irrelevant because the exception occurs first.
        mic.addDependencyDefinition(badDd);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> mic.getDependeeNamesForModel(modelA), "Exception from a DependencyDefinition should propagate unchanged");
        assertEquals("boom", ex.getMessage());
    }
}
