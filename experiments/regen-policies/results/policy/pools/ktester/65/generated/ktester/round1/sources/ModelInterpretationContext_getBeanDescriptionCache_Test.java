package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getBeanDescriptionCache_Test {

    @Mock
    Context // mocked Context supplied to the MIC
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @Test
    void getBeanDescriptionCache_ShouldReturnNonNullInstance() {
        BeanDescriptionCache cache = mic.getBeanDescriptionCache();
        assertNotNull(cache, "BeanDescriptionCache must not be null");
        assertInstanceOf(BeanDescriptionCache.class, cache, "Returned object should be an instance of BeanDescriptionCache");
    }

    @Test
    void getBeanDescriptionCache_ShouldReturnSameInstanceOnRepeatedCalls() {
        BeanDescriptionCache firstCall = mic.getBeanDescriptionCache();
        BeanDescriptionCache secondCall = mic.getBeanDescriptionCache();
        assertSame(firstCall, secondCall, "Multiple invocations should return the identical BeanDescriptionCache instance");
    }

    @Test
    void copyConstructor_ShouldCreateIndependentBeanDescriptionCache() {
        // Original instance and its cache
        BeanDescriptionCache originalCache = mic.getBeanDescriptionCache();
        // Create a copy using the copy‑constructor
        ModelInterpretationContext micCopy = new ModelInterpretationContext(mic);
        BeanDescriptionCache copyCache = micCopy.getBeanDescriptionCache();
        // The caches must be distinct objects (the copy creates a new BeanDescriptionCache)
        assertNotSame(originalCache, copyCache, "Copy constructor should create a new BeanDescriptionCache, not reuse the original");
        assertNotNull(copyCache, "BeanDescriptionCache of the copy must not be null");
    }

    @Test
    void getBeanDescriptionCache_DistinctInstancesForSeparateContexts() {
        // Two independent ModelInterpretationContext objects
        ModelInterpretationContext mic1 = new ModelInterpretationContext(mockContext);
        ModelInterpretationContext mic2 = new ModelInterpretationContext(mockContext);
        BeanDescriptionCache cache1 = mic1.getBeanDescriptionCache();
        BeanDescriptionCache cache2 = mic2.getBeanDescriptionCache();
        assertNotSame(cache1, cache2, "Each ModelInterpretationContext must have its own BeanDescriptionCache instance");
        assertNotNull(cache1);
        assertNotNull(cache2);
    }

    @Test
    void getBeanDescriptionCache_ShouldRemainSameAfterStackManipulations() {
        BeanDescriptionCache initialCache = mic.getBeanDescriptionCache();
        // Perform a series of stack operations. The actual objects are irrelevant for the cache test.
        mic.pushObject(new Object());
        mic.pushObject("testString");
        // anonymous Model implementation
        mic.pushModel(new Model() {
        });
        mic.popObject();
        mic.popModel();
        BeanDescriptionCache afterOpsCache = mic.getBeanDescriptionCache();
        assertSame(initialCache, afterOpsCache, "BeanDescriptionCache reference must stay unchanged after stack operations");
    }

    @Test
    void getBeanDescriptionCache_ShouldRemainSameAfterConfiguratorHintChange() {
        BeanDescriptionCache beforeHintCache = mic.getBeanDescriptionCache();
        // Set a non‑null configurator hint
        mic.setConfiguratorHint(new Object());
        BeanDescriptionCache afterHintCache = mic.getBeanDescriptionCache();
        assertSame(beforeHintCache, afterHintCache, "BeanDescriptionCache must not be affected by configurator hint changes");
    }

    @Test
    void getBeanDescriptionCache_ConcurrentAccess_ShouldReturnIdenticalInstance() throws Exception {
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            // Latch to make all threads start at the same moment
            CountDownLatch startLatch = new CountDownLatch(1);
            // Latch to wait for all threads to finish
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            // Thread‑safe collection for the results
            List<BeanDescriptionCache> results = new CopyOnWriteArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        // wait for the go‑signal
                        startLatch.await();
                        results.add(mic.getBeanDescriptionCache());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            // Release all threads simultaneously
            startLatch.countDown();
            // Wait for all threads to complete (with a timeout to avoid deadlock)
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish within the timeout");
            // All collected caches must be the same instance
            BeanDescriptionCache first = results.get(0);
            for (BeanDescriptionCache cache : results) {
                assertSame(first, cache, "Concurrent calls must return the identical BeanDescriptionCache instance");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
