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
package com.ngc.seaside.jellyfish.service.impl.promptuserservice;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PromptUserServiceTest {
   private static PrintStream SYSTEM_OUT;

   private PromptUserService delegate;

   @BeforeClass
   public static void setupClass() {
      SYSTEM_OUT = System.out;
   }

   @AfterClass
   public static void cleanUpClass() {
      System.setOut(SYSTEM_OUT);
   }

   @Before
   public void setup() {
      delegate = new PromptUserService();
   }

   @Test
   public void doesPromptReturnDefault() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String emptyInput = "";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(emptyInput.getBytes()));

      String value = delegate.prompt("myParam", "test", null);
      outputStream.flush();
      assertEquals("Enter value for myParam (test): ", outputStream.toString());

      assertEquals("The default value should be returned", "test", value);
   }

   @Test
   public void doesPromptReturnEntered() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "myValue";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      String value = delegate.prompt("myParam", "test", null);
      outputStream.flush();
      assertEquals("Enter value for myParam (test): ", outputStream.toString());

      assertEquals("myValue should be returned", "myValue", value);
   }

   @Test
   public void doesRePrompt() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "\nabcd";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      String value = delegate.prompt("myParam", null, null);

      outputStream.flush();
      assertEquals("Enter value for myParam: Enter value for myParam: ", outputStream.toString());
      assertEquals("abcd", value);
   }

   @Test
   public void doesPromptTestValidity() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "abcd";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      ValidCheck check = new ValidCheck();
      String value = delegate.prompt("myParam", "value", check);

      outputStream.flush();
      assertEquals("Enter value for myParam (value): ", outputStream.toString());

      assertEquals("abcd", value);
      assertEquals(1, check.count);
   }

   @Test
   public void doesPromptDataEntry() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "abcd";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      ValidCheck check = new ValidCheck();
      String value = delegate.promptDataEntry("Do you wish to continue?", "(y or n, press enter for 'y')", "y", check);

      outputStream.flush();
      assertEquals("Do you wish to continue? (y or n, press enter for 'y'): ", outputStream.toString());

      assertEquals("abcd", value);
      assertEquals(1, check.count);
   }

   @Test
   public void doesPromptDataEntryDefault() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      ValidCheck check = new ValidCheck();
      String
               value =
               delegate.promptDataEntry("Please enter the group ID", "(Press enter for 'com.ngc.seaside')",
                                        "com.ngc.seaside", check);

      outputStream.flush();
      assertEquals("Please enter the group ID (Press enter for 'com.ngc.seaside'): ", outputStream.toString());

      assertEquals("com.ngc.seaside", value);
      assertEquals(0, check.count);
   }

   @Test
   public void doesPromptDataEntryDefaultEmpty() throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      String input = "";
      PrintStream ps = new PrintStream(outputStream);
      System.setOut(ps);

      delegate.setInputStream(new ByteArrayInputStream(input.getBytes()));

      ValidCheck check = new ValidCheck();
      String value = delegate.promptDataEntry("Please enter the group ID", "", "", check);

      outputStream.flush();
      assertEquals("Please enter the group ID : ", outputStream.toString());

      assertEquals("", value);
      assertEquals(0, check.count);
   }

   private class ValidCheck implements Predicate<String> {
      int count = 0;

      @Override
      public boolean test(String s) {
         count++;
         return s.contains("b");
      }

   }

}
