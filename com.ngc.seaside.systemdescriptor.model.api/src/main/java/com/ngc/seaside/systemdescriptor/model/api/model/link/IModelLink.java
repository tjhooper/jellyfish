package com.ngc.seaside.systemdescriptor.model.api.model.link;

import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IReferenceField;

/**
 * Represents a link between two fields of a model.  Links usually join together two {@link IModel}s or {@link
 * com.ngc.seaside.systemdescriptor.model.api.data.IData}s types.  Operations that change the state of this object may
 * throw {@code UnsupportedOperationException}s if the object is immutable.
 *
 * @param <T> the type of reference field that link is connecting
 */
public interface IModelLink<T extends IReferenceField> {

  /**
   * Gets the source of the link.
   *
   * @return the source of the link
   */
  T getSource();

  /**
   * Sets the source of the link
   *
   * @param source the source of the link
   * @return this link
   */
  IModelLink<T> setSource(T source);

  /**
   * Gets the target of the link.
   */
  T getTarget();

  /**
   * Sets the target of the link
   *
   * @param target the target of the link
   * @return this link
   */
  IModelLink<T> setTarget(T target);

  /**
   * Gets the parent model that contains this link.
   *
   * @return the parent model that contains this link
   */
  IModel getParent();
}
