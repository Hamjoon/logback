package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import java.lang.reflect.Field;
import org.mockito.MockitoAnnotations;

@ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_getInterpretationContext_Test {

    private TestConfigurator configurator;

    @Mock
    private SaxEventInterpreter mockInterpreter;

    @Mock
    private SaxEventInterpretationContext mockInterpretationContext;

    // for closing MockitoAnnotations
    private AutoCloseable mocks;

    /**
     * Concrete subclass used only for testing.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for the purpose of this test.
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op for the purpose of this test.
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for the purpose of this test.
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // No‑op for the purpose of this test.
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // No‑op for the purpose of this test.
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // Expose the protected method so the test can call it.
            super.setImplicitRuleSupplier(interpreter);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
        configurator = new TestConfigurator();
        // Stub the interpreter to return the mocked interpretation context.
        when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(mockInterpretationContext);
        // Inject the mock interpreter into the configurator.
        configurator.setImplicitRuleSupplier(mockInterpreter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(mockInterpreter, mockInterpretationContext);
        // Close the Mockito open‑mocks resource.
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void getInterpretationContext_returnsContextProvidedByInterpreter() {
        // When
        SaxEventInterpretationContext result = configurator.getInterpretationContext();
        // Then
        assertNotNull(result, "The returned context should not be null");
        assertSame(mockInterpretationContext, result, "The configurator should return the context supplied by the interpreter");
        // Verify that the configurator indeed called the interpreter.
        verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
    }

    @Test
    void getInterpretationContext_returnsNullWhenInterpreterReturnsNull() {
        // Arrange: change stub to return null
        when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(null);
        // When
        SaxEventInterpretationContext result = configurator.getInterpretationContext();
        // Then
        assertNull(result, "If the interpreter returns null, the configurator should also return null");
        verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
    }

    @Test
    void getInterpretationContext_throwsNPEWhenInterpreterFieldIsNull() throws Exception {
        // Use reflection to set the protected field `saxEventInterpreter` to null.
        Field field = GenericXMLConfigurator.class.getDeclaredField("saxEventInterpreter");
        field.setAccessible(true);
        field.set(configurator, null);
        // When / Then
        assertThrows(NullPointerException.class, () -> configurator.getInterpretationContext(), "Calling getInterpretationContext() with a null saxEventInterpreter should throw NPE");
    }

    @Test
    void getInterpretationContext_propagatesExceptionFromInterpreter() {
        // Arrange: make the interpreter throw an IllegalStateException.
        when(mockInterpreter.getSaxEventInterpretationContext()).thenThrow(new IllegalStateException("Interpreter failure"));
        // When / Then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configurator.getInterpretationContext(), "The configurator must propagate exceptions thrown by the interpreter");
        assertEquals("Interpreter failure", ex.getMessage());
    }
}
