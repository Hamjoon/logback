package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getBeanDescriptionCache_Test {

    @Mock
    private Context // mocked dependency required by the MIC constructor
    mockContext;

    // instance under test
    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test method
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references (helps GC in large test suites)
        mic = null;
    }

    /**
     * Verify that {@code getBeanDescriptionCache()} never returns {@code null}
     * and that repeated invocations return the exact same instance (object identity).
     */
    @Test
    void getBeanDescriptionCache_returnsSameNonNullInstance() {
        BeanDescriptionCache firstCall = mic.getBeanDescriptionCache();
        BeanDescriptionCache secondCall = mic.getBeanDescriptionCache();
        assertNotNull(firstCall, "BeanDescriptionCache should not be null on first call");
        assertNotNull(secondCall, "BeanDescriptionCache should not be null on second call");
        assertSame(firstCall, secondCall, "Subsequent calls must return the same BeanDescriptionCache instance");
        assertTrue(firstCall instanceof BeanDescriptionCache, "Returned object should be of type BeanDescriptionCache");
    }

    /**
     * When a {@link ModelInterpretationContext} is created via the copy‑constructor,
     * it should obtain its own {@link BeanDescriptionCache} instance (i.e. not the one
     * from the original context). The cache objects should be distinct but of the same type.
     */
    @Test
    void copyConstructorCreatesNewBeanDescriptionCacheInstance() {
        // Original instance
        BeanDescriptionCache originalCache = mic.getBeanDescriptionCache();
        // Create a second MIC using the copy‑constructor
        ModelInterpretationContext micCopy = new ModelInterpretationContext(mic);
        BeanDescriptionCache copyCache = micCopy.getBeanDescriptionCache();
        assertNotNull(copyCache, "BeanDescriptionCache of copied MIC should not be null");
        assertNotSame(originalCache, copyCache, "Copy constructor must create a new BeanDescriptionCache, not reuse the original");
        assertTrue(copyCache instanceof BeanDescriptionCache, "BeanDescriptionCache of copied MIC should be of correct type");
    }

    /**
     * Two independent {@code ModelInterpretationContext} objects must each hold
     * their own {@code BeanDescriptionCache}. The caches must not be shared.
     */
    @Test
    void distinctInstancesPerContext_haveSeparateCaches() {
        // First context (already created in @BeforeEach)
        BeanDescriptionCache cache1 = mic.getBeanDescriptionCache();
        // Second context – use a fresh mock Context to emphasise independence
        Context anotherMockContext = mock(Context.class);
        ModelInterpretationContext anotherMic = new ModelInterpretationContext(anotherMockContext);
        BeanDescriptionCache cache2 = anotherMic.getBeanDescriptionCache();
        assertNotNull(cache2, "Second context's BeanDescriptionCache should not be null");
        assertNotSame(cache1, cache2, "Each ModelInterpretationContext must have its own BeanDescriptionCache instance");
    }

    /**
     * Internal state changes (push/pop on object and model stacks) must not affect
     * the {@code BeanDescriptionCache} reference.
     */
    @Test
    void cacheReferenceUnchangedAfterStackManipulations() {
        BeanDescriptionCache before = mic.getBeanDescriptionCache();
        // Mock objects to push onto the stacks
        Object dummyObject = new Object();
        Model dummyModel = mock(Model.class);
        // Perform a series of stack operations
        mic.pushObject(dummyObject);
        mic.pushModel(dummyModel);
        mic.popObject();
        mic.popModel();
        BeanDescriptionCache after = mic.getBeanDescriptionCache();
        assertSame(before, after, "BeanDescriptionCache instance must remain identical after stack manipulations");
    }

    /**
     * Verify that the returned object is exactly an instance of the expected concrete class.
     */
    @Test
    void returnedTypeVerification() {
        BeanDescriptionCache cache = mic.getBeanDescriptionCache();
        assertNotNull(cache, "BeanDescriptionCache should not be null");
        assertEquals(BeanDescriptionCache.class, cache.getClass(), "Returned object must be an instance of BeanDescriptionCache (no subclassing)");
    }
}
