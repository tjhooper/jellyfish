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
package com.ngc.seaside.systemdescriptor.validation;

import com.google.inject.Inject;

import com.ngc.seaside.systemdescriptor.systemDescriptor.BaseLinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.GivenDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Input;
import com.ngc.seaside.systemdescriptor.systemDescriptor.InputDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkableReference;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Output;
import com.ngc.seaside.systemdescriptor.systemDescriptor.OutputDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.PartDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Parts;
import com.ngc.seaside.systemdescriptor.systemDescriptor.RequireDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Requires;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Scenario;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Step;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;
import com.ngc.seaside.systemdescriptor.systemDescriptor.ThenDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.WhenDeclaration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;
import org.eclipse.xtext.validation.Check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Validates a {@code Model} is correct. This validator mostly handles checking
 * for duplicate declarations of fields within a model.
 */
public class ModelValidator extends AbstractUnregisteredSystemDescriptorValidator {

   // TODO TH: Refactor this impl and the error messages.  The error messages should not use the word "class" because
   // the SD language does not have classes.

   // TODO TH: inject the interface instead of the impl
   @Inject
   private ResourceDescriptionsProvider resourceDescriptionsProvider;

   // TODO TH: why are we using this?  Need comments.
   @Inject
   private IContainer.Manager containerManager;

   // The grammar allows the various blocks to be declared in the following
   // order:
   //
   // 1) requires
   // 2) input
   // 3) output
   // 4) scenarios
   // 5) parts
   //
   // Therefore, we check for duplicate declarations in the manner shown
   // below. The requires block has the fewest checks and the parts block
   // has the most. This is because we consider a requirement declared in
   // the requires block to have a "higher precedence" than a part with the
   // same name in the parts block because requires is listed first. Thus,
   // the part declaration is the duplicate (not the requires declaration).

   /**
    * Validates that the user did not try to escape a keyword with ^ in the
    * name of the model.
    *
    * @param model is the model to evaluate
    */
   @Check
   public void checkUsageOfEscapeHatCharacter(Model model) {
      // Verify the data name doesn't not have the escape hat
      if (model.getName().indexOf('^') >= 0) {
         String msg = String.format(
               "Cannot use '^' to escape the model name %s.",
               model.getName());
         error(msg, model, SystemDescriptorPackage.Literals.ELEMENT__NAME);
      }

   }

