package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addElementSelectorAndActionAssociations_Test {

    private TestConfigurator configurator;

    /**
     * Concrete subclass used only for exercising the protected method.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        // The abstract methods of the parent are not needed for this test,
        // therefore we provide empty implementations.
        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        }

        @Override
        public void buildModelInterpretationContext() {
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return null;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
        }

        @Override
        protected void sanityCheck(Model topModel) {
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        }

        /**
         * Exposes the protected method for the test.
         */
        void invokeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
    }

    @Test
    void addsExpectedRulesAndTransparentPathParts() {
        // Arrange
        RuleStore rs = mock(RuleStore.class);
        // Act
        configurator.invokeAddElementSelectorAndActionAssociations(rs);
        // Assert – verify number of rule registrations
        verify(rs, times(21)).addRule(any(ElementSelector.class), Mockito.<Supplier<Action>>any());
        // Capture arguments for deeper inspection
        ArgumentCaptor<ElementSelector> selectorCaptor = ArgumentCaptor.forClass(ElementSelector.class);
        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(rs, atLeast(21)).addRule(selectorCaptor.capture(), supplierCaptor.capture());
        List<String> selectors = selectorCaptor.getAllValues().stream().map(ElementSelector::toString).collect(Collectors.toList());
        // Expected selector‑to‑action mapping
        String[][] expected = { { "*/variable", PropertyAction.class.getName() }, { "*/property", PropertyAction.class.getName() }, { "*/substitutionProperty", PropertyAction.class.getName() }, { "configuration/import", ImportAction.class.getName() }, { "configuration/timestamp", TimestampAction.class.getName() }, { "configuration/shutdownHook", ShutdownHookAction.class.getName() }, { "configuration/sequenceNumberGenerator", SequenceNumberGeneratorAction.class.getName() }, { "configuration/serializeModel", SerializeModelAction.class.getName() }, { "configuration/define", DefinePropertyAction.class.getName() }, { "configuration/evaluator", EventEvaluatorAction.class.getName() }, { "configuration/contextProperty", ContextPropertyAction.class.getName() }, { "configuration/conversionRule", ConversionRuleAction.class.getName() }, { "configuration/statusListener", StatusListenerAction.class.getName() }, { "*/appender", AppenderAction.class.getName() }, { "configuration/appender/appender-ref", AppenderRefAction.class.getName() }, { "configuration/newRule", NewRuleAction.class.getName() }, { "*/param", ParamAction.class.getName() }, { "*/if", IfAction.class.getName() }, { "*/if/then", ThenAction.class.getName() }, { "*/if/else", ElseAction.class.getName() }, { "*/appender/sift", SiftAction.class.getName() } };
        // Verify each selector is present and the supplier creates the correct Action type
        for (String[] pair : expected) {
            String expectedPattern = pair[0];
            String expectedActionClass = pair[1];
            int idx = selectors.indexOf(expectedPattern);
            assertTrue(idx >= 0, () -> "Expected selector pattern not found: " + expectedPattern);
            @SuppressWarnings("unchecked")
            Supplier<Action> supplier = (Supplier<Action>) supplierCaptor.getAllValues().get(idx);
            assertNotNull(supplier, "Supplier should not be null for selector: " + expectedPattern);
            Action actionInstance = supplier.get();
            assertNotNull(actionInstance, "Supplier must produce an instance for selector: " + expectedPattern);
            assertEquals(expectedActionClass, actionInstance.getClass().getName(), "Incorrect Action type for selector: " + expectedPattern);
        }
        // Verify transparent path parts – they are added after the rule registrations
        verify(rs).addTransparentPathPart("if");
        verify(rs).addTransparentPathPart("then");
        verify(rs).addTransparentPathPart("else");
        verify(rs).addTransparentPathPart("sift");
    }

    /* --------------------------------------------------------------
     *  2.  Verify exact call count and registration order.
     * -------------------------------------------------------------- */
    @Test
    void registersSelectorsAndTransparentPartsInExactOrder() {
        // Arrange
        RuleStore rs = mock(RuleStore.class);
        InOrder inOrder = inOrder(rs);
        // Act
        configurator.invokeAddElementSelectorAndActionAssociations(rs);
        // Assert – order of addRule calls
        String[] ruleOrder = { "*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift" };
        for (String pattern : ruleOrder) {
            inOrder.verify(rs).addRule(argThat(es -> es.toString().equals(pattern)), Mockito.<Supplier<Action>>any());
        }
        // Order of transparent path parts
        inOrder.verify(rs).addTransparentPathPart("if");
        inOrder.verify(rs).addTransparentPathPart("then");
        inOrder.verify(rs).addTransparentPathPart("else");
        inOrder.verify(rs).addTransparentPathPart("sift");
        // No further interactions
        verifyNoMoreInteractions(rs);
    }

    /* --------------------------------------------------------------
     *  3.  Null RuleStore should result in NPE.
     * -------------------------------------------------------------- */
    @Test
    void nullRuleStoreThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> configurator.invokeAddElementSelectorAndActionAssociations(null));
    }

    /* --------------------------------------------------------------
     *  4.  Propagation of RuntimeException from addRule.
     * -------------------------------------------------------------- */
    @Test
    void exceptionFromAddRuleIsPropagated() {
        // Arrange – mock that throws on the first addRule call
        RuleStore rs = mock(RuleStore.class);
        doThrow(new RuntimeException("boom")).when(rs).addRule(any(ElementSelector.class), Mockito.<Supplier<Action>>any());
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.invokeAddElementSelectorAndActionAssociations(rs));
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  5.  Propagation of RuntimeException from addTransparentPathPart.
     * -------------------------------------------------------------- */
    @Test
    void exceptionFromAddTransparentPathPartIsPropagated() {
        // Arrange – let addRule work, but throw on the first transparent part addition
        RuleStore rs = mock(RuleStore.class);
        // make addRule a no‑op
        doNothing().when(rs).addRule(any(ElementSelector.class), Mockito.<Supplier<Action>>any());
        // throw on first call to addTransparentPathPart
        doThrow(new RuntimeException("transparent boom")).when(rs).addTransparentPathPart("if");
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.invokeAddElementSelectorAndActionAssociations(rs));
        assertEquals("transparent boom", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  6.  Idempotence – calling the method twice registers everything twice.
     * -------------------------------------------------------------- */
    @Test
    void repeatedInvocationRegistersSelectorsAndTransparentPartsAgain() {
        // Arrange
        RuleStore rs = mock(RuleStore.class);
        // Act – invoke twice
        configurator.invokeAddElementSelectorAndActionAssociations(rs);
        configurator.invokeAddElementSelectorAndActionAssociations(rs);
        // Assert – 42 addRule calls (2 × 21) and 8 transparent part calls (2 × 4)
        verify(rs, times(42)).addRule(any(ElementSelector.class), Mockito.<Supplier<Action>>any());
        verify(rs, times(8)).addTransparentPathPart(anyString());
        // Verify that each expected selector appears exactly twice
        ArgumentCaptor<ElementSelector> selectorCaptor = ArgumentCaptor.forClass(ElementSelector.class);
        verify(rs, atLeast(42)).addRule(selectorCaptor.capture(), Mockito.<Supplier<Action>>any());
        List<String> selectors = selectorCaptor.getAllValues().stream().map(ElementSelector::toString).collect(Collectors.toList());
        String[] expectedPatterns = { "*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift" };
        for (String pattern : expectedPatterns) {
            long count = selectors.stream().filter(p -> p.equals(pattern)).count();
            assertEquals(2L, count, "Selector '" + pattern + "' should be added twice");
        }
        // Verify transparent parts are added twice each
        verify(rs, times(2)).addTransparentPathPart("if");
        verify(rs, times(2)).addTransparentPathPart("then");
        verify(rs, times(2)).addTransparentPathPart("else");
        verify(rs, times(2)).addTransparentPathPart("sift");
    }
}