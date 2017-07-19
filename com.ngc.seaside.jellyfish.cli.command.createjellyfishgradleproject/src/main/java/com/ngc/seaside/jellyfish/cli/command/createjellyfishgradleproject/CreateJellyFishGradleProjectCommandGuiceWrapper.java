package com.ngc.seaside.jellyfish.cli.command.createjellyfishgradleproject;

import com.google.inject.Inject;
import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.bootstrap.service.promptuser.api.IPromptUserService;
import com.ngc.seaside.bootstrap.service.template.api.ITemplateService;
import com.ngc.seaside.command.api.IUsage;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;

public class CreateJellyFishGradleProjectCommandGuiceWrapper implements IJellyFishCommand {

   private final CreateJellyFishGradleProjectCommand delegate = new CreateJellyFishGradleProjectCommand();

   @Inject
   public CreateJellyFishGradleProjectCommandGuiceWrapper(ILogService logService, IPromptUserService promptService,
            ITemplateService templateService) {
      delegate.setLogService(logService);
      delegate.setPromptService(promptService);
      delegate.setTemplateService(templateService);
   }

   @Override
   public String getName() {
      return delegate.getName();
   }

   @Override
   public IUsage getUsage() {
      return delegate.getUsage();
   }

   @Override
   public void run(IJellyFishCommandOptions commandOptions) {
      delegate.run(commandOptions);
   }

}
