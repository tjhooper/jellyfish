package com.ngc.seaside.jellyfish.cli.command.createjavacucumbertests;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.bootstrap.service.template.api.ITemplateService;
import com.ngc.seaside.bootstrap.utilities.file.FileUtilitiesException;
import com.ngc.seaside.bootstrap.utilities.file.GradleSettingsUtilities;
import com.ngc.seaside.command.api.CommandException;
import com.ngc.seaside.command.api.DefaultParameter;
import com.ngc.seaside.command.api.DefaultParameterCollection;
import com.ngc.seaside.command.api.DefaultUsage;
import com.ngc.seaside.command.api.IUsage;
import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.cli.command.createjavacucumbertests.dto.CucumberDto;
import com.ngc.seaside.jellyfish.service.codegen.api.IJavaServiceGenerationService;
import com.ngc.seaside.jellyfish.service.feature.api.IFeatureInformation;
import com.ngc.seaside.jellyfish.service.feature.api.IFeatureService;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectInformation;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;

import org.apache.commons.io.FileUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.TreeMap;

@Component(service = IJellyFishCommand.class)
public class CreateJavaCucumberTestsCommand implements IJellyFishCommand {

   private static final String NAME = "create-java-cucumber-tests";
   private static final IUsage USAGE = createUsage();

   public static final String GROUP_ID_PROPERTY = CommonParameters.GROUP_ID.getName();
   public static final String ARTIFACT_ID_PROPERTY = CommonParameters.ARTIFACT_ID.getName();
   public static final String OUTPUT_DIRECTORY_PROPERTY = CommonParameters.OUTPUT_DIRECTORY.getName();
   public static final String MODEL_PROPERTY = CommonParameters.MODEL.getName();
   public static final String CLEAN_PROPERTY = CommonParameters.CLEAN.getName();
   public static final String REFRESH_FEATURE_FILES_PROPERTY = "refreshFeatureFiles";

   public static final String MODEL_OBJECT_PROPERTY = "modelObject";

   private ILogService logService;
   private ITemplateService templateService;
   private IProjectNamingService projectNamingService;
   private IPackageNamingService packageNamingService;
   private IJavaServiceGenerationService generationService;
   private IFeatureService featureService;

