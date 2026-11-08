package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.conditional.*;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import org.junit.jupiter.api.*;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.sift.AppenderTracker;
import ch.qos.logback.core.sift.SiftModelHandler;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.joran.spi.ElementPath;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import ch.qos.logback.core.joran.action.Action;

@ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addElementSelectorAndActionAssociations_Test {

    @Mock
    RuleStore mockRuleStore;

    private TestConfigurator configurator;

    /**
     * A concrete subclass that simply exposes the protected method.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        // expose the protected method for the test
        void exposeAddElementSelectorAndActionAssociations(RuleStore rs) {
            super.addElementSelectorAndActionAssociations(rs);
        }

        // -----------------------------------------------------------------
        // The following abstract methods are not needed for this test.
        // They are implemented with empty bodies.
        // -----------------------------------------------------------------
        @Override
        protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
            // no‑op
        }

        @Override
        public void buildModelInterpretationContext() {
            // no‑op
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return null;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // no‑op
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // no‑op
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // no‑op
        }
    }

    /**
     * Simple RuleStore implementation that records calls – used as a Mockito‑like mock.
     */
    private static class RecordingRuleStore implements RuleStore {

        private final List<ElementSelector> selectors = new ArrayList<>();

        private final List<String> transparentParts = new ArrayList<>();

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            selectors.add(selector);
        }

        @Override
        public void addTransparentPathPart(String part) {
            transparentParts.add(part);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            // not needed for these tests
            return () -> null;
        }

        List<ElementSelector> getSelectors() {
            return Collections.unmodifiableList(selectors);
        }

        List<String> getTransparentParts() {
            return Collections.unmodifiableList(transparentParts);
        }
    }

    /**
     * Throws RuntimeException from every {@code addRule} call.
     */
    private static class ExceptionThrowingRuleStore implements RuleStore {

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            throw new RuntimeException("addRule failure");
        }

        @Override
        public void addTransparentPathPart(String part) {
            // no‑op – not reached in this test
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }
    }

    /**
     * Throws RuntimeException from every {@code addTransparentPathPart} call.
     */
    private static class ExceptionThrowingTransparentPathRuleStore implements RuleStore {

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            // no‑op – not reached in this test
        }

        @Override
        public void addTransparentPathPart(String part) {
            throw new RuntimeException("addTransparentPathPart failure");
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }
    }

    /**
     * Detects duplicate rule/transparent‑path registrations.
     */
    private static class DuplicateDetectingRuleStore implements RuleStore {

        private final Set<String> selectors = new HashSet<>();

        private final Set<String> transparentParts = new HashSet<>();

        private boolean duplicateRuleAdditions = false;

        private boolean duplicateTransparentPathAdditions = false;

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            if (!selectors.add(selector.getPattern())) {
                duplicateRuleAdditions = true;
            }
        }

        @Override
        public void addTransparentPathPart(String part) {
            if (!transparentParts.add(part)) {
                duplicateTransparentPathAdditions = true;
            }
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }

        int getAddedRuleCount() {
            return selectors.size();
        }

        int getTransparentPathPartsCount() {
            return transparentParts.size();
        }

        boolean hasDuplicateRuleAdditions() {
            return duplicateRuleAdditions;
        }

        boolean hasDuplicateTransparentPathAdditions() {
            return duplicateTransparentPathAdditions;
        }
    }

    /**
     * Records the exact order of rule and transparent‑path invocations.
     */
    private static class OrderedRecordingRuleStore implements RuleStore {

        private final List<String> ruleCallSequence = new ArrayList<>();

        private final List<String> transparentPathCallSequence = new ArrayList<>();

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            ruleCallSequence.add(selector.getPattern());
        }

        @Override
        public void addTransparentPathPart(String part) {
            transparentPathCallSequence.add(part);
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }

        List<String> getRuleCallSequence() {
            return Collections.unmodifiableList(ruleCallSequence);
        }

        List<String> getTransparentPathCallSequence() {
            return Collections.unmodifiableList(transparentPathCallSequence);
        }
    }

    /**
     * Simple collector that stores all selector patterns added.
     */
    private static class CollectingRuleStore implements RuleStore {

        private final Set<String> addedSelectors = new HashSet<>();

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            addedSelectors.add(selector.getPattern());
        }

        @Override
        public void addTransparentPathPart(String part) {
            // transparent parts are irrelevant for this particular test
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }

        Set<String> getAddedSelectors() {
            return Collections.unmodifiableSet(addedSelectors);
        }
    }

    /**
     * Simulates a NPE from {@code addTransparentPathPart}.
     */
    private static class NullTransparentPathRuleStore implements RuleStore {

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            // normal behaviour – do nothing
        }

        @Override
        public void addTransparentPathPart(String part) {
            throw new NullPointerException("transparent path part is null");
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }
    }

    /**
     * Throws IllegalArgumentException from {@code addRule}.
     */
    private static class IllegalArgumentRuleStore implements RuleStore {

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            throw new IllegalArgumentException("illegal argument in addRule");
        }

        @Override
        public void addTransparentPathPart(String part) {
            // no‑op
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }
    }

    /**
     * Throws IllegalArgumentException from {@code addTransparentPathPart}.
     */
    private static class TransparentPathIllegalArgumentRuleStore implements RuleStore {

        @Override
        public void addRule(ElementSelector selector, Supplier<Action> actionSupplier) {
            // no‑op
        }

        @Override
        public void addTransparentPathPart(String part) {
            throw new IllegalArgumentException("illegal argument in addTransparentPathPart");
        }

        @Override
        public Supplier<Action> matchActions(ElementPath elementPath) {
            return () -> null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        configurator = new TestConfigurator();
        mockRuleStore = new RecordingRuleStore();
    }

    @Test
    void testAddElementSelectorAndActionAssociations() {
        // Act
        configurator.exposeAddElementSelectorAndActionAssociations(mockRuleStore);
        // -------------------------------------------------------------
        // Verify that 21 rules have been added with the expected selectors
        // -------------------------------------------------------------
        List<String> expectedPatterns = List.of("*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift");
        List<ElementSelector> capturedSelectors = ((RecordingRuleStore) mockRuleStore).getSelectors();
        assertEquals(expectedPatterns.size(), capturedSelectors.size(), "Number of captured selectors does not match expectation");
        for (int i = 0; i < expectedPatterns.size(); i++) {
            String expected = expectedPatterns.get(i);
            String actual = capturedSelectors.get(i).getPattern();
            assertEquals(expected, actual, "Selector at index " + i + " differs");
        }
        // -------------------------------------------------------------
        // Verify transparent path parts
        // -------------------------------------------------------------
        List<String> expectedTransparent = List.of("if", "then", "else", "sift");
        assertEquals(expectedTransparent, ((RecordingRuleStore) mockRuleStore).getTransparentParts(), "Transparent path parts differ");
    }

    /* --------------------------------------------------------------------- *
     *  1.  Normal behaviour – selectors & transparent path parts registration *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_normal() {
        // Act
        configurator.exposeAddElementSelectorAndActionAssociations(mockRuleStore);
        // Verify selectors
        List<String> expectedPatterns = List.of("*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift");
        List<ElementSelector> capturedSelectors = ((RecordingRuleStore) mockRuleStore).getSelectors();
        assertEquals(expectedPatterns.size(), capturedSelectors.size(), "Number of captured selectors does not match expectation");
        for (int i = 0; i < expectedPatterns.size(); i++) {
            assertEquals(expectedPatterns.get(i), capturedSelectors.get(i).getPattern(), "Selector at index " + i + " differs");
        }
        // Verify transparent path parts
        List<String> expectedTransparent = List.of("if", "then", "else", "sift");
        assertEquals(expectedTransparent, ((RecordingRuleStore) mockRuleStore).getTransparentParts(), "Transparent path parts differ");
    }

    /* --------------------------------------------------------------------- *
     *  2.  Null RuleStore argument should raise NPE                        *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_nullRuleStore() {
        assertThrows(NullPointerException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(null), "Passing a null RuleStore must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  3.  Propagation of unchecked exceptions from addRule                *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_ruleStoreAddRuleThrows() {
        RuleStore throwingStore = new ExceptionThrowingRuleStore();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(throwingStore), "RuntimeException from RuleStore.addRule must propagate");
        assertEquals("addRule failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  4.  Propagation of unchecked exceptions from addTransparentPathPart *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_ruleStoreAddTransparentPathThrows() {
        RuleStore throwingStore = new ExceptionThrowingTransparentPathRuleStore();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(throwingStore), "RuntimeException from RuleStore.addTransparentPathPart must propagate");
        assertEquals("addTransparentPathPart failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  5.  Duplicate invocation – the store should ignore duplicates     *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_duplicateInvocation() {
        DuplicateDetectingRuleStore dupStore = new DuplicateDetectingRuleStore();
        // first invocation
        configurator.exposeAddElementSelectorAndActionAssociations(dupStore);
        // second invocation – duplicates must be ignored, not cause failures
        configurator.exposeAddElementSelectorAndActionAssociations(dupStore);
        // The store should have recorded each selector only once
        assertEquals(21, dupStore.getAddedRuleCount(), "Exactly 21 unique selectors should be recorded");
        assertEquals(4, dupStore.getTransparentPathPartsCount(), "Exactly 4 unique transparent path parts should be recorded");
        assertFalse(dupStore.hasDuplicateRuleAdditions(), "No duplicate rule additions should have been detected");
        assertFalse(dupStore.hasDuplicateTransparentPathAdditions(), "No duplicate transparent‑path additions should have been detected");
    }

    /* --------------------------------------------------------------------- *
     *  6.  Call order verification                                         *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_callOrder() {
        OrderedRecordingRuleStore orderedStore = new OrderedRecordingRuleStore();
        configurator.exposeAddElementSelectorAndActionAssociations(orderedStore);
        List<String> expectedRuleOrder = List.of("*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift");
        List<String> expectedTransparentOrder = List.of("if", "then", "else", "sift");
        assertEquals(expectedRuleOrder, orderedStore.getRuleCallSequence(), "Rule selectors must be added in the exact source order");
        assertEquals(expectedTransparentOrder, orderedStore.getTransparentPathCallSequence(), "Transparent path parts must be added in the exact source order");
    }

    /* --------------------------------------------------------------------- *
     *  7.  No extra selectors are added                                     *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_noExtraSelectors() {
        CollectingRuleStore collectingStore = new CollectingRuleStore();
        configurator.exposeAddElementSelectorAndActionAssociations(collectingStore);
        Set<String> expected = Set.of("*/variable", "*/property", "*/substitutionProperty", "configuration/import", "configuration/timestamp", "configuration/shutdownHook", "configuration/sequenceNumberGenerator", "configuration/serializeModel", "configuration/define", "configuration/evaluator", "configuration/contextProperty", "configuration/conversionRule", "configuration/statusListener", "*/appender", "configuration/appender/appender-ref", "configuration/newRule", "*/param", "*/if", "*/if/then", "*/if/else", "*/appender/sift");
        assertEquals(expected, collectingStore.getAddedSelectors(), "Only the expected selectors should be present");
        assertEquals(21, collectingStore.getAddedSelectors().size(), "Exactly 21 selectors must be added");
    }

    /* --------------------------------------------------------------------- *
     *  8.  NullPointerException from transparent‑path registration          *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_nullTransparentPathPart() {
        RuleStore nullTransparentStore = new NullTransparentPathRuleStore();
        assertThrows(NullPointerException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(nullTransparentStore), "NullPointerException from addTransparentPathPart must propagate");
    }

    /* --------------------------------------------------------------------- *
     *  9.  IllegalArgumentException from addRule                             *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_ruleStoreIllegalArgument() {
        RuleStore illegalArgumentStore = new IllegalArgumentRuleStore();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(illegalArgumentStore), "IllegalArgumentException from addRule must propagate");
        assertEquals("illegal argument in addRule", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     * 10. IllegalArgumentException from addTransparentPathPart               *
     * --------------------------------------------------------------------- */
    @Test
    void testAddElementSelectorAndActionAssociations_transparentPathIllegalArgument() {
        RuleStore illegalTransparentStore = new TransparentPathIllegalArgumentRuleStore();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> configurator.exposeAddElementSelectorAndActionAssociations(illegalTransparentStore), "IllegalArgumentException from addTransparentPathPart must propagate");
        assertEquals("illegal argument in addTransparentPathPart", ex.getMessage());
    }
}
