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
package com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice;

import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.config.api.dto.HttpMethod;
import com.ngc.seaside.jellyfish.service.config.api.dto.RestConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.telemetry.RestTelemetryConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.telemetry.TelemetryConfiguration;
import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.INamedChildCollection;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperties;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperty;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IPropertyDataValue;
import com.ngc.seaside.systemdescriptor.service.api.ISystemDescriptorService;
import com.ngc.seaside.systemdescriptor.test.systemdescriptor.ModelUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice.ConfigurationTestUtils.getMockedReference;
import static com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice.ConfigurationTestUtils.getMockedRestConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TelemetryConfigurationServiceTest {

   private TelemetryConfigurationService service;

   @Mock
   private ISystemDescriptorService sdService;

   @Before
   public void setup() {
      service = new TelemetryConfigurationService();
      service.setSystemDescriptorService(sdService);
   }
   
   @Test
   public void testTelemetryConfiguration() {
      String deploymentModelName = "com.ngc.DeploymentModel";
      IModel deploymentModel = mock(IModel.class, RETURNS_DEEP_STUBS);
      String modelName = "com.ngc.Model";
      IModel model = mock(IModel.class, RETURNS_DEEP_STUBS);
      String address = "localhost";
      String interfaceName = "0.0.0.0";
      int port = 8081;
      String path = "/path2";
      String contentType = "application/x-protobuf";
      HttpMethod method = HttpMethod.POST;

      IProperty configProperty = getMockedRestTelemetryConfiguration(address, interfaceName, port, path, contentType,
               method);

      IJellyFishCommandOptions options = mock(IJellyFishCommandOptions.class, RETURNS_DEEP_STUBS);
      when(options.getParameters()
               .getParameter(CommonParameters.DEPLOYMENT_MODEL.getName())
               .getStringValue()).thenReturn(deploymentModelName);
      when(options.getSystemDescriptor().findModel(deploymentModelName)).thenReturn(Optional.of(deploymentModel));
      when(options.getSystemDescriptor().findModel(modelName)).thenReturn(Optional.of(model));
      when(sdService.getAggregatedView(deploymentModel)).thenReturn(deploymentModel);
      when(sdService.getAggregatedView(model)).thenReturn(model);
      IModelReferenceField part = getMockedReference("model", model, configProperty);
      INamedChildCollection<IModel, IModelReferenceField> parts = ModelUtils.mockedNamedCollectionOf(part);
      when(deploymentModel.getParts()).thenReturn(parts);
      when(model.getProperties()).thenReturn(IProperties.EMPTY_PROPERTIES);

      Collection<TelemetryConfiguration> configurations = service.getConfigurations(options, model);
      assertEquals(1, configurations.size());
      assertTrue(configurations.iterator().next() instanceof RestTelemetryConfiguration);
      RestTelemetryConfiguration config = (RestTelemetryConfiguration) configurations.iterator().next();
      RestConfiguration restConfig = config.getConfig();
      assertEquals(address, restConfig.getNetworkAddress().getAddress());
      assertEquals(interfaceName, restConfig.getNetworkInterface().getName());
      assertEquals(port, restConfig.getPort());
      assertEquals(path, restConfig.getPath());
      assertEquals(contentType, restConfig.getContentType());
      assertEquals(method, restConfig.getHttpMethod());

   }

   @Test
   public void testTelemetryConfigurationTypeOnlyAppliedForSpecificModel() {
      String deploymentModelName = "com.ngc.DeploymentModel";
      IModel deploymentModel = mock(IModel.class, RETURNS_DEEP_STUBS);
      String modelName = "com.ngc.Model";
      IModel model = mock(IModel.class, RETURNS_DEEP_STUBS);
      when(model.getFullyQualifiedName()).thenReturn(modelName);
      IModel noTelemetryModel = mock(IModel.class, RETURNS_DEEP_STUBS);
      String address = "localhost";
      String interfaceName = "0.0.0.0";
      int port = 8081;
      String path = "/path2";
      String contentType = "application/x-protobuf";
      HttpMethod method = HttpMethod.POST;

      IProperty configProperty = getMockedRestTelemetryConfiguration(address, interfaceName, port, path, contentType,
               method);

      IJellyFishCommandOptions options = mock(IJellyFishCommandOptions.class, RETURNS_DEEP_STUBS);
      when(options.getParameters()
               .getParameter(CommonParameters.DEPLOYMENT_MODEL.getName())
               .getStringValue()).thenReturn(deploymentModelName);
      when(options.getSystemDescriptor().findModel(deploymentModelName)).thenReturn(Optional.of(deploymentModel));
      when(options.getSystemDescriptor().findModel(modelName)).thenReturn(Optional.of(noTelemetryModel));
      when(sdService.getAggregatedView(deploymentModel)).thenReturn(deploymentModel);
      when(sdService.getAggregatedView(noTelemetryModel)).thenReturn(noTelemetryModel);
      IModelReferenceField part = getMockedReference("model", model, configProperty);
      IModelReferenceField noTelemetryPart = getMockedReference("noTelemetry", noTelemetryModel);
      INamedChildCollection<IModel, IModelReferenceField> parts = ModelUtils.mockedNamedCollectionOf(part,
               noTelemetryPart);
      when(deploymentModel.getParts()).thenReturn(parts);
      when(model.getProperties()).thenReturn(IProperties.EMPTY_PROPERTIES);
      when(noTelemetryModel.getProperties()).thenReturn(IProperties.EMPTY_PROPERTIES);

      Collection<TelemetryConfiguration> configs = service.getConfigurations(options, noTelemetryModel);
      assertEquals(Collections.emptySet(), configs);
   }

   private static IProperty getMockedRestTelemetryConfiguration(String address, String interfaceName, int port,
            String path, String contentType, HttpMethod method) {
      IProperty property = mock(IProperty.class, RETURNS_DEEP_STUBS);
      when(property.getName()).thenReturn(UUID.randomUUID().toString());
      when(property.getCardinality()).thenReturn(FieldCardinality.SINGLE);
      when(property.getType()).thenReturn(DataTypes.DATA);
      when(property.getReferencedDataType().getFullyQualifiedName()).thenReturn(
               TelemetryConfigurationService.REST_TELEMETRY_CONFIGURATION_QUALIFIED_NAME);
      IDataField configField = mock(IDataField.class);
      when(property.getData().isSet()).thenReturn(true);
      when(property.getData().getReferencedDataType().getFullyQualifiedName())
               .thenReturn(TelemetryConfigurationService.REST_TELEMETRY_CONFIGURATION_QUALIFIED_NAME);
      when(property.getData().getFieldByName(TelemetryConfigurationService.CONFIG_FIELD_NAME)).thenReturn(
               Optional.of(configField));
      IPropertyDataValue restConfig = getMockedRestConfiguration(address, interfaceName, port, path, contentType,
               method).getData();
      when(property.getData().getData(configField)).thenReturn(restConfig);
      return property;
   }
}
