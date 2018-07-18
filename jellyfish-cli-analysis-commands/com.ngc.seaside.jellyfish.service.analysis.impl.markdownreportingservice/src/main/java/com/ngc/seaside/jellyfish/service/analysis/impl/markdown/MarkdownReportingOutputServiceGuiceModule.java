package com.ngc.seaside.jellyfish.service.analysis.impl.markdown;

import com.google.inject.AbstractModule;

import com.ngc.seaside.jellyfish.service.analysis.api.IReportingOutputService;

/**
 * Register the markdown reporting output service.
 */
public class MarkdownReportingOutputServiceGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(IReportingOutputService.class)
            .to(MarkdownReportingOutputService.class)
            .asEagerSingleton();
   }

}