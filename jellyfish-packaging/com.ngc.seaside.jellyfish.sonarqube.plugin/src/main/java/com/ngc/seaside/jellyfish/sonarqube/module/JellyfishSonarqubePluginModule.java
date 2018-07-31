package com.ngc.seaside.jellyfish.sonarqube.module;

import com.google.common.base.Preconditions;
import com.google.inject.Module;

import com.ngc.blocs.guice.module.LogServiceModule;
import com.ngc.seaside.jellyfish.DefaultJellyfishModule;
import com.ngc.seaside.systemdescriptor.service.impl.xtext.module.XTextSystemDescriptorServiceModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * A Guava module that register all components need to use Jellyfish and parse System Descriptor files.  Use either
 * {@link #withNormalLogging()} to create a module that uses normal logging or {@link #withSuppressedLogging()} to
 * create a module where all Jellyfish logging is suppressed.
 */
public class JellyfishSonarqubePluginModule extends DefaultJellyfishModule {

   /**
    * The configured logging module.
    */
   private final Module loggingModule;

   /**
    * Creates the module with a configured logging module.  Use the static methods to create an instance of this class.
    *
    * @param loggingModule the logging module
    */
   private JellyfishSonarqubePluginModule(Module loggingModule) {
      this.loggingModule = loggingModule;
   }

   /**
    * Creates a new {@code JellyfishSonarqubePluginModule} that is configured with normal logging.  All logging output
    * generated by Jellyfish will be directed to the Sonarqube logs when using this option.  This is usually the
    * preferred option when running the sensor.
    *
    * @return a new {@code JellyfishSonarqubePluginModule}
    */
   public static JellyfishSonarqubePluginModule withNormalLogging() {
      return new JellyfishSonarqubePluginModule(new SonarqubeLogServiceModule());
   }

   /**
    * Creates a new {@code JellyfishSonarqubePluginModule} that is configured with suppressed logging.  All logging
    * output generated by Jellyfish will be ignored and will not be present in the Sonarqube logs.  Use this option
    * when running Jellyfish just to get an instance of the injector, such as when discovering all the configured
    * {@code ISystemDescriptorFindingType}s.
    *
    * @return a new {@code JellyfishSonarqubePluginModule}
    */
   public static JellyfishSonarqubePluginModule withSuppressedLogging() {
      return new JellyfishSonarqubePluginModule(new NoOpLogServiceModule());
   }

   @Override
   protected Collection<Module> filterAllModules(Collection<Module> modules) {
      modules.removeIf(m -> m.getClass() == LogServiceModule.class);
      modules.add(loggingModule);
      return modules;
   }

   @Override
   protected Collection<Module> configureModulesFromClasspath(Collection<Module> modules) {
      // We can't use the default behavior in DefaultJellyfishModule because of how Sonarqube sets up the classpath
      // and classloader for plugins.  Thus we do this extra logic to figure out which Guice modules to include.
      InputStream is = JellyfishSonarqubePluginModule.class.getClassLoader().getResourceAsStream("guice-modules");
      Preconditions.checkState(is != null,
                               "failed to load file guice-modules from classpath!");
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
         String line = br.readLine();
         while (line != null) {
            Module m = (Module) JellyfishSonarqubePluginModule.class.getClassLoader().loadClass(line).newInstance();
            if (m.getClass() != XTextSystemDescriptorServiceModule.class) {
               modules.add(m);
            }
            line = br.readLine();
         }
      } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
         throw new RuntimeException("failed to create instance of a required Guice module!", e);
      }

      return modules;
   }
}
