package com.ngc.seaside.systemdescriptor.ui.quickfix.imports;

import com.google.inject.Inject;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.model.edit.IssueModificationContext;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.Fixes;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.validation.IssueCodes;

import java.util.Set;

public class ImportQuickfixProvider extends DefaultQuickfixProvider {

   @Inject
   private OrganizeImportsHandler organizeImportsHandler;

   @Inject
   private IssueModificationContext.Factory factory;
   
   @Inject
   private IReferenceResolver referenceResolver;

   @Fixes({ @Fix(IssueCodes.IMPORT_UNUSED), @Fix(IssueCodes.IMPORT_DUPLICATE), @Fix(IssueCodes.IMPORT_COLLISION),
            @Fix(IssueCodes.IMPORT_CONFLICT), @Fix(IssueCodes.IMPORT_UNRESOLVED) })
   public void fixUnusedImport(Issue issue, IssueResolutionAcceptor acceptor) {
      removeImport(issue, acceptor);
      organizeImports(issue, acceptor);
   }

   @Fix(Diagnostic.LINKING_DIAGNOSTIC)
   public void fixedMissingImports(Issue issue, IssueResolutionAcceptor acceptor) throws BadLocationException {
      addImports(issue, acceptor);
   }

   private void removeImport(Issue issue, IssueResolutionAcceptor acceptor) {
      acceptor.accept(issue, "Remove unused import", "", getRemoveImportImage(), context -> {
         IXtextDocument document = context.getXtextDocument();
         DocumentRewriteSession session = null;
         if (document instanceof IDocumentExtension4) {
            session = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
         }
         document.replace(issue.getOffset(), issue.getLength() + 1, "");
         if (session != null) {
            ((IDocumentExtension4) document).stopRewriteSession(session);
         }
      });
   }

   private void organizeImports(Issue issue, IssueResolutionAcceptor acceptor) {
      acceptor.accept(issue, "Organize imports", "", getOrganizeImportsImage(), context -> {
         IXtextDocument document = context.getXtextDocument();
         organizeImportsHandler.organizeImports(document);
      });
   }

   private void addImports(Issue issue, IssueResolutionAcceptor acceptor) throws BadLocationException {
      IModificationContext modificationContext = factory.createModificationContext(issue);
      IXtextDocument document = modificationContext.getXtextDocument();
      String reference = document.get(issue.getOffset(), issue.getLength());
      ResourceSet resourceSet = document.readOnly(state -> {
         return state.getResourceSet();
      });
      Set<QualifiedName> names = referenceResolver.findPossibleTypes(reference, resourceSet, __ -> true);
      for (QualifiedName name : names) {
         String qualifiedName = name.toString();
         String qualifiedPackage = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
         acceptor.accept(issue,
            "Import " + reference + " (" + qualifiedPackage + ")",
            "",
            getAddImportImage(),
            context -> organizeImportsHandler.addImports(context.getXtextDocument(), name));
      }
   }

   private String getRemoveImportImage() {
      return "";
   }

   private String getOrganizeImportsImage() {
      return "";
   }

   private String getAddImportImage() {
      return "";
   }
}
