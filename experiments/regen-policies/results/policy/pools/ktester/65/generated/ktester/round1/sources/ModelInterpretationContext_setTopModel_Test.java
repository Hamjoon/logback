package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_setTopModel_Test {

    @Mock
    Context // dependency required by the constructor
    mockContext;

    @Mock
    Model // first model instance
    mockModel1;

    @Mock
    Model // second model instance
    mockModel2;

    // system under test
    ModelInterpretationContext mic;

    /**
     * Minimal concrete subclass of {@link Model}. The real {@code Model}
     * class in Logback is abstract; this dummy implementation provides a
     * concrete type that can be instantiated for testing purposes.
     */
    static class DummyModel extends Model {
        // No additional behaviour required for the current tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the class under test with a mocked Context.
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        mic = null;
    }

    private static Stream<Arguments> provideModelsForSetTopModel() {
        // Using a real subclass instance and a Mockito mock
        return Stream.of(// subclass of Model
        arguments(new DummyModel()), // explicit null case
        arguments(null));
    }

    @Test
    void setTopModel_assignsModel_correctly() {
        // GIVEN
        // mockModel1 is already prepared by Mockito
        // WHEN
        mic.setTopModel(mockModel1);
        // THEN
        assertSame(mockModel1, mic.getTopModel(), "The top model returned by getTopModel() should be the same instance passed to setTopModel()");
    }

    @Test
    void setTopModel_overwritesPreviousValue() {
        // GIVEN
        mic.setTopModel(mockModel1);
        assertSame(mockModel1, mic.getTopModel(), "Initial assignment should succeed");
        // WHEN
        mic.setTopModel(mockModel2);
        // THEN
        assertSame(mockModel2, mic.getTopModel(), "A subsequent call to setTopModel should replace the previous model");
    }

    @Test
    void setTopModel_acceptsNull_andClearsPreviousValue() {
        // GIVEN a previous non‑null model
        mic.setTopModel(mockModel1);
        assertNotNull(mic.getTopModel());
        // WHEN
        mic.setTopModel(null);
        // THEN
        assertNull(mic.getTopModel(), "Passing null to setTopModel should result in a null top model");
    }

    @ParameterizedTest(name = "{index} => topModel={0}")
    @MethodSource("provideModelsForSetTopModel")
    void setTopModel_storesProvidedModel(Model topModel) {
        // WHEN
        mic.setTopModel(topModel);
        // THEN
        assertSame(topModel, mic.getTopModel(), "getTopModel() must return exactly the instance supplied to setTopModel()");
    }

    @Test
    void setTopModel_idempotentSameInstance() {
        // GIVEN
        mic.setTopModel(mockModel1);
        assertSame(mockModel1, mic.getTopModel());
        // WHEN: set the same instance again
        mic.setTopModel(mockModel1);
        // THEN
        assertSame(mockModel1, mic.getTopModel(), "Calling setTopModel with the same reference should leave the stored reference unchanged");
    }

    @Test
    void setTopModel_doesNotAffectModelStack() {
        // GIVEN: push a model onto the internal model stack
        mic.pushModel(mockModel1);
        assertSame(mockModel1, mic.peekModel(), "Pre‑condition: the model stack's top element should be mockModel1");
        // WHEN: set a different top model
        mic.setTopModel(mockModel2);
        // THEN: topModel is updated, but stack remains untouched
        assertSame(mockModel2, mic.getTopModel(), "Top model should be mockModel2 after setTopModel");
        assertSame(mockModel1, mic.peekModel(), "The model stack should still return mockModel1 as its top element");
    }
}
