package com.helger.as2lib.crypto;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;

final class CanonicalizeLineEndingsOutputStream extends FilterOutputStream
{
  private boolean m_bLastCR = false;

  CanonicalizeLineEndingsOutputStream (@Nonnull final OutputStream aOS)
  {
    super (aOS);
  }

  @Override
  public void write (final int b) throws IOException
  {
    if (b == '\r')
      m_bLastCR = true;
    else
    {
      if (b == '\n')
      {
        // Always write \r\n
        out.write ('\r');
        out.write ('\n');
      }
      else
      {
        if (m_bLastCR)
        {
          // Mac mode (\r only) - write \r\n
          out.write ('\r');
          out.write ('\n');
        }
        out.write (b);
      }
      m_bLastCR = false;
    }
  }
}
