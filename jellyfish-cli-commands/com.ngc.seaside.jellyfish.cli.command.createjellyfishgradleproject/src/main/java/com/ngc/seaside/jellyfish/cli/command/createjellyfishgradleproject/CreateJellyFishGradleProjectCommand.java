package com.ngc.seaside.jellyfish.cli.command.createjellyfishgradleproject;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.CommandException;
import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.DefaultParameter;
import com.ngc.seaside.jellyfish.api.DefaultParameterCollection;
import com.ngc.seaside.jellyfish.api.DefaultUsage;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IParameter;
import com.ngc.seaside.jellyfish.api.IUsage;
import com.ngc.seaside.jellyfish.cli.command.createjellyfishgradleproject.dto.GradleProjectDto;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.CommonDependencies;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.DependencyScope;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.IBuildDependency;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.IBuildManagementService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectInformation;
import com.ngc.seaside.jellyfish.service.template.api.ITemplateService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 */
@Component(service = IJellyFishCommand.class)
public class CreateJellyFishGradleProjectCommand implements IJellyFishCommand {

   private static final String NAME = "create-jellyfish-gradle-project";
   private static final IUsage USAGE = createUsage();

   public static final String OUTPUT_DIR_PROPERTY = CommonParameters.OUTPUT_DIRECTORY.getName();
   public static final String GROUP_ID_PROPERTY = CommonParameters.GROUP_ID.getName();
   public static final String SYSTEM_DESCRIPTOR_GAV_PROPERTY = CommonParameters.GROUP_ARTIFACT_VERSION.getName();
   public static final String MODEL_NAME_PROPERTY = CommonParameters.MODEL.getName();
   public static final String DEPLOYMENT_MODEL_NAME_PROPERTY = CommonParameters.DEPLOYMENT_MODEL.getName();

   public static final String PROJECT_NAME_PROPERTY = "projectName";
   public static final String VERSION_PROPERTY = "version";
   public static final String JELLYFISH_GRADLE_PLUGINS_VERSION_PROPERTY = "jellyfishGradlePluginsVersion";
   public static final String DEFAULT_GROUP_ID = "com.ngc.seaside";

   private ILogService logService;
   private ITemplateService templateService;
   private IBuildManagementService buildManagementService;

   @Activate
   public void activate() {
      logService.trace(getClass(), "Activated");
   }

   @Deactivate
   public void deactivate() {
      logService.trace(getClass(), "Deactivated");
   }

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
      DefaultParameterCollection collection = new DefaultParameterCollection();
      collection.addParameters(commandOptions.getParameters().getAllParameters());

      final String projectName = collection.getParameter(PROJECT_NAME_PROPERTY).getStringValue();

      // Create project directory.
      final Path outputDirectory = Paths.get(collection.getParameter(OUTPUT_DIR_PROPERTY).getStringValue());
      final Path projectDirectory = outputDirectory.resolve(projectName);
      try {
         Files.createDirectories(projectDirectory);
      } catch (IOException e) {
         logService.error(CreateJellyFishGradleProjectCommand.class, e);
         throw new CommandException(e);
      }

      registerRequiredDependencies(commandOptions);

      String groupId = collection.containsParameter(GROUP_ID_PROPERTY)
                       ? collection.getParameter(GROUP_ID_PROPERTY).getStringValue()
                       : DEFAULT_GROUP_ID;
      GradleProjectDto dto = new GradleProjectDto()
            .setGroupId(groupId)
            .setProjectName(collection.getParameter(PROJECT_NAME_PROPERTY).getStringValue())
            .setVersion(collection.getParameter(VERSION_PROPERTY).getStringValue())
            .setSystemDescriptorGav(collection.getParameter(SYSTEM_DESCRIPTOR_GAV_PROPERTY).getStringValue())
            .setModelName(collection.getParameter(MODEL_NAME_PROPERTY).getStringValue())
            .setDeploymentModelName(getDeploymentModel(commandOptions))
            .setBuildScriptDependencies(getBuildScriptDependencies(commandOptions))
            .setVersionProperties(getVersionProperties(commandOptions))
            .setProjects(getProjects(commandOptions));
      collection.addParameter(new DefaultParameter<>("dto", dto));

