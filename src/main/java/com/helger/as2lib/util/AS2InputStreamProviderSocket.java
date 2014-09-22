package com.helger.as2lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.annotation.Nonnull;

import com.helger.commons.io.streams.StreamUtils;

public final class AS2InputStreamProviderSocket implements IAS2InputStreamProvider
{
  private final Socket m_aSocket;

  public AS2InputStreamProviderSocket (@Nonnull final Socket aSocket)
  {
    m_aSocket = aSocket;
  }

  public InputStream getInputStream () throws IOException
  {
    return StreamUtils.getBuffered (m_aSocket.getInputStream ());
  }
}