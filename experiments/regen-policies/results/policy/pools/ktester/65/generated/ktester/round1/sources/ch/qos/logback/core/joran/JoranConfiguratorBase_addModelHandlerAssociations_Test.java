package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.processor.DefaultProcessor;
// import ch.qos.logback.core.model.sift.SiftModelHandler;
import ch.qos.logback.core.model.processor.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.core.model.processor.conditional.*;
import org.mockito.stubbing.Answer;
import java.util.*;
// import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ElseModel;
import org.mockito.quality.Strictness;
// import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import ch.qos.logback.core.sift.SiftModelHandler;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addModelHandlerAssociations_Test {

    @Mock
    Context mockContext;

    @Mock
    SaxEventInterpretationContext mockMic;

    @Mock
    DefaultProcessor mockProcessor;

    /**
     * Mapping from model class to the expected concrete handler class.
     */
    private static final Map<Class<? extends Model>, Class<?>> EXPECTED_HANDLER_MAP = Map.ofEntries(Map.entry(ImportModel.class, ImportModelHandler.class), Map.entry(ShutdownHookModel.class, ShutdownHookModelHandler.class), Map.entry(SequenceNumberGeneratorModel.class, SequenceNumberGeneratorModelHandler.class), Map.entry(EventEvaluatorModel.class, EventEvaluatorModelHandler.class), Map.entry(DefineModel.class, DefineModelHandler.class), Map.entry(IncludeModel.class, IncludeModelHandler.class), Map.entry(ParamModel.class, ParamModelHandler.class), Map.entry(PropertyModel.class, PropertyModelHandler.class), Map.entry(TimestampModel.class, TimestampModelHandler.class), Map.entry(StatusListenerModel.class, StatusListenerModelHandler.class), Map.entry(ImplicitModel.class, ImplicitModelHandler.class), Map.entry(IfModel.class, IfModelHandler.class), Map.entry(ThenModel.class, ThenModelHandler.class), Map.entry(ElseModel.class, ElseModelHandler.class), Map.entry(SiftModel.class, SiftModelHandler.class));

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}.
     * All abstract methods are implemented as no‑ops because they are
     * irrelevant for the {@code addModelHandlerAssociations} test.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for this test.
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for this test.
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return null;
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No‑op for this test.
        }

        /**
         * Public wrapper that delegates to the protected method under test.
         */
        void invokeAddModelHandlerAssociations(DefaultProcessor processor) {
            super.addModelHandlerAssociations(processor);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations and reset the mock before each test.
        MockitoAnnotations.openMocks(this);
        reset(mockProcessor);
    }

    @Test
    void addModelHandlerAssociations_registersAllExpectedHandlers() {
        TestConfigurator configurator = new TestConfigurator();
        configurator.invokeAddModelHandlerAssociations(mockProcessor);
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        verify(mockProcessor).addHandler(eq(ParamModel.class), any());
        verify(mockProcessor).addHandler(eq(PropertyModel.class), any());
        verify(mockProcessor).addHandler(eq(TimestampModel.class), any());
        verify(mockProcessor).addHandler(eq(StatusListenerModel.class), any());
        verify(mockProcessor).addHandler(eq(ImplicitModel.class), any());
        verify(mockProcessor).addHandler(eq(IfModel.class), any());
        verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        verify(mockProcessor).addHandler(eq(ElseModel.class), any());
        verify(mockProcessor).addHandler(eq(SiftModel.class), any());
        verify(mockProcessor, times(14)).addHandler(any(Class.class), any());
    }

    @Test
    void addModelHandlerAssociations_nullProcessor_throwsNPE() {
        TestConfigurator configurator = new TestConfigurator();
        assertThrows(NullPointerException.class, () -> configurator.invokeAddModelHandlerAssociations(null));
    }

    @Test
    void addModelHandlerAssociations_processorThrowsRuntimeException_propagates() {
        doThrow(new RuntimeException("boom")).when(mockProcessor).addHandler(eq(ThenModel.class), any());
        TestConfigurator configurator = new TestConfigurator();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.invokeAddModelHandlerAssociations(mockProcessor));
        assertEquals("boom", ex.getMessage());
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor).addHandler(eq(IfModel.class), any());
    }

    @Test
    void addModelHandlerAssociations_factoryMethodsReturnCorrectHandlerInstances() {
        TestConfigurator configurator = new TestConfigurator();
        ArgumentCaptor<Class<? extends Model>> classCaptor = ArgumentCaptor.forClass((Class) Class.class);
        ArgumentCaptor<ModelHandlerFactoryMethod> factoryCaptor = ArgumentCaptor.forClass(ModelHandlerFactoryMethod.class);
        configurator.invokeAddModelHandlerAssociations(mockProcessor);
        verify(mockProcessor, times(14)).addHandler(classCaptor.capture(), factoryCaptor.capture());
        List<Class<? extends Model>> capturedClasses = classCaptor.getAllValues();
        List<ModelHandlerFactoryMethod> capturedFactories = factoryCaptor.getAllValues();
        Map<Class<? extends Model>, ModelHandlerFactoryMethod> registrationMap = new HashMap<>();
        for (int i = 0; i < capturedClasses.size(); i++) {
            registrationMap.put(capturedClasses.get(i), capturedFactories.get(i));
        }
        EXPECTED_HANDLER_MAP.forEach((modelClass, expectedHandlerClass) -> {
            ModelHandlerFactoryMethod factory = registrationMap.get(modelClass);
            assertNotNull(factory, "Factory for " + modelClass.getSimpleName() + " should be registered");
            Model mockModel = mock(modelClass);
            // Use reflection to invoke the factory's create method (its exact signature may vary across versions).
            Object handler;
            try {
                java.lang.reflect.Method m = factory.getClass().getMethod("create", Model.class);
                handler = m.invoke(factory, mockModel);
            } catch (Exception e) {
                fail("Failed to invoke factory method for " + modelClass.getSimpleName(), e);
                // unreachable, but required for compilation
                return;
            }
            assertNotNull(handler, "Factory for " + modelClass.getSimpleName() + " returned null");
            assertTrue(expectedHandlerClass.isInstance(handler), "Factory for " + modelClass.getSimpleName() + " should return instance of " + expectedHandlerClass.getSimpleName());
        });
    }

    @Test
    void addModelHandlerAssociations_partialFailure_propagatesAndRegistersPriorHandlers() {
        doThrow(new IllegalStateException("property failure")).when(mockProcessor).addHandler(eq(PropertyModel.class), any());
        TestConfigurator configurator = new TestConfigurator();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> configurator.invokeAddModelHandlerAssociations(mockProcessor));
        assertEquals("property failure", ex.getMessage());
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(ParamModel.class), any());
    }

    @Test
    void addModelHandlerAssociations_calledTwice_registersTwice() {
        TestConfigurator configurator = new TestConfigurator();
        configurator.invokeAddModelHandlerAssociations(mockProcessor);
        configurator.invokeAddModelHandlerAssociations(mockProcessor);
        verify(mockProcessor, times(28)).addHandler(any(Class.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(IfModel.class), any());
    }

    @Test
    void addModelHandlerAssociations_interactsOnlyWithAddHandler() {
        DefaultProcessor strictProcessor = mock(DefaultProcessor.class, withSettings().strictness(Strictness.STRICT_STUBS));
        TestConfigurator configurator = new TestConfigurator();
        configurator.invokeAddModelHandlerAssociations(strictProcessor);
        verify(strictProcessor, times(14)).addHandler(any(Class.class), any());
        verifyNoMoreInteractions(strictProcessor);
    }

    @Test
    void addModelHandlerAssociations_processorThrowsIllegalArgumentException_propagates() {
        doThrow(new IllegalArgumentException("duplicate import")).when(mockProcessor).addHandler(eq(ImportModel.class), any());
        TestConfigurator configurator = new TestConfigurator();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> configurator.invokeAddModelHandlerAssociations(mockProcessor));
        assertEquals("duplicate import", ex.getMessage());
        verify(mockProcessor, never()).addHandler(eq(ShutdownHookModel.class), any());
    }

    @Test
    void addModelHandlerAssociations_processorThrowsAssertionError_propagates() {
        doThrow(new AssertionError("assertion failure")).when(mockProcessor).addHandler(eq(ThenModel.class), any());
        TestConfigurator configurator = new TestConfigurator();
        AssertionError err = assertThrows(AssertionError.class, () -> configurator.invokeAddModelHandlerAssociations(mockProcessor));
        assertEquals("assertion failure", err.getMessage());
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor).addHandler(eq(IfModel.class), any());
        verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(ElseModel.class), any());
    }

    @Test
    void addModelHandlerAssociations_exceptionOnLastRegistration_propagatesAfterEarlierSuccesses() {
        doThrow(new RuntimeException("last failure")).when(mockProcessor).addHandler(eq(ElseModel.class), any());
        TestConfigurator configurator = new TestConfigurator();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.invokeAddModelHandlerAssociations(mockProcessor));
        assertEquals("last failure", ex.getMessage());
        EXPECTED_HANDLER_MAP.keySet().stream().filter(cls -> !cls.equals(ElseModel.class)).forEach(cls -> verify(mockProcessor).addHandler(eq(cls), any()));
        verify(mockProcessor).addHandler(eq(ElseModel.class), any());
    }
}