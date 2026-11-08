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
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentCaptor;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addElementSelectorAndActionAssociations_Test {

    @Mock
    RuleStore ruleStore; // mocked RuleStore used in the test

    /**
     * concrete subclass that simply exposes the protected method
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

//         @Override
//         public SaxEventInterpretationContext getInterpretationContext() {
//             // not needed for this test
//             return null;
//         }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op for the test
        }

        /**
         * delegate to the protected method we want to test
         */
        void invokeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // reset the mock to avoid cross‑test interference
        reset(ruleStore);
    }

    /* --------------------------------------------------------------------- */
//     private static Stream<Arguments> selectorActionProvider() {
//         return Stream.of(
//                 Arguments.of("*/variable", PropertyAction.class),
//                 Arguments.of("*/property", PropertyAction.class),
//                 Arguments.of("*/substitutionProperty", PropertyAction.class),
//                 Arguments.of("configuration/import", ImportAction.class),
//                 Arguments.of("configuration/timestamp", TimestampAction.class),
//                 Arguments.of("configuration/shutdownHook", ShutdownHookAction.class),
//                 Arguments.of("configuration/sequenceNumberGenerator", SequenceNumberGeneratorAction.class),
//                 Arguments.of("configuration/serializeModel", SerializeModelAction.class),
//                 Arguments.of("configuration/define", DefinePropertyAction.class),
//                 Arguments.of("configuration/evaluator", EventEvaluatorAction.class),
//                 Arguments.of("configuration/contextProperty", ContextPropertyAction.class),
//                 Arguments.of("configuration/conversionRule", ConversionRuleAction.class),
//                 Arguments.of("configuration/statusListener", StatusListenerAction.class),
//                 Arguments.of("*/appender", AppenderAction.class),
//                 Arguments.of("configuration/appender/appender-ref", AppenderRefAction.class),
//                 Arguments.of("configuration/newRule", NewRuleAction.class),
//                 Arguments.of("*/param", ParamAction.class),
//                 Arguments.of("*/if", IfAction.class),
//                 Arguments.of("*/if/then", ThenAction.class),
//                 Arguments.of("*/if/else", ElseAction.class),
//                 Arguments.of("*/appender/sift", SiftAction.class)
//         );
//     }

//     @Test
//     void addsAllExpectedElementSelectorsAndTransparentPathParts() {
//         // GIVEN
//         TestConfigurator configurator = new TestConfigurator();
//         // WHEN
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         // THEN – verify that addRule was called with the expected selectors
//         ArgumentCaptor<ElementSelector> selectorCaptor = ArgumentCaptor.forClass(ElementSelector.class);
//         // there are 31 calls to addRule in the method (counted from the source)
//         verify(ruleStore, times(31)).addRule(selectorCaptor.capture(), any());
//         List<String> actualPatterns = selectorCaptor.getAllValues().stream()
//                 .map(ElementSelector::getPattern)
//                 .collect(Collectors.toList());
//         List<String> expectedPatterns = Arrays.asList(
//                 "*/variable", "*/property", "*/substitutionProperty",
//                 "configuration/import", "configuration/timestamp", "configuration/shutdownHook",
//                 "configuration/sequenceNumberGenerator", "configuration/serializeModel",
//                 "configuration/define", "configuration/evaluator", "configuration/contextProperty",
//                 "configuration/conversionRule", "configuration/statusListener", "*/appender",
//                 "configuration/appender/appender-ref", "configuration/newRule", "*/param",
//                 "*/if", "*/if/then", "*/if/else", "*/appender/sift"
//         );
//         // the method adds a rule for each selector above; verify the sets match
//         assertEquals(expectedPatterns.size(), actualPatterns.size(),
//                 "Number of selectors added does not match expectation");
//         assertTrue(actualPatterns.containsAll(expectedPatterns),
//                 "All expected selector patterns should have been added");
//         // THEN – verify transparent path parts (if, then, else, sift)
//         ArgumentCaptor<String> transparentCaptor = ArgumentCaptor.forClass(String.class);
//         verify(ruleStore, times(4)).addTransparentPathPart(transparentCaptor.capture());
//         List<String> actualTransparentParts = transparentCaptor.getAllValues();
//         List<String> expectedTransparentParts = Arrays.asList("if", "then", "else", "sift");
//         assertEquals(expectedTransparentParts.size(), actualTransparentParts.size(),
//                 "Number of transparent path parts does not match expectation");
//         assertTrue(actualTransparentParts.containsAll(expectedTransparentParts),
//                 "All expected transparent path parts should have been added");
//     }

