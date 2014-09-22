package com.helger.as2lib.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.annotation.Nonnull;

import com.helger.commons.io.streams.StreamUtils;

public final class AS2OutputStreamCreatorSocket implements IAS2OutputStreamCreator
{
  private final Socket m_aSocket;

  public AS2OutputStreamCreatorSocket (@Nonnull final Socket aSocket)
  {
    m_aSocket = aSocket;
  }

  @Nonnull
  public OutputStream createOutputStream () throws IOException
  {
    return StreamUtils.getBuffered (m_aSocket.getOutputStream ());
  }
}