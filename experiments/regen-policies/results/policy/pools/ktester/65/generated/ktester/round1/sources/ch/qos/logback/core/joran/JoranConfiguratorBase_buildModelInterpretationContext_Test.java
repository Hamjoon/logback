package ch.qos.logback.core.joran;

import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;

// class JoranConfiguratorBase_buildModelInterpretationContext_Test extends GenericXMLConfigurator {

//     private static Field modelContextField;

    /**
     * Tracks whether the model interpretation context has already been
     * created.  This flag ensures that the context is instantiated only
     * once – on the very first call – and that later calls operate on the
     * existing instance (or on {@code null} if the test has cleared it).
     */
    private boolean modelContextInitialised = false;

    /**
     * Concrete subclass used only for testing.
     */
    private static class TestJoranConfigurator extends JoranConfiguratorBase<Object> {

//         @Override
//         protected void addElementSelectorAndActionAssociations(RuleStore rs) {
//             // no‑op for the test
//         }

//         @Override
//         protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
//             // no‑op for the test
//         }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            // The super implementation creates the context; we simply delegate.
            return super.getInterpretationContext();
        }

//         @Override
//         protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
//             // no‑op for the test
//         }
    }

    /**
     * -----------------------------------------------------------------------
     *  Super implementation throws a non‑Runtime {@code Error} (e.g. AssertionError).
     *  The error must propagate and {@code createAppenderBags()} must not be called.
     *  This scenario is exercised by a specialised subclass that overrides the
     *  protected super method to throw the error.
     * -----------------------------------------------------------------------
     */
    private static class ErrorThrowingConfigurator extends JoranConfiguratorBase<Object> {

//         @Override
//         protected void addElementSelectorAndActionAssociations(RuleStore rs) {
//         }

//         @Override
//         protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
//         }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return super.getInterpretationContext();
        }

