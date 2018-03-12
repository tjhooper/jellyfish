package com.ngc.seaside.systemdescriptor.model.impl.xtext.model.link;

import com.google.common.base.Preconditions;

import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.link.IModelLink;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.AbstractWrappedXtext;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.exception.UnrecognizedXtextTypeException;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.exception.XtextObjectNotFoundException;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.store.IWrapperResolver;
import com.ngc.seaside.systemdescriptor.systemDescriptor.BaseLinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldReference;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkableExpression;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkableReference;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.RefinedLinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.RefinedLinkNameDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;

import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.Optional;

/**
 * Adapts a {@link LinkDeclaration} to an {@link IModelLink} that links together data elements.
 *
 * This class is not threadsafe.
 */
public class WrappedDataReferenceLink extends AbstractWrappedXtext<LinkDeclaration>
      implements IModelLink<IDataReferenceField> {

   private IModelLink<IDataReferenceField> refinedLink;
   private IDataReferenceField source;
   private IDataReferenceField target;

   /**
    * Creates a new wrapper for the given link declaration.  If the link declaration does not wrap data an {@code
    * IllegalArgumentException} is thrown.  Use {@link #tryToWrap(IWrapperResolver, LinkDeclaration)} as an alternative
    * to avoid the exception and determine if the declaration really does link data.
    *
    * @see #tryToWrap(IWrapperResolver, LinkDeclaration)
    */
   public WrappedDataReferenceLink(IWrapperResolver resolver, LinkDeclaration wrapped) {
      super(resolver, wrapped);
      switch (wrapped.eClass().getClassifierID()) {
         case SystemDescriptorPackage.BASE_LINK_DECLARATION:
            source = getReferenceTo(((BaseLinkDeclaration) wrapped).getSource());
            target = getReferenceTo(((BaseLinkDeclaration) wrapped).getTarget());
            break;
         case SystemDescriptorPackage.REFINED_LINK_DECLARATION:
            refinedLink = getRefinedLink((RefinedLinkDeclaration) wrapped);
            break;
         case SystemDescriptorPackage.REFINED_LINK_NAME_DECLARATION:
            refinedLink = getRefinedLink((RefinedLinkNameDeclaration) wrapped);
            break;
         default:
            throw new UnrecognizedXtextTypeException(wrapped);
      }
   }

   @Override
   public IDataReferenceField getSource() {
      return source;
   }

   @Override
   public IModelLink<IDataReferenceField> setSource(IDataReferenceField source) {
      throw new UnsupportedOperationException("the source of a link cannot be currently modified!");
   }

   @Override
   public IDataReferenceField getTarget() {
      return target;
   }

   @Override
   public IModelLink<IDataReferenceField> setTarget(IDataReferenceField target) {
      throw new UnsupportedOperationException("the target of a link cannot be currently modified!");
   }

   @Override
   public Optional<String> getName() {
      return Optional.ofNullable(wrapped.getName());
   }

   @Override
   public IModelLink<IDataReferenceField> setName(String name) {
      wrapped.setName(name);
      return this;
   }

   @Override
   public Optional<IModelLink<IDataReferenceField>> getRefinedLink() {
      return Optional.ofNullable(refinedLink);
   }

   @Override
   public IModelLink<IDataReferenceField> setRefinedLink(IModelLink<IDataReferenceField> refinedLink) {
      throw new UnsupportedOperationException("refined link cannot be changed!");
   }

   @Override
   public IModel getParent() {
      return resolver.getWrapperFor((Model) wrapped.eContainer().eContainer());
   }

   /**
    * Tries to create an {@code IModelLink} that links data.  If the given link declaration does not link data the
    * returned {@code Optional} is empty.
    */
   public static Optional<IModelLink<IDataReferenceField>> tryToWrap(IWrapperResolver resolver,
                                                                     LinkDeclaration wrapper) {
      Optional<IModelLink<IDataReferenceField>> result = Optional.empty();
      try {
         result = Optional.of(new WrappedDataReferenceLink(resolver, wrapper));
      } catch (IllegalArgumentException e) {
         // Do nothing, this means the link is not linking data.
      }
      return result;
   }

   private IDataReferenceField getReferenceTo(LinkableReference ref) {
      // What kind of a link is this?
      switch (ref.eClass().getClassifierID()) {
         case SystemDescriptorPackage.FIELD_REFERENCE:
            // If this is a field reference, it must be referencing a field of the parent model.
            return getFieldOf(((FieldReference) ref).getFieldDeclaration());
         case SystemDescriptorPackage.LINKABLE_EXPRESSION:
            // Otherwise, this is an expression that may be pointing to another model.
            return getFieldOf(((LinkableExpression) ref).getTail());
         default:
            throw new UnrecognizedXtextTypeException(ref);
      }
   }

   private IDataReferenceField getFieldOf(FieldDeclaration declaration) {
      // Only models can have field declarations.
      IModel parent = resolver.getWrapperFor((Model) declaration.eContainer().eContainer());
      // Get the wrapper for the field.  Note that a model may not have duplicate field names.  Therefore, the declaration
      // is either for input or output.
      Optional<IDataReferenceField> field = parent.getInputs() == null
                                            ? Optional.empty()
                                            : parent.getInputs().getByName(declaration.getName());
      if (!field.isPresent()) {
         field = parent.getOutputs() == null ? Optional.empty()
                                             : parent.getOutputs().getByName(declaration.getName());
      }
      return field.orElseThrow(() -> new IllegalArgumentException(String.format(
            "could not find input or output field named %s in model %s!",
            declaration.getName(),
            parent)));
   }

   @SuppressWarnings({"unchecked"})
   private IModelLink<IDataReferenceField> getRefinedLink(RefinedLinkDeclaration link) {
      IModelLink<IDataReferenceField> refinedLink = null;

      IModel model = getParent().getRefinedModel().orElse(null);
      while (model != null && refinedLink == null) {
         // Get the refined model.
         Model xtext = resolver.findXTextModel(model.getName(), model.getParent().getName())
               .orElse(null);
         if (xtext == null) {
            throw XtextObjectNotFoundException.forModel(model);
         }

         // Does the refined model contain the link?
         LinkDeclaration xtextLink = xtext.getLinks().getDeclarations()
               .stream()
               .filter(l -> l instanceof BaseLinkDeclaration)
               .map(l -> (BaseLinkDeclaration) l)
               .filter(l -> EcoreUtil.equals(l.getSource(), link.getSource()))
               .filter(l -> EcoreUtil.equals(l.getTarget(), link.getTarget()))
               .findFirst()
               .orElse(null);
         if (xtextLink != null) {
            // If so, find the wrapped link in the wrapped model.
            for (IModelLink<?> wrappedLink : resolver.getWrapperFor(xtext).getLinks()) {
               // Note both WrappedDataReferenceLink and WrappedModelReferenceLink extend
               // AbstractWrappedXtext<LinkDeclaration> so this cast is safe.
               AbstractWrappedXtext<LinkDeclaration> casted = (AbstractWrappedXtext<LinkDeclaration>) wrappedLink;
               if (casted.unwrap().equals(xtextLink)) {
                  refinedLink = (IModelLink<IDataReferenceField>) casted;
               }
            }
         }

         model = model.getRefinedModel().orElse(null);
      }

      Preconditions.checkState(refinedLink != null,
                               "unable to find refined link in refine hierarchy of %s!",
                               getParent().getFullyQualifiedName());
      return refinedLink;
   }

   @SuppressWarnings({"unchecked"})
   private IModelLink<IDataReferenceField> getRefinedLink(RefinedLinkNameDeclaration link) {
      IModelLink<IDataReferenceField> refinedLink = null;

      IModel model = getParent().getRefinedModel().orElse(null);
      while (model != null && refinedLink == null) {
         refinedLink = (IModelLink<IDataReferenceField>) model.getLinkByName(link.getName()).orElse(null);
         model = model.getRefinedModel().orElse(null);
      }

      Preconditions.checkState(refinedLink != null,
                               "unable to find refined link with name '%s' in refine hierarchy of %s!",
                               link.getName(),
                               getParent().getFullyQualifiedName());
      return refinedLink;
   }
}
