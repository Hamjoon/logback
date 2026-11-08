package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setTopModel_Test {

    @Mock
    Context // mocked Context – no real behaviour needed
    mockContext;

    private ModelInterpretationContext mic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ModelInterpretationContext for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no state leaks between tests
        mic = null;
    }

    private static Stream<Arguments> basicAssignmentProvider() {
        Model modelA = mock(Model.class);
        Model modelB = mock(Model.class);
        return Stream.of(// Assign a non‑null Model
        Arguments.of(new Model() {
        }, new Model() {
        }, "non‑null fresh Model"), // Assign null (clears the top model)
        Arguments.of(null, null, "null clears top model"), // Assign the same instance again
        Arguments.of(modelA, modelA, "same instance re‑assigned"), // Replace with a different instance
        Arguments.of(modelB, modelB, "different instance replaces previous"));
    }

    @Test
    void setTopModel_assignsTopModelCorrectly() {
        // Given: a fresh ModelInterpretationContext with no top model
        assertNull(mic.getTopModel(), "Top model should be null before setting it");
        // And: a mock Model instance to be used as the new top model
        Model mockModel = org.mockito.Mockito.mock(Model.class);
        // When: setTopModel is invoked
        mic.setTopModel(mockModel);
        // Then: getTopModel should return the same instance that was set
        assertSame(mockModel, mic.getTopModel(), "getTopModel should return the model that was set via setTopModel");
    }

    /* --------------------------------------------------------------
     *  Basic assignment tests (including null)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: setTopModel({0})")
    @MethodSource("basicAssignmentProvider")
    void setTopModel_assignsCorrectly(Model inputModel, Model expectedModel, String description) {
        // GIVEN the context starts with no top model
        assertNull(mic.getTopModel(), "Pre‑condition: top model must be null");
        // WHEN setTopModel is invoked
        mic.setTopModel(inputModel);
        // THEN getTopModel returns the expected instance (may be null)
        assertSame(expectedModel, mic.getTopModel(), "Top model should be the instance that was set – " + description);
    }

    /* --------------------------------------------------------------
     *  Side‑effect checks – stacks must stay untouched
     * -------------------------------------------------------------- */
    @Test
    void setTopModel_doesNotAlterObjectOrModelStacks() {
        // GIVEN a fresh context – both stacks are empty
        assertTrue(mic.getObjectStack().isEmpty(), "Object stack should start empty");
        assertTrue(mic.isModelStackEmpty(), "Model stack should start empty");
        // WHEN we set a top model
        Model anyModel = mock(Model.class);
        mic.setTopModel(anyModel);
        // THEN the stacks remain unchanged
        assertTrue(mic.getObjectStack().isEmpty(), "Setting top model must not add anything to the object stack");
        assertTrue(mic.isModelStackEmpty(), "Setting top model must not add anything to the model stack");
    }

    /* --------------------------------------------------------------
     *  Repeated calls with same or different models
     * -------------------------------------------------------------- */
    @Test
    void setTopModelRepeatedCalls_behaveAsExpected() {
        Model modelA = mock(Model.class);
        Model modelB = mock(Model.class);
        // 1st call – set to modelA
        mic.setTopModel(modelA);
        assertSame(modelA, mic.getTopModel(), "After first call top model should be modelA");
        // 2nd call – same instance again
        mic.setTopModel(modelA);
        assertSame(modelA, mic.getTopModel(), "Calling again with same instance must leave it unchanged");
        // 3rd call – different instance
        mic.setTopModel(modelB);
        assertSame(modelB, mic.getTopModel(), "Top model should now be modelB");
        // 4th call – null clears it
        mic.setTopModel(null);
        assertNull(mic.getTopModel(), "After setting null the top model must be null");
    }

    /* --------------------------------------------------------------
     *  Acceptance of a concrete subclass of Model
     * -------------------------------------------------------------- */
    @Test
    void setTopModel_acceptsAnonymousSubclassOfModel() {
        // GIVEN an anonymous subclass instance
        Model anonymousSubclass = new Model() {
        };
        // WHEN we set it as the top model
        mic.setTopModel(anonymousSubclass);
        // THEN the same instance is returned and its class is a subclass of Model
        Model retrieved = mic.getTopModel();
        assertSame(anonymousSubclass, retrieved, "The exact anonymous subclass instance must be stored");
        assertTrue(retrieved.getClass().getSuperclass() == Model.class, "Runtime type should be a direct subclass of Model");
    }
}
