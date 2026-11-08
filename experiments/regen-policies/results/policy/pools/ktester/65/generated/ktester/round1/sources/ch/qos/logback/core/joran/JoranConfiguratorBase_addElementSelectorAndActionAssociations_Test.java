package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.sift.SiftModelHandler;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentCaptor;
// import org.mockito.junit.jupiter.MockitoExtension;
// <-- needed for MockitoExtension
// import org.mockito.junit.jupiter.MockitoExtension;

// <-- activate Mockito's extension
// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addElementSelectorAndActionAssociations_Test {

    @Mock
    RuleStore // mocked RuleStore used in the test
    ruleStore;

    /**
     * Concrete subclass that simply exposes the protected method.
     */
    static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // no‑op for the test
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op for the test
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op for the test
        }

        /**
         * Delegates to the protected method we want to test.
         */
        void invokeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference
        reset(ruleStore);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullRuleStoreThrowsNullPointerException() {
        TestConfigurator configurator = new TestConfigurator();
        assertThrows(NullPointerException.class, () -> configurator.invokeAddElementSelectorAndActionAssociations(null), "Method should throw NPE when RuleStore is null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addTransparentPathPartExceptionIsPropagated() {
        // make the mock throw when "if" is registered as a transparent part
        doThrow(new IllegalStateException("illegal")).when(ruleStore).addTransparentPathPart("if");
        TestConfigurator configurator = new TestConfigurator();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configurator.invokeAddElementSelectorAndActionAssociations(ruleStore));
        assertEquals("illegal", ex.getMessage(), "Exception from RuleStore.addTransparentPathPart should propagate unchanged");
    }
}