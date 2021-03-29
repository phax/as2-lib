package com.helger.as2lib.util.http;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.crypto.MIC;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * Remote communication callback for easy logging of remove interactions.
 *
 * @author Philip Helger
 * @since 4.7.1
 */
public interface IAS2OutgoingHttpCallback
{
  /**
   * Notify on outgoing messages.
   *
   * @param bIsMessage
   *        <code>true</code> if it is a message that was sent out,
   *        <code>false</code> if it was an MDN.
   * @param sSenderAS2ID
   *        The AS2 ID of the sender. May be <code>null</code>.
   * @param sReceiverAS2ID
   *        The AS2 ID of the receiver. May be <code>null</code>.
   * @param sAS2MessageID
   *        The AS2 message ID of the outgoing message. May be
   *        <code>null</code>.
   * @param aMIC
   *        The MIC that was calculated for the message. Only set for messages.
   *        May be <code>null</code>.
   * @param eCTE
   *        The content transfer encoding uses for the message. Only set for
   *        messages. May be <code>null</code>.
   * @param aURL
   *        The URL the message was sent to. Never <code>null</code>.
   * @param nHttpResponseCode
   *        The HTTP response code received.
   */
  void onOutgoingHttpMessage (boolean bIsMessage,
                              @Nullable String sSenderAS2ID,
                              @Nullable String sReceiverAS2ID,
                              @Nullable String sAS2MessageID,
                              @Nullable MIC aMIC,
                              @Nullable EContentTransferEncoding eCTE,
                              @Nonnull String aURL,
                              int nHttpResponseCode);
}
