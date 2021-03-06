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
package com.ngc.seaside.jellyfish.service.codegen.dataservice.impl;

import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.codegen.api.java.IGeneratedJavaField;
import com.ngc.seaside.jellyfish.service.codegen.api.proto.IGeneratedProtoField;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.INamedChild;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.data.IEnumeration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataFieldGenerationServiceTest {

   private DataFieldGenerationService service = new DataFieldGenerationService();

   @Mock
   private IJellyFishCommandOptions options;

   @Mock
   private IPackageNamingService packageService;

   @Before
   public void setup() {
      service.setPackageNamingService(packageService);
   }

   @Test
   public void testIntField() {
      IDataField field = getMockPrimitiveField(DataTypes.INT, "testInt", true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertTrue(Arrays.asList("int", "long").contains(javaField.getJavaType()));
      assertEquals("testInt", javaField.getJavaFieldName());
      assertEquals("getTestInt", javaField.getJavaGetterName());
      assertEquals("setTestInt", javaField.getJavaSetterName());
      assertTrue(
            Arrays.asList("int64", "int32", "uint64", "uint32", "sint32", "sint64", "fixed32", "fixed64", "sfixed32",
                          "sfixed64")
                  .contains(protoField.getProtoType()));
      assertEquals("testInt", protoField.getProtoFieldName());
      assertTrue(Arrays.asList("int", "long").contains(protoField.getJavaField().getJavaType()));
      assertEquals("getTestInt", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestInt", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testManyIntField() {
      IDataField field = getMockPrimitiveField(DataTypes.INT, "testInt", false);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertTrue(javaField.isMultiple());
      assertTrue(protoField.isMultiple());
      assertTrue(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertTrue(
            Arrays.asList("java.lang.Integer", "java.lang.Long").contains(javaField.getJavaType()));
      assertEquals("testInt", javaField.getJavaFieldName());
      assertEquals("getTestInt", javaField.getJavaGetterName());
      assertEquals("setTestInt", javaField.getJavaSetterName());
      assertTrue(
            Arrays.asList("int64", "int32", "uint64", "uint32", "sint32", "sint64", "fixed32", "fixed64", "sfixed32",
                          "sfixed64")
                  .contains(protoField.getProtoType()));
      assertEquals("testInt", protoField.getProtoFieldName());
      assertTrue(Arrays.asList("java.lang.Integer", "java.lang.Long")
                       .contains(protoField.getJavaField().getJavaType()));
      assertEquals("getTestIntList", protoField.getJavaField().getJavaGetterName());
      assertEquals("addAllTestInt", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testFloatField() {
      IDataField field = getMockPrimitiveField(DataTypes.FLOAT, "testFloat", true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertTrue(Arrays.asList("float", "double").contains(javaField.getJavaType()));
      assertEquals("testFloat", javaField.getJavaFieldName());
      assertEquals("getTestFloat", javaField.getJavaGetterName());
      assertEquals("setTestFloat", javaField.getJavaSetterName());
      assertTrue(Arrays.asList("float", "double")
                       .contains(protoField.getProtoType()));
      assertEquals("testFloat", protoField.getProtoFieldName());
      assertTrue(Arrays.asList("float", "double").contains(protoField.getJavaField().getJavaType()));
      assertEquals("getTestFloat", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestFloat", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testBooleanField() {
      IDataField field = getMockPrimitiveField(DataTypes.BOOLEAN, "testBoolean", true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertEquals("boolean", javaField.getJavaType());
      assertEquals("testBoolean", javaField.getJavaFieldName());
      assertEquals("getTestBoolean", javaField.getJavaGetterName());
      assertEquals("setTestBoolean", javaField.getJavaSetterName());
      assertEquals("bool", protoField.getProtoType());
      assertEquals("testBoolean", protoField.getProtoFieldName());
      assertEquals("boolean", protoField.getJavaField().getJavaType());
      assertEquals("getTestBoolean", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestBoolean", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testStringField() {
      IDataField field = getMockPrimitiveField(DataTypes.STRING, "testString", true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertEquals("java.lang.String", javaField.getJavaType());
      assertEquals("testString", javaField.getJavaFieldName());
      assertEquals("getTestString", javaField.getJavaGetterName());
      assertEquals("setTestString", javaField.getJavaSetterName());
      assertEquals("string", protoField.getProtoType());
      assertEquals("testString", protoField.getProtoFieldName());
      assertEquals("java.lang.String", protoField.getJavaField().getJavaType());
      assertEquals("getTestString", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestString", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testDataField() {
      IDataField field = getMockDataField("com.ngc.test.TestDataField", "testData", true, false);
      when(packageService.getEventPackageName(options, field.getReferencedDataType())).thenReturn(
            "com.ngc.test.events");
      when(packageService.getMessagePackageName(options, field.getReferencedDataType())).thenReturn(
            "com.ngc.test.messages");
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertEquals("com.ngc.test.events.TestDataField", javaField.getJavaType());
      assertEquals("testData", javaField.getJavaFieldName());
      assertEquals("getTestData", javaField.getJavaGetterName());
      assertEquals("setTestData", javaField.getJavaSetterName());
      assertEquals("com.ngc.test.messages.TestDataField", protoField.getProtoType());
      assertEquals("testData", protoField.getProtoFieldName());
      assertEquals("com.ngc.test.messages.TestDataField", protoField.getJavaField().getJavaType());
      assertEquals("getTestData", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestData", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testEnumField() {
      IDataField field = getMockDataField("com.ngc.test.TestEnumField", "testEnum", true, true);
      when(packageService.getEventPackageName(options, field.getReferencedEnumeration())).thenReturn(
            "com.ngc.test.events");
      when(packageService.getMessagePackageName(options, field.getReferencedEnumeration())).thenReturn(
            "com.ngc.test.messages");
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedProtoField protoField = service.getMessagesField(options, field);
      assertFalse(javaField.isMultiple());
      assertFalse(protoField.isMultiple());
      assertFalse(protoField.getJavaField().isMultiple());
      assertEquals(field, javaField.getDataField());
      assertEquals(field, protoField.getDataField());
      assertEquals(field, protoField.getJavaField().getDataField());
      assertEquals("com.ngc.test.events.TestEnumField", javaField.getJavaType());
      assertEquals("testEnum", javaField.getJavaFieldName());
      assertEquals("getTestEnum", javaField.getJavaGetterName());
      assertEquals("setTestEnum", javaField.getJavaSetterName());
      assertEquals("com.ngc.test.messages.TestEnumField", protoField.getProtoType());
      assertEquals("testEnum", protoField.getProtoFieldName());
      assertEquals("com.ngc.test.messages.TestEnumField", protoField.getJavaField().getJavaType());
      assertEquals("getTestEnum", protoField.getJavaField().getJavaGetterName());
      assertEquals("setTestEnum", protoField.getJavaField().getJavaSetterName());
   }

   @Test
   public void testOddFieldName() {
      IDataField field = getMockPrimitiveField(DataTypes.INT,
                                               "__abc_abC__aBc_Abc__aBC_AbC__ABc_ABC__1abc_abc1__a1bc_1Abc__"
                                                     + "Abc1_A1bc__Ab1c_Ab1c__1ABc_ABc1__A1Bc_AB1c__1ABC_ABC1__A1B1C_",
                                               true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      assertEquals("getAbcAbCABcAbcABCAbCABcABC1AbcAbc1A1Bc1AbcAbc1A1BcAb1CAb1C1ABcABc1A1BcAB1C1ABCABC1A1B1C",
                   javaField.getJavaGetterName());
      assertEquals("setAbcAbCABcAbcABCAbCABcABC1AbcAbc1A1Bc1AbcAbc1A1BcAb1CAb1C1ABcABc1A1BcAB1C1ABCABC1A1B1C",
                   javaField.getJavaSetterName());
      IGeneratedJavaField javaProtoField = service.getMessagesField(options, field).getJavaField();
      assertEquals("getAbcAbCABcAbcABCAbCABcABC1AbcAbc1A1Bc1AbcAbc1A1BcAb1CAb1C1ABcABc1A1BcAB1C1ABCABC1A1B1C",
                   javaProtoField.getJavaGetterName());
      assertEquals("setAbcAbCABcAbcABCAbCABcABC1AbcAbc1A1Bc1AbcAbc1A1BcAb1CAb1C1ABcABc1A1BcAB1C1ABCABC1A1B1C",
                   javaProtoField.getJavaSetterName());
   }

   @Test
   public void testJavaReservedWord() {
      IDataField field = getMockPrimitiveField(DataTypes.INT, "long", true);
      IDataField underbarField = getMockPrimitiveField(DataTypes.INT, "_long", true);
      IDataField fieldUnderbar = getMockPrimitiveField(DataTypes.INT, "long_", true);
      IGeneratedJavaField javaField = service.getEventsField(options, field);
      IGeneratedJavaField underbarJavaField = service.getEventsField(options, underbarField);
      IGeneratedJavaField javaFieldUnderbar = service.getEventsField(options, fieldUnderbar);
      assertNotEquals("long", javaField.getJavaFieldName());
      assertNotEquals(underbarJavaField.getJavaFieldName(), javaField.getJavaFieldName());
      assertNotEquals(javaFieldUnderbar.getJavaFieldName(), javaField.getJavaFieldName());
   }

   private static IDataField getMockDataField(String type, String name, boolean single, boolean isEnum) {
      IDataField field = mock(IDataField.class);
      when(field.getCardinality()).thenReturn(single ? FieldCardinality.SINGLE : FieldCardinality.MANY);
      when(field.getName()).thenReturn(name);
      INamedChild<IPackage> referenceType;
      if (isEnum) {
         when(field.getType()).thenReturn(DataTypes.ENUM);
         referenceType = mock(IEnumeration.class);
         when(field.getReferencedEnumeration()).thenReturn((IEnumeration) referenceType);
      } else {
         when(field.getType()).thenReturn(DataTypes.DATA);
         referenceType = mock(IData.class);
         when(field.getReferencedDataType()).thenReturn((IData) referenceType);
      }
      when(referenceType.getName()).thenReturn(type.substring(type.lastIndexOf('.') + 1));
      return field;
   }

   private static IDataField getMockPrimitiveField(DataTypes type, String name, boolean single) {
      assertFalse(name.isEmpty());
      IDataField field = mock(IDataField.class);
      when(field.getCardinality()).thenReturn(single ? FieldCardinality.SINGLE : FieldCardinality.MANY);
      when(field.getName()).thenReturn(name);
      when(field.getType()).thenReturn(type);
      return field;
   }

}
