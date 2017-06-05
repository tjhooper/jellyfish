package com.ngc.seaside.bootstrap.service.impl.promptuserservice;

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
public class PromptUserServiceDelegateTest {
   private static PrintStream SYSTEM_OUT;

   private PromptUserServiceDelegate delegate;

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
      delegate = new PromptUserServiceDelegate();
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

   private class ValidCheck implements Predicate<String> {
      int count = 0;

      @Override
      public boolean test(String s) {
         count++;
         return s.contains("b");
      }

   }

}
