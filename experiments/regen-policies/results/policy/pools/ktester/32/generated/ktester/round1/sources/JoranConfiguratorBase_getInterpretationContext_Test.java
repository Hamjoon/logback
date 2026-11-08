package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_getInterpretationContext_Test {

    // ------------------------------------------------------------------------
    @Mock
    SaxEventInterpreter mockInterpreter;

    @Mock
    SaxEventInterpretationContext mockInterpretationContext;

    // ------------------------------------------------------------------------
    TestJoranConfigurator configurator;

    // ------------------------------------------------------------------------
    static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for the purpose of this test
        }

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // No‑op for the purpose of this test
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for the purpose of this test
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No‑op for the purpose of this test
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise the object under test
        configurator = new TestJoranConfigurator();
        // Stub the interpreter to return the mocked interpretation context
        when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(mockInterpretationContext);
        // Inject the mock interpreter into the configurator.
        // The field `saxEventInterpreter` is defined in the superclass hierarchy
        // (it is protected in the actual Logback source).  Using reflection
        // respects the rule of not calling private members directly.
        Field interpreterField = JoranConfiguratorBase.class.getDeclaredField("saxEventInterpreter");
        interpreterField.setAccessible(true);
        interpreterField.set(configurator, mockInterpreter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        clearInvocations(mockInterpreter, mockInterpretationContext);
    }

    // ------------------------------------------------------------------------
    @Test
    void getInterpretationContext_returnsContextProvidedByInterpreter() {
        // WHEN
        SaxEventInterpretationContext result = configurator.getInterpretationContext();
        // THEN
        assertSame(mockInterpretationContext, result, "The returned context should be exactly the one supplied by the interpreter");
        verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
    }

    /**
     * Verify that when the internal interpreter returns {@code null},
     * {@code getInterpretationContext()} also returns {@code null} and does not
     * throw any exception.
     */
    @Test
    void getInterpretationContext_returnsNullWhenInterpreterReturnsNull() {
        // Arrange: change the stub for this specific test
        when(mockInterpreter.getSaxEventInterpretationContext()).thenReturn(null);
        // WHEN
        SaxEventInterpretationContext result = configurator.getInterpretationContext();
        // THEN
        assertNull(result, "When the interpreter returns null, the method should return null");
        verify(mockInterpreter, times(1)).getSaxEventInterpretationContext();
    }
}
