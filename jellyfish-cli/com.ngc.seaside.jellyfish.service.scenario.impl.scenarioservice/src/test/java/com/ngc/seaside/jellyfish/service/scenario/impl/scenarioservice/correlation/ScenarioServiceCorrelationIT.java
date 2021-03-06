/**
 * UNCLASSIFIED
 *
 * Copyright 2020 Northrop Grumman Systems Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ngc.seaside.jellyfish.service.scenario.impl.scenarioservice.correlation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.scenario.api.IScenarioService;
import com.ngc.seaside.jellyfish.service.scenario.correlation.api.ICorrelationDescription;
import com.ngc.seaside.jellyfish.service.scenario.correlation.api.ICorrelationExpression;
import com.ngc.seaside.jellyfish.service.scenario.impl.scenarioservice.ScenarioServiceGuiceWrapper;
import com.ngc.seaside.systemdescriptor.model.api.INamedChild;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.data.IEnumeration;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;
import com.ngc.seaside.systemdescriptor.scenario.impl.module.StepsSystemDescriptorServiceModule;
import com.ngc.seaside.systemdescriptor.service.impl.xtext.module.XTextSystemDescriptorServiceModule;
import com.ngc.seaside.systemdescriptor.service.log.api.ILogService;
import com.ngc.seaside.systemdescriptor.service.repository.api.IRepositoryService;
import com.ngc.seaside.systemdescriptor.test.systemdescriptor.ModelUtils;
import com.ngc.seaside.systemdescriptor.test.systemdescriptor.ModelUtils.PubSubModel;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ScenarioServiceCorrelationIT {

   private final Injector injector = getInjector();

   private IScenarioService service = injector.getInstance(ScenarioServiceGuiceWrapper.class);

   @Mock
   private IJellyFishCommandOptions options;

   @Before
   public void setup() {
   }

   @Test
   public void testCorrelationExpressions() {
      IEnumeration enum1 = ModelUtils.getMockNamedChild(IEnumeration.class, "com.ngc.Enum1");
      IData nestedData1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.NestedData1");
      ModelUtils.mockData(nestedData1, null, "nestedField1", DataTypes.INT, "nestedField2", DataTypes.BOOLEAN);

      IData data1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data1");
      ModelUtils.mockData(data1, null, "field1", DataTypes.INT, "field2", enum1, "field3", nestedData1);

      IData nestedData2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.NestedData2");
      ModelUtils.mockData(nestedData2, null, "nestedField1", enum1, "nestedField2", DataTypes.INT);

      IData data2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data2, null, "field1", DataTypes.BOOLEAN, "field2", nestedData2);

      PubSubModel model1 = new PubSubModel("com.ngc.Model1");
      IScenario scenario = model1.addPubSub("scenario1", 0, 2, 1, "input1", data1, "input2", data2, "output1", data1);
      model1.correlate("scenario1", "input1.field1", "input2.field2.nestedField2");
      ICorrelationDescription description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCorrelationExpressions().size());
      Collection<ICorrelationExpression> correlations = description.getCompletenessExpressions();
      assertEquals(1, correlations.size());
      ICorrelationExpression correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.INT,
                        null,
                        data1.getFields().getByName("field1").get(),
                        nestedData2.getFields().getByName("nestedField2").get());

      PubSubModel model2 = new PubSubModel("com.ngc.Model2");
      scenario = model2.addPubSub("scenario1", 0, 2, 1, "input1", data1, "input2", data2, "output1", data1);
      model2.correlate("scenario1", "input1.field2", "input2.field2.nestedField1");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCorrelationExpressions().size());
      correlations = description.getCompletenessExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.ENUM,
                        enum1,
                        data1.getFields().getByName("field2").get(),
                        nestedData2.getFields().getByName("nestedField1").get());

      PubSubModel model3 = new PubSubModel("com.ngc.Model3");
      scenario = model3.addPubSub("scenario1", 0, 2, 1, "input1", data1, "input2", data2, "output1", data1);
      model3.correlate("scenario1", "input1.field3.nestedField1", "input2.field2.nestedField2");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCorrelationExpressions().size());
      correlations = description.getCompletenessExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.INT,
                        null,
                        nestedData1.getFields().getByName("nestedField1").get(),
                        nestedData2.getFields().getByName("nestedField2").get());

      PubSubModel model4 = new PubSubModel("com.ngc.Model4");
      scenario = model4.addPubSub("scenario1", 0, 2, 1, "input1", data1, "input2", data2, "output1", data1);
      model4.correlate("scenario1", "input1.field3.nestedField2", "input2.field1");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCorrelationExpressions().size());
      correlations = description.getCompletenessExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.BOOLEAN,
                        null,
                        nestedData1.getFields().getByName("nestedField2").get(),
                        data2.getFields().getByName("field1").get());

   }

   @Test
   public void testCompletenessExpressions() {
      IEnumeration enum1 = ModelUtils.getMockNamedChild(IEnumeration.class, "com.ngc.Enum1");
      IData nestedData1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.NestedData1");
      ModelUtils.mockData(nestedData1, null, "nestedField1", DataTypes.INT, "nestedField2", DataTypes.BOOLEAN);

      IData data1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data1");
      ModelUtils.mockData(data1, null, "field1", DataTypes.INT, "field2", enum1, "field3", nestedData1);

      IData nestedData2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.NestedData2");
      ModelUtils.mockData(nestedData2, null, "nestedField1", enum1, "nestedField2", DataTypes.INT);

      IData data2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data2, null, "field1", DataTypes.BOOLEAN, "field2", nestedData2);

      PubSubModel model1 = new PubSubModel("com.ngc.Model1");
      IScenario scenario = model1.addPubSub("scenario1",
                                            "input1", data1,
                                            "output1", data2,
                                            "input1.field1", "output1.field2.nestedField2");
      ICorrelationDescription description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCompletenessExpressions().size());
      Collection<ICorrelationExpression> correlations = description.getCorrelationExpressions();
      assertEquals(1, correlations.size());
      ICorrelationExpression correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.INT,
                        null,
                        data1.getFields().getByName("field1").get(),
                        nestedData2.getFields().getByName("nestedField2").get());

      PubSubModel model2 = new PubSubModel("com.ngc.Model2");
      scenario = model2.addPubSub("scenario1",
                                  "input1", data1,
                                  "output1", data2,
                                  "input1.field2", "output1.field2.nestedField1");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCompletenessExpressions().size());
      correlations = description.getCorrelationExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.ENUM,
                        enum1,
                        data1.getFields().getByName("field2").get(),
                        nestedData2.getFields().getByName("nestedField1").get());

      PubSubModel model3 = new PubSubModel("com.ngc.Model3");
      scenario = model3.addPubSub("scenario1",
                                  "input1", data1,
                                  "output1", data2,
                                  "input1.field3.nestedField1", "output1.field2.nestedField2");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCompletenessExpressions().size());
      correlations = description.getCorrelationExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.INT,
                        null,
                        nestedData1.getFields().getByName("nestedField1").get(),
                        nestedData2.getFields().getByName("nestedField2").get());

      PubSubModel model4 = new PubSubModel("com.ngc.Model4");
      scenario = model4.addPubSub("scenario1",
                                  "input1",
                                  data1,
                                  "output1",
                                  data2,
                                  "input1.field3.nestedField2",
                                  "output1.field1");
      description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();
      assertEquals(0, description.getCompletenessExpressions().size());
      correlations = description.getCorrelationExpressions();
      assertEquals(1, correlations.size());
      correlation = correlations.iterator().next();
      verifyCorrelation(correlation,
                        DataTypes.BOOLEAN,
                        null,
                        nestedData1.getFields().getByName("nestedField2").get(),
                        data2.getFields().getByName("field1").get());

   }

   @Test
   public void testMultipleExpressions() {
      IEnumeration enum1 = ModelUtils.getMockNamedChild(IEnumeration.class, "com.ngc.Enum1");

      IData data1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data1");
      ModelUtils.mockData(data1, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      IData data2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data2, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      IData data3 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data3, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      IData data4 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data4, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      IData data5 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data5, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      IData data6 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data6, null, "field1", DataTypes.INT, "field2", enum1, "field3", DataTypes.BOOLEAN);

      PubSubModel model = new PubSubModel("com.ngc.Model");
      IScenario scenario = model.addPubSub("scenario1", 0, 3, 3,
                                           "input1", data1,
                                           "input2", data2,
                                           "input3", data3,
                                           "output1", data4,
                                           "output2", data5,
                                           "output3", data6);
      model.correlate("scenario1", "input1.field1", "input2.field1");
      model.correlate("scenario1", "input1.field2", "input3.field2");
      model.correlate("scenario1", "input2.field3", "input3.field3");

      model.correlate("scenario1", "input1.field1", "output1.field1");
      model.correlate("scenario1", "input1.field2", "output2.field2");
      model.correlate("scenario1", "input2.field3", "output3.field3");

      ICorrelationDescription description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();

      Collection<ICorrelationExpression> completions = description.getCompletenessExpressions();
      assertEquals(3, completions.size());
      ICorrelationExpression
            completion1 =
            completions.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field1")).findAny()
                  .get();
      ICorrelationExpression
            completion2 =
            completions.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field2")).findAny()
                  .get();
      ICorrelationExpression
            completion3 =
            completions.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field3")).findAny()
                  .get();
      verifyCorrelation(completion1, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data2.getFields().getByName("field1").get());
      verifyCorrelation(completion2, DataTypes.ENUM, enum1, data1.getFields().getByName("field2").get(),
                        data3.getFields().getByName("field2").get());
      verifyCorrelation(completion3, DataTypes.BOOLEAN, null, data2.getFields().getByName("field3").get(),
                        data3.getFields().getByName("field3").get());

      Collection<ICorrelationExpression>
            completions1 =
            description.getCompletenessExpressionForInput(model.getInputs().getByName("input1").get());
      assertEquals(2, completions1.size());
      ICorrelationExpression
            completion11 =
            completions1.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field1")).findAny()
                  .get();
      ICorrelationExpression
            completion12 =
            completions1.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field2")).findAny()
                  .get();
      verifyCorrelation(completion11, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data2.getFields().getByName("field1").get());
      verifyCorrelation(completion12, DataTypes.ENUM, enum1, data1.getFields().getByName("field2").get(),
                        data3.getFields().getByName("field2").get());

      Collection<ICorrelationExpression>
            completions2 =
            description.getCompletenessExpressionForInput(model.getInputs().getByName("input2").get());
      assertEquals(2, completions2.size());
      ICorrelationExpression
            completion21 =
            completions2.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field1")).findAny()
                  .get();
      ICorrelationExpression
            completion22 =
            completions2.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field3")).findAny()
                  .get();
      verifyCorrelation(completion21, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data2.getFields().getByName("field1").get());
      verifyCorrelation(completion22, DataTypes.BOOLEAN, null, data2.getFields().getByName("field3").get(),
                        data3.getFields().getByName("field3").get());

      Collection<ICorrelationExpression>
            completions3 =
            description.getCompletenessExpressionForInput(model.getInputs().getByName("input3").get());
      assertEquals(2, completions2.size());
      ICorrelationExpression
            completion31 =
            completions3.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field2")).findAny()
                  .get();
      ICorrelationExpression
            completion32 =
            completions3.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field3")).findAny()
                  .get();
      verifyCorrelation(completion31, DataTypes.ENUM, enum1, data1.getFields().getByName("field2").get(),
                        data3.getFields().getByName("field2").get());
      verifyCorrelation(completion32, DataTypes.BOOLEAN, null, data2.getFields().getByName("field3").get(),
                        data3.getFields().getByName("field3").get());

      Collection<ICorrelationExpression> correlations = description.getCorrelationExpressions();
      assertEquals(3, correlations.size());
      ICorrelationExpression
            correlation1 =
            correlations.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field1")).findAny()
                  .get();
      ICorrelationExpression
            correlation2 =
            correlations.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field2")).findAny()
                  .get();
      ICorrelationExpression
            correlation3 =
            correlations.stream().filter(c -> c.getLeftHandOperand().getEnd().getName().equals("field3")).findAny()
                  .get();
      verifyCorrelation(correlation1, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data4.getFields().getByName("field1").get());
      verifyCorrelation(correlation2, DataTypes.ENUM, enum1, data1.getFields().getByName("field2").get(),
                        data5.getFields().getByName("field2").get());
      verifyCorrelation(correlation3, DataTypes.BOOLEAN, null, data2.getFields().getByName("field3").get(),
                        data6.getFields().getByName("field3").get());

      Collection<ICorrelationExpression>
            correlations1 =
            description.getCorrelationExpressionForOutput(model.getOutputs().getByName("output1").get());
      assertEquals(1, correlations1.size());
      ICorrelationExpression correlation11 = correlations1.iterator().next();
      verifyCorrelation(correlation11, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data4.getFields().getByName("field1").get());

      Collection<ICorrelationExpression>
            correlations2 =
            description.getCorrelationExpressionForOutput(model.getOutputs().getByName("output2").get());
      assertEquals(1, correlations2.size());
      ICorrelationExpression correlation21 = correlations2.iterator().next();
      verifyCorrelation(correlation21, DataTypes.ENUM, enum1, data1.getFields().getByName("field2").get(),
                        data5.getFields().getByName("field2").get());

      Collection<ICorrelationExpression>
            correlations3 =
            description.getCorrelationExpressionForOutput(model.getOutputs().getByName("output3").get());
      assertEquals(1, correlations3.size());
      ICorrelationExpression correlation31 = correlations3.iterator().next();
      verifyCorrelation(correlation31, DataTypes.BOOLEAN, null, data2.getFields().getByName("field3").get(),
                        data6.getFields().getByName("field3").get());

   }

   @Test
   public void testWithSource() {
      IData data1 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data1");
      ModelUtils.mockData(data1, null, "field1", DataTypes.INT);

      IData data2 = ModelUtils.getMockNamedChild(IData.class, "com.ngc.Data2");
      ModelUtils.mockData(data2, null, "field1", DataTypes.INT);

      PubSubModel model = new PubSubModel("com.ngc.Model");
      IScenario scenario = model.addPubSub("scenario1", 0, 2, 0,
                                           "input1", data1,
                                           "input2", data2);
      model.correlate("scenario1", "input1.field1", "input2.field1");

      ICorrelationDescription description = service.getPubSubMessagingFlow(options, scenario)
            .get()
            .getCorrelationDescription()
            .get();

      assertEquals(0, description.getCorrelationExpressions().size());

      Collection<ICorrelationExpression> completions = description.getCompletenessExpressions();
      assertEquals(1, completions.size());
      ICorrelationExpression correlation = completions.iterator().next();
      verifyCorrelation(correlation, DataTypes.INT, null, data1.getFields().getByName("field1").get(),
                        data2.getFields().getByName("field1").get());

   }

   private static void verifyCorrelation(ICorrelationExpression correlation, DataTypes expectedType,
                                         INamedChild<IPackage> expectedChild, IDataField expectedLeft,
                                         IDataField expectedRight) {
      assertEquals(expectedType, correlation.getCorrelationEventIdType());
      if (expectedChild != null) {
         assertEquals(expectedChild, correlation.getCorrelationEventIdReferenceType());
      }
      assertEquals(expectedLeft, correlation.getLeftHandOperand().getEnd());
      assertEquals(expectedRight, correlation.getRightHandOperand().getEnd());
   }

   private static Injector getInjector() {
      Collection<Module> modules = new ArrayList<>();
      modules.add(new AbstractModule() {
         @Override
         protected void configure() {
            bind(ILogService.class).toInstance(mock(ILogService.class));
            bind(IRepositoryService.class).toInstance(mock(IRepositoryService.class));
         }
      });
      modules.add(XTextSystemDescriptorServiceModule.forStandaloneUsage());
      modules.add(new StepsSystemDescriptorServiceModule());
      return Guice.createInjector(modules);
   }

}
