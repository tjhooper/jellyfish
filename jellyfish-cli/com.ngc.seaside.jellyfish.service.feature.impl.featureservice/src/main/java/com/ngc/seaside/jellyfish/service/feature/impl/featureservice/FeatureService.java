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
package com.ngc.seaside.jellyfish.service.feature.impl.featureservice;

import com.google.common.base.Preconditions;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.feature.api.IFeatureInformation;
import com.ngc.seaside.jellyfish.service.feature.api.IFeatureService;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;

import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = IFeatureService.class)
public class FeatureService implements IFeatureService {

   private static final Collector<IFeatureInformation, ?, ? extends Collection<IFeatureInformation>> COLLECTOR =
            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(IFeatureInformation::getPath)));

   // TODO TH: use the Gherkin service to actually parse feature files.

   @Override
   public Collection<IFeatureInformation> getFeatures(IJellyFishCommandOptions options, IModel model) {
      Preconditions.checkNotNull(options, "options may not be null!");
      Preconditions.checkNotNull(model, "model may not be null!");
      return getFeaturesStream(options)
               .filter(feature -> model.equals(feature.getModel().orElse(null)))
               .collect(COLLECTOR);
   }

   @Override
   public Collection<IFeatureInformation> getFeatures(IJellyFishCommandOptions options, IScenario scenario) {
      Preconditions.checkNotNull(options, "options may not be null!");
      Preconditions.checkNotNull(scenario, "scenario may not be null!");
      return getFeaturesStream(options)
               .filter(feature -> scenario.equals(feature.getScenario().orElse(null)))
               .collect(COLLECTOR);
   }

   @Override
   public Collection<IFeatureInformation> getAllFeatures(IJellyFishCommandOptions options) {
      Preconditions.checkNotNull(options, "options may not be null!");
      return getFeaturesStream(options)
               .collect(COLLECTOR);
   }

   private static Stream<IFeatureInformation> getFeaturesStream(IJellyFishCommandOptions options) {
      return getFeaturesFilesStream(getFeaturesBaseDir(options))
               .map(file -> new FeatureInformation(file, getModel(options, file), getScenario(options, file)));
   }

   /**
    * Returns the model associated with the given feature, or {@code null} if there is none.
    * 
    * @param options options
    * @param featureFile feature file path
    * @return the model
    */
   private static IModel getModel(IJellyFishCommandOptions options, Path featureFile) {
      Path relativePath = getFeaturesBaseDir(options).relativize(featureFile);
      if (relativePath.getNameCount() <= 1) {
         return null;
      }
      String pkg = "";
      for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
         pkg += relativePath.getName(i).toString() + ".";
      }
      String[] parts = relativePath.getFileName().toString().split("\\.");
      if (parts.length != 3) {
         return null;
      }
      String model = pkg + parts[0];
      return options.getSystemDescriptor().findModel(model).orElse(null);
   }

   /**
    * Returns the scenario associated with the given feature, or {@code null} if there is none.
    * 
    * @param options options
    * @param featureFile feature file path
    * @return the scenario
    */
   private static IScenario getScenario(IJellyFishCommandOptions options, Path featureFile) {
      IModel model = getModel(options, featureFile);
      if (model == null) {
         return null;
      }
      Path relativePath = getFeaturesBaseDir(options).relativize(featureFile);
      String[] parts = relativePath.getFileName().toString().split("\\.");
      if (parts.length != 3) {
         return null;
      }
      return model.getScenarios().getByName(parts[1]).orElse(null);
   }

   private static Path getFeaturesBaseDir(IJellyFishCommandOptions options) {
      Path p = options.getParsingResult().getTestSourcesRoot();
      if (p == null) {
         throw new IllegalStateException("Unable to get test features for the system descriptor project");
      }
      return p;
   }

   private static Stream<Path> getFeaturesFilesStream(Path baseDir) {
      PathMatcher matcher = baseDir.getFileSystem().getPathMatcher("glob:**.feature");
      try {
         return Files.walk(baseDir)
                  .filter(Files::isRegularFile)
                  .filter(matcher::matches);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

}
