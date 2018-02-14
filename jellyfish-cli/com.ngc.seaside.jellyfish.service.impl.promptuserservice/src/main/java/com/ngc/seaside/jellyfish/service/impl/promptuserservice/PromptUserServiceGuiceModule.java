package com.ngc.seaside.jellyfish.service.impl.promptuserservice;

import com.google.inject.AbstractModule;

import com.ngc.seaside.jellyfish.service.promptuser.api.IPromptUserService;

/**
 * Configure the service for use in Guice
 */
public class PromptUserServiceGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(IPromptUserService.class).to(PromptUserServiceGuiceWrapper.class);
   }


}