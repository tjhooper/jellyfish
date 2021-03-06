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
package com.ngc.seaside.jellyfish.cli.command.report.requirementsallocation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ngc.seaside.jellyfish.api.DefaultParameter;
import com.ngc.seaside.jellyfish.api.DefaultUsage;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IUsage;
import com.ngc.seaside.jellyfish.api.ParameterCategory;
import com.ngc.seaside.jellyfish.cli.command.report.requirementsallocation.utilities.MatrixUtils;
import com.ngc.seaside.jellyfish.cli.command.report.requirementsallocation.utilities.ModelUtils;
import com.ngc.seaside.jellyfish.service.requirements.api.IRequirementsService;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.service.log.api.ILogService;

@Component(service = IJellyFishCommand.class)
public class RequirementsAllocationMatrixCommand implements IJellyFishCommand {

   public static enum OutputFormatType {
      DEFAULT("default"), CSV("csv");

      public final String type;

      OutputFormatType(String type) {
         this.type = type;
      }
   }

   private static final String NAME = "requirements-allocation-matrix";
   private static final IUsage USAGE = createUsage();

   public static final String OUTPUT_FORMAT_PROPERTY = "outputFormat";
   public static final String OUTPUT_PROPERTY = "output";
   public static final String SCOPE_PROPERTY = "scope";
   public static final String VALUES_PROPERTY = "values";
   public static final String OPERATOR_PROPERTY = "operator";

   public static final String DEFAULT_OUTPUT_FORMAT_PROPERTY = OutputFormatType.DEFAULT.type;
   public static final String DEFAULT_OUTPUT_PROPERTY = "STDOUT";
   public static final String DEFAULT_SCOPE_PROPERTY = "model.metadata.json.stereotypes";
   public static final String DEFAULT_VALUES_PROPERTY = "service";
   public static final String DEFAULT_OPERATOR_PROPERTY = "OR";

   private ILogService logService;
   private IRequirementsService requirementsService;

   @Override
   public void run(IJellyFishCommandOptions commandOptions) {
      String outputFormat = evaluateOutputFormat(commandOptions);
      String output = evaluateOutput(commandOptions);
      String values = evaluateValues(commandOptions);
      String operator = evaluateOperator(commandOptions);

      Collection<IModel> models = new TreeSet<>(new Comparator<IModel>() {
         @Override
         public int compare(IModel o1, IModel o2) {
            return o1.getName().compareTo(o2.getName());
         }
      });
      models.addAll(ModelUtils.searchModels(commandOptions, values, operator));
      Collection<Requirement> requirements = searchForRequirements(commandOptions, models);

      String report = "";
      if (outputFormat.equalsIgnoreCase("csv")) {
         report = MatrixUtils.generateCsvAllocationMatrix(requirements, models);
      } else {
         report = String.valueOf(MatrixUtils.generateDefaultAllocationMatrix(requirements, models));
      }

      boolean success = false;

      if (output.equalsIgnoreCase(DEFAULT_OUTPUT_PROPERTY)) {
         logService.info(RequirementsAllocationMatrixCommand.class, "Printing report to console...");
         MatrixUtils.printAllocationConsole(report);
         success = true;
      } else {
         Path outputPath = getAbsoluteOutputPath(output, commandOptions);

         try {
            logService.info(RequirementsAllocationMatrixCommand.class, "Printing report to location: %s", outputPath);
            MatrixUtils.printAllocationMatrixToFile(report, outputPath);
            success = true;
         } catch (IOException e) {
            logService.error(RequirementsAllocationMatrixCommand.class, "Failed to print report to location: %s",
                             outputPath.toString(), e);
         }
      }

      if (success) {
         logService.info(RequirementsAllocationMatrixCommand.class,
                         "%s requirements allocation matrix successfully created", values);
      } else {
         logService.info(RequirementsAllocationMatrixCommand.class,
                         "%s requirements allocation matrix not successfully created", values);
      }
   }

   /**
    * Searches a collection of models for all requirements satisfied
    *
    * @param models         the list of models to search for requirements
    * @param commandOptions the command options
    * @return colletion of all requirements satisfied
    */
   private Collection<Requirement> searchForRequirements(IJellyFishCommandOptions commandOptions,
                                                         Collection<IModel> models) {
      final Map<String, Requirement> requirementsMap = new TreeMap<>();

      models.forEach(m -> searchForRequirements(commandOptions, m, requirementsMap));

      return requirementsMap.values();
   }

   /**
    * Populates a map with all requirements that a given model satisfies
    *
    * @param commandOptions  the command options
    * @param model           the model to search for requirements
    * @param requirementsMap the requirements map to populate
    */
   private void searchForRequirements(IJellyFishCommandOptions commandOptions, IModel model,
                                      Map<String, Requirement> requirementsMap) {
      Collection<String>
            requirementsSet =
            ModelUtils.getAllRequirementsForModel(commandOptions, requirementsService, model);

      requirementsSet.forEach(eachReqName -> {
         if (!requirementsMap.containsKey(eachReqName)
               || requirementsMap.get(eachReqName) == null) {
            requirementsMap.put(eachReqName, new Requirement(eachReqName));
         }

         requirementsMap.get(eachReqName).addModel(model);
      });
   }

