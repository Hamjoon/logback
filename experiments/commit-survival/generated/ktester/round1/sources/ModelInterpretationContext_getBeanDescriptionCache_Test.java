package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getBeanDescriptionCache_Test {

    private Context context;

    private ModelInterpretationContext mic;

    private ModelInterpretationContext micCopy;

    @BeforeEach
    void setupBeforeEach() {
        // Use a concrete Context implementation provided by Logback
        context = new ContextBase();
        // Instance created with the simple constructor
        mic = new ModelInterpretationContext(context);
        // Instance created with the copy‑constructor to verify behaviour there as well
        micCopy = new ModelInterpretationContext(mic);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the test tidy
        mic = null;
        micCopy = null;
        context = null;
    }

    @Test
    @DisplayName("getBeanDescriptionCache() returns a non‑null cache linked to the supplied Context")
    void testCacheIsCreatedAndLinkedToContext() {
        BeanDescriptionCache cache = mic.getBeanDescriptionCache();
        assertNotNull(cache, "BeanDescriptionCache should not be null");
        // BeanDescriptionCache stores the Context it was created with; we can verify via reflection
        // without accessing private fields directly – we rely on its public behaviour:
        // calling getBeanInfo on a known class should succeed when the context is valid.
        assertDoesNotThrow(() -> cache.getBeanInfo(String.class), "BeanDescriptionCache should be functional with the provided Context");
    }

    @Test
    @DisplayName("getBeanDescriptionCache() returns the same instance on repeated calls")
    void testCacheIsSingletonPerInstance() {
        BeanDescriptionCache first = mic.getBeanDescriptionCache();
        BeanDescriptionCache second = mic.getBeanDescriptionCache();
        assertSame(first, second, "Repeated calls must return the same BeanDescriptionCache instance");
    }

    @Test
    @DisplayName("Copy‑constructor creates a distinct BeanDescriptionCache")
    void testCopyConstructorCreatesNewCache() {
        BeanDescriptionCache originalCache = mic.getBeanDescriptionCache();
        BeanDescriptionCache copyCache = micCopy.getBeanDescriptionCache();
        assertNotNull(originalCache, "Original cache must not be null");
        assertNotNull(copyCache, "Copy cache must not be null");
        // The two caches should be different objects (the copy constructor creates a new one)
        assertNotSame(originalCache, copyCache, "Copy constructor should create a new BeanDescriptionCache instance");
        // Both caches must still be functional with the same Context
        assertDoesNotThrow(() -> originalCache.getBeanInfo(Integer.class));
        assertDoesNotThrow(() -> copyCache.getBeanInfo(Integer.class));
    }

    @Test
    @DisplayName("Constructor with configuratorHint still provides a functional BeanDescriptionCache")
    void testConstructorWithConfiguratorHint() {
        Object hint = new Object();
        ModelInterpretationContext micWithHint = new ModelInterpretationContext(context, hint);
        BeanDescriptionCache cache = micWithHint.getBeanDescriptionCache();
        assertNotNull(cache, "Cache should be created even when configuratorHint is supplied");
        assertDoesNotThrow(() -> cache.getBeanInfo(Double.class));
    }

    @Test
    @DisplayName("Returned cache is of the expected concrete type")
    void testCacheIsOfExpectedType() {
        BeanDescriptionCache cache = mic.getBeanDescriptionCache();
        assertTrue(cache instanceof BeanDescriptionCache, "Returned object should be an instance of BeanDescriptionCache");
    }

    @Test
    @DisplayName("Two independent ModelInterpretationContext instances have distinct caches")
    void testDistinctCachesForSeparateContexts() {
        ModelInterpretationContext mic2 = new ModelInterpretationContext(context);
        BeanDescriptionCache cache1 = mic.getBeanDescriptionCache();
        BeanDescriptionCache cache2 = mic2.getBeanDescriptionCache();
        assertNotSame(cache1, cache2, "Each ModelInterpretationContext must own its own BeanDescriptionCache");
    }

    @Test
    @DisplayName("Cache reference remains unchanged after stack manipulation")
    void testCacheStabilityAfterStackOperations() {
        BeanDescriptionCache before = mic.getBeanDescriptionCache();
        // Perform a series of stack operations
        mic.pushObject("testObject");
        // simple anonymous Model
        Model mockModel = Assertions.assertDoesNotThrow(() -> new Model() {
        });
        mic.pushModel(mockModel);
        mic.popObject();
        mic.popModel();
        BeanDescriptionCache after = mic.getBeanDescriptionCache();
        assertSame(before, after, "Cache instance must stay the same after stack operations");
    }

    @Test
    @DisplayName("Cache reference remains unchanged after adding imports and substitution properties")
    void testCacheStabilityAfterImportsAndProperties() {
        BeanDescriptionCache before = mic.getBeanDescriptionCache();
        mic.addImport("stem", "java.lang.String");
        mic.addSubstitutionProperty("key", "value");
        BeanDescriptionCache after = mic.getBeanDescriptionCache();
        assertSame(before, after, "Cache instance must stay the same after imports/property additions");
    }

    @Test
    @DisplayName("Cache reference remains unchanged after setting configurator hint and top model")
    void testCacheStabilityAfterHintAndTopModel() {
        BeanDescriptionCache before = mic.getBeanDescriptionCache();
        mic.setConfiguratorHint(new Object());
        Model mockTopModel = Assertions.assertDoesNotThrow(() -> new Model() {
        });
        mic.setTopModel(mockTopModel);
        BeanDescriptionCache after = mic.getBeanDescriptionCache();
        assertSame(before, after, "Cache instance must stay the same after configurator hint/top model changes");
    }
}
