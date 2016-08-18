package com.helger.as2lib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * This is a dummy class to test for generic exceptions in combinations with
 * Lambdas
 *
 * @author Philip Helger
 */
public class LambdaExceptionDeductionFuncTest
{
  @FunctionalInterface
  public static interface IBaseGetter
  {
    String get () throws Exception;
  }

  @FunctionalInterface
  public static interface IThrowingGetter <EX extends Exception> extends IBaseGetter
  {
    String get () throws EX;
  }

  @FunctionalInterface
  public static interface ICreator <EX extends Exception>
  {
    IThrowingGetter <EX> create ();
  }

  public static void main (final String [] args) throws IOException
  {
    final InputStream aIS = new ByteArrayInputStream ("abc".getBytes (StandardCharsets.UTF_8));
    final IThrowingGetter <IOException> aGetter = () -> Character.toString ((char) aIS.read ());
    final ICreator <IOException> aCreator = () -> aGetter;
    // Expected outcome: ab
    System.out.println (aCreator.create ().get () + aCreator.create ().get ());
  }
}
