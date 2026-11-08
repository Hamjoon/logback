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
    Context // mocked dependency required by the constructor
    mockContext;

    // instance under test
    ModelInterpretationContext mic;

    /**
     * Simple concrete {@link Model} used only for identity checks.
     */
    private static final class MockModel extends Model {

        private final String name;

        MockModel(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    /**
     * Helper holder for the parameterized test.
     */
    private static final class ArgumentsHolder {

        final MockModel initial;

        final MockModel overwrite;

        // either model name or "null"
        final String expected;

        ArgumentsHolder(MockModel initial, MockModel overwrite, String expected) {
            this.initial = initial;
            this.overwrite = overwrite;
            this.expected = expected;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        mic = new ModelInterpretationContext(mockContext);
    }

    private static Stream<ArgumentsHolder> topModelProvider() {
        return Stream.of(// case: not set -> null
        new ArgumentsHolder(null, null, "null"), // case: set once -> same instance
        new ArgumentsHolder(new MockModel("first"), null, "first"), // case: set then overwrite -> second instance
        new ArgumentsHolder(new MockModel("first"), new MockModel("second"), "second"), // case: set then null -> null
        new ArgumentsHolder(new MockModel("first"), null, "null"));
    }

    @Test
    void getTopModel_returnsNullWhenNotSet() {
        // Given: a newly created ModelInterpretationContext (set up in @BeforeEach)
        // When
        Model result = mic.getTopModel();
        // Then
        assertNull(result, "Top model should be null when it has not been set");
    }

    @Test
    void getTopModel_returnsModelSetViaSetter() {
        // Given
        Model dummyModel = new Model() {
        };
        mic.setTopModel(dummyModel);
        // When
        Model result = mic.getTopModel();
        // Then
        assertSame(dummyModel, result, "getTopModel should return the model that was set via setTopModel");
    }

    @Test
    void copyConstructorDoesNotCopyTopModel() {
        // Given: a MIC with a top model already set
        Model originalModel = new Model() {
        };
        mic.setTopModel(originalModel);
        // When: we create a copy using the copy‑constructor
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // Then: the copy's top model should be null (the original top model is not duplicated)
        assertNull(copy.getTopModel(), "The copy constructor should not copy the topModel field; it should be null in the new instance");
    }

    @Test
    void copyConstructorPreservesTopModelWhenExplicitlySetAfterCopy() {
        // Given: a fresh copy of the original MIC
        ModelInterpretationContext copy = new ModelInterpretationContext(mic);
        // When: we set a top model on the copy
        MockModel newModel = new MockModel("new");
        copy.setTopModel(newModel);
        // Then: getTopModel returns the newly set model
        assertSame(newModel, copy.getTopModel(), "After setting a top model on the copy, getTopModel should return that model");
    }

    @Test
    void getTopModel_overwritesPreviousValueWhenSetTwice() {
        MockModel first = new MockModel("first");
        MockModel second = new MockModel("second");
        mic.setTopModel(first);
        // second call should overwrite the first
        mic.setTopModel(second);
        Model result = mic.getTopModel();
        assertSame(second, result, "The most recently set model should be returned after multiple setTopModel calls");
    }

    @Test
    void getTopModel_returnsNullWhenExplicitlySetToNull() {
        // Explicitly set to null – the getter must reflect that.
        mic.setTopModel(null);
        assertNull(mic.getTopModel(), "Setting top model to null should make getTopModel return null");
    }

    @ParameterizedTest(name = "[{index}] initial={0}, overwrite={1} => expected={2}")
    @MethodSource("topModelProvider")
    void getTopModel_parameterized(ArgumentsHolder holder) {
        // initial set (may be null)
        mic.setTopModel(holder.initial);
        // optional overwrite
        if (holder.overwrite != null) {
            mic.setTopModel(holder.overwrite);
        }
        Model result = mic.getTopModel();
        if ("null".equals(holder.expected)) {
            assertNull(result, "Expected top model to be null");
        } else {
            // expected name corresponds to the instance that should be returned
            assertNotNull(result, "Result should not be null");
            assertTrue(result instanceof MockModel, "Result should be a MockModel");
            MockModel mm = (MockModel) result;
            assertEquals(holder.expected, mm.getName(), "Returned model should have the expected name");
        }
    }

    @Test
    void getTopModel_unchangedAfterPushingDifferentModelOntoStack() {
        // Set a top model first
        MockModel top = new MockModel("top");
        mic.setTopModel(top);
        // Push a *different* model onto the internal modelStack
        MockModel stacked = new MockModel("stacked");
        mic.pushModel(stacked);
        // The getter must still return the original top model
        Model result = mic.getTopModel();
        assertSame(top, result, "getTopModel should still return the model set via setTopModel even after pushModel");
    }

    @Test
    void getTopModel_unchangedAfterPoppingFromModelStack() {
        // Set a top model and also push another model so that popModel has something to remove
        MockModel top = new MockModel("top");
        mic.setTopModel(top);
        mic.pushModel(new MockModel("stacked"));
        // Pop the model we just pushed
        Model popped = mic.popModel();
        assertNotNull(popped, "Popped model should not be null");
        // Verify that topModel is still the original instance
        Model result = mic.getTopModel();
        assertSame(top, result, "getTopModel must remain unchanged after popping from modelStack");
    }
}
