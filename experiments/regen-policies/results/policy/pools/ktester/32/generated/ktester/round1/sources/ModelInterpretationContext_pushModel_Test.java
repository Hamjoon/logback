package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_pushModel_Test {

    /**
     * The MIC instance used in each test.
     */
    private ModelInterpretationContext mic;

    /**
     * Mocked Context – required only for the MIC constructor.
     */
    @Mock
    private Context mockContext;

    /**
     * Simple concrete {@link Model} implementation used in the tests.
     */
    private static class SimpleModel extends Model {

        private final String name;

        SimpleModel(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "SimpleModel{" + name + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SimpleModel))
                return false;
            SimpleModel that = (SimpleModel) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for every test method.
        mic = new ModelInterpretationContext(mockContext);
        // The stacks are initialised empty by the constructor, no further setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the MIC does not hold external resources.
        mic = null;
    }

    @Test
    void pushModel_whenStackIsEmpty_shouldContainTheModel() {
        // given
        // anonymous concrete instance
        Model model = new Model() {
        };
        // pre‑condition check
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty before push");
        // when
        mic.pushModel(model);
        // then
        assertFalse(mic.isModelStackEmpty(), "Model stack should not be empty after push");
        assertEquals(model, mic.peekModel(), "The model on top of the stack must be the one that was pushed");
    }

    @Test
    void pushModel_multipleCalls_shouldMaintainLIFOOrder() {
        // given
        Model first = new Model() {
        };
        Model second = new Model() {
        };
        // when
        mic.pushModel(first);
        mic.pushModel(second);
        // then
        // The most recently pushed model should be on top.
        assertEquals(second, mic.peekModel(), "The top of the stack must be the last pushed model");
        // Popping should return models in reverse order of insertion.
        Model poppedFirst = mic.popModel();
        Model poppedSecond = mic.popModel();
        assertEquals(second, poppedFirst, "First pop should return the second model");
        assertEquals(first, poppedSecond, "Second pop should return the first model");
        // After popping both, the stack must be empty again.
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty after popping all models");
    }

    /**
     * Push a regular, non‑null Model onto an initially empty stack.
     */
    @Test
    void pushModel_regularModel_updatesStackState() {
        Model model = new SimpleModel("first");
        mic.pushModel(model);
        // Verify size via peek and emptiness checks.
        assertFalse(mic.isModelStackEmpty());
        assertEquals(model, mic.peekModel());
    }

    /**
     * Push a {@code null} reference – Stack permits null values.
     */
    @Test
    void pushModel_nullReference_isHandledGracefully() {
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty(), "Stack should contain a null entry after push");
        assertNull(mic.peekModel(), "peekModel should return null after pushing null");
        assertNull(mic.popModel(), "popModel should return null for the null entry");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after popping the null entry");
    }

    /**
     * Push three distinct models and verify LIFO ordering.
     */
    @Test
    void pushModel_threeModels_maintainsLIFO() {
        Model first = new SimpleModel("first");
        Model second = new SimpleModel("second");
        Model third = new SimpleModel("third");
        mic.pushModel(first);
        mic.pushModel(second);
        mic.pushModel(third);
        assertEquals(third, mic.peekModel());
        assertEquals(third, mic.popModel());
        assertEquals(second, mic.popModel());
        assertEquals(first, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    /**
     * Simulate a corrupted context where {@code modelStack} is null.
     */
    @Test
    void pushModel_corruptedModelStack_throwsNPE() throws Exception {
        // Use reflection to nullify the private field.
        Field stackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // Expect a NullPointerException when pushModel tries to use the null stack.
        assertThrows(NullPointerException.class, () -> mic.pushModel(new SimpleModel("any")));
    }

    /**
     * Push null then a non‑null model and verify order.
     */
    @Test
    void pushNullThenNonNull_verifyStackContents() {
        Model nullModel = null;
        Model second = new SimpleModel("second");
        mic.pushModel(nullModel);
        mic.pushModel(second);
        assertEquals(second, mic.peekModel());
        assertEquals(second, mic.popModel());
        assertNull(mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    /**
     * Push the same instance twice – both entries should exist.
     */
    @Test
    void pushDuplicateInstance_allowsDuplicates() {
        SimpleModel duplicate = new SimpleModel("dup");
        mic.pushModel(duplicate);
        mic.pushModel(duplicate);
        assertEquals(duplicate, mic.peekModel());
        assertEquals(duplicate, mic.popModel());
        assertEquals(duplicate, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    /**
     * Push, pop to empty, then push another model – ensure push works after emptying.
     */
    @Test
    void pushAfterEmptyingStack_behavesCorrectly() {
        SimpleModel first = new SimpleModel("first");
        SimpleModel second = new SimpleModel("second");
        mic.pushModel(first);
        assertEquals(first, mic.peekModel());
        assertEquals(first, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
        mic.pushModel(second);
        assertEquals(second, mic.peekModel());
        assertEquals(second, mic.popModel());
        assertTrue(mic.isModelStackEmpty());
    }

    /**
     * Verify that {@code pushModel} does not modify the {@code topModel} field.
     */
    @Test
    void pushModelDoesNotAffectTopModel() {
        SimpleModel top = new SimpleModel("top");
        // set the topModel beforehand
        mic.setTopModel(top);
        SimpleModel other = new SimpleModel("other");
        mic.pushModel(other);
        assertEquals(top, mic.getTopModel(), "Top model should remain unchanged after pushModel");
        assertEquals(other, mic.peekModel(), "pushModel should store the new model in the stack");
    }

    /**
     * Verify that {@code pushModel} does not affect the {@code objectMap}.
     */
    @Test
    void pushModelDoesNotAffectObjectMap() {
        // Populate objectMap with a sample entry.
        mic.getObjectMap().put("key", "value");
        SimpleModel model = new SimpleModel("model");
        mic.pushModel(model);
        // Ensure map entry is still present and unchanged.
        assertEquals("value", mic.getObjectMap().get("key"));
        assertEquals(model, mic.peekModel());
    }

    /**
     * Concurrent pushes – each thread pushes a distinct model.
     */
    @Test
    void concurrentPushModel_multipleThreads_allModelsPresent() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<SimpleModel> models = new ArrayList<>();
        for (int i = 1; i <= threadCount; i++) {
            SimpleModel model = new SimpleModel("c" + i);
            models.add(model);
            executor.submit(() -> {
                try {
                    // wait for all threads to be ready
                    startLatch.await();
                    mic.pushModel(model);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads simultaneously.
        startLatch.countDown();
        // Wait for all pushes to complete.
        assertTrue(doneLatch.await(2, TimeUnit.SECONDS), "All pushModel calls should finish quickly");
        // Pop all models and collect them.
        List<Model> popped = new ArrayList<>();
        while (!mic.isModelStackEmpty()) {
            popped.add(mic.popModel());
        }
        // Verify that we retrieved exactly the number of pushed models.
        assertEquals(threadCount, popped.size(), "Stack size after concurrent pushes should equal thread count");
        // Since Stack is LIFO, the order will be reverse of insertion order,
        // but we only need to ensure each expected model appears once.
        for (SimpleModel expected : models) {
            assertTrue(popped.contains(expected), "Popped list should contain model " + expected);
        }
        executor.shutdownNow();
    }
}