   private Path getAbsoluteOutputPath(String output, IJellyFishCommandOptions commandOptions) {
      Path path = Paths.get(output);
      if (!path.isAbsolute()) {
         path = Paths.get(output);
      }
      return path;
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
    * Sets requirements service.
    *
    * @param ref the ref
    */
   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC,
         unbind = "removeRequirementsService")
   public void setRequirementsService(IRequirementsService ref) {
      this.requirementsService = ref;
   }

   /**
    * Remove requirements service.
    */
   public void removeRequirementsService(IRequirementsService ref) {
      setRequirementsService(null);
   }

   /**
    * Create the usage for this command.
    *
    * @return the usage.
    */
   @SuppressWarnings("rawtypes")
   private static IUsage createUsage() {
      return new DefaultUsage("Generates a matrix showing requirements and which services cover them",
                              new DefaultParameter(OUTPUT_FORMAT_PROPERTY).setDescription(
                                    "Format of the output; possible values are default and csv")
                                    .setParameterCategory(ParameterCategory.OPTIONAL),
                              new DefaultParameter(OUTPUT_PROPERTY).setDescription(
                                    "File where the output is sent; default prints to stdout")
                                    .setParameterCategory(ParameterCategory.OPTIONAL),
                              new DefaultParameter(SCOPE_PROPERTY).setDescription(
                                    "Keyword scope (metadata, input, output, etc..); "
                                    + "default: model.metadata.json.stereotypes")
                                    .setParameterCategory(ParameterCategory.OPTIONAL),
                              new DefaultParameter(VALUES_PROPERTY).setDescription(
                                    "The values in which to search for in the scope defined as a comma-separated "
                                    + "string; default: service")
                                    .setParameterCategory(ParameterCategory.OPTIONAL),
                              new DefaultParameter(OPERATOR_PROPERTY).setDescription(
                                    "How the values should be logically combined; "
                                    + "possible values are AND, OR, NOT; default: OR")
                                    .setParameterCategory(ParameterCategory.OPTIONAL));
   }

   /**
    * Retrieve the output format property value based on user input. Default is a table formatted string.
    *
    * @param commandOptions Jellyfish command options containing user params
    */
   private static String evaluateOutputFormat(IJellyFishCommandOptions commandOptions) {
      String outputFormat = DEFAULT_OUTPUT_FORMAT_PROPERTY;
      if (commandOptions.getParameters().containsParameter(OUTPUT_FORMAT_PROPERTY)) {
         String helper = commandOptions.getParameters().getParameter(OUTPUT_FORMAT_PROPERTY).getStringValue();
         outputFormat = (helper.equalsIgnoreCase("CSV")) ? helper.toUpperCase()
                                                         : DEFAULT_OUTPUT_FORMAT_PROPERTY;
      }
      return outputFormat;
   }

   /**
    * Retrieve the output property value based on user input. Default is: {@value #DEFAULT_OUTPUT_PROPERTY}
    *
    * @param commandOptions Jellyfish command options containing user params
    */
   private static String evaluateOutput(IJellyFishCommandOptions commandOptions) {
      String output = DEFAULT_OUTPUT_PROPERTY;
      if (commandOptions.getParameters().containsParameter(OUTPUT_PROPERTY)) {
         output = commandOptions.getParameters().getParameter(OUTPUT_PROPERTY).getStringValue();
      }

      return output;
   }

   /**
    * Retrieve the values property value based on user input. Default is: {@value #DEFAULT_VALUES_PROPERTY}
    *
    * @param commandOptions Jellyfish command options containing user params
    */
   private static String evaluateValues(IJellyFishCommandOptions commandOptions) {
      String values = DEFAULT_VALUES_PROPERTY;
      if (commandOptions.getParameters().containsParameter(VALUES_PROPERTY)) {
         values = commandOptions.getParameters().getParameter(VALUES_PROPERTY).getStringValue();
      }
      return values;
   }

   /**
    * Retrieve the operator property value based on user input. Default is: {@value #DEFAULT_OPERATOR_PROPERTY}
    *
    * @param commandOptions Jellyfish command options containing user params
    */
   private static String evaluateOperator(IJellyFishCommandOptions commandOptions) {
      String operator = DEFAULT_OPERATOR_PROPERTY;
      if (commandOptions.getParameters().containsParameter(OPERATOR_PROPERTY)) {
         String helper = commandOptions.getParameters().getParameter(OPERATOR_PROPERTY).getStringValue().toUpperCase();
         operator = (helper.equalsIgnoreCase("AND") || helper.equalsIgnoreCase("NOT")) ? helper : operator;
      }
      return operator;
   }

}