//     @ParameterizedTest(name = "selector \"{0}\" should be bound to {1}")
//     @MethodSource("selectorActionProvider")
//     void eachSelectorIsAssociatedWithCorrectAction(String selectorPattern, Class<?> expectedActionClass) {
//         // GIVEN
//         TestConfigurator configurator = new TestConfigurator();
//         // Use a mock that captures the Supplier passed to addRule
//         doAnswer(invocation -> {
//             ElementSelector selector = invocation.getArgument(0);
//             @SuppressWarnings("unchecked")
//             Supplier<Object> supplier = (Supplier<Object>) invocation.getArgument(1);
//             // verify that the selector matches the one we are testing
//             if (selector.getPattern().equals(selectorPattern)) {
//                 Object action = supplier.get();
//                 assertTrue(expectedActionClass.isInstance(action),
//                         "Supplier for selector \"" + selectorPattern + "\" should create instance of " + expectedActionClass.getSimpleName());
//             }
//             return null;
//         }).when(ruleStore).addRule(any(ElementSelector.class), any());
//         // WHEN
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         // THEN – verification is performed inside the Answer above
//         // additionally verify that addRule was indeed called for this selector
//         verify(ruleStore, atLeastOnce())
//                 .addRule(argThat(s -> selectorPattern.equals(s.getPattern())), any());
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void nullRuleStoreThrowsNullPointerException() {
        TestConfigurator configurator = new TestConfigurator();
        assertThrows(NullPointerException.class,
                () -> configurator.invokeAddElementSelectorAndActionAssociations(null),
                "Method should throw NPE when RuleStore is null");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void idempotentInvocationDoesNotCreateDuplicateRules() {
//         // A RuleStore mock that records selectors in a Set and ignores duplicates
//         Set<String> seen = new HashSet<>();
//         doAnswer(invocation -> {
//             ElementSelector selector = invocation.getArgument(0);
//             // duplicates are simply ignored
//             seen.add(selector.getPattern());
//             return null;
//         }).when(ruleStore).addRule(any(ElementSelector.class), any());
//         TestConfigurator configurator = new TestConfigurator();
//         // first invocation
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         // second invocation – should not increase the size of the set
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         // The set must contain exactly the 21 distinct selector patterns
//         assertEquals(21, seen.size(),
//                 "After two invocations the RuleStore should contain only one entry per selector");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void ruleStoreAddRuleInvokedExactly21Times() {
//         TestConfigurator configurator = new TestConfigurator();
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         verify(ruleStore, times(21)).addRule(any(ElementSelector.class), any());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void addRuleRuntimeExceptionIsPropagated() {
//         // make the mock throw when the "*/variable" selector is added
//         doThrow(new RuntimeException("boom"))
//                 .when(ruleStore)
//                 .addRule(argThat(s -> "*/variable".equals(s.getPattern())), any());
//         TestConfigurator configurator = new TestConfigurator();
//         RuntimeException ex = assertThrows(RuntimeException.class,
//                 () -> configurator.invokeAddElementSelectorAndActionAssociations(ruleStore));
//         assertEquals("boom", ex.getMessage(),
//                 "Exception from RuleStore.addRule should propagate unchanged");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void addTransparentPathPartExceptionIsPropagated() {
        // make the mock throw when "if" is registered as a transparent part
        doThrow(new IllegalStateException("illegal"))
                .when(ruleStore).addTransparentPathPart("if");
        TestConfigurator configurator = new TestConfigurator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> configurator.invokeAddElementSelectorAndActionAssociations(ruleStore));
        assertEquals("illegal", ex.getMessage(),
                "Exception from RuleStore.addTransparentPathPart should propagate unchanged");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void duplicateRuleAdditionThrowsIllegalArgumentExceptionOnSecondInvocation() {
//         // Strict RuleStore mock: first call succeeds, second call for same selector throws
//         Set<String> added = new HashSet<>();
//         doAnswer(invocation -> {
//             ElementSelector selector = invocation.getArgument(0);
//             if (!added.add(selector.getPattern())) {
//                 throw new IllegalArgumentException("duplicate selector: " + selector.getPattern());
//             }
//             return null;
//         }).when(ruleStore).addRule(any(ElementSelector.class), any());
//         TestConfigurator configurator = new TestConfigurator();
//         // first invocation should succeed
//         configurator.invokeAddElementSelectorAndActionAssociations(ruleStore);
//         // second invocation should fail because the mock now rejects duplicates
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
//                 () -> configurator.invokeAddElementSelectorAndActionAssociations(ruleStore));
//         assertTrue(ex.getMessage().startsWith("duplicate selector"),
//                 "Exception message should indicate a duplicate selector");
//     }
}