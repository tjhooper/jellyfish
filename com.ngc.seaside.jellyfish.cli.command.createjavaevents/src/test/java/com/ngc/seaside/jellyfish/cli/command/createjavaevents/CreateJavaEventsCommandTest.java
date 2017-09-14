package com.ngc.seaside.jellyfish.cli.command.createjavaevents;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.blocs.test.impl.common.resource.MockedResourceService;
import com.ngc.seaside.bootstrap.service.promptuser.api.IPromptUserService;
import com.ngc.seaside.command.api.DefaultParameter;
import com.ngc.seaside.command.api.DefaultParameterCollection;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandProvider;
import com.ngc.seaside.jellyfish.cli.command.createdomain.CreateDomainCommand;
import com.ngc.seaside.jellyfish.service.name.api.IProjectInformation;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.systemdescriptor.model.api.ISystemDescriptor;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.service.api.IParsingResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class CreateJavaEventsCommandTest {

   private CreateJavaEventsCommand command;

   private DefaultParameterCollection parameters;

   private MockedResourceService resourceService;

   @Mock
   private IJellyFishCommandOptions options;

   @Mock
   private ILogService logService;

   @Mock
   private IJellyFishCommandProvider commandProvider;

   @Mock
   private IPromptUserService promptUserService;

   @Before
   public void setup() {
      IParsingResult result = mock(IParsingResult.class);

      IModel model = mock(IModel.class);
      when(model.getName()).thenReturn("Model");
      ISystemDescriptor systemDescriptor = mock(ISystemDescriptor.class);
      when(systemDescriptor.findModel("my.Model")).thenReturn(Optional.of(model));

      parameters = new DefaultParameterCollection();
      when(options.getParameters()).thenReturn(parameters);
      when(options.getParsingResult()).thenReturn(result);
      when(options.getSystemDescriptor()).thenReturn(systemDescriptor);

      resourceService = new MockedResourceService();
      
      IProjectInformation projectInformation = mock(IProjectInformation.class);
      IProjectNamingService projectNamingService = mock(IProjectNamingService.class);
      when(projectNamingService.getEventsProjectName(any(), model)).thenReturn(projectInformation);

      command = new CreateJavaEventsCommand();
      command.setLogService(logService);
      command.setResourceService(resourceService);
      command.setJellyFishCommandProvider(commandProvider);
      command.setPromptUserService(promptUserService);
      command.activate();

   }

   @Test
   public void testDoesCommandInvokeDomainCommandWithDefaults() {
      resourceService.onNextReadDrain(
            CreateJavaEventsCommandTest.class
                  .getClassLoader()
                  .getResourceAsStream(CreateJavaEventsCommand.EVENT_SOURCE_VELOCITY_TEMPLATE));

      parameters.addParameter(new DefaultParameter<>("model", "my.Model"));
      command.run(options);

      ArgumentCaptor<IJellyFishCommandOptions> optionsCapture = ArgumentCaptor.forClass(IJellyFishCommandOptions.class);
      verify(commandProvider).run(eq(CreateJavaEventsCommand.CREATE_DOMAIN_COMMAND_NAME),
                                  optionsCapture.capture());

      IJellyFishCommandOptions delegateOptions = optionsCapture.getValue();
      assertTrue("does not include original parameters!",
                 delegateOptions.getParameters().containsParameter("model"));

      assertTrue("does not contain domain template file property!",
                 delegateOptions.getParameters().containsParameter(
                       CreateJavaEventsCommand.DOMAIN_TEMPLATE_FILE_PROPERTY));
      assertTrue("domain template file default is a null value!",
                 delegateOptions.getParameters()
                       .getParameter(CreateJavaEventsCommand.DOMAIN_TEMPLATE_FILE_PROPERTY)
                       .getStringValue() != null);

      assertTrue("does not contain package suffix property!",
                 delegateOptions.getParameters()
                       .containsParameter(CreateDomainCommand.ARTIFACT_ID_PROPERTY));
      assertEquals("artifact ID default not correct!",
                   "model.events",
                   delegateOptions.getParameters()
                         .getParameter(CreateDomainCommand.ARTIFACT_ID_PROPERTY)
                         .getStringValue());

      assertTrue("does not contain build.gradle template property!",
                 delegateOptions.getParameters()
                       .containsParameter(CreateDomainCommand.BUILD_GRADLE_TEMPLATE_PROPERTY));
      assertEquals("build.gradle templatedefault not correct!",
                   CreateJavaEventsCommand.class.getPackage().getName(),
                   delegateOptions.getParameters()
                         .getParameter(CreateDomainCommand.BUILD_GRADLE_TEMPLATE_PROPERTY)
                         .getStringValue());
   }

   @Test
   public void testDoesCommandAllowForCustomVelocityTemplateFile() throws Throwable {
      parameters.addParameter(new DefaultParameter<>(CreateJavaEventsCommand.EVENT_TEMPLATE_FILE_PROPERTY,
                                                     "my/template/file"));
      parameters.addParameter(new DefaultParameter<>("model", "my.Model"));
      command.run(options);

      ArgumentCaptor<IJellyFishCommandOptions> optionsCapture = ArgumentCaptor.forClass(IJellyFishCommandOptions.class);
      verify(commandProvider).run(eq(CreateJavaEventsCommand.CREATE_DOMAIN_COMMAND_NAME),
                                  optionsCapture.capture());

      IJellyFishCommandOptions delegateOptions = optionsCapture.getValue();
      assertTrue("does not contain domain template file property!",
                 delegateOptions.getParameters().containsParameter(
                       CreateJavaEventsCommand.DOMAIN_TEMPLATE_FILE_PROPERTY));
      assertEquals("domain template file default incorrect!",
                   "my/template/file",
                   delegateOptions.getParameters()
                         .getParameter(CreateJavaEventsCommand.DOMAIN_TEMPLATE_FILE_PROPERTY).getStringValue());
   }
}