   @Override
   public void run(IJellyFishCommandOptions commandOptions) {

      DefaultParameterCollection parameters = new DefaultParameterCollection();
      parameters.addParameters(commandOptions.getParameters().getAllParameters());

      String modelId = parameters.getParameter(MODEL_PROPERTY).getStringValue();
      final IModel model = commandOptions.getSystemDescriptor()
                                         .findModel(modelId)
                                         .orElseThrow(() -> new CommandException("Unknown model:" + modelId));
      parameters.addParameter(new DefaultParameter<>(MODEL_OBJECT_PROPERTY, model));

      final Path outputDirectory = Paths.get(parameters.getParameter(OUTPUT_DIRECTORY_PROPERTY).getStringValue());
      doCreateDirectories(outputDirectory);

      IProjectInformation info = projectNamingService.getCucumberTestsProjectName(commandOptions, model);

      final String packageName = packageNamingService.getCucumberTestsPackageName(commandOptions, model);
      final String projectName = info.getDirectoryName();
      final boolean clean = CommonParameters.evaluateBooleanParameter(commandOptions.getParameters(), CLEAN_PROPERTY);
      
      
      if (!CommonParameters.evaluateBooleanParameter(commandOptions.getParameters(), REFRESH_FEATURE_FILES_PROPERTY)) {

         CucumberDto dto = new CucumberDto().setProjectName(projectName)
                                            .setPackageName(packageName)
                                            .setClassName(model.getName())
                                            .setTransportTopicsClass(generationService.getTransportTopicsDescription(commandOptions, model).getFullyQualifiedName())
                                            .setDependencies(new LinkedHashSet<>(Arrays.asList(
                                               projectNamingService.getMessageProjectName(commandOptions, model)
                                                                   .getArtifactId(),
                                               projectNamingService.getBaseServiceProjectName(commandOptions, model)
                                                                   .getArtifactId())));

         parameters.addParameter(new DefaultParameter<>("dto", dto));

         templateService.unpack(CreateJavaCucumberTestsCommand.class.getPackage().getName(),
            parameters,
            outputDirectory,
            clean);
         logService.info(CreateJavaCucumberTestsCommand.class, "%s project successfully created", model.getName());
         updateGradleDotSettings(outputDirectory, info);
      }
      copyFeatureFilesToGeneratedProject(commandOptions, model, outputDirectory.resolve(projectName), clean);
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public IUsage getUsage() {
      return USAGE;
   }

   @Activate
   public void activate() {
      logService.trace(getClass(), "Activated");
   }

   @Deactivate
   public void deactivate() {
      logService.trace(getClass(), "Deactivated");
   }

   /**
    * Sets log service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removeLogService")
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
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removeTemplateService")
   public void setTemplateService(ITemplateService ref) {
      this.templateService = ref;
   }

   /**
    * Remove template service.
    */
   public void removeTemplateService(ITemplateService ref) {
      setTemplateService(null);
   }
   
   /**
    * Sets project naming service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removeProjectNamingService")
   public void setProjectNamingService(IProjectNamingService ref) {
      this.projectNamingService = ref;
   }

   /**
    * Remove project naming service.
    */
   public void removeProjectNamingService(IProjectNamingService ref) {
      setProjectNamingService(null);
   }
   
   /**
    * Sets package naming service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removePackageNamingService")
   public void setPackageNamingService(IPackageNamingService ref) {
      this.packageNamingService = ref;
   }

   /**
    * Remove package naming service.
    */
   public void removePackageNamingService(IPackageNamingService ref) {
      setPackageNamingService(null);
   }
   
   /**
    * Sets feature service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removeFeatureService")
   public void setFeatureService(IFeatureService ref) {
      this.featureService = ref;
   }

   /**
    * Remove feature service.
    */
   public void removeFeatureService(IFeatureService ref) {
      setFeatureService(null);
   }
   
   /**
    * Sets java service generation service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "removeJavaServiceGenerationService")
   public void setJavaServiceGenerationService(IJavaServiceGenerationService ref) {
      this.generationService = ref;
   }

   /**
    * Remove java service generation service.
    */
   public void removeJavaServiceGenerationService(IJavaServiceGenerationService ref) {
      setJavaServiceGenerationService(null);
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

   protected void doCreateDirectories(Path outputDirectory) {
      try {
         Files.createDirectories(outputDirectory);
      } catch (IOException e) {
         logService.error(CreateJavaCucumberTestsCommand.class, e);
         throw new CommandException(e);
      }
   }

   /**
    * Copies feature files from a System Descriptor project to a newly generated test project. Only feature files that
    * apply to scenarios in the given model will be copied. Any features files that are already in the test project
    * will be deleted before coping the new files.
    *
    * @param model the model for which the feature files will be copied
    * @param commandOptions the options the command was run with
    * @param generatedProjectDirectory the directory that contains the generated tests project
    * @param clean if true, deletes the features and resources before copying them
    * @throws IOException
    */
   private void copyFeatureFilesToGeneratedProject(IJellyFishCommandOptions commandOptions, IModel model,
            Path generatedProjectDirectory, boolean clean) {
      
      TreeMap<String, IFeatureInformation> features = featureService.getFeatures(commandOptions.getSystemDescriptorProjectPath(), model);
      
      final Path dataFile = commandOptions.getSystemDescriptorProjectPath()
               .resolve(Paths.get("src", "test", "resources", "data"))
               .toAbsolutePath();

      final Path destination = generatedProjectDirectory.resolve(Paths.get("src", "main", "resources"));
      final Path dataDestination = generatedProjectDirectory.resolve(Paths.get("src", "main", "resources", "data"));

      deleteDir(destination.resolve(model.getParent().getName()).toFile());
      deleteDir(dataDestination.toFile());

      for (IFeatureInformation featureInfo : features.values()) {
         Path featureDestination = destination.resolve(featureInfo.getRelativePath());
         try {
            Files.createDirectories(featureDestination.getParent());
            Files.copy(featureInfo.getAbsolutePath(), featureDestination, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            throw new CommandException("Failed to copy " + featureInfo.getAbsolutePath() + " to " + featureDestination, e);
         }
      }
      if (Files.isDirectory(dataFile)) {
         try {
            FileUtils.copyDirectory(dataFile.toFile(), dataDestination.toFile());
         } catch (IOException e) {
            throw new CommandException("Failed to copy resources  to " + destination, e);
         }
      }
   }

   /**
    * Create the usage for this command.
    *
    * @return the usage.
    */
   @SuppressWarnings("rawtypes")
   private static IUsage createUsage() {
      return new DefaultUsage("Generates the gradle distribution project for a Java application",
         CommonParameters.GROUP_ID,
         CommonParameters.ARTIFACT_ID,
         CommonParameters.OUTPUT_DIRECTORY.required(),
         CommonParameters.MODEL.required(),
         CommonParameters.CLEAN,
         new DefaultParameter(REFRESH_FEATURE_FILES_PROPERTY).setDescription(
            "If true, only copy the feature files and resources from the system descriptor project into src/main/resources.")
                                                             .setRequired(false));
   }

   /**
    * Helper method to delete folder/files
    *
    * @param file file/folder to delete
    */
   private static void deleteDir(File file) {
      File[] contents = file.listFiles();
      if (contents != null) {
         for (File f : contents) {
            deleteDir(f);
         }
      }
      file.delete();
   }

}
