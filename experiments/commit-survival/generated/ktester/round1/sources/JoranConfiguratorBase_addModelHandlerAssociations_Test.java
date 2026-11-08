package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import ch.qos.logback.core.model.ModelHandlerFactoryMethod;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.*;

// @ExtendWith(MockitoExtension.class)
class JoranConfiguratorBase_addModelHandlerAssociations_Test {

    private TestConfigurator configurator;

    @Mock
    private DefaultProcessor mockProcessor;

    /**
     * Minimal concrete implementation of {@link JoranConfiguratorBase}
     * required for testing the protected {@code addModelHandlerAssociations}
     * method. All abstract methods are stubbed out because they are not
     * relevant for this unit test.
     */
    private static class TestConfigurator extends JoranConfiguratorBase<Object> {

        @Override
        protected void addElementSelectorAndActionAssociations(RuleStore rs) {
            // No‑op for this test
        }

        @Override
        public void buildModelInterpretationContext() {
            // No‑op for this test
        }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // Not needed for this test
            return null;
        }

        @Override
        protected void performCheck(SanityChecker sc, Model model) {
            // No‑op for this test
        }

        @Override
        protected void sanityCheck(Model topModel) {
            // No‑op for this test
        }

        @Override
        protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
            // No‑op for this test
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields
        MockitoAnnotations.openMocks(this);
        configurator = new TestConfigurator();
    }

    @Test
    void testAddModelHandlerAssociationsRegistersAllHandlers() {
        // Act
        configurator.addModelHandlerAssociations(mockProcessor);
        // Assert – verify that each model class is associated with a handler factory
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
        // Ensure no unexpected interactions
        verifyNoMoreInteractions(mockProcessor);
    }

    /* --------------------------------------------------------------
     *  Null processor – method should throw NPE (no guard present).
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_nullProcessor_throwsNPE() {
        assertThrows(NullPointerException.class, () -> configurator.addModelHandlerAssociations(null));
    }

    /* --------------------------------------------------------------
     *  Registration order – handlers must be added in the source order.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_registrationOrder() {
        // use a mock that records the call order via InOrder verification
        configurator.addModelHandlerAssociations(mockProcessor);
        InOrder inOrder = inOrder(mockProcessor);
        inOrder.verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ShutdownHookModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(EventEvaluatorModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(DefineModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IncludeModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ParamModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(PropertyModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(TimestampModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(StatusListenerModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ImplicitModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(IfModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(ElseModel.class), any());
        inOrder.verify(mockProcessor).addHandler(eq(SiftModel.class), any());
    }

    /* --------------------------------------------------------------
     *  Exception while registering ThenModel – propagation test.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_throwsOnThenModel() {
        // configure mock to throw when ThenModel is processed
        doAnswer(invocation -> {
            Class<?> modelClass = invocation.getArgument(0);
            if (modelClass.equals(ThenModel.class)) {
                throw new RuntimeException("forced failure for ThenModel");
            }
            return null;
        }).when(mockProcessor).addHandler(any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("forced failure for ThenModel", ex.getMessage());
        // verify that preceding registrations happened
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
        // ThenModel should have been attempted (and thrown)
        verify(mockProcessor).addHandler(eq(ThenModel.class), any());
        // No registrations after ThenModel
        verify(mockProcessor, never()).addHandler(eq(ElseModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(SiftModel.class), any());
    }

    /* --------------------------------------------------------------
     *  Exception while registering ImportModel – first‑call failure test.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_throwsOnImportModel() {
        doAnswer(invocation -> {
            Class<?> modelClass = invocation.getArgument(0);
            if (modelClass.equals(ImportModel.class)) {
                throw new RuntimeException("forced failure for ImportModel");
            }
            return null;
        }).when(mockProcessor).addHandler(any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("forced failure for ImportModel", ex.getMessage());
        // Only ImportModel should have been attempted
        verify(mockProcessor).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(ShutdownHookModel.class), any());
    }

    /* --------------------------------------------------------------
     *  Idempotent multiple invocations – each call registers again.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_multipleInvocations_countsPerModel() {
        configurator.addModelHandlerAssociations(mockProcessor);
        // second call
        configurator.addModelHandlerAssociations(mockProcessor);
        // each model class must have been added exactly twice
        verify(mockProcessor, times(2)).addHandler(eq(ImportModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(ShutdownHookModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(SequenceNumberGeneratorModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(EventEvaluatorModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(DefineModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(IncludeModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(ParamModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(PropertyModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(TimestampModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(StatusListenerModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(ImplicitModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(IfModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(ThenModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(ElseModel.class), any());
        verify(mockProcessor, times(2)).addHandler(eq(SiftModel.class), any());
    }

    /* --------------------------------------------------------------
     *  Duplicate registration illegal‑argument test.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_duplicateRegistration_throwsIllegalArgument() {
        // mock that throws IllegalArgumentException on a second registration of the same class
        Set<Class<?>> seen = new HashSet<>();
        doAnswer(invocation -> {
            Class<?> modelClass = invocation.getArgument(0);
            if (!seen.add(modelClass)) {
                throw new IllegalArgumentException("duplicate: " + modelClass);
            }
            return null;
        }).when(mockProcessor).addHandler(any(), any());
        // first invocation succeeds
        configurator.addModelHandlerAssociations(mockProcessor);
        // second invocation should fail on the first duplicate (ImportModel)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertTrue(ex.getMessage().contains("duplicate"));
    }

    /* --------------------------------------------------------------
     *  UnsupportedOperationException for IfModel – propagation test.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_throwsUnsupportedOnIfModel() {
        doAnswer(invocation -> {
            Class<?> modelClass = invocation.getArgument(0);
            if (modelClass.equals(IfModel.class)) {
                throw new UnsupportedOperationException("unsupported IfModel");
            }
            return null;
        }).when(mockProcessor).addHandler(any(), any());
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> configurator.addModelHandlerAssociations(mockProcessor));
        assertEquals("unsupported IfModel", ex.getMessage());
        // verify preceding registrations
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
        // IfModel should have been attempted (and threw)
        verify(mockProcessor).addHandler(eq(IfModel.class), any());
        // No later registrations
        verify(mockProcessor, never()).addHandler(eq(ThenModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(ElseModel.class), any());
        verify(mockProcessor, never()).addHandler(eq(SiftModel.class), any());
    }

    /* --------------------------------------------------------------
     *  Handler‑factory reference validation using SerializedLambda.
     * -------------------------------------------------------------- */
    @Test
    void testAddModelHandlerAssociations_handlerFactoryReferences() throws Exception {
        // capture the second argument of each addHandler call
        ArgumentCaptor<ModelHandlerFactoryMethod> factoryCaptor = ArgumentCaptor.forClass(ModelHandlerFactoryMethod.class);
        configurator.addModelHandlerAssociations(mockProcessor);
        verify(mockProcessor, times(15)).addHandler(any(), factoryCaptor.capture());
        List<ModelHandlerFactoryMethod> factories = factoryCaptor.getAllValues();
        // map model class -> expected handler class name
        Map<Class<?>, String> expectedHandler = new LinkedHashMap<>();
        expectedHandler.put(ImportModel.class, "ImportModelHandler");
        expectedHandler.put(ShutdownHookModel.class, "ShutdownHookModelHandler");
        expectedHandler.put(SequenceNumberGeneratorModel.class, "SequenceNumberGeneratorModelHandler");
        expectedHandler.put(EventEvaluatorModel.class, "EventEvaluatorModelHandler");
        expectedHandler.put(DefineModel.class, "DefineModelHandler");
        expectedHandler.put(IncludeModel.class, "NOPModelHandler");
        expectedHandler.put(ParamModel.class, "ParamModelHandler");
        expectedHandler.put(PropertyModel.class, "PropertyModelHandler");
        expectedHandler.put(TimestampModel.class, "TimestampModelHandler");
        expectedHandler.put(StatusListenerModel.class, "StatusListenerModelHandler");
        expectedHandler.put(ImplicitModel.class, "ImplicitModelHandler");
        expectedHandler.put(IfModel.class, "IfModelHandler");
        expectedHandler.put(ThenModel.class, "ThenModelHandler");
        expectedHandler.put(ElseModel.class, "ElseModelHandler");
        expectedHandler.put(SiftModel.class, "SiftModelHandler");
        int index = 0;
        for (Map.Entry<Class<?>, String> entry : expectedHandler.entrySet()) {
            ModelHandlerFactoryMethod factory = factories.get(index++);
            assertNotNull(factory, "Factory for " + entry.getKey().getSimpleName() + " must not be null");
            // Extract SerializedLambda from the lambda (method reference)
            Method writeReplace = factory.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object serializedForm = writeReplace.invoke(factory);
            assertTrue(serializedForm instanceof SerializedLambda, "Factory should be a lambda producing SerializedLambda");
            SerializedLambda sl = (SerializedLambda) serializedForm;
            String implClass = sl.getImplClass().replace('/', '.');
            String implMethod = sl.getImplMethodName();
            // The implementing class should end with the expected handler class name
            assertTrue(implClass.endsWith(entry.getValue()), "Factory for " + entry.getKey().getSimpleName() + " should be " + entry.getValue() + " but was " + implClass);
            // The method name must be "makeInstance"
            assertEquals("makeInstance", implMethod, "Factory method for " + entry.getKey().getSimpleName() + " must be makeInstance");
        }
    }
}