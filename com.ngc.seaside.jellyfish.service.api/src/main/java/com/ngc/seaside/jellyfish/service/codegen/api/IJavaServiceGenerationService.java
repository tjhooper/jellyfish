package com.ngc.seaside.jellyfish.service.codegen.api;

import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.ClassDto;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;

/**
 * Assists with the generation of Java service code.
 */
public interface IJavaServiceGenerationService {

   /**
    * Gets a description of a Java service interface for the given model.
    *
    * @param options options the options the current command is being executed with
    * @param model   the model to get the interface description for
    * @return a description of a Java service interface
    */
   ClassDto getServiceInterfaceDescription(IJellyFishCommandOptions options, IModel model);

   /**
    * Gets a description of the Java service base class for the given model.
    *
    * @param options options the options the current command is being executed with
    * @param model   the model to get the interface description for
    * @return a description of the Java service base class
    */
   ClassDto getBaseServiceDescription(IJellyFishCommandOptions options, IModel model);
}
