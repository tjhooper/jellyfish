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
package com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.admin;

import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.ConfigurationContext;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.ConfigurationType;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.transporttopic.DefaultTransportTopicConfigurationDto;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.transporttopic.ITransportTopicConfigurationDto;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.transporttopic.ITransportTopicConfigurationPlugin;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.transporttopic.rest.RestConfigurationDto;
import com.ngc.seaside.jellyfish.service.config.api.IAdministrationConfigurationService;
import com.ngc.seaside.jellyfish.service.config.api.dto.admin.RestAdministrationConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Adds configuration topics so that a service can receive and respond to administration requests.
 */
public class RestAdminTopicPlugin implements ITransportTopicConfigurationPlugin<RestConfigurationDto> {

   static final String ADMIN_TOPIC_TYPE = "com.ngc.seaside.service.admin.api.IAdministrationService";
   static final String SHUTDOWN_TOPIC_VALUE = "ADMINISTRATION_SHUTDOWN_REQUEST_TOPIC";
   static final String RESTART_TOPIC_VALUE = "ADMINISTRATION_RESTART_REQUEST_TOPIC";

   private IAdministrationConfigurationService service;

   @Inject
   public RestAdminTopicPlugin(IAdministrationConfigurationService service) {
      this.service = service;
   }

   @Override
   public Set<ITransportTopicConfigurationDto<RestConfigurationDto>>
            getTopicConfigurations(ConfigurationContext context) {
      if (context.isSystemModel()) {
         return Collections.emptySet();
      }

      ConfigurationType configurationType = context.getConfigurationType();
      if (configurationType != ConfigurationType.SERVICE && configurationType != ConfigurationType.TEST) {
         return Collections.emptySet();
      }
      boolean internal = configurationType == ConfigurationType.SERVICE;

      return service.getConfigurations(context.getOptions(), context.getModel())
               .stream()
               .filter(RestAdministrationConfiguration.class::isInstance)
               .map(RestAdministrationConfiguration.class::cast)
               .map(config -> getTopicConfiguration(context, config, internal))
               .flatMap(List::stream)
               .collect(Collectors.toCollection(LinkedHashSet::new));
   }

   private List<ITransportTopicConfigurationDto<RestConfigurationDto>> getTopicConfiguration(
            ConfigurationContext context, RestAdministrationConfiguration config, boolean internal) {
      RestConfigurationDto shutdownDto = new RestConfigurationDto(config.getShutdown(), !internal, internal);
      DefaultTransportTopicConfigurationDto<RestConfigurationDto> shutdownConfigDto =
               new DefaultTransportTopicConfigurationDto<>(shutdownDto);
      shutdownConfigDto.addTransportTopic(ADMIN_TOPIC_TYPE, SHUTDOWN_TOPIC_VALUE);

      RestConfigurationDto restartDto = new RestConfigurationDto(config.getRestart(), !internal, internal);
      DefaultTransportTopicConfigurationDto<RestConfigurationDto> restartConfigDto =
               new DefaultTransportTopicConfigurationDto<>(restartDto);
      restartConfigDto.addTransportTopic(ADMIN_TOPIC_TYPE, RESTART_TOPIC_VALUE);
      return Arrays.asList(shutdownConfigDto, restartConfigDto);
   }

   @Override
   public Set<String> getDependencies(ConfigurationContext context, DependencyType dependencyType) {
      return getAdminDependencies(context, dependencyType);
   }

   static Set<String> getAdminDependencies(ConfigurationContext context, DependencyType dependencyType) {
      switch (dependencyType) {
         case COMPILE:
            return Collections.singleton("com.ngc.seaside:service.admin.api");
         case BUNDLE:
            return Collections.singleton("com.ngc.seaside:service.admin.impl.defaultadminservice");
         case MODULE:
            return Collections.emptySet();
         default:
            throw new AssertionError();
      }
   }

}
