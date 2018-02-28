package com.ngc.seaside.jellyfish.cli.command.createjavapubsubconnector;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.IBuildManagementService;
import com.ngc.seaside.jellyfish.service.template.api.ITemplateService;
import com.ngc.seaside.jellyfish.utilities.file.FileUtilitiesException;
import com.ngc.seaside.jellyfish.utilities.file.GradleSettingsUtilities;
import com.ngc.seaside.jellyfish.api.CommandException;
import com.ngc.seaside.jellyfish.api.DefaultParameter;
import com.ngc.seaside.jellyfish.api.DefaultParameterCollection;
import com.ngc.seaside.jellyfish.api.DefaultUsage;
import com.ngc.seaside.jellyfish.api.IUsage;
import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.cli.command.createjavapubsubconnector.dto.ConnectorDto;
import com.ngc.seaside.jellyfish.service.codegen.api.IDataFieldGenerationService;
import com.ngc.seaside.jellyfish.service.codegen.api.IJavaServiceGenerationService;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.EnumDto;
import com.ngc.seaside.jellyfish.service.config.api.ITransportConfigurationService;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectInformation;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.jellyfish.service.requirements.api.IRequirementsService;
import com.ngc.seaside.jellyfish.service.scenario.api.IPublishSubscribeMessagingFlow;
import com.ngc.seaside.jellyfish.service.scenario.api.IScenarioService;
import com.ngc.seaside.systemdescriptor.model.api.INamedChild;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.data.IEnumeration;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

@Component(service = IJellyFishCommand.class)
public class CreateJavaPubsubConnectorCommand implements IJellyFishCommand {

   private static final String NAME = "create-java-pubsub-connector";
   private static final IUsage USAGE = createUsage();

   public static final String OUTPUT_DIRECTORY_PROPERTY = CommonParameters.OUTPUT_DIRECTORY.getName();
   public static final String MODEL_PROPERTY = CommonParameters.MODEL.getName();
   public static final String GROUP_ID_PROPERTY = CommonParameters.GROUP_ID.getName();
   public static final String ARTIFACT_ID_PROPERTY = CommonParameters.ARTIFACT_ID.getName();
   public static final String CLEAN_PROPERTY = CommonParameters.CLEAN.getName();

   private static final Function<IData, Collection<IDataField>> FIELDS_FUNCTION = data -> {
      Set<IDataField> fields = new LinkedHashSet<>();
      while (data != null) {
         fields.addAll(data.getFields());
         data = data.getExtendedDataType().orElse(null);
      }
      return fields;
   };
   
   private ILogService logService;
   private ITemplateService templateService;
   private IScenarioService scenarioService;
   private ITransportConfigurationService transportConfigService;
   private IRequirementsService requirementsService;
   private IPackageNamingService packageService;
   private IProjectNamingService projectService;
   private IJavaServiceGenerationService generationService;
   private IDataFieldGenerationService dataFieldService;
   private IBuildManagementService buildManagementService;

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public IUsage getUsage() {
      return USAGE;
   }
   
   @Override
   public void run(IJellyFishCommandOptions commandOptions) {
      final Path outputDirectory = Paths.get(commandOptions.getParameters().getParameter(OUTPUT_DIRECTORY_PROPERTY).getStringValue());
      final String modelName = commandOptions.getParameters().getParameter(MODEL_PROPERTY).getStringValue();
      final IModel model = commandOptions.getSystemDescriptor().findModel(modelName).orElseThrow(
         () -> new CommandException("Unknown model: " + modelName));

      final boolean clean = CommonParameters.evaluateBooleanParameter(commandOptions.getParameters(), CLEAN_PROPERTY);

      IProjectInformation info = projectService.getConnectorProjectName(commandOptions, model);

      ConnectorDto dto = new ConnectorDto(buildManagementService);

      final String packageName = packageService.getConnectorPackageName(commandOptions, model);

      dto.setProjectName(info.getDirectoryName());
      dto.setPackageName(packageName);
      dto.setModel(model);
      dto.setOptions(commandOptions);
      dto.setPackageService(packageService);
      dto.setDataFieldService(dataFieldService);
      dto.setFields(FIELDS_FUNCTION);
      EnumDto transportTopics = generationService.getTransportTopicsDescription(commandOptions, model);
      dto.setTransportTopicsClass(transportTopics.getFullyQualifiedName());
      dto.setProjectDependencies(new LinkedHashSet<>(
         Arrays.asList(projectService.getBaseServiceProjectName(commandOptions, model).getArtifactId(),
            projectService.getEventsProjectName(commandOptions, model).getArtifactId(),
            projectService.getMessageProjectName(commandOptions, model).getArtifactId())));

      evaluateIO(commandOptions, dto);

      DefaultParameterCollection parameters = new DefaultParameterCollection();
      parameters.addParameter(new DefaultParameter<>("dto", dto));
      parameters.addParameter(new DefaultParameter<>("IData", IData.class));
      parameters.addParameter(new DefaultParameter<>("IEnumeration", IEnumeration.class));

      templateService.unpack(CreateJavaPubsubConnectorCommand.class.getPackage().getName(),
         parameters,
         outputDirectory,
         clean);

      if (CommonParameters.evaluateBooleanParameter(commandOptions.getParameters(),
                                                    CommonParameters.UPDATE_GRADLE_SETTING.getName(),
                                                    true)) {
         updateGradleDotSettings(outputDirectory, info);
      }
      logService.info(CreateJavaPubsubConnectorCommand.class, "%s project successfully created", modelName);
   }

