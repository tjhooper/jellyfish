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
package com.ngc.seaside.systemdescriptor.ui.dynamic;

import com.google.inject.Module;

import com.ngc.seaside.systemdescriptor.ui.internal.SystemdescriptorActivator;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.ServiceLoader;

/**
 * A simple component that can load all instances of Guice {@code Module}s
 * without Declarative Services. This implementation will scan all bundles for a
 * files in META-INF/services. If there is a file in this directory that is
 * named {@code com.google.inject.Module}, the file will be treated as a text
 * file. Every line in the file will be assumed to be a fully qualified class
 * name of a {@code Module} implementation. It will be created via its default
 * instructor and included in the Guice configuration. Note for this to work,
 * the package that contains the implementation must be exported. This follows
 * the standard {@link ServiceLoader} pattern.
 */
public class DynamicModuleLoader {

   private static final Logger LOGGER = Logger.getLogger(DynamicModuleLoader.class);

   /**
    * Creates a new {@code DynamicModuleLoader}.
    */
   public DynamicModuleLoader() {
   }

   /**
    * Gets all Guice modules that have been installed thus far.
    *
    * @return all Guice modules that have been installed thus far
    */
   public Collection<Module> loadModules() {
      Collection<Module> modules = new ArrayList<>();

      // Scan all bundles for service loader files.
      for (Bundle b : SystemdescriptorActivator.getInstance().getBundle().getBundleContext().getBundles()) {
         // Get the files.
         Enumeration<URL> urls = b.findEntries("META-INF/services", "*", false);
         while (urls != null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            // If the file is for a Guice module, load the module.
            if (isGuiceModule(url)) {
               modules.addAll(loadModulesFrom(url, b));
            }
         }
      }

      LOGGER.debug("Loaded " + modules.size() + " from service loader files.");
      return modules;
   }

   private static boolean isGuiceModule(URL url) {
      String name = url.getPath();
      int index = name.lastIndexOf("/");
      return Module.class.getName().equals(name.substring(index + 1));
   }

   private static Collection<Module> loadModulesFrom(URL url, Bundle bundle) {
      Collection<Module> modules = new ArrayList<>();

      // Read in all the lines and treat each line as a fully qualified class name.
      try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
         // The bundle must be active for dynamic imports to resolve correctly.
         // If the bundle is already active, this method will do nothing.  Note this method
         // may not return until the bundle is actually activated (see the OSGi API JavaDoc).
         bundle.start();

         String line = in.readLine();
         while (line != null) {
            // Skip comments.
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
               try {
                  // Create a new instance of the module and add it to the list.
                  modules.add((Module) DynamicModuleLoader.class.getClassLoader().loadClass(line).getConstructor()
                           .newInstance());
                  LOGGER.debug(String.format(
                        "Including module %s from bundle %s in Guice configuration.",
                        line,
                        bundle.getSymbolicName()));
               } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                        | InvocationTargetException | IllegalArgumentException | NoSuchMethodException
                        | SecurityException e) {
                  LOGGER.error(String.format(
                        "Failed to include module %s from bundle %s in Guice configuration due to exception.",
                        line,
                        bundle.getSymbolicName()),
                               e);
               }
            }
            line = in.readLine();
         }
      } catch (IOException | BundleException e) {
         LOGGER.error(String.format(
               "Failed to load modules from bundle %s due to exception where reading service loader file.",
               bundle.getSymbolicName()),
                      e);
      }

      return modules;
   }
}
