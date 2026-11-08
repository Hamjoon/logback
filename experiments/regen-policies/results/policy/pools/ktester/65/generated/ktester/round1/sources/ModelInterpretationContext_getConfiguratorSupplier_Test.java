package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.GenericXMLConfigurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.model.Model;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getConfiguratorSupplier_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context mockContext;

    @Mock
    private GenericXMLConfigurator mockConfigurator;

    @Mock
    private Supplier<? extends GenericXMLConfigurator> mockSupplier;

    // -----------------------------------------------------------------
    private static class TestSupplierFactory {

        static GenericXMLConfigurator createConfigurator() {
            // In a real scenario this could create a real configurator;
            // for the test we simply return null – the getter never invokes it.
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the class under test with a mocked Context
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        mic = null;
    }

    @Test
    void getConfiguratorSupplier_returnsSupplierSetViaSetter() {
        // GIVEN: a supplier is set on the ModelInterpretationContext
        mic.setConfiguratorSupplier(mockSupplier);
        // WHEN: the getter is invoked
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN: the same instance that was set is returned
        assertSame(mockSupplier, result, "The returned supplier should be the exact instance that was set");
    }

    @Test
    void getConfiguratorSupplier_returnsNullWhenNotSet() {
        // GIVEN: no supplier has been set (default state)
        // WHEN: the getter is invoked
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN: the result should be null
        assertNull(result, "The configurator supplier should be null when it has not been set");
    }

    @Test
    void getConfiguratorSupplier_returnsLastSupplierWhenSetTwice() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> firstSupplier = mock(Supplier.class);
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> secondSupplier = mock(Supplier.class);
        mic.setConfiguratorSupplier(firstSupplier);
        // overwrite
        mic.setConfiguratorSupplier(secondSupplier);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertSame(secondSupplier, result, "The getter must return the most recently set supplier");
    }

    @Test
    void getConfiguratorSupplier_multipleSetsWithSameInstance_returnsThatInstance() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> supplier = mock(Supplier.class);
        mic.setConfiguratorSupplier(supplier);
        // same instance again
        mic.setConfiguratorSupplier(supplier);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertSame(supplier, result, "Even when the same supplier is set repeatedly, the getter should return that instance");
    }

    @Test
    void getConfiguratorSupplier_returnsNullAfterExplicitReset() {
        // GIVEN
        mic.setConfiguratorSupplier(mockSupplier);
        // explicit reset
        mic.setConfiguratorSupplier(null);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertNull(result, "After resetting to null, the getter must return null");
    }

    @Test
    void getConfiguratorSupplier_doesNotInvokeSupplierGetEvenIfItThrows() {
        // GIVEN a supplier whose get() throws
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> throwingSupplier = mock(Supplier.class);
        when(throwingSupplier.get()).thenThrow(new RuntimeException("Should not be called"));
        mic.setConfiguratorSupplier(throwingSupplier);
        // WHEN – we only call the getter
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN – no exception should have been thrown and the same instance is returned
        assertSame(throwingSupplier, result, "Getter must return the supplier without invoking its get() method");
        // verify that get() was never called
        verify(throwingSupplier, never()).get();
    }

    @Test
    void getConfiguratorSupplier_returnsSupplierEvenIfItsGetReturnsNull() {
        // GIVEN a supplier that returns null when get() is called
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> nullReturningSupplier = mock(Supplier.class);
        when(nullReturningSupplier.get()).thenReturn(null);
        mic.setConfiguratorSupplier(nullReturningSupplier);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertSame(nullReturningSupplier, result, "Getter must return the supplier regardless of what its get() would return");
        verify(nullReturningSupplier, never()).get();
    }

    @Test
    void getConfiguratorSupplier_multipleCallsReturnSameInstance() {
        // GIVEN
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> supplier = mock(Supplier.class);
        mic.setConfiguratorSupplier(supplier);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> first = mic.getConfiguratorSupplier();
        Supplier<? extends GenericXMLConfigurator> second = mic.getConfiguratorSupplier();
        Supplier<? extends GenericXMLConfigurator> third = mic.getConfiguratorSupplier();
        // THEN
        assertAll(() -> assertSame(supplier, first, "First call should return the stored supplier"), () -> assertSame(supplier, second, "Second call should return the same instance"), () -> assertSame(supplier, third, "Third call should also return the same instance"));
    }

    @Test
    void getConfiguratorSupplier_unchangedAfterStackManipulations() {
        // GIVEN a supplier set before any stack operations
        @SuppressWarnings("unchecked")
        Supplier<? extends GenericXMLConfigurator> supplier = mock(Supplier.class);
        mic.setConfiguratorSupplier(supplier);
        // Mutate unrelated internal state
        mic.pushObject("obj1");
        mic.pushObject(123);
        mic.pushObject(new Object());
        Model model1 = mock(Model.class);
        Model model2 = mock(Model.class);
        mic.pushModel(model1);
        mic.pushModel(model2);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertSame(supplier, result, "Getter must remain unaffected by pushes onto object/model stacks");
    }

    @Test
    void getConfiguratorSupplier_storesMethodReferenceSupplier() {
        // GIVEN a method‑reference supplier
        Supplier<GenericXMLConfigurator> methodRefSupplier = TestSupplierFactory::createConfigurator;
        mic.setConfiguratorSupplier(methodRefSupplier);
        // WHEN
        Supplier<? extends GenericXMLConfigurator> result = mic.getConfiguratorSupplier();
        // THEN
        assertSame(methodRefSupplier, result, "The exact method‑reference supplier instance should be stored and returned");
    }
}
