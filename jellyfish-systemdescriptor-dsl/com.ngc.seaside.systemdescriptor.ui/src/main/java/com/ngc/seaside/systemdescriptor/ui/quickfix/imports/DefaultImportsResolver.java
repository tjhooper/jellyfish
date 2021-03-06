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
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;
import com.ngc.seaside.systemdescriptor.systemDescriptor.impl.ImportImpl;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.ITextRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class DefaultImportsResolver implements IImportsResolver {

   /**
    * A selector for choosing imports.
    */
   @Inject
   private IReferenceSelector importChooser;

   /**
    * A provider that is used to get the IResourceDescriptions which serves as
    * the main index or cache for Xtext. This is used do reverse lookups to
    * elements using their qualified names.
    */
   @Inject
   private IResourceDescriptionsProvider resourceDescriptionsProvider;

   /**
    * A provider that is used to determine the location of elements in a document.
    */
   @Inject
   private ILocationInFileProvider locationInFileProvider;

   /**
    * Used to construct a QualifiedName from a fually qualified name
    * represented as a string.
    */
   @Inject
   private IQualifiedNameConverter qualifiedNameConverter;

   /**
    * Used to determine possible types a reference could be referring to.
    */
   @Inject
   private IReferenceResolver referenceResolver;

   @Override
   public List<Import> resolveImports(Package pkg, XtextResource resource) {
      final IResourceDescriptions descriptions = resourceDescriptionsProvider.getResourceDescriptions(
            pkg.eResource().getResourceSet());
      final Set<QualifiedName> namesToImport = new HashSet<>();
      TreeIterator<EObject> contents = EcoreUtil.getAllContents(pkg, true);
      Map<String, QualifiedName> resolvedReferences = new HashMap<>();

      // Search all of the contents of the package for external references
      while (contents.hasNext()) {
         EObject element = contents.next();

         // Search the cross-references of the given element for external references
         EContentsEList.FeatureIterator<EObject> iter =
               (EContentsEList.FeatureIterator<EObject>) element.eCrossReferences().iterator();
         while (iter.hasNext()) {
            EObject referencedElement = iter.next();
            EStructuralFeature feature = iter.feature();
            ITextRegion refRegion = locationInFileProvider.getSignificantTextRegion(element, feature, 0);
            String referencedName = getReferencedType(resource, refRegion);
            if (referencedName.isEmpty() || referencedName.indexOf('.') >= 0) {
               // Don't import references that use fully-qualified names
               continue;
            }

            boolean isUnresolved = referencedElement.eIsProxy();
            if (isUnresolved) {
               // Resource has not been resolved
               // Search for potential references and select one
               Predicate<? super EObject> predicate = obj -> referencedElement.eClass().isInstance(obj);
               Optional<QualifiedName> choice = findUnknownReference(resource,
                                                                     resource.getResourceSet(),
                                                                     resolvedReferences,
                                                                     referencedName,
                                                                     refRegion,
                                                                     predicate);
               if (choice.isPresent()) {
                  namesToImport.add(choice.get());
               }
            } else {
               // Resource has been resolve
               // Add resources to imports set
               for (IEObjectDescription description : descriptions.getExportedObjectsByObject(referencedElement)) {
                  if (SystemDescriptorPackage.Literals.ELEMENT.isInstance(description.getEObjectOrProxy())) {
                     namesToImport.add(description.getQualifiedName());
                  }
               }
            }
         }
      }

      List<Import> imports = namesToImport.stream().map(name -> new ImportImpl() {
         @Override
         public String getImportedNamespace() {
            return qualifiedNameConverter.toString(name);
         }
      }).collect(Collectors.toList());
      return imports;
   }

   /**
    * Returns the actual text of a reference.
    *
    * @param resource resource containing text
    * @param region   region of reference
    * @return actual text of reference
    */
   private String getReferencedType(XtextResource resource, ITextRegion region) {
      IParseResult parseResult = resource.getParseResult();
      if (parseResult != null && region != null) {
         return parseResult.getRootNode().getText().substring(region.getOffset(),
                                                              region.getOffset() + region.getLength());
      }
      return "";
   }

   /**
    * Finds the qualified name for the unknown reference.
    *
    * @param resource    resource
    * @param resourceSet resource set
    * @param references  import references that have previously been resolved by the user
    * @param region      region of the reference in the resource
    * @return the qualified name, or {@link Optional#empty()} if the reference cannot be resolved
    * @throws CancellationException if the user cancels the operation
    */
   private Optional<QualifiedName> findUnknownReference(XtextResource resource, ResourceSet resourceSet,
                                                        Map<String, QualifiedName> references,
                                                        String referenceText,
                                                        ITextRegion region, Predicate<? super EObject> choiceFilter) {
      if (references.containsKey(referenceText)) {
         return Optional.of(references.get(referenceText));
      }
      Set<QualifiedName> possibleTypes = referenceResolver.findPossibleTypes(referenceText, resourceSet, choiceFilter);
      if (possibleTypes.isEmpty()) {
         return Optional.empty();
      }
      Optional<QualifiedName> name = selectReference(resource, possibleTypes, region);
      if (!name.isPresent()) {
         throw new CancellationException();
      }
      references.put(referenceText, name.get());
      return name;
   }

   /**
    * Selects the qualified name from the given set of possible qualified name
    *
    * @param region region of the import reference
    * @return the selected import or {@link Optional#empty()} if the user cancels the operation
    */
   private Optional<QualifiedName> selectReference(XtextResource resource, Set<QualifiedName> possibleImports,
                                                   ITextRegion region) {
      if (possibleImports.isEmpty()) {
         return Optional.empty();
      } else if (possibleImports.size() == 1) {
         return Optional.of(possibleImports.iterator().next());
      } else {
         List<QualifiedName> choices = new ArrayList<>(possibleImports);
         OptionalInt choice = importChooser.select(choices, resource, region);
         if (choice.isPresent()) {
            return Optional.of(choices.get(choice.getAsInt()));
         } else {
            return Optional.empty();
         }
      }
   }

}
