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
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setTopModel_Test {

    @Mock
    Context // injected by Mockito
    mockContext;

    ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test case
        mic = new ModelInterpretationContext(mockContext);
    }

    /**
     * Supplies the arguments for {@link #setTopModel_behaviour(String, Model, Model, Model)}.
     *
     * <p>Four distinct cases are provided, mirroring the JSON test specifications.
     *
     * @return a {@code Stream} of {@code Arguments}
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> topModelProvider() {
        // Create distinct mock Model instances for clarity in the assertions
        Model modelA = Mockito.mock(Model.class);
        Model modelB = Mockito.mock(Model.class);
        Model modelC = Mockito.mock(Model.class);
        return Stream.of(// 1. Assign a non‑null Model instance when no previous model exists
        org.junit.jupiter.params.provider.Arguments.of("Assign non‑null Model A when no previous top model", // initialModel
        null, // newModel
        modelA, // expectedModel
        modelA), // 2. Assign null, clearing any existing top model
        org.junit.jupiter.params.provider.Arguments.of("Assign null to clear existing top model", // initialModel
        modelA, // newModel
        null, // expectedModel
        null), // 3. Replace an existing top model with a different instance
        org.junit.jupiter.params.provider.Arguments.of("Replace Model A with a different Model B", // initialModel
        modelA, // newModel
        modelB, // expectedModel
        modelB), // 4. Set the same Model instance again (idempotent)
        org.junit.jupiter.params.provider.Arguments.of("Assign the same Model C again (idempotent)", // initialModel
        modelC, // newModel
        modelC, // expectedModel
        modelC));
    }

    @Test
    void setTopModel_assignsModelCorrectly() {
        // given
        Model mockModel = org.mockito.Mockito.mock(Model.class);
        // when
        mic.setTopModel(mockModel);
        // then
        assertSame(mockModel, mic.getTopModel(), "The top model returned by getTopModel() should be the same instance that was set.");
    }

    @Test
    void setTopModel_withNull_clearsTopModel() {
        // given: a non‑null model is already set
        Model firstModel = org.mockito.Mockito.mock(Model.class);
        mic.setTopModel(firstModel);
        assertSame(firstModel, mic.getTopModel(), "Pre‑condition: top model should be the first model.");
        // when: setTopModel is called with null
        mic.setTopModel(null);
        // then
        assertNull(mic.getTopModel(), "After setting null, getTopModel() should return null.");
    }

    /**
     * Parameterized test that exercises {@link ModelInterpretationContext#setTopModel(Model)}.
     *
     * @param description   a human‑readable description of the scenario (used as test display name)
     * @param initialModel  the model that is set before the actual call under test (may be {@code null})
     * @param newModel      the model passed to {@code setTopModel} in this test step (may be {@code null})
     * @param expectedModel the model we expect {@code getTopModel()} to return after the call
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("topModelProvider")
    void setTopModel_behaviour(String description, Model initialModel, Model newModel, Model expectedModel) {
        // Arrange: optionally set an initial top model to simulate a prior state
        if (initialModel != null) {
            mic.setTopModel(initialModel);
            assertSame(initialModel, mic.getTopModel(), "Pre‑condition failed: initial top model should be set correctly.");
        }
        // Act: set the new top model (could be null or the same as the initial one)
        mic.setTopModel(newModel);
        // Assert: the top model after the call must match the expectation
        assertSame(expectedModel, mic.getTopModel(), "After invoking setTopModel, getTopModel() should return the expected instance.");
    }
}
