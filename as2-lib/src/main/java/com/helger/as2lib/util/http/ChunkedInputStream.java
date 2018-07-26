package com.helger.as2lib.util.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream to read a chuncked body stream. Input stream should be at the
 * beginning of a chunk, i.e. at the body beginning (after the end of headers
 * marker). THe resulting stream reads the data through the chunks.
 *
 * @author Ziv Harpaz
 */
public class ChunkedInputStream extends FilterInputStream
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ChunkedInputStream.class);
  /*
   * Number of bytes left in current chunk
   */
  private int m_nLeft = 0;
  private final InputStream m_aIS;
  private boolean m_bAfterFirstChunk = false;

  public ChunkedInputStream (@Nonnull final InputStream aIS)
  {
    super (aIS);
    m_aIS = aIS;
  }

  @Override
  public final int read () throws IOException
  {
    if (m_nLeft < 0)
      return -1;

    if (m_nLeft == 0)
    {
      if (m_bAfterFirstChunk)
      {
        // read the CRLF after chunk data
        HTTPHelper.readTillNexLine (m_aIS);
      }
      else
      {
        m_bAfterFirstChunk = true;
      }

      m_nLeft = HTTPHelper.readChunkLen (m_aIS);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Read chunk size: " + m_nLeft);

      // check for end of data
      if (m_nLeft <= 0)
      {
        // No more chunks means EOF
        m_nLeft = -1;
        // mark end of stream
        return -1;
      }
    }
    m_nLeft--;
    return super.read ();
  }

  @Override
  public final int read (@Nonnull final byte [] aBuf, final int nOffset, final int nLength) throws IOException
  {
    if (m_nLeft < 0)
      return -1;
    int nReadCount = 0;
    int nRealOffset = nOffset;
    while (nLength > nReadCount)
    {
      if (m_nLeft == 0)
      {
        if (m_bAfterFirstChunk)
        {
          // read the CRLF after chunk data
          HTTPHelper.readTillNexLine (m_aIS);
        }
        else
        {
          m_bAfterFirstChunk = true;
        }

        m_nLeft = HTTPHelper.readChunkLen (m_aIS);
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Read chunk size: " + m_nLeft);

        // check for end of data
        if (m_nLeft <= 0)
        {
          // No more chunks means EOF
          m_nLeft = -1;
          // mark end of stream
          return nReadCount > 0 ? nReadCount : -1;
        }
      }

      final int ret = super.read (aBuf, nRealOffset, Math.min (nLength - nReadCount, m_nLeft));
      nRealOffset += ret;
      m_nLeft -= ret;
      nReadCount += ret;
    }
    return nReadCount;
  }
}
