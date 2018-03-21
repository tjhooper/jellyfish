package com.ngc.seaside.systemdescriptor.scenario.impl.standardsteps;

import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenarioStep;
import com.ngc.seaside.systemdescriptor.model.impl.basic.data.Data;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.DataReferenceField;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.Model;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.scenario.Scenario;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.scenario.ScenarioStep;
import com.ngc.seaside.systemdescriptor.scenario.api.VerbTense;
import com.ngc.seaside.systemdescriptor.validation.api.IValidationContext;
import com.ngc.seaside.systemdescriptor.validation.api.Severity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReceiveRequestStepHandlerTest {

   private ReceiveRequestStepHandler handler;

   private ScenarioStep step;

   private Data data;

   private DataReferenceField field;

   private Scenario scenario;

   private Model model;

   @Mock
   private IValidationContext<IScenarioStep> context;

   @Before
   public void setup() {
      data = new Data("TestData");

      field = new DataReferenceField("input1");
      field.setType(data);

      model = new Model("TestModel");
      model.addInput(field);

      scenario = new Scenario("test");
      scenario.setParent(model);

      step = new ScenarioStep();
      step.setKeyword(ReceiveRequestStepHandler.PRESENT.getVerb());
      step.getParameters().add(field.getName());
      step.setParent(scenario);
      scenario.addWhen(step);

      when(context.getObject()).thenReturn(step);

      handler = new ReceiveRequestStepHandler();
   }

   @Test
   public void testDoesRegisterVerbs() {
      assertEquals("did not register correct past tense!",
                   ReceiveRequestStepHandler.PAST,
                   handler.getVerbs().get(VerbTense.PAST_TENSE));
      assertEquals("did not register correct present tense!",
                   ReceiveRequestStepHandler.PRESENT,
                   handler.getVerbs().get(VerbTense.PRESENT_TENSE));
      assertEquals("did not register correct future tense!",
                   ReceiveRequestStepHandler.FUTURE,
                   handler.getVerbs().get(VerbTense.FUTURE_TENSE));
   }

   @Test
   public void testDoesValidateWrongNumberOfParameters() {
      step.getParameters().add(field.getName());

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(step))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getParameters();
   }

   @Test
   public void testDoesRequireFieldToBeAnInput() {
      model.getInputs().clear();
      model.addOutput(field);

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(step))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getParameters();
   }

   @Test
   public void testDoesRequireFieldNameToBeValid() {
      step.getParameters().clear();
      step.getParameters().add("foo");

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(step))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getParameters();
   }

   @Test
   public void testDoesAllowOnlyOneReceiveRequestStepPerScenario() {
      ScenarioStep extraStep = new ScenarioStep();
      extraStep.setKeyword(ReceiveRequestStepHandler.PRESENT.getVerb());
      scenario.addWhen(extraStep);

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(extraStep))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getKeyword();
   }

   @Test
   public void testDoesNotAllowCorrelation() {
      ScenarioStep extraStep = new ScenarioStep();
      extraStep.setKeyword(CorrelateStepHandler.PRESENT.getVerb());
      scenario.addWhen(extraStep);

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(extraStep))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getKeyword();
   }

   @Test
   public void testDoesNotAllowReceive() {
      ScenarioStep extraStep = new ScenarioStep();
      extraStep.setKeyword(ReceiveStepHandler.PRESENT.getVerb());
      scenario.addWhen(extraStep);

      IScenarioStep mockedStep = mock(IScenarioStep.class);
      when(context.declare(eq(Severity.ERROR), anyString(), eq(extraStep))).thenReturn(mockedStep);

      handler.doValidateStep(context);
      verify(mockedStep).getKeyword();
   }

   @Test
   public void testGetGetInputs() {
      assertEquals("did not return correct field!",
                   field,
                   handler.getInputs(step));
   }
}