   /**
    * Validates that the user did not try to escape a keyword with ^ in any
    * requires declaration.
    *
    * @param declaration is the OutputDeclaration to evaluate
    */
   @Check
   public void checkUsageOfEscapeHatCharacter(FieldDeclaration declaration) {
      // Verify the data name doesn't not have the escape hat
      if (declaration.getName().indexOf('^') >= 0) {
         String msg = String.format(
               "Cannot use '^' to escape the field declaration name %s.",
               declaration.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
      }

   }

   /**
    * Validates that a require declaration is correct. Requires the containing
    * model not contain another requirement with the same name.
    */
   @Check
   public void checkForDuplicateRequirements(RequireDeclaration declaration) {
      // Ensure that the model does not already have a requirement with the
      // same
      // name.
      Requires requires = (Requires) declaration.eContainer();
      Model model = (Model) requires.eContainer();

      if (getNumberOfRequirementsNamed(model, declaration.getName()) > 1) {
         String msg = String.format(
               "A requirement named '%s' is already defined for the model '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
      }
   }


   /**
    * Validates that an input declaration is correct. Requires the containing
    * model not contain another input field with the same name and requires the
    * model have no requirement with the same name.
    */
   @Check
   public void checkForDuplicateInputFields(InputDeclaration declaration) {
      // Ensure that the model does not already have a declared input data
      // field with the same name.
      Input input = (Input) declaration.eContainer();
      Model model = (Model) input.eContainer();
      if (getNumberOfInputFieldsNamed(model, declaration.getName()) > 1) {
         String msg = String.format(
               "An input named '%s' is already defined for the model '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure that the model does not already have a declared
         // requirement with the same name.
      } else if (getNumberOfRequirementsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "A requirement named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
      }
   }

   /**
    * Validates that an output declaration is correct. Requires the containing
    * model not contain another output field with the same name, requires the
    * model have no requirement with the same name, and requires the model have
    * no input field with the same name.
    */
   @Check
   public void checkForDuplicateOutputFields(OutputDeclaration declaration) {
      // Ensure that the model does not already have a declared output data
      // field with the same name.
      Output output = (Output) declaration.eContainer();
      Model model = (Model) output.eContainer();

      if (getNumberOfOutputFieldsNamed(model, declaration.getName()) > 1) {
         String msg = String.format(
               "An output named '%s' is already defined for the model '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure that the model does not already have a declared
         // requirement with the same name.
      } else if (getNumberOfRequirementsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "A requirement named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure that the model does not already have a declared input
         // data field with the same name.
      } else if (getNumberOfInputFieldsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "An input named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
      }
   }

   /**
    * Validates that a scenario is correct. Requires the containing model not
    * contain another scenario with the same name, requires the model not
    * contain an output field with the same name, requires the model have no
    * requirement with the same name, and requires the model have no input
    * field with the same name.
    */
   @Check
   public void checkForDuplicateScenarios(Scenario scenario) {
      // Ensure that the model does not already have a scenario with the same
      // name.
      Model model = (Model) scenario.eContainer();

      if (getNumberOfScenariosNamed(model, scenario.getName()) > 1) {
         String msg = String.format(
               "A scenario named '%s' is already defined for the model '%s'.",
               scenario.getName(),
               model.getName());
         error(msg, scenario, SystemDescriptorPackage.Literals.SCENARIO__NAME);

         // Ensure that the model does not already have a declared
         // requirement with the same name.
      } else if (getNumberOfRequirementsNamed(model, scenario.getName()) > 0) {
         String msg = String.format(
               "A requirement named '%s' is already defined for the element '%s'.",
               scenario.getName(),
               model.getName());
         error(msg, scenario, SystemDescriptorPackage.Literals.SCENARIO__NAME);

         // Ensure that the model does not already have a declared input
         // data field with the same name.
      } else if (getNumberOfInputFieldsNamed(model, scenario.getName()) > 0) {
         String msg = String.format(
               "An input named '%s' is already defined for the element '%s'.",
               scenario.getName(),
               model.getName());
         error(msg, scenario, SystemDescriptorPackage.Literals.SCENARIO__NAME);

         // Ensure that the model does not already have a declared output
         // data field with the same name.
      } else if (getNumberOfOutputFieldsNamed(model, scenario.getName()) > 0) {
         String msg = String.format(
               "An output named '%s' is already defined for the element '%s'.",
               scenario.getName(),
               model.getName());
         error(msg, scenario, SystemDescriptorPackage.Literals.SCENARIO__NAME);
      }
   }

   /**
    * Validates that the part declaration is correct. Requires the containing
    * model not to contain another part declaration with the same name,
    * requires the model have no requirement with the same name, requires the
    * mode not to have another input field with the same name, requires the
    * model not to have another output field with the same name, and requires
    * the model not to have a scenario with the same name.
    */
   @Check
   public void checkForDuplicateParts(PartDeclaration declaration) {
      // Ensure that the model does not already have a part with the same
      // name.
      Parts parts = (Parts) declaration.eContainer();
      Model model = (Model) parts.eContainer();

      if (getNumberOfPartsNamed(model, declaration.getName()) > 1) {
         String msg = String.format(
               "A part named '%s' is already defined for the model '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure that the model does not already have a declared
         // requirement with the same name.
      } else if (getNumberOfRequirementsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "A requirement named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure that the model does not already have a declared input
         // data field with the same name.
      } else if (getNumberOfInputFieldsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "An input named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure there is no output field with the same name.
      } else if (getNumberOfOutputFieldsNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "An output named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);

         // Ensure there is no scenario with the same name.
      } else if (getNumberOfScenariosNamed(model, declaration.getName()) > 0) {
         String msg = String.format(
               "A scenario named '%s' is already defined for the element '%s'.",
               declaration.getName(),
               model.getName());
         error(msg, declaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
      }
   }


   private void checkModelScenarios(Model model, List<String> superclasses,
                                    HashMap<String, String> dataFieldDeclarations) {
      if (model.getScenarios() != null) {
         //Only need to check input and output at the moment since parts and requires use models.

         for (Scenario scenario : model.getScenarios()) {
            GivenDeclaration given = scenario.getGiven();
            if (given != null) {
               for (Step step : given.getSteps()) {
                  for (String objectname : step.getParameters()) {
                     checkModelScenarioStep(superclasses, dataFieldDeclarations.get(objectname), step);
                  }
               }
            }

            WhenDeclaration when = scenario.getWhen();
            if (when != null) {
               for (Step step : when.getSteps()) {
                  for (String objectname : step.getParameters()) {
                     checkModelScenarioStep(superclasses, dataFieldDeclarations.get(objectname), step);
                  }
               }
            }

            ThenDeclaration then = scenario.getThen();
            if (then != null) {
               for (Step step : then.getSteps()) {
                  for (String objectname : step.getParameters()) {
                     checkModelScenarioStep(superclasses, dataFieldDeclarations.get(objectname), step);
                  }
               }
            }
         }
      }
   }

   private void checkModelScenarioStep(List<String> superclasses, String classname, Step step) {
      if (classname != null && superclasses.contains(classname)) {
         String msg = String.format(
               "You are using class '%s', a superclass, in your scenario declaration. Try using a class that inherits"
                     + " from '%s' instead.",
               classname, classname);
         warning(msg, step, SystemDescriptorPackage.Literals.STEP__PARAMETERS);
      }
   }


   private void checkModelLinks(Model model, List<String> superclasses, HashMap<String, String> dataFieldDeclarations) {
      // TODO TH: refactor this
      if (model.getLinks() != null) {
         //Only need to check input and output at the moment since parts and requires use models.
         //if target or source has an object name that corresponds to a class name that is in the superclasses list,
         //send a warning
         EList<LinkDeclaration> linkDecs = model.getLinks().getDeclarations();

         for (int i = 0; i < linkDecs.size(); i++) {

            List<LinkableReference> linksList = new ArrayList<LinkableReference>();
            //compile linkable references from sources and targets.
            if (linkDecs.get(i).eClass().equals(SystemDescriptorPackage.Literals.BASE_LINK_DECLARATION)) {
               linksList.add(((BaseLinkDeclaration) linkDecs.get(i)).getSource());
               linksList.add(((BaseLinkDeclaration) linkDecs.get(i)).getTarget());
            }

            for (LinkableReference linkRef : linksList) {
               //For linkable references
               for (EObject crossLink : linkRef.eCrossReferences()) {
                  //FieldDeclaration
                  EClass crossClass = crossLink.eClass();
                  if (crossClass == SystemDescriptorPackage.Literals.INPUT_DECLARATION
                        || crossClass == SystemDescriptorPackage.Literals.OUTPUT_DECLARATION) {
                     FieldDeclaration crossLinkData = (FieldDeclaration) crossLink;
                     String classname = dataFieldDeclarations.get(crossLinkData.getName());
                     if (classname != null && superclasses.contains(classname)) {
                        EStructuralFeature feature = null;
                        if (linkRef.eClass().equals(SystemDescriptorPackage.Literals.FIELD_REFERENCE)) {
                           feature = SystemDescriptorPackage.Literals.FIELD_REFERENCE__FIELD_DECLARATION;
                        } else if (linkRef.eClass().equals(SystemDescriptorPackage.Literals.LINKABLE_REFERENCE)) {
                           feature = SystemDescriptorPackage.Literals.LINKABLE_EXPRESSION__REF;
                        }

                        //if this classname is already identified as a superclass.
                        String msg = String.format(
                              "You are using class '%s', a superclass, in your  link declaration. Try using a class"
                                    + " that inherits from '%s' instead.",
                              classname, classname);
                        warning(msg, linkRef, feature);
                     }
                  }
               }
            }
         }
      }
   }

   private List<EObject> getAllProjectObjectsFor(EObject object) {
      List<EObject> objects = new ArrayList<>();
      IResourceDescriptions
            resourceDescriptions =
            resourceDescriptionsProvider.getResourceDescriptions(object.eResource());
      IResourceDescription
            resourceDescription =
            resourceDescriptions.getResourceDescription(object.eResource().getURI());
      for (IContainer container : containerManager.getVisibleContainers(resourceDescription, resourceDescriptions)) {
         for (IEObjectDescription objectDescription : container.getExportedObjects()) {
            EObject objectOrProxy = objectDescription.getEObjectOrProxy();
            objects.add(objectOrProxy);
         }
      }

      return objects;
   }

   /**
    * If the model is using a base class in the links, scenarios, output, or input fields
    * it needs to warn the user.
    */
   @Check
   public void checkForSuperClassDataObject(Model model) {
      // TODO TH: refactor this
      HashMap<String, String>
            dataFieldDeclarations =
            new HashMap<>();
      List<String> superclasses = new ArrayList<>();

      Resource resource = model.eResource();
      if (resource != null) {

         for (EObject object : getAllProjectObjectsFor(model)) {
            if (object.eClass().equals(SystemDescriptorPackage.Literals.DATA)) {

               if (object.eIsProxy()) {
                  object = model.eResource().getResourceSet().getEObject(EcoreUtil.getURI(object), true);
               }

               Data data = (Data) object;
               Data superclass = data.getExtendedDataType();
               if (superclass != null) {
                  String superclassName = superclass.getName();
                  if (!superclasses.contains(superclassName)) {
                     superclasses.add(superclassName);
                  }
               }
            }
         }

         if (model.getInput() != null) {
            for (InputDeclaration inputDeclaration : model.getInput().getDeclarations()) {

               Data inputDeclarationType = inputDeclaration.getType();

               if (inputDeclarationType.eIsProxy()) {
                  inputDeclarationType =
                        (Data) model.eResource().getResourceSet()
                              .getEObject(EcoreUtil.getURI(inputDeclarationType), true);
               }

               String classname = inputDeclarationType.getName();
               dataFieldDeclarations.put(inputDeclaration.getName(), classname); //Keep track for later
               if (superclasses.contains(classname)) {
                  //if this classname is already identified as a superclass.
                  String msg = String.format(
                        "You are using class '%s', a superclass, in your  input declaration. Try using a class that"
                              + " inherits from '%s' instead.",
                        classname, classname);
                  warning(msg, inputDeclaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
               }
            }
         }

         if (model.getOutput() != null) {
            for (OutputDeclaration outputDeclaration : model.getOutput().getDeclarations()) {
               String classname = outputDeclaration.getType().getName();
               dataFieldDeclarations.put(outputDeclaration.getName(), classname); //Keep track for later
               if (superclasses.contains(classname)) {
                  //if this classname is already identified as a superclass.
                  String msg = String.format(
                        "You are using class '%s', a superclass, in your  output declaration. Try using a class that"
                              + " inherits from '%s' instead.",
                        classname, classname);
                  warning(msg, outputDeclaration, SystemDescriptorPackage.Literals.FIELD_DECLARATION__NAME);
               }
            }
         }

         checkModelLinks(model, superclasses, dataFieldDeclarations);
         checkModelScenarios(model, superclasses, dataFieldDeclarations);
      }
   }

   private static int getNumberOfInputFieldsNamed(
         Model model,
         String fieldName) {
      Input input = model.getInput();
      return input == null
             ? 0
             : (int) input.getDeclarations()
                   .stream()
                   .filter(d -> d.getName().equals(fieldName))
                   .count();
   }

   private static int getNumberOfOutputFieldsNamed(
         Model model,
         String fieldName) {
      Output output = model.getOutput();
      return output == null
             ? 0
             : (int) output.getDeclarations()
                   .stream()
                   .filter(d -> d.getName().equals(fieldName))
                   .count();
   }

   private static int getNumberOfPartsNamed(
         Model model,
         String partName) {
      Parts parts = model.getParts();
      return parts == null
             ? 0
             : (int) parts.getDeclarations()
                   .stream()
                   .filter(d -> d.getName().equals(partName))
                   .count();
   }

   private static int getNumberOfRequirementsNamed(
         Model model,
         String requirementName) {
      Requires requires = model.getRequires();
      return requires == null
             ? 0
             : (int) requires.getDeclarations()
                   .stream()
                   .filter(d -> d.getName().equals(requirementName))
                   .count();
   }

   private static int getNumberOfScenariosNamed(
         Model model,
         String scenarioName) {
      return (int) model.getScenarios()
            .stream()
            .filter(d -> d.getName().equals(scenarioName))
            .count();
   }
}
