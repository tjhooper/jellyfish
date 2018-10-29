/**
 * UNCLASSIFIED
 * Northrop Grumman Proprietary
 * ____________________________
 *
 * Copyright (C) 2018, Northrop Grumman Systems Corporation
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of
 * Northrop Grumman Systems Corporation. The intellectual and technical concepts
 * contained herein are proprietary to Northrop Grumman Systems Corporation and
 * may be covered by U.S. and Foreign Patents or patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Northrop Grumman.
 */
package com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice;

import com.ngc.seaside.jellyfish.service.buildmgmt.api.DependencyScope;
import com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration;

import static com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration.artifact;
import static com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration.currentJellyfishVersion;

/**
 * This is the default configuration for a dependencies that are referenced in projects generated by Jellyfish.
 */
public class DefaultDependenciesConfiguration {

   private DefaultDependenciesConfiguration() {
   }

   /**
    *
    * @return the DependenciesConfiguration
    */
   public static DependenciesConfiguration getConfig() {
      DependenciesConfiguration config = new DependenciesConfiguration();

      configureSeasideGradlePlugins(config);
      configureStarfish(config);
      configureJellyfish(config);
      configureBlocs(config);
      configureSonarQube(config);
      configureProtoBuffers(config);
      configureOsgi(config);
      configureGoogle(config);
      configureSpark(config);
      configureCucumber(config);
      configureUnitTesting(config);

      return config;
   }

   private static void configureSeasideGradlePlugins(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("seasidePluginsVersion")
            .version("2.22.4")
            .includes(artifact("gradle.plugins")
                            .groupId("com.ngc.seaside")
                            .scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureStarfish(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("starfishVersion")
            .version("2.14.4")
            .defaultGroupId("com.ngc.seaside")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("service.api"),
                      artifact("service.transport.api"),
                      artifact("service.correlation.impl.correlationservice"),
                      artifact("service.fault.impl.faultloggingservice"),
                      artifact("service.fault.impl.faultloggingservice.module"),
                      artifact("service.monitoring.impl.loggingmonitoringservice"),
                      artifact("service.request.impl.microservicerequestservice"),
                      artifact("service.transport.impl.defaulttransportservice"),
                      artifact("service.transport.impl.defaulttransportservice.module"),
                      artifact("service.transport.impl.testutils"),
                      artifact("service.transport.impl.topic.multicast"),
                      artifact("service.transport.impl.provider.multicast"),
                      artifact("service.transport.impl.provider.multicast.module"),
                      artifact("service.transport.impl.topic.spark"),
                      artifact("service.transport.impl.provider.spark"),
                      artifact("service.transport.impl.provider.spark.module"),
                      artifact("service.log.impl.common.sl4jlogservicebridge"),
                      artifact("service.transport.impl.topic.httpclient"),
                      artifact("service.transport.impl.provider.httpclient"),
                      artifact("service.transport.impl.provider.httpclient.module"),
                      artifact("service.transport.impl.topic.zeromq"),
                      artifact("service.transport.impl.provider.zeromq"),
                      artifact("service.transport.impl.provider.zeromq.module"),
                      artifact("service.readiness.impl.defaultreadinessservice"),
                      artifact("service.telemetry.api"),
                      artifact("service.telemetry.impl.basetelemetryservice"),
                      artifact("service.telemetry.impl.jsontelemetryservice"),
                      artifact("service.telemetry.impl.jsontelemetryservice.module"),
                      artifact("service.admin.api"),
                      artifact("service.admin.impl.defaultadminservice"),
                      artifact("cucumber.runner"));
   }

   private static void configureJellyfish(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("jellyfishVersion")
            .version(currentJellyfishVersion())
            .defaultGroupId("com.ngc.seaside")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("guice.modules"),
                      artifact("jellyfish.cli.gradle.plugins").scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureBlocs(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("blocsPluginVersion")
            .version("0.5")
            .includes(artifact("gradle.plugin")
                            .groupId("com.ngc.blocs")
                            .scope(DependencyScope.BUILDSCRIPT));

      config.addGroup()
            .versionPropertyName("blocsCoreVersion")
            .version("3.0.0")
            .defaultGroupId("com.ngc.blocs")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("api"),
                      artifact("service.api"),
                      artifact("file.impl.common.fileutilities"),
                      artifact("jaxb.impl.common.jaxbutilities"),
                      artifact("component.impl.common.componentutilities"),
                      artifact("notification.impl.common.notificationsupport"),
                      artifact("properties.resource.impl.common.propertiesresource"),
                      artifact("xml.resource.impl.common.xmlresource"),
                      artifact("security.impl.common.securityutilities"),
                      artifact("service.thread.impl.common.threadservice"),
                      artifact("service.log.impl.common.logservice"),
                      artifact("service.event.impl.common.eventservice"),
                      artifact("service.resource.impl.common.resourceservice"),
                      artifact("service.framework.impl.common.frameworkmgmtservice"),
                      artifact("service.notification.impl.common.notificationservice"),
                      artifact("service.deployment.impl.common.autodeploymentservice"),
                      artifact("test.impl.common.testutilities"));
   }

   private static void configureSonarQube(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("sonarqubePluginVersion")
            .version("2.5")
            .includes(artifact("sonarqube-gradle-plugin")
                            .groupId("org.sonarsource.scanner.gradle")
                            .scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureProtoBuffers(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("protobufPluginVersion")
            .version("0.8.5")
            .includes(artifact("protobuf-gradle-plugin")
                            .groupId("com.google.protobuf")
                            .scope(DependencyScope.BUILDSCRIPT));

      config.addGroup()
            .versionPropertyName("protobufVersion")
            .version("3.2.0")
            .defaultGroupId("com.google.protobuf")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("protobuf-java"),
                      artifact("protoc"));
   }

   private static void configureOsgi(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("osgiVersion")
            .version("6.0.0")
            .defaultGroupId("org.osgi")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("osgi.core"),
                      artifact("osgi.enterprise"));
   }

   private static void configureGoogle(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("guavaVersion")
            .version("26.0-jre")
            .includes(artifact("guava")
                            .groupId("com.google.guava")
                            .scope(DependencyScope.BUILD));

      config.addGroup()
            .versionPropertyName("guiceVersion")
            .version("4.1.0")
            .includes(artifact("guice")
                            .groupId("com.google.inject")
                            .scope(DependencyScope.BUILD));

      config.addGroup()
            .versionPropertyName("gsonVersion")
            .version("2.8.2")
            .includes(artifact("gson")
                            .groupId("com.google.code.gson")
                            .scope(DependencyScope.BUILD));

      config.addGroup()
            .versionPropertyName("protobufVersion")
            .version("3.2.0")
            .includes(artifact("protobuf-java")
                            .groupId("com.google.protobuf")
                            .scope(DependencyScope.BUILD));
   }

   private static void configureSpark(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("sparkVersion")
            .version("2.6.0")
            .includes(artifact("spark-core")
                            .groupId("com.sparkjava")
                            .scope(DependencyScope.BUILD));
   }

   private static void configureCucumber(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("cucumberVersion")
            .version("3.0.2")
            .defaultGroupId("io.cucumber")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("cucumber-java"),
                      artifact("cucumber-guice"));
   }

   private static void configureUnitTesting(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("junitVersion")
            .version("4.12")
            .includes(artifact("junit")
                            .groupId("junit")
                            .scope(DependencyScope.TEST));

      config.addGroup()
            .versionPropertyName("mockitoVersion")
            .version("2.19.0")
            .includes(artifact("mockito-core")
                            .groupId("org.mockito")
                            .scope(DependencyScope.TEST));
   }
}