   private void updateGradleDotSettings(Path outputDir, IProjectInformation info) {
      DefaultParameterCollection updatedParameters = new DefaultParameterCollection();
      updatedParameters.addParameter(new DefaultParameter<>(OUTPUT_DIRECTORY_PROPERTY,
         outputDir.resolve(info.getDirectoryName()).getParent().toString()));
      updatedParameters.addParameter(new DefaultParameter<>(GROUP_ID_PROPERTY, info.getGroupId()));
      updatedParameters.addParameter(new DefaultParameter<>(ARTIFACT_ID_PROPERTY, info.getArtifactId()));
      try {
         if (!GradleSettingsUtilities.tryAddProject(updatedParameters)) {
            logService.warn(getClass(), "Unable to add the new project to settings.gradle.");
         }
      } catch (FileUtilitiesException e) {
         throw new CommandException("failed to update settings.gradle!", e);
      }
   }

   private void evaluateIO(IJellyFishCommandOptions options, ConnectorDto dto) {

      IModel model = dto.getModel();

      Set<INamedChild<IPackage>> inputs = new LinkedHashSet<>();
      Set<INamedChild<IPackage>> outputs = new LinkedHashSet<>();

      Map<String, IData> inputTopics = new TreeMap<>();
      Map<String, IData> outputTopics = new TreeMap<>();
      Map<String, Set<String>> topicRequirements = new TreeMap<>();

      final Set<String> modelRequirements = requirementsService.getRequirements(options, model);
      for (IScenario scenario : model.getScenarios()) {

         final Set<String> scenarioRequirements = requirementsService.getRequirements(options, scenario);

         Optional<IPublishSubscribeMessagingFlow> optionalFlow = scenarioService.getPubSubMessagingFlow(options, scenario);
         if (optionalFlow.isPresent()) {
            IPublishSubscribeMessagingFlow flow = optionalFlow.get();

            for (IDataReferenceField input : flow.getInputs()) {
               final IData type = input.getType();
               inputs.add(type);

               final String topic = transportConfigService.getTransportTopicName(flow, input);
               IData previous = inputTopics.put(topic, type);
               if (previous != null && !previous.equals(type)) {
                  throw new IllegalStateException(String.format("Conflicting data types for topic <%s>: %s and %s",
                     topic,
                     previous.getClass(),
                     type.getClass()));
               }

               final Set<String> requirements = topicRequirements.computeIfAbsent(topic, t -> new TreeSet<>());
               requirements.addAll(requirementsService.getRequirements(options, input));
               requirements.addAll(requirementsService.getRequirements(options, type));
               requirements.addAll(scenarioRequirements);
               requirements.addAll(modelRequirements);

            }

            for (IDataReferenceField output : flow.getOutputs()) {
               final IData type = output.getType();
               outputs.add(type);

               final String topic = transportConfigService.getTransportTopicName(flow, output);
               IData previous = outputTopics.put(topic, type);
               if (previous != null && !previous.equals(type)) {
                  throw new IllegalStateException(String.format("Conflicting data types for topic <%s>: %s and %s",
                     topic,
                     previous.getClass(),
                     type.getClass()));
               }

               final Set<String> requirements = topicRequirements.computeIfAbsent(topic, t -> new TreeSet<>());
               requirements.addAll(requirementsService.getRequirements(options, output));
               requirements.addAll(requirementsService.getRequirements(options, type));
               requirements.addAll(scenarioRequirements);
               requirements.addAll(modelRequirements);
            }

         }

      }

      addNestedData(inputs);
      addNestedData(outputs);

      dto.setAllInputs(inputs);
      dto.setAllOutputs(outputs);
      dto.setInputTopics(inputTopics);
      dto.setOutputTopics(outputTopics);
      dto.setTopicRequirements(topicRequirements);
   }

