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
public class LambdaExceptionDeduction2FuncTest
{
  @FunctionalInterface
  public static interface IBaseGetter <T>
  {
    T get () throws Exception;
  }

  @FunctionalInterface
  public static interface IThrowingGetter <T, EX extends Exception> extends IBaseGetter <T>
  {
    T get () throws EX;
  }

  public static <T, EX extends Exception> T execLogged (final IThrowingGetter <T, EX> aGetter) throws EX
  {
    final T ret = aGetter.get ();
    System.out.println ("Returned: " + ret);
    return ret;
  }

  public static void main (final String [] args) throws IOException
  {
    final InputStream aIS = new ByteArrayInputStream ("abc".getBytes (StandardCharsets.UTF_8));
    final IThrowingGetter <String, IOException> aGetter = () -> Character.toString ((char) aIS.read ());
    // Expected outcome: ab
    System.out.println (execLogged (aGetter) + execLogged (aGetter));
  }
}
