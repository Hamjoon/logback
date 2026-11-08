package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.EmptyStackException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Stack;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_popModel_Test {

    private ModelInterpretationContext mic;

    @Mock
    private Context // Context has no behaviour needed for this test
    mockContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh MIC for each test
        mic = new ModelInterpretationContext(mockContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        mic = null;
    }

    private static Stream<Arguments> singlePushProvider() {
        Model model = new Model() {
        };
        return Stream.of(// after pop stack not empty? false because only one element
        Arguments.of(model, model, false), // null push case – stack becomes empty after pop
        Arguments.of(null, null, true));
    }

    @Test
    void popModel_returnsPushedModel_andRemovesItFromStack() {
        // GIVEN
        // anonymous concrete Model instance
        Model first = new Model() {
        };
        Model second = new Model() {
        };
        mic.pushModel(first);
        // second is now on top
        mic.pushModel(second);
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(second, popped, "popModel should return the model that was last pushed");
        assertFalse(mic.isModelStackEmpty(), "Stack should still contain one model after one pop");
        assertSame(first, mic.peekModel(), "The remaining model on the stack should be the first one pushed");
    }

    @Test
    void popModel_onEmptyStack_throwsEmptyStackException() {
        // GIVEN
        assertTrue(mic.isModelStackEmpty(), "Pre‑condition: model stack must be empty");
        // WHEN / THEN
        assertThrows(EmptyStackException.class, mic::popModel, "popModel should throw EmptyStackException when the stack is empty");
    }

    @Test
    void popModel_singleModel_returnsItAndEmptiesStack() {
        // GIVEN a single model on the stack
        Model modelA = new Model() {
        };
        mic.pushModel(modelA);
        assertFalse(mic.isModelStackEmpty());
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(modelA, popped, "popModel should return the only model present");
        assertTrue(mic.isModelStackEmpty(), "Stack must be empty after popping the sole element");
    }

    @Test
    void popModel_twoModels_returnsSecondAndLeavesFirst() {
        // GIVEN two models pushed in order A then B
        Model modelA = new Model() {
        };
        Model modelB = new Model() {
        };
        mic.pushModel(modelA);
        mic.pushModel(modelB);
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(modelB, popped, "popModel must return the most‑recently pushed model (B)");
        assertFalse(mic.isModelStackEmpty(), "Stack should still contain model A");
        assertSame(modelA, mic.peekModel(), "Remaining model should be the first one (A)");
    }

    @Test
    void popModel_threeSequentialPops_behaveAsExpected() {
        // GIVEN three models A, B, C pushed in that order
        Model modelA = new Model() {
        };
        Model modelB = new Model() {
        };
        Model modelC = new Model() {
        };
        mic.pushModel(modelA);
        mic.pushModel(modelB);
        mic.pushModel(modelC);
        // FIRST POP -> C
        assertSame(modelC, mic.popModel(), "First pop should return C");
        // SECOND POP -> B
        assertSame(modelB, mic.popModel(), "Second pop should return B");
        // THIRD POP -> A
        assertSame(modelA, mic.popModel(), "Third pop should return A");
        // FOURTH POP -> exception
        assertThrows(EmptyStackException.class, mic::popModel, "Fourth pop on an empty stack must throw EmptyStackException");
    }

    @Test
    void popModel_nullReference_returnsNullWithoutException() {
        // GIVEN a null reference pushed onto the stack
        mic.pushModel(null);
        assertFalse(mic.isModelStackEmpty());
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertNull(popped, "popModel should return null when a null reference was pushed");
        assertTrue(mic.isModelStackEmpty(), "Stack must be empty after popping the null entry");
    }

    @Test
    void popModel_duplicateInstance_returnsSameInstanceTwice() {
        // GIVEN the same model instance pushed twice
        Model modelX = new Model() {
        };
        mic.pushModel(modelX);
        // duplicate push
        mic.pushModel(modelX);
        // FIRST POP -> modelX
        Model firstPop = mic.popModel();
        // SECOND POP -> modelX again
        Model secondPop = mic.popModel();
        // THEN
        assertSame(modelX, firstPop, "First pop should return the duplicated model instance");
        assertSame(modelX, secondPop, "Second pop should also return the same instance");
        assertTrue(mic.isModelStackEmpty(), "Stack should be empty after both pops");
    }

    @Test
    void popModel_internalStackIsNull_throwsNullPointerException() throws Exception {
        // GIVEN: force the private field 'modelStack' to be null via reflection
        Field stackField = ModelInterpretationContext.class.getDeclaredField("modelStack");
        stackField.setAccessible(true);
        stackField.set(mic, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, mic::popModel, "If the internal modelStack is null, popModel must throw NullPointerException");
    }

    @ParameterizedTest(name = "popModel after pushing {0}")
    @MethodSource("singlePushProvider")
    void popModel_parameterisedSinglePush(Model toPush, Model expected, boolean expectEmptyAfter) {
        // GIVEN
        mic.pushModel(toPush);
        // WHEN
        Model popped = mic.popModel();
        // THEN
        assertSame(expected, popped, "popModel should return the element that was pushed");
        assertEquals(expectEmptyAfter, mic.isModelStackEmpty(), "Stack emptiness after pop should match expectation");
    }
}
