package com.ngc.seaside.jellyfish.cli.command.validate;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.CommandException;
import com.ngc.seaside.jellyfish.api.DefaultUsage;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IUsage;
import com.ngc.seaside.jellyfish.utilities.parsing.ParsingResultLogging;
import com.ngc.seaside.systemdescriptor.service.api.IParsingIssue;
import com.ngc.seaside.systemdescriptor.service.api.IParsingResult;
import com.ngc.seaside.systemdescriptor.validation.api.Severity;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * This class provides the implementation of the validate command.
 */
@Component(service = IJellyFishCommand.class)
public class ValidateCommand implements IJellyFishCommand {

   private static final String NAME = "validate";
   private static final IUsage
         COMMAND_USAGE =
         new DefaultUsage("Validates the System Descriptor. "
                          + "Requires a system descriptor project within src/main/sd");

   private ILogService logService;

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

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public IUsage getUsage() {
      return COMMAND_USAGE;
   }

   @Override
   public boolean requiresValidSystemDescriptorProject() {
      return false;
   }

   @Override
   public void run(IJellyFishCommandOptions commandOptions) {
      IParsingResult result = commandOptions.getParsingResult();
      if (commandOptions.getParsingResult().isSuccessful()) {
         ParsingResultLogging.logWarnings(result).forEach(l -> logService.warn(getClass(), "%s", l));
         logService.info(ValidateCommand.class, "System Descriptor project is valid.");
      } else {
         // The formatting of the logging line with "%s" avoids issues if the line in the model file contains
         // "%s" symbol.
         ParsingResultLogging.logErrors(result).forEach(l -> logService.error(getClass(), "%s", l));
         throw new CommandException("System Descriptor failed validation!");
      }
   }

   @Override
   public String toString() {
      return getName();
   }
}
