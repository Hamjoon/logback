package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getTopModel_Test {

    @Mock
    Context // mocked Context required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    /* --------------------------------------------------------------------- */
    private static class SimpleModel extends Model {

        private final int id;

        SimpleModel(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "SimpleModel{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    private static SimpleModel model(int id) {
        return new SimpleModel(id);
    }

    private static Stream<Arguments> setTopModelScenarios() {
        return Stream.of(// first set, then replace with a different model
        Arguments.of(model(1), model(2), model(2), "replace with different instance"), // set null explicitly
        Arguments.of(null, null, null, "explicit null set"), // set same instance twice
        Arguments.of(model(3), model(3), model(3), "same instance set twice"));
    }

    @Test
    void getTopModel_ShouldReturnNull_WhenNotSet() {
        // GIVEN – a newly created ModelInterpretationContext (setUp does this)
        // WHEN
        Model result = mic.getTopModel();
        // THEN
        assertNull(result, "Top model should be null before any value is set");
    }

    @Test
    void getTopModel_ShouldReturnModel_ThatWasSet() {
        // GIVEN
        // Model is a simple POJO; no special init required
        Model expectedModel = new Model();
        mic.setTopModel(expectedModel);
        // WHEN
        Model actual = mic.getTopModel();
        // THEN
        assertSame(expectedModel, actual, "getTopModel should return the model that was set via setTopModel");
    }

    @Test
    void getTopModel_ShouldBeNull_InCopyConstructedInstance_WhenOriginalHasTopModelSet() {
        // GIVEN – original MIC with a top model
        Model originalTop = new Model();
        mic.setTopModel(originalTop);
        // WHEN – create a copy via the copy‑constructor
        ModelInterpretationContext copyMic = new ModelInterpretationContext(mic);
        // THEN – the copy does NOT inherit the top model (copy‑constructor does not copy it)
        assertNull(copyMic.getTopModel(), "Copy‑constructed ModelInterpretationContext should have a null top model even if the original had one");
    }

    @Test
    void getTopModel_ShouldReplacePreviousModel_WhenSetTwiceWithDifferentInstances() {
        // GIVEN – first model set
        SimpleModel first = model(1);
        mic.setTopModel(first);
        assertSame(first, mic.getTopModel(), "Pre‑condition: first model should be returned");
        // WHEN – second (different) model is set
        SimpleModel second = model(2);
        mic.setTopModel(second);
        // THEN – the new model replaces the old one
        assertSame(second, mic.getTopModel(), "After setting a different model, getTopModel should return the newly set instance");
    }

    @Test
    void getTopModel_ShouldReturnNull_WhenExplicitlySetToNull() {
        // GIVEN – set top model to null explicitly
        mic.setTopModel(null);
        // WHEN
        Model result = mic.getTopModel();
        // THEN
        assertNull(result, "Explicitly setting top model to null must be reflected by getTopModel()");
    }

    @Test
    void getTopModel_ShouldReturnSameInstance_WhenSetTwiceWithSameInstance() {
        // GIVEN – a single model instance
        SimpleModel model = model(3);
        mic.setTopModel(model);
        // second call with the same reference
        mic.setTopModel(model);
        // WHEN
        Model result = mic.getTopModel();
        // THEN
        assertSame(model, result, "Calling setTopModel twice with the same instance must leave getTopModel returning that instance");
    }

    @Test
    void getTopModel_ShouldRemainUnchanged_AfterPushAndPopOnModelStack() {
        // GIVEN – push a model onto the internal stack
        SimpleModel stacked = model(99);
        mic.pushModel(stacked);
        // Also set a different top model
        SimpleModel top = model(4);
        mic.setTopModel(top);
        // WHEN – pop the previously pushed model
        Model popped = mic.popModel();
        assertSame(stacked, popped, "Sanity check: popped model should be the one we pushed");
        // THEN – getTopModel must still return the model set via setTopModel()
        assertSame(top, mic.getTopModel(), "Popping the model stack must not affect the top model stored via setTopModel()");
    }

    @Test
    void getTopModel_ShouldRemainUnchanged_AfterClearingModelStack() {
        // GIVEN – set an initial top model
        SimpleModel initialTop = model(5);
        mic.setTopModel(initialTop);
        // Push several other models onto the stack
        mic.pushModel(model(10));
        mic.pushModel(model(11));
        mic.pushModel(model(12));
        // Pop all models until the stack is empty
        while (!mic.isModelStackEmpty()) {
            mic.popModel();
        }
        assertTrue(mic.isModelStackEmpty(), "Model stack should be empty after popping all elements");
        // WHEN – we query the top model
        Model result = mic.getTopModel();
        // THEN – it must still be the originally set top model
        assertSame(initialTop, result, "Clearing the internal model stack must not alter the top model previously set");
    }

    @ParameterizedTest(name = "Scenario: {3}")
    @MethodSource("setTopModelScenarios")
    void getTopModel_ParameterizedScenarios(Model firstSet, Model secondSet, Model expected, String description) {
        // GIVEN – optionally set the first model (null is allowed)
        if (firstSet != null) {
            mic.setTopModel(firstSet);
        } else {
            mic.setTopModel(null);
        }
        // WHEN – set the second model (could be null or same as first)
        mic.setTopModel(secondSet);
        // THEN – verify the resulting top model matches expectation
        assertSame(expected, mic.getTopModel(), () -> "After scenario '" + description + "', getTopModel should return the expected instance");
    }
}
