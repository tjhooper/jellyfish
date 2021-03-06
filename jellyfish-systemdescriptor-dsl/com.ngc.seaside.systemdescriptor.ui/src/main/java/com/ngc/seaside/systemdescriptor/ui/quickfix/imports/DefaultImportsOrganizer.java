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
package com.ngc.seaside.systemdescriptor.ui.quickfix.imports;

import com.google.inject.Inject;

import com.ngc.seaside.systemdescriptor.systemDescriptor.Import;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package;
import com.ngc.seaside.systemdescriptor.ui.quickfix.IDocumentWriter;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.ReplaceRegion;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DefaultImportsOrganizer implements IImportsOrganizer {

   @Inject
   private IImportsResolver importsResolver;

   @Inject
   private IImportsRegionIdentifier importsRegionIdentifier;

   @Inject
   private IDocumentWriter writer;

   @Override
   public void organizeImports(IXtextDocument document) {
      ReplaceRegion replacement = document.readOnly(state -> {
         Package pkg = getPackage(state).orElseThrow(IllegalStateException::new);
         ITextRegion oldImports = importsRegionIdentifier.getImportsRegion(pkg, state, true);
         try {
            List<Import> imports = importsResolver.resolveImports(pkg, state);
            return new ReplaceRegion(oldImports,
                                     System.lineSeparator() + System.lineSeparator() + imports.stream()
                                           .map(Import::getImportedNamespace)
                                           .map("import "::concat)
                                           .collect(
                                                 Collectors.joining(System.lineSeparator()))
                                           + System.lineSeparator() + System.lineSeparator());
         } catch (CancellationException e) {
            // Do nothing.
         }
         return null;
      });

      if (replacement != null) {
         writer.replace(document, replacement.getOffset(), replacement.getLength(), replacement.getText());
      }
   }

   @Override
   public void addImports(IXtextDocument document, Import... imports) {
      ITextRegion importsRegion = document.readOnly(state -> {
         Package pkg = getPackage(state).orElseThrow(IllegalStateException::new);
         return importsRegionIdentifier.getImportsAppendRegion(pkg, state);
      });
      if (imports.length > 0) {
         String importsText = System.lineSeparator()
               + Stream.of(imports).map(Import::getImportedNamespace).map("import "::concat).collect(
               Collectors.joining(System.lineSeparator()))
               + System.lineSeparator() + System.lineSeparator();
         writer.replace(document, importsRegion.getOffset(), importsRegion.getLength(), importsText);
      }
   }

   private Optional<Package> getPackage(XtextResource state) {
      List<EObject> objects = state.getContents();
      EObject object;
      if (objects.size() != 1 || !((object = objects.get(0)) instanceof Package)) {
         return Optional.empty();
      } else {
         return Optional.of((Package) object);
      }
   }

}
