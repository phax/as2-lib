/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2servlet.util;

import java.io.File;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IMessageProcessor;
import com.helger.as2lib.processor.module.IProcessorModule;
import com.helger.as2lib.session.AS2Session;
import com.helger.as2lib.util.XMLHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.serialize.MicroReader;

/**
 * A special {@link AS2Session} that loads its configuration from a file.
 *
 * @author Philip Helger
 */
public final class AS2ServletSession extends AS2Session
{
  public static final String EL_CERTIFICATES = "certificates";
  public static final String EL_PROCESSOR = "processor";
  public static final String EL_PARTNERSHIPS = "partnerships";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2ServletSession.class);

  private final String m_sBaseDirectory;

  public AS2ServletSession (@Nonnull final File aFile) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aFile, "File");
    if (!aFile.exists ())
      throw new OpenAS2Exception ("AS2Session configuration file " + aFile.getAbsolutePath () + " does not exist!");
    m_sBaseDirectory = aFile.getParentFile ().getAbsolutePath ();
    s_aLogger.info ("Loading AS2 configuration file '" + aFile.getAbsolutePath ());
    _load (FileHelper.getInputStream (aFile));
  }

  @Nonnull
  public String getBaseDirectory ()
  {
    return m_sBaseDirectory;
  }

  private void _loadCertificateFactory (@Nonnull final IMicroElement aElement) throws OpenAS2Exception
  {
    s_aLogger.info ("Loading certificates");
    final ICertificateFactory aFactory = XMLHelper.createComponent (aElement, ICertificateFactory.class, this, m_sBaseDirectory);
    setCertificateFactory (aFactory);
  }

  private void _loadPartnershipFactory (final IMicroElement eRootNode) throws OpenAS2Exception
  {
    s_aLogger.info ("Loading partnerships");
    final IPartnershipFactory aFactory = XMLHelper.createComponent (eRootNode, IPartnershipFactory.class, this, m_sBaseDirectory);
    setPartnershipFactory (aFactory);
  }

  private void _loadProcessorModule (@Nonnull final IMessageProcessor aMsgProcessor, @Nonnull final IMicroElement eModule) throws OpenAS2Exception
  {
    final IProcessorModule aProcessorModule = XMLHelper.createComponent (eModule, IProcessorModule.class, this, m_sBaseDirectory);
    aMsgProcessor.addModule (aProcessorModule);
    s_aLogger.info ("  Loaded processor module " + aProcessorModule.getName ());
  }

  private void _loadMessageProcessor (final IMicroElement eRootNode) throws OpenAS2Exception
  {
    s_aLogger.info ("Loading message processor");
    final IMessageProcessor aMsgProcessor = XMLHelper.createComponent (eRootNode, IMessageProcessor.class, this, m_sBaseDirectory);
    setMessageProcessor (aMsgProcessor);

    for (final IMicroElement eModule : eRootNode.getAllChildElements ("module"))
      _loadProcessorModule (aMsgProcessor, eModule);
  }

  private void _load (@Nonnull @WillClose final InputStream aIS) throws OpenAS2Exception
  {
    final IMicroDocument aDoc = MicroReader.readMicroXML (aIS);
    final IMicroElement eRoot = aDoc.getDocumentElement ();

    for (final IMicroElement eRootChild : eRoot.getAllChildElements ())
    {
      final String sNodeName = eRootChild.getTagName ();

      if (sNodeName.equals (EL_CERTIFICATES))
        _loadCertificateFactory (eRootChild);
      else
        if (sNodeName.equals (EL_PROCESSOR))
          _loadMessageProcessor (eRootChild);
        else
          if (sNodeName.equals (EL_PARTNERSHIPS))
            _loadPartnershipFactory (eRootChild);
          else
            throw new OpenAS2Exception ("Undefined tag: " + sNodeName);
    }
  }
}
