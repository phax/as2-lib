package com.helger.as2lib.util.http;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

class MockAS2InputStreamProvider implements IAS2InputStreamProvider
{
  private final InputStream m_aIS;

  public MockAS2InputStreamProvider (@Nonnull final InputStream aIS)
  {
    m_aIS = aIS;
  }

  @Nonnull
  public InputStream getInputStream () throws IOException
  {
    return m_aIS;
  }

  @Nonnull
  public InputStream getNonUpwardClosingInputStream () throws IOException
  {
    return m_aIS;
  }
}