   private static void addNestedData(Set<INamedChild<IPackage>> data) {
      Queue<INamedChild<IPackage>> queue = new ArrayDeque<>(data);
      Set<IData> superTypes = new LinkedHashSet<>();
      while (!queue.isEmpty()) {
         INamedChild<IPackage> value = queue.poll();
         IData datum;
         if (value instanceof IData) {
            datum = (IData) value;
         } else {
            continue;
         }
         for (IDataField field : datum.getFields()) {
            switch (field.getType()) {
            case DATA:
               if (data.add(field.getReferencedDataType())) {
                  queue.add(field.getReferencedDataType());
               }
               break;
            case ENUM:
               data.add(field.getReferencedEnumeration());
               break;
            default:
               // Ignore primitive field types
               break;
            }
         }

         while (datum.getExtendedDataType().isPresent()) {
            datum = datum.getExtendedDataType().get();
            if (superTypes.add(datum)) {
               queue.add(datum);
            }
         }
      }
   }

   @Activate
   public void activate() {
      logService.trace(getClass(), "Activated");
   }

   @Deactivate
   public void deactivate() {
      logService.trace(getClass(), "Deactivated");
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setTemplateService(ITemplateService ref) {
      this.templateService = ref;
   }

   public void removeTemplateService(ITemplateService ref) {
      setTemplateService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setLogService(ILogService ref) {
      this.logService = ref;
   }

   public void removeLogService(ILogService ref) {
      setLogService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setScenarioService(IScenarioService ref) {
      this.scenarioService = ref;
   }

   public void removeScenarioService(IScenarioService ref) {
      setLogService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setTransportConfigurationService(ITransportConfigurationService ref) {
      this.transportConfigService = ref;
   }

   public void removeTransportConfigurationService(ITransportConfigurationService ref) {
      setTransportConfigurationService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setRequirementsService(IRequirementsService ref) {
      this.requirementsService = ref;
   }

   public void removeRequirementsService(IRequirementsService ref) {
      setRequirementsService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setPackageNamingService(IPackageNamingService ref) {
      this.packageService = ref;
   }

   public void removePackageNamingService(IPackageNamingService ref) {
      setPackageNamingService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setProjectNamingService(IProjectNamingService ref) {
      this.projectService = ref;
   }

   public void removeProjectNamingService(IProjectNamingService ref) {
      setProjectNamingService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setJavaServiceGenerationService(IJavaServiceGenerationService ref) {
      this.generationService = ref;
   }

   public void removeJavaServiceGenerationService(IJavaServiceGenerationService ref) {
      setJavaServiceGenerationService(null);
   }
   
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setDataFieldGenerationService(IDataFieldGenerationService ref) {
      this.dataFieldService = ref;
   }

   public void removeDataFieldGenerationService(IDataFieldGenerationService ref) {
      setDataFieldGenerationService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
   public void setBuildManagementService(IBuildManagementService ref) {
      this.buildManagementService = ref;
   }

   public void removeBuildManagementService(IBuildManagementService ref) {
      setBuildManagementService(null);
   }

   /**
    * Create the usage for this command.
    *
    * @return the usage.
    */
   private static IUsage createUsage() {
      return new DefaultUsage("Creates a new JellyFish Pub/Sub Connector project.",
         CommonParameters.OUTPUT_DIRECTORY.required(),
         CommonParameters.GROUP_ID,
         CommonParameters.ARTIFACT_ID,
         CommonParameters.MODEL.required(),
         CommonParameters.CLEAN,
         CommonParameters.UPDATE_GRADLE_SETTING);
   }
}
