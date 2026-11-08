package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setConfiguratorSupplier_Test {

    /**
     * The Mic instance that is exercised in the tests.
     */
    private ModelInterpretationContext mic;

    /**
     * A mock {@link Context} required by the Mic constructor.
     */
    @Mock
    private Context mockContext;

    /**
     * Simple subclass used to verify that the setter accepts Suppliers of subclasses.
     */
    private static class CustomXMLConfigurator extends GenericXMLConfigurator {
        // No additional behaviour required for the test.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor under test does not perform any heavy lifting,
        // so a simple mock Context is sufficient.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference to help the garbage collector and avoid
        // accidental cross‑test contamination.
        mic = null;
    }

    @Test
    @DisplayName("setConfiguratorSupplier stores the supplied Supplier")
    void setConfiguratorSupplier_assignsSupplier() {
        // GIVEN
        // a simple lambda supplier
        Supplier<GenericXMLConfigurator> supplier = () -> null;
        // WHEN
        mic.setConfiguratorSupplier(supplier);
        // THEN
        Supplier<? extends GenericXMLConfigurator> retrieved = mic.getConfiguratorSupplier();
        assertSame(supplier, retrieved, "The supplier returned by getConfiguratorSupplier() should be the same instance passed to setConfiguratorSupplier()");
    }

    @Test
    @DisplayName("setConfiguratorSupplier accepts null without throwing")
    void setConfiguratorSupplier_acceptsNull() {
        // GIVEN
        Supplier<? extends GenericXMLConfigurator> nullSupplier = null;
        // WHEN / THEN
        assertDoesNotThrow(() -> mic.setConfiguratorSupplier(nullSupplier), "Setting a null supplier should not throw an exception");
        assertNull(mic.getConfiguratorSupplier(), "After setting a null supplier, getConfiguratorSupplier() should return null");
    }

    @Test
    @DisplayName("Non‑null Supplier is stored and retrieved unchanged")
    void setConfiguratorSupplier_nonNullSupplierIsStored() {
        // GIVEN
        Supplier<GenericXMLConfigurator> supplier = GenericXMLConfigurator::new;
        // WHEN
        mic.setConfiguratorSupplier(supplier);
        // THEN
        assertSame(supplier, mic.getConfiguratorSupplier(), "The same Supplier instance should be returned after assignment");
    }

    @Test
    @DisplayName("Subsequent call overwrites previous Supplier")
    void setConfiguratorSupplier_overwritesPrevious() {
        // GIVEN
        Supplier<GenericXMLConfigurator> first = () -> new GenericXMLConfigurator();
        Supplier<GenericXMLConfigurator> second = () -> new GenericXMLConfigurator();
        // WHEN
        mic.setConfiguratorSupplier(first);
        mic.setConfiguratorSupplier(second);
        // THEN
        assertSame(second, mic.getConfiguratorSupplier(), "The second Supplier should replace the first one");
    }

    @Test
    @DisplayName("Supplier that returns null is stored unchanged")
    void setConfiguratorSupplier_supplierReturnsNull() {
        // GIVEN
        Supplier<GenericXMLConfigurator> supplier = () -> null;
        // WHEN
        mic.setConfiguratorSupplier(supplier);
        // THEN
        Supplier<? extends GenericXMLConfigurator> stored = mic.getConfiguratorSupplier();
        assertSame(supplier, stored, "The stored Supplier should be the exact instance supplied");
        assertNull(stored.get(), "Invoking the stored Supplier's get() should return null");
    }

    @Test
    @DisplayName("Supplier that throws on get() does not affect setter")
    void setConfiguratorSupplier_supplierThrowsOnGet() {
        // GIVEN
        Supplier<GenericXMLConfigurator> throwingSupplier = () -> {
            throw new RuntimeException("boom");
        };
        // WHEN
        assertDoesNotThrow(() -> mic.setConfiguratorSupplier(throwingSupplier), "setConfiguratorSupplier must not invoke the Supplier, therefore no exception should be thrown");
        // THEN
        Supplier<? extends GenericXMLConfigurator> stored = mic.getConfiguratorSupplier();
        assertSame(throwingSupplier, stored, "The stored Supplier should be the same instance that was set");
        assertThrows(RuntimeException.class, stored::get, "Calling get() on the stored Supplier should propagate the original RuntimeException");
    }

    @Test
    @DisplayName("Supplier returning a subclass of GenericXMLConfigurator is accepted")
    void setConfiguratorSupplier_supplierReturnsSubclass() {
        // GIVEN
        Supplier<GenericXMLConfigurator> subclassSupplier = CustomXMLConfigurator::new;
        // WHEN
        mic.setConfiguratorSupplier(subclassSupplier);
        // THEN
        GenericXMLConfigurator instance = mic.getConfiguratorSupplier().get();
        assertTrue(instance instanceof CustomXMLConfigurator, "The instance produced by the Supplier should be of type CustomXMLConfigurator");
    }

    @Test
    @DisplayName("Assigning the identical lambda Supplier multiple times retains the same reference")
    void setConfiguratorSupplier_sameLambdaInstanceMultipleTimes() {
        // GIVEN
        Supplier<GenericXMLConfigurator> lambda = () -> new GenericXMLConfigurator();
        // WHEN
        mic.setConfiguratorSupplier(lambda);
        // assign the same instance again
        mic.setConfiguratorSupplier(lambda);
        // THEN
        assertSame(lambda, mic.getConfiguratorSupplier(), "After assigning the same lambda twice, the stored reference should still be the original lambda");
    }

    @Test
    @DisplayName("Mockito mock Supplier is stored and its invocations can be verified")
    void setConfiguratorSupplier_mockSupplierInvocationCount() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Supplier<GenericXMLConfigurator> mockSupplier = mock(Supplier.class);
        GenericXMLConfigurator dummyConfig = new GenericXMLConfigurator();
        when(mockSupplier.get()).thenReturn(dummyConfig);
        // WHEN
        mic.setConfiguratorSupplier(mockSupplier);
        // retrieve and invoke once
        mic.getConfiguratorSupplier().get();
        // THEN
        verify(mockSupplier, times(1)).get();
        assertSame(mockSupplier, mic.getConfiguratorSupplier(), "The stored Supplier should be the exact mock supplied");
    }

    @Test
    @DisplayName("A Supplier that creates a new instance each call can be overwritten by null")
    void setConfiguratorSupplier_overwriteWithNull() {
        // GIVEN
        Supplier<GenericXMLConfigurator> newInstanceSupplier = GenericXMLConfigurator::new;
        // WHEN: first assign a non‑null supplier
        mic.setConfiguratorSupplier(newInstanceSupplier);
        assertSame(newInstanceSupplier, mic.getConfiguratorSupplier(), "Initial non‑null supplier should be stored");
        // WHEN: overwrite with null
        mic.setConfiguratorSupplier(null);
        // THEN
        assertNull(mic.getConfiguratorSupplier(), "After setting a null supplier, the getter should return null");
    }
}
