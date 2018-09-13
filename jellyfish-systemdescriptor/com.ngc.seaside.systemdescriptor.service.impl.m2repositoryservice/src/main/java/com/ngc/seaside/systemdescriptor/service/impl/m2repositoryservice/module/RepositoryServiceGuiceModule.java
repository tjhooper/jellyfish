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
package com.ngc.seaside.systemdescriptor.service.impl.m2repositoryservice.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.systemdescriptor.service.impl.m2repositoryservice.GradlePropertiesService;
import com.ngc.seaside.systemdescriptor.service.impl.m2repositoryservice.RepositoryServiceGuiceWrapper;
import com.ngc.seaside.systemdescriptor.service.repository.api.IRepositoryService;

/**
 * Configure the service for use in Guice
 */
public class RepositoryServiceGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(IRepositoryService.class).to(RepositoryServiceGuiceWrapper.class);
   }

   /**
    * Guice wrapper for {@link GradlePropertiesService}.
    */
   @Provides
   public GradlePropertiesService getGradlePropertiesService(ILogService ref) {
      GradlePropertiesService service = new GradlePropertiesService();
      service.setLogService(ref);
      service.activate();
      return service;
   }
}