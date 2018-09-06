/**
 * UNCLASSIFIED
 * Northrop Grumman Proprietary
 * ____________________________
 *
 * Copyright (C) 2018, Northrop Grumman Systems Corporation
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of
 * Northrop Grumman Systems Corporation. The intellectual and technical concepts
 * contained herein are proprietary to Northrop Grumman Systems Corporation and
 * may be covered by U.S. and Foreign Patents or patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Northrop Grumman.
 */
package com.ngc.seaside.jellyfish.utilities.parsing;

import com.ngc.seaside.systemdescriptor.service.api.IParsingIssue;
import com.ngc.seaside.systemdescriptor.service.api.IParsingResult;
import com.ngc.seaside.systemdescriptor.service.source.api.ISourceLocation;
import com.ngc.seaside.systemdescriptor.validation.api.Severity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Aids in logging the results of parsing a System Descriptor project.
 */
public class ParsingResultLogging {

   private ParsingResultLogging() {
   }

   /**
    * Gets a collection of strings that should be printed one per line that describes the parsing errors in the given
    * result.
    */
   public static Collection<String> logErrors(IParsingResult result) {
      Collection<String> lines = new ArrayList<>();

      Collection<IParsingIssue> errors = result.getIssues()
            .stream()
            .filter(i -> i.getSeverity() == Severity.ERROR)
            .collect(Collectors.toList());
      if (errors.size() > 0) {
         lines.add(String.format("System Descriptor project contains %d errors.", errors.size()));
      }
      for (IParsingIssue error : errors) {
         lines.addAll(printIssue(error));
      }

      return lines;
   }

   /**
    * Gets a collection of strings that should be printed one per line that describes the parsing warnings in the given
    * result.
    */
   public static Collection<String> logWarnings(IParsingResult result) {
      Collection<String> lines = new ArrayList<>();

      Collection<IParsingIssue> warnings = result.getIssues()
            .stream()
            .filter(i -> i.getSeverity() == Severity.WARNING)
            .collect(Collectors.toList());
      if (warnings.size() > 0) {
         lines.add(String.format("System Descriptor project contains %d warnings.", warnings.size()));
         for (IParsingIssue warning : warnings) {
            lines.addAll(printIssue(warning));
         }
      }

      return lines;
   }

   private static Collection<String> printIssue(IParsingIssue issue) {
      Collection<String> lines = new ArrayList<>();

      Path offendingFile = issue.getLocation().getPath();
      lines.add("----------------------------------------");
      lines.add(String.format("File: %s", offendingFile == null ? "unknown"
                                                                : offendingFile.toAbsolutePath()));
      lines.add(String.format("%s: %s", issue.getSeverity(), issue.getMessage()));
      if (offendingFile != null && Files.isRegularFile(offendingFile)) {
         lines.addAll(printOffendingLine(issue));
      }

      return lines;
   }

   private static Collection<String> printOffendingLine(IParsingIssue issue) {
      Collection<String> lines = new ArrayList<>();

      try {
         ISourceLocation location = issue.getLocation();
         String line = Files.lines(location.getPath(), StandardCharsets.UTF_8)
                  .skip(location.getLineNumber() - 1)
                  .findFirst()
                  .get();
         if (line != null) {
            lines.add("");
            lines.add(line);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < location.getColumn(); i++) {
               sb.append(' ');
            }
            lines.add(sb.append("^").toString());
         }
      } catch (IOException e) {
         // Do nothing.
      }

      return lines;
   }

}
