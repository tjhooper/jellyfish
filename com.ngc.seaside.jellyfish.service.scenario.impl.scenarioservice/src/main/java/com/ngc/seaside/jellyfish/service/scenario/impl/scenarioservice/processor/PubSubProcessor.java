package com.ngc.seaside.jellyfish.service.scenario.impl.scenarioservice.processor;

import com.ngc.seaside.jellyfish.service.scenario.api.IPublishSubscribeMessagingFlow;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenarioStep;
import com.ngc.seaside.systemdescriptor.service.impl.xtext.scenario.PublishStepHandler;
import com.ngc.seaside.systemdescriptor.service.impl.xtext.scenario.ReceiveStepHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles the discovery of pub/sub flows of scenarios.
 */
public class PubSubProcessor {

   private final PublishStepHandler publishStepHandler;
   private final ReceiveStepHandler receiveStepHandler;

   public PubSubProcessor(PublishStepHandler publishStepHandler,
                          ReceiveStepHandler receiveStepHandler) {
      this.publishStepHandler = publishStepHandler;
      this.receiveStepHandler = receiveStepHandler;
   }

   public boolean isPublishSubscribe(IScenario scenario) {
      return !getFlows(scenario).isEmpty();
   }

   public Collection<IPublishSubscribeMessagingFlow> getFlows(IScenario scenario) {
      Collection<IPublishSubscribeMessagingFlow> flows = new ArrayList<>();
      flows.addAll(getFlowPaths(scenario));
      flows.addAll(getFlowSinks(scenario));
      flows.addAll(getFlowSources(scenario));
      return flows;
   }

   private Collection<IPublishSubscribeMessagingFlow> getFlowSinks(IScenario scenario) {
      Collection<IPublishSubscribeMessagingFlow> flows = new ArrayList<>();

      // If this scenario has any thens with a publish verb, than the scenario has no sinks.
      boolean doesPublish = false;
      for (IScenarioStep step : scenario.getThens()) {
         doesPublish |= PublishStepHandler.FUTURE.getVerb().equals(step.getKeyword());
      }

      if (!doesPublish) {
         for (IScenarioStep step : scenario.getWhens()) {
            if (ReceiveStepHandler.PRESENT.getVerb().equals(step.getKeyword())) {
               PublishSubscribeMessagingFlow flow =
                     new PublishSubscribeMessagingFlow(IPublishSubscribeMessagingFlow.FlowType.SINK)
                           .setScenario(scenario);
               flow.getInputsModifiable().add(receiveStepHandler.getInputs(step));
               flows.add(flow);
            }
         }
      }

      return flows;
   }

   private Collection<IPublishSubscribeMessagingFlow> getFlowSources(IScenario scenario) {
      Collection<IPublishSubscribeMessagingFlow> flows = new ArrayList<>();

      // If this scenario has any when with a receive verb, than the scenario has no sources.
      boolean doesReceive = false;
      for (IScenarioStep step : scenario.getWhens()) {
         doesReceive |= ReceiveStepHandler.PRESENT.getVerb().equals(step.getKeyword());
      }

      if (!doesReceive) {
         for (IScenarioStep step : scenario.getThens()) {
            if (PublishStepHandler.FUTURE.getVerb().equals(step.getKeyword())) {
               PublishSubscribeMessagingFlow flow =
                     new PublishSubscribeMessagingFlow(IPublishSubscribeMessagingFlow.FlowType.SOURCE)
                           .setScenario(scenario);
               flow.getOutputsModifiable().add(publishStepHandler.getOutputs(step));
               flows.add(flow);
            }
         }
      }

      return flows;
   }

   private Collection<IPublishSubscribeMessagingFlow> getFlowPaths(IScenario scenario) {
      // Right now, a scenario can have at most one flow path but it can reference any number of inputs and outputs.
      PublishSubscribeMessagingFlow flow =
            new PublishSubscribeMessagingFlow(IPublishSubscribeMessagingFlow.FlowType.PATH)
                  .setScenario(scenario);

      for (IScenarioStep step : scenario.getWhens()) {
         if (ReceiveStepHandler.PRESENT.getVerb().equals(step.getKeyword())) {
            flow.getInputsModifiable().add(receiveStepHandler.getInputs(step));
         }
      }
      for (IScenarioStep step : scenario.getThens()) {
         if (PublishStepHandler.FUTURE.getVerb().equals(step.getKeyword())) {
            flow.getOutputsModifiable().add(publishStepHandler.getOutputs(step));
         }
      }

      // If the flow has no inputs or no outputs it's not a path so ignore it.
      return flow.getInputsModifiable().isEmpty() || flow.getOutputsModifiable().isEmpty()
             ? Collections.emptyList()
             : Collections.singletonList(flow);
   }
}
