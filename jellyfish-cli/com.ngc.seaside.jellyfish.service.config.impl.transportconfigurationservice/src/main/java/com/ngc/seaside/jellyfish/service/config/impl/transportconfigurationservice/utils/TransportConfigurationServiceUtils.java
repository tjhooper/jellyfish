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
package com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice.utils;

import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IParameter;
import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.link.IModelLink;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperties;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperty;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IPropertyDataValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TransportConfigurationServiceUtils {

   /**
    * Returns the collection of configurations from the provided properties for a given type.
    * 
    * @param propertiesSupplier supplier for properties
    * @param qualifiedName qualified name of property type
    * @param function function to convert property to the desired value configuration type
    * @param notSetErrorMessage error message if property is not set
    * @return collection of configurations
    */
   public static <T> Collection<T> getConfigurations(Supplier<IProperties> propertiesSupplier, String qualifiedName, 
            Function<IPropertyDataValue, T> function, 
            Supplier<String> notSetErrorMessage) {
      Collection<IPropertyDataValue> propertyValues = propertiesSupplier.get()
            .stream()
            .filter(property -> DataTypes.DATA == property.getType())
            .filter(property -> qualifiedName.equals(
                  property.getReferencedDataType().getFullyQualifiedName()))
            .filter(
                  property -> FieldCardinality.SINGLE == property.getCardinality())
            .map(IProperty::getData)
            .collect(Collectors.toList());
      Collection<T> configurations = new ArrayList<>(propertyValues.size());
      for (IPropertyDataValue value : propertyValues) {
         if (!value.isSet()) {
            throw new IllegalStateException(notSetErrorMessage.get());
         }
         configurations.add(function.apply(value));
      }
      return configurations;
   }

   /**
    * Returns the data field for the given property with the given field name.
    * 
    * @param value property data value
    * @param fieldName field name
    * @return data field
    */
   public static IDataField getField(IPropertyDataValue value, String fieldName) {
      return value.getFieldByName(fieldName)
            .orElseThrow(() -> new IllegalStateException("Missing " + fieldName + " field"));
   }

   /**
    * Returns all of the given model's links that contain the given field as either a target or source.
    */
   public static Collection<IModelLink<?>> findLinks(IModel model, IDataReferenceField field) {
      return model.getLinks()
            .stream()
            .filter(link -> Objects.equals(field, link.getSource()) || Objects.equals(field, link.getTarget()))
            .collect(Collectors.toList());
   }

   /**
    * Gets the deployment model from the options.
    * 
    * @param options jellyfish command options
    * @return deployment model
    */
   public static IModel getDeploymentModel(IJellyFishCommandOptions options) {
      IParameter<?> deploymentModelParameter = options.getParameters()
            .getParameter(CommonParameters.DEPLOYMENT_MODEL.getName());
      if (deploymentModelParameter == null) {
         throw new IllegalStateException(CommonParameters.DEPLOYMENT_MODEL.getName() + " parameter is not set");
      }
      String deploymentModel = deploymentModelParameter.getStringValue();
      return options.getSystemDescriptor()
            .findModel(deploymentModel)
            .orElseThrow(() -> new IllegalStateException("Cannot find deployment model " + deploymentModel));
   }

   /**
    * Gets the deployment model from the options, or {@link Optional#empty()} if there is none.
    * 
    * @param options jellyfish command options
    * @return deployment model
    */
   public static Optional<IModel> getOptionalDeploymentModel(IJellyFishCommandOptions options) {
      IParameter<?> deploymentModelParameter = options.getParameters()
            .getParameter(CommonParameters.DEPLOYMENT_MODEL.getName());
      if (deploymentModelParameter == null || deploymentModelParameter.getValue() == null) {
         return Optional.empty();
      }
      String deploymentModel = deploymentModelParameter.getStringValue();
      if (deploymentModel == null) {
         return Optional.empty();
      }
      return options.getSystemDescriptor().findModel(deploymentModel);
   }
}