      boolean clean = CommonParameters.evaluateBooleanParameter(collection, CommonParameters.CLEAN.getName(), false);
      String templateName = CreateJellyFishGradleProjectCommand.class.getPackage().getName();
      templateService.unpack(templateName, collection, projectDirectory, clean);
   }

   /**
    * Sets log service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC,
         unbind = "removeLogService")
   public void setLogService(ILogService ref) {
      this.logService = ref;
   }

   /**
    * Remove log service.
    */
   public void removeLogService(ILogService ref) {
      setLogService(null);
   }

   /**
    * Sets template service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC,
         unbind = "removeTemplateService")
   public void setTemplateService(ITemplateService ref) {
      this.templateService = ref;
   }

   /**
    * Remove template service.
    */
   public void removeTemplateService(ITemplateService ref) {
      setTemplateService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC)
   public void setBuildManagementService(IBuildManagementService ref) {
      this.buildManagementService = ref;
   }

   public void removeBuildManagementService(IBuildManagementService ref) {
      setBuildManagementService(null);
   }

   private void registerRequiredDependencies(IJellyFishCommandOptions commandOptions) {
      // These dependencies are required but are not registered directly by a template or command, so we do this here.
      buildManagementService.registerDependency(commandOptions,
                                                CommonDependencies.JELLYFISH_GRADLE_PLUGINS.getGropuId(),
                                                CommonDependencies.JELLYFISH_GRADLE_PLUGINS.getArtifactId());
      buildManagementService.registerDependency(commandOptions,
                                                CommonDependencies.SEASIDE_GRADLE_PLUGINS.getGropuId(),
                                                CommonDependencies.SEASIDE_GRADLE_PLUGINS.getArtifactId());
      buildManagementService.registerDependency(commandOptions,
                                                CommonDependencies.SONARQUBE_GRADLE_PLUGIN.getGropuId(),
                                                CommonDependencies.SONARQUBE_GRADLE_PLUGIN.getArtifactId());
   }

   private Collection<IBuildDependency> getBuildScriptDependencies(IJellyFishCommandOptions commandOptions) {
      return buildManagementService.getRegisteredDependencies(commandOptions, DependencyScope.BUILDSCRIPT);
   }

   private SortedMap<String, String> getVersionProperties(IJellyFishCommandOptions commandOptions) {
      SortedMap<String, String> versions = new TreeMap<>();

      EnumSet<DependencyScope> scopes = EnumSet.complementOf(EnumSet.of(DependencyScope.BUILDSCRIPT));
      for (DependencyScope scope : scopes) {
         for (IBuildDependency dependency : buildManagementService.getRegisteredDependencies(commandOptions, scope)) {
            versions.put(dependency.getVersionPropertyName(), dependency.getVersion());
         }
      }

      return versions;
   }

   private Collection<IProjectInformation> getProjects(IJellyFishCommandOptions commandOptions) {
      return buildManagementService.getRegisteredProjects();
   }

   private String getDeploymentModel(IJellyFishCommandOptions options) {
      IParameter<?> deploymentParameter = options.getParameters().getParameter(DEPLOYMENT_MODEL_NAME_PROPERTY);
      if (deploymentParameter == null) {
         return null;
      } else {
         return deploymentParameter.getStringValue();
      }
   }

   /**
    * Create the usage for this command.
    *
    * @return the usage.
    */
   private static IUsage createUsage() {
      return new DefaultUsage(
            "Creates a new JellyFish Gradle project. This requires that a settings.gradle file be present in the output"
            + " directory. It also requires that the jellyfishAPIVersion be set in the parent build.gradle.",
            CommonParameters.OUTPUT_DIRECTORY.required(),
            new DefaultParameter<>(PROJECT_NAME_PROPERTY)
                  .setDescription("The name of the Gradle project. This should use hyphens and lower case letters,"
                                  + " (ie my-project)")
                  .setRequired(false),
            new DefaultParameter<>(JELLYFISH_GRADLE_PLUGINS_VERSION_PROPERTY)
                  .setDescription("The version of the Jellyfish Gradle plugins to use when generating the script."
                                  + "  Defaults to the current version of Jellyfish.")
                  .setRequired(false),
            CommonParameters.GROUP_ID,
            new DefaultParameter<>(VERSION_PROPERTY)
                  .setDescription("The version to use for the Gradle project")
                  .setRequired(true),
            CommonParameters.GROUP_ARTIFACT_VERSION.required(),
            CommonParameters.MODEL.required(),
            CommonParameters.DEPLOYMENT_MODEL,
            CommonParameters.CLEAN);
   }

}