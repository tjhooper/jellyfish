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
package com.ngc.seaside.jellyfish.service.sequence.impl.sequenceservice;

import com.google.common.collect.Multimap;

import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.scenario.api.IMessagingFlow;
import com.ngc.seaside.jellyfish.service.scenario.api.IPublishSubscribeMessagingFlow;
import com.ngc.seaside.jellyfish.service.scenario.api.IScenarioService;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.link.IModelLink;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SequencingTest {

   @Test
   public void testDoesDetermineIfCollectionsAreEquivalent() {
      List<String> example1 = Arrays.asList("one", "two");
      List<String> example2 = new ArrayList<>(example1);
      Collections.reverse(example2);
      Set<String> example3 = new HashSet<>(example1);
      Set<String> example4 = Collections.singleton("foo");

      assertTrue("should be the same!",
                 Sequencing.equivalent(example1, example2));
      assertTrue("should be the same!",
                 Sequencing.equivalent(example1, example3));
      assertFalse("should not be the same!",
                  Sequencing.equivalent(example1, example4));
   }

   @Test
   public void testDoesDetermineIfLinksAreConnected() {
      IDataReferenceField field1 = mock(IDataReferenceField.class);
      IDataReferenceField field2 = mock(IDataReferenceField.class);

      @SuppressWarnings({"unchecked"})
      IModelLink<IDataReferenceField> link1 = mock(IModelLink.class);
      @SuppressWarnings({"unchecked"})
      IModelLink<IDataReferenceField> link2 = mock(IModelLink.class);
      @SuppressWarnings({"unchecked"})
      IModelLink<IDataReferenceField> link3 = mock(IModelLink.class);

      when(link1.getTarget()).thenReturn(field1);
      when(link2.getSource()).thenReturn(field1);
      when(link3.getSource()).thenReturn(field2);

      assertTrue("links should be connected!",
                 Sequencing.areLinksConnected(link1, link2));
      assertFalse("links should not be connected!",
                  Sequencing.areLinksConnected(link1, link3));
   }

   @Test
   public void testDoesGetFlows() {
      IPublishSubscribeMessagingFlow pubSubFlow = mock(IPublishSubscribeMessagingFlow.class);
      IScenario scenario = mock(IScenario.class);
      IScenarioService scenarioService = mock(IScenarioService.class);
      IJellyFishCommandOptions options = mock(IJellyFishCommandOptions.class);
      when(scenarioService.getPubSubMessagingFlow(options, scenario)).thenReturn(Optional.of(pubSubFlow));
      when(scenarioService.getRequestResponseMessagingFlow(options, scenario)).thenReturn(Optional.empty());

      Collection<IMessagingFlow> flows = Sequencing.getFlows(scenarioService, options, scenario);
      assertEquals("should contain only one flow!",
                   1,
                   flows.size());
      assertEquals("flow not correct!",
                   pubSubFlow,
                   flows.iterator().next());
   }

   @Test
   @Ignore("this will be an used method for deletion")
   public void testDoesGetLinksReferencingInputs() {
      IModel model = mock(IModel.class);
      IDataReferenceField field1 = mock(IDataReferenceField.class);
      IDataReferenceField field2 = mock(IDataReferenceField.class);

      @SuppressWarnings({"unchecked"})
      IModelLink<IDataReferenceField> link1 = mock(IModelLink.class);
      @SuppressWarnings({"unchecked"})
      IModelLink<IDataReferenceField> link2 = mock(IModelLink.class);
      when(link1.getSource()).thenReturn(field1);
      when(link2.getSource()).thenReturn(field2);

      Multimap<IModelReferenceField, IModelLink<IDataReferenceField>> links = Sequencing.getLinksReferencingInputs(
            model,
            Arrays.asList(field1, field2));
      assertEquals("does not contain the correct number of link!",
                   1,
                   links.size());
      assertEquals("link not correct!",
                   link1,
                   links.get(null).iterator().next());
   }
}
