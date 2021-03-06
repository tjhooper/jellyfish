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
package com.ngc.seaside.systemdescriptor.model.impl.xtext.data;

import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.data.IEnumeration;
import com.ngc.seaside.systemdescriptor.model.api.metadata.IMetadata;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.AbstractWrappedXtextTest;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Cardinality;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Enumeration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package;
import com.ngc.seaside.systemdescriptor.systemDescriptor.ReferencedDataModelFieldDeclaration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WrappedReferencedEnumFieldTest extends AbstractWrappedXtextTest {

   private WrappedReferencedEnumField wrappedEnumField;

   private ReferencedDataModelFieldDeclaration field;

   private Enumeration referencedEnum;

   @Mock
   private IEnumeration referenced;

   @Mock
   private IData parent;

   @Mock
   private IPackage pack;

   @Before
   public void setup() throws Throwable {
      Data parentData = factory().createData();
      parentData.setName("Foo");

      referencedEnum = factory().createEnumeration();
      referencedEnum.setName("ReferencedEnum");

      Package packageZ = factory().createPackage();
      packageZ.setName("my.foo.enums");
      packageZ.setElement(referencedEnum);

      field = factory().createReferencedDataModelFieldDeclaration();
      field.setName("field1");
      field.setDataModel(referencedEnum);
      field.setCardinality(Cardinality.DEFAULT);
      parentData.getFields().add(field);

      when(referenced.getName()).thenReturn(referencedEnum.getName());
      when(referenced.getParent()).thenReturn(pack);
      when(pack.getName()).thenReturn(packageZ.getName());

      when(resolver().getWrapperFor(parentData)).thenReturn(parent);
      when(resolver().getWrapperFor(referencedEnum)).thenReturn(referenced);
      when(resolver().findXTextEnum(referenced.getName(), packageZ.getName())).thenReturn(Optional.of(referencedEnum));
   }

   @Test
   public void testDoesWrapXtextObject() throws Throwable {
      wrappedEnumField = new WrappedReferencedEnumField(resolver(), field);
      assertEquals("name not correct!",
                   wrappedEnumField.getName(),
                   field.getName());
      assertEquals("parent not correct!",
                   parent,
                   wrappedEnumField.getParent());
      assertEquals("metadata not set!",
                   IMetadata.EMPTY_METADATA,
                   wrappedEnumField.getMetadata());
      assertEquals("referenced enum not correct!",
                   referenced,
                   wrappedEnumField.getReferencedEnumeration());
      assertEquals("cardinality not correct!",
                   FieldCardinality.SINGLE,
                   wrappedEnumField.getCardinality());
   }

   @Test
   public void testDoesUpdateXtextObject() throws Throwable {
      Enumeration anotherReferencedEnum = factory().createEnumeration();
      anotherReferencedEnum.setName("ReferencedEnum2");

      Package packageZ = factory().createPackage();
      packageZ.setName("more.of.foo.enums");
      packageZ.setElement(anotherReferencedEnum);

      IPackage anotherPackage = mock(IPackage.class);
      when(anotherPackage.getName()).thenReturn(packageZ.getName());
      IEnumeration anotherReference = mock(IEnumeration.class);
      when(anotherReference.getName()).thenReturn(anotherReferencedEnum.getName());
      when(anotherReference.getParent()).thenReturn(anotherPackage);

      when(resolver().findXTextEnum(anotherReference.getName(), packageZ.getName()))
            .thenReturn(Optional.of(anotherReferencedEnum));

      wrappedEnumField = new WrappedReferencedEnumField(resolver(), field);
      // Should not throw an error.
      wrappedEnumField.setType(DataTypes.ENUM);
      wrappedEnumField.setReferencedEnumeration(anotherReference);
      assertEquals("enum not correct!",
                   anotherReferencedEnum,
                   field.getDataModel());

      wrappedEnumField.setCardinality(FieldCardinality.MANY);
      assertEquals("cardinality not correct!",
                   FieldCardinality.MANY,
                   wrappedEnumField.getCardinality());
   }

   @Test
   public void testDoesCreateXtextObject() throws Throwable {
      IDataField newField = mock(IDataField.class);
      when(newField.getName()).thenReturn("newField");
      when(newField.getType()).thenReturn(DataTypes.ENUM);
      when(newField.getReferencedEnumeration()).thenReturn(referenced);
      when(newField.getCardinality()).thenReturn(FieldCardinality.MANY);

      ReferencedDataModelFieldDeclaration xtext = WrappedReferencedEnumField.toXtext(resolver(), newField);
      assertEquals("name not correct!",
                   newField.getName(),
                   xtext.getName());
      assertEquals("referenced enum not correct!",
                   referencedEnum,
                   xtext.getDataModel());
      assertEquals("cardinality not correct!",
                   xtext.getCardinality(),
                   Cardinality.MANY);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNotAllowEnumTypeToBeChangedToPrimitiveType() throws Throwable {
      wrappedEnumField = new WrappedReferencedEnumField(resolver(), field);
      wrappedEnumField.setType(DataTypes.INT);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNotAllowDataTypeToBeChangedToDataType() throws Throwable {
      wrappedEnumField = new WrappedReferencedEnumField(resolver(), field);
      wrappedEnumField.setType(DataTypes.DATA);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testDoesNotCreateXtextObjectForPrimitiveType() throws Throwable {
      IDataField newField = mock(IDataField.class);
      when(newField.getType()).thenReturn(DataTypes.INT);
      WrappedReferencedEnumField.toXtext(resolver(), newField);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testDoesNotCreateXtextObjectForDataType() throws Throwable {
      IDataField newField = mock(IDataField.class);
      when(newField.getType()).thenReturn(DataTypes.DATA);
      WrappedReferencedEnumField.toXtext(resolver(), newField);
   }
}
