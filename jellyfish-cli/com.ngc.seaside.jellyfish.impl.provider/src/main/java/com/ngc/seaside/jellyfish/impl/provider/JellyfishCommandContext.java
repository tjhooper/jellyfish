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
package com.ngc.seaside.jellyfish.impl.provider;

import com.google.common.base.Preconditions;

import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.DefaultParameterCollection;
import com.ngc.seaside.jellyfish.api.IParameter;
import com.ngc.seaside.jellyfish.api.IParameterCollection;

/**
 * A simple structure for storing information about how Jellyfish was invoked to perform a command.
 */
public class JellyfishCommandContext {

   /**
    * The name of the command being executed.
    */
   private final String command;

   /**
    * The unmodified set of parameters that Jellyfish was originally invoked with.
    */
   private final IParameterCollection originalParameters;

   /**
    * The parameters to run Jellyfish with which include the original parameters plus any parameters added later that
    * were automatically calculated.
    */
   private final DefaultParameterCollection parameters;

   /**
    * Creates a new context.
    *
    * @param command    the command that is being executed
    * @param parameters the original parameters as given by the user
    */
   public JellyfishCommandContext(String command, IParameterCollection parameters) {
      this.command = Preconditions.checkNotNull(command, "command may not be null!");
      this.originalParameters = Preconditions.checkNotNull(parameters, "parameters may not be null!");
      this.parameters = new DefaultParameterCollection(this.originalParameters);
   }

   /**
    * Returns true if the {@link CommonParameters#GROUP_ARTIFACT_VERSION} parameter was specified with the original
    * parameters, false otherwise.
    *
    * @return true if the {@link CommonParameters#GROUP_ARTIFACT_VERSION} parameter was specified
    */
   public boolean isGavSpecified() {
      return originalParameters.containsParameter(CommonParameters.GROUP_ARTIFACT_VERSION.getName());
   }

   /**
    * Gets the command that is being executed
    *
    * @return the command being executed
    */
   public String getCommand() {
      return command;
   }

   /**
    * Gets the original, unmodified set of parameters that Jellyfish was initially invoked with.
    *
    * @return the original, unmodified set of parameters that Jellyfish was initially invoked with
    */
   public IParameterCollection getOriginalParameters() {
      return originalParameters;
   }

   /**
    * Gets the parameters to run Jellyfish with.  These parameters include the {@link #getOriginalParameters() original
    * parameters} plus any parameters {@link #addParameter(IParameter) added} to the context after it was created.
    *
    * @return the parameters to run Jellyfish with
    */
   public IParameterCollection getParameters() {
      return parameters;
   }

   /**
    * Adds a parameter to this context.  Parameters added this way will not be included in the {@link
    * #getOriginalParameters() original parameters}.
    *
    * @param parameter the parameter to add
    */
   public void addParameter(IParameter<?> parameter) {
      parameters.addParameter(Preconditions.checkNotNull(parameter, "parameter may not be null!"));
   }
}
