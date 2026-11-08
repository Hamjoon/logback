package ch.qos.logback.core.model.processor;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ModelInterpretationContext_getTopModel_Test {

    @Mock
    Context // provided by MockitoExtension
    mockContext;

    @Test
    void getTopModel_ShouldReturnNull_WhenContextIsNewlyCreated() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        // when
        Model result = mic.getTopModel();
        // then
        assertNull(result, "Top model should be null for a freshly created context");
    }

    @Test
    void getTopModel_ShouldReturnModelSetViaSetTopModel() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Model mockModel = mock(Model.class);
        mic.setTopModel(mockModel);
        // when
        Model result = mic.getTopModel();
        // then
        assertSame(mockModel, result, "Top model returned should be the same instance set via setTopModel");
    }

    @Test
    void copyConstructor_ShouldNotCopyTopModel_FromOriginalContext() {
        // given
        ModelInterpretationContext original = new ModelInterpretationContext(mockContext);
        Model mockModel = mock(Model.class);
        // set a top model in the original
        original.setTopModel(mockModel);
        // when
        ModelInterpretationContext copy = new ModelInterpretationContext(original);
        Model copyTopModel = copy.getTopModel();
        // then
        assertNull(copyTopModel, "Copy constructor does not copy the topModel field; the copied context should have a null top model");
    }

    @Test
    void getTopModel_ShouldReturnNull_AfterSetTopModelNull() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Model firstModel = mock(Model.class);
        // set a non‑null model first
        mic.setTopModel(firstModel);
        // now clear it
        mic.setTopModel(null);
        // when
        Model result = mic.getTopModel();
        // then
        assertNull(result, "Top model should be null after setTopModel(null) is called");
    }

    @Test
    void getTopModel_ShouldRemainUnchanged_WhenModelStackIsManipulated() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Model topModel = mock(Model.class);
        Model stackedModel = mock(Model.class);
        // establish the top model
        mic.setTopModel(topModel);
        // push a different model onto the internal stack
        mic.pushModel(stackedModel);
        // when
        Model result = mic.getTopModel();
        // then
        assertSame(topModel, result, "Top model should stay the same regardless of changes to the internal model stack");
    }

    @Test
    void getTopModel_ShouldReturnMostRecentlySetModel_WhenSetMultipleTimes() {
        // given
        ModelInterpretationContext mic = new ModelInterpretationContext(mockContext);
        Model firstModel = mock(Model.class);
        Model secondModel = mock(Model.class);
        // first assignment
        mic.setTopModel(firstModel);
        // second (most recent) assignment
        mic.setTopModel(secondModel);
        // when
        Model result = mic.getTopModel();
        // then
        assertSame(secondModel, result, "The most recently set model should be returned by getTopModel()");
    }
}
