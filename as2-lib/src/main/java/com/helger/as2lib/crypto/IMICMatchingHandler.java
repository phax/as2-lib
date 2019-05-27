package com.helger.as2lib.crypto;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;

/**
 * A special handler if MIC is not matched.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public interface IMICMatchingHandler extends Serializable
{
  /**
   * Invoked upon MIC match
   *
   * @param aMsg
   *        The message it is all about. Never null
   * @param sMIC
   *        The MIC calculated and received. May not be <code>null</code>.
   * @throws OpenAS2Exception
   *         In case of error
   */
  void onMICMatch (@Nonnull IMessage aMsg, @Nonnull String sMIC) throws OpenAS2Exception;

  /**
   * Invoked upon MIC mismatch
   *
   * @param aMsg
   *        The message it is all about. Never null
   * @param sOriginalMIC
   *        The MIC calculated here. May be <code>null</code>.
   * @param sReceivedMIC
   *        The MIC received from the other side. May be <code>null</code>.
   * @throws OpenAS2Exception
   *         In case of error
   */
  void onMICMismatch (@Nonnull IMessage aMsg,
                      @Nullable String sOriginalMIC,
                      @Nullable String sReceivedMIC) throws OpenAS2Exception;
}
