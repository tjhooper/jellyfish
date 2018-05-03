package com.ngc.seaside.jellyfish.sonarqube;

import com.ngc.seaside.jellyfish.sonarqube.languages.SystemDescriptorLanguage;
import com.ngc.seaside.jellyfish.sonarqube.rules.SystemDescriptorRulesDefinition;
import com.ngc.seaside.jellyfish.sonarqube.rules.SystemDescriptorSensor;

import org.sonar.api.Plugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * The main entry point for the Sonarqube Jellyfish plugin.
 */
public class JellyfishPlugin implements Plugin {

   private static final Logger LOGGER = Loggers.get(JellyfishPlugin.class);

   @Override
   public void define(Context c) {
      c.addExtension(SystemDescriptorLanguage.class);
      c.addExtension(SystemDescriptorRulesDefinition.class);
      c.addExtension(SystemDescriptorSensor.class);

      LOGGER.info("Jellyfish plugin successfully installed.");
   }
}