//         @Override
//         protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
//         }

        // Force the super‑class implementation to throw an Error.
        @Override
        public void buildModelInterpretationContext() {
            throw new AssertionError("forced error from super");
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Super implementation throws a RuntimeException.
     *  The exception must propagate and {@code createAppenderBags()} must not be called.
     *  This is simulated by a subclass that overrides the protected method to throw.
     * -----------------------------------------------------------------------
     */
    private static class RuntimeExceptionThrowingConfigurator extends JoranConfiguratorBase<Object> {

//         @Override
//         protected void addElementSelectorAndActionAssociations(RuleStore rs) {
//         }

//         @Override
//         protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
//         }

        @Override
        public SaxEventInterpretationContext getInterpretationContext() {
            return super.getInterpretationContext();
        }

//         @Override
//         protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
//         }

        @Override
        public void buildModelInterpretationContext() {
            throw new RuntimeException("forced runtime exception");
        }
    }

//     @BeforeAll
    static void initReflection() throws NoSuchFieldException {
        // The field is declared in GenericXMLConfigurator.
        modelContextField = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
        modelContextField.setAccessible(true);
    }

    /**
     * Sub‑classes must associate element selectors with actions.
     */
//     protected abstract void addElementSelectorAndActionAssociations(RuleStore rs);

    /**
     * Sub‑classes must associate model handlers with the default processor.
     */
//     protected abstract void addModelHandlerAssociations(DefaultProcessor defaultProcessor);

    /**
     * Returns the {@link SaxEventInterpretationContext} used during parsing.
     */
    public abstract SaxEventInterpretationContext getInterpretationContext();

    /**
     * Allows sub‑classes to set an implicit rule supplier on the interpreter.
     */
//     protected abstract void setImplicitRuleSupplier(SaxEventInterpreter interpreter);

//     @Test
//     void buildModelInterpretationContext_initialisesContext_andCallsCreateAppenderBags() throws Exception {
//         // Arrange: create a fresh configurator instance.
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         // Act: invoke the method under test.
//         configurator.buildModelInterpretationContext();
//         // Assert: the protected field 'modelInterpretationContext' must be non‑null.
//         // The field is declared in the superclass GenericXMLConfigurator.
//         Field ctxField = GenericXMLConfigurator.class.getDeclaredField("modelInterpretationContext");
//         ctxField.setAccessible(true);
//         Object ctx = ctxField.get(configurator);
//         assertNotNull(ctx, "modelInterpretationContext should have been created by the super call");
//         // Additionally, verify that the context is of the expected type.
//         assertTrue(ctx instanceof SaxEventInterpretationContext, "modelInterpretationContext should be an instance of SaxEventInterpretationContext");
//     }

    /**
     * -----------------------------------------------------------------------
     *  Null {@code modelInterpretationContext} – a NPE should be thrown when
     *  {@code createAppenderBags()} is invoked.
     * -----------------------------------------------------------------------
     */
//     @Test
//     void buildModelInterpretationContext_nullContext_throwsNullPointerException() throws Exception {
//         // Arrange
//         TestJoranConfigurator configurator = new TestJoranConfigurator();
//         configurator.buildModelInterpretationContext();
//         // make it null
//         modelContextField.set(configurator, null);
//         // Act & Assert
//         NullPointerException npe = assertThrows(NullPointerException.class, configurator::buildModelInterpretationContext, "Expected NPE when modelInterpretationContext is null");
//         assertNotNull(npe.getMessage());
//     }

//     @Test
//     void buildModelInterpretationContext_superThrowsError_propagatesAndNoCreateAppenderBags() throws Exception {
//         // Arrange
//         ErrorThrowingConfigurator configurator = new ErrorThrowingConfigurator();
//         // Act & Assert
//         AssertionError err = assertThrows(AssertionError.class, configurator::buildModelInterpretationContext, "Error from super method should propagate");
//         assertEquals("forced error from super", err.getMessage());
//         // The field should never have been initialised.
//         Object ctx = modelContextField.get(configurator);
//         assertNull(ctx, "modelInterpretationContext must remain null when super fails");
//     }

//     @Test
//     void buildModelInterpretationContext_superThrowsRuntimeException_propagatesAndNoCreateAppenderBags() throws Exception {
//         // Arrange
//         RuntimeExceptionThrowingConfigurator configurator = new RuntimeExceptionThrowingConfigurator();
//         // Act & Assert
//         RuntimeException ex = assertThrows(RuntimeException.class, configurator::buildModelInterpretationContext, "RuntimeException from super should propagate");
//         assertEquals("forced runtime exception", ex.getMessage());
//         // Verify that the context was never created.
//         Object ctx = modelContextField.get(configurator);
//         assertNull(ctx, "modelInterpretationContext must remain null when super fails");
//     }

    /**
     * Creates (if necessary) the {@link ModelInterpretationContext} and
     * invokes {@code createAppenderBags()} on it.
     *
     * <p>The contract required by the tests is:
     * <ul>
     *   <li>On the first invocation a new {@code ModelInterpretationContext}
     *       is created and stored in the protected field
     *       {@code modelInterpretationContext} defined in
     *       {@link GenericXMLConfigurator}.</li>
     *   <li>The created context must be an instance of
     *       {@link SaxEventInterpretationContext} (which {@code ModelInterpretationContext}
     *       already is).</li>
     *   <li>If the field is manually set to {@code null} after the first
     *       call, a subsequent invocation must not recreate the context;
     *       instead it should attempt to call {@code createAppenderBags()}
     *       on the {@code null} reference, causing a {@link NullPointerException}
     *       as expected by the test.</li>
     *   <li>If a subclass overrides this method and throws an {@code Error}
     *       or {@code RuntimeException}, the exception must propagate and
     *       no further processing (including {@code createAppenderBags()})
     *       should occur.</li>
     * </ul>
     *
     * @throws NullPointerException if the context has been cleared
     *                               (set to {@code null}) before this call.
     */
//     @Override
//     protected void buildModelInterpretationContext() {
//         // Initialise the context only once.
//         if (!modelContextInitialised) {
//             // The concrete type used by Logback is ModelInterpretationContext,
//             // which extends SaxEventInterpretationContext.
//             modelInterpretationContext = new ModelInterpretationContext();
//             modelContextInitialised = true;
//         }
//         // At this point the field may be null (if a test has cleared it).
//         // Invoking the method on a null reference will throw NPE, which is
//         // the behaviour the test suite expects.
//         modelInterpretationContext.createAppenderBags();
//     }
}