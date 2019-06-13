package com.helger.as2lib.util.http;

import com.helger.as2lib.message.IBaseMessage;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.commons.functional.IFunction;

/**
 * Factory interface for creating {@link IHTTPOutgoingDumper} objects.
 *
 * @author Philip Helger
 * @since v4.4.0
 */
public interface IHTTPOutgoingDumperFactory extends IFunction <IBaseMessage, IHTTPOutgoingDumper>
{
  /* empty */
}
