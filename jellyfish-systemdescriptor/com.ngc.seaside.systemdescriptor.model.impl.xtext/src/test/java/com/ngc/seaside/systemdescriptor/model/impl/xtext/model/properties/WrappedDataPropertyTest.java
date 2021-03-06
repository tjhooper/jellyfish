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
package com.ngc.seaside.systemdescriptor.model.impl.xtext.model.properties;

import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperties;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperty;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IPropertyValues;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.AbstractWrappedXtextTest;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Cardinality;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Properties;
import com.ngc.seaside.systemdescriptor.systemDescriptor.ReferencedPropertyFieldDeclaration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WrappedDataPropertyTest extends AbstractWrappedXtextTest {

   private WrappedDataProperty wrappedProperty;

   private ReferencedPropertyFieldDeclaration property;

   private Data referencedData;

   @Mock
   private IData referenced;

   @Mock
   private IProperties parent;

   @Mock
   private IPackage pack;

   @Before
   public void setup() throws Throwable {
      Properties parentData = factory().createProperties();

      referencedData = factory().createData();
      referencedData.setName("referencedData");

      Package packageZ = factory().createPackage();
      packageZ.setName("my.foo.data");
      packageZ.setElement(referencedData);

      property = factory().createReferencedPropertyFieldDeclaration();
      property.setName("property1");
      property.setDataModel(referencedData);
      property.setCardinality(Cardinality.DEFAULT);
      parentData.getDeclarations().add(property);

      when(referenced.getName()).thenReturn(referencedData.getName());
      when(referenced.getParent()).thenReturn(pack);
      when(pack.getName()).thenReturn(packageZ.getName());

      when(resolver().getWrapperFor(parentData)).thenReturn(parent);
      when(resolver().getWrapperFor(referencedData)).thenReturn(referenced);
      when(resolver().findXTextData(referenced.getName(), packageZ.getName())).thenReturn(Optional.of(referencedData));
   }

   @Test
   public void testDoesWrapXtextObject() throws Throwable {
      wrappedProperty = new WrappedDataProperty(resolver(), property, IPropertyValues.emptyPropertyValues());

      assertEquals("name not correct!",
                   wrappedProperty.getName(),
                   property.getName());
      assertEquals("parent not correct!",
                   parent,
                   wrappedProperty.getParent());
      assertEquals("referenced data not correct!",
                   referenced,
                   wrappedProperty.getReferencedDataType());
      assertEquals("cardinality not correct!",
                   FieldCardinality.SINGLE,
                   wrappedProperty.getCardinality());
   }

   @Test
   public void testDoesCreateXtextObject() throws Throwable {
      IProperty newField = mock(IProperty.class);
      when(newField.getName()).thenReturn("newField");
      when(newField.getType()).thenReturn(DataTypes.DATA);
      when(newField.getReferencedDataType()).thenReturn(referenced);
      when(newField.getCardinality()).thenReturn(FieldCardinality.MANY);

      ReferencedPropertyFieldDeclaration xtext = WrappedDataProperty.toXtextReferencedPropertyFieldDeclaration(
            resolver(),
            newField);
      assertEquals("name not correct!",
                   newField.getName(),
                   xtext.getName());
      assertEquals("referenced data not correct!",
                   referencedData,
                   xtext.getDataModel());
      assertEquals("cardinality not correct!",
                   xtext.getCardinality(),
                   Cardinality.MANY);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testDoesNotCreateXtextObjectForPrimitiveType() throws Throwable {
      IProperty newProperty = mock(IProperty.class);
      when(newProperty.getType()).thenReturn(DataTypes.INT);
      WrappedDataProperty.toXtextReferencedPropertyFieldDeclaration(resolver(), newProperty);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testDoesNotCreateXtextObjectForEnumType() throws Throwable {
      IProperty newProperty = mock(IProperty.class);
      when(newProperty.getType()).thenReturn(DataTypes.ENUM);
      WrappedDataProperty.toXtextReferencedPropertyFieldDeclaration(resolver(), newProperty);
   }
}
