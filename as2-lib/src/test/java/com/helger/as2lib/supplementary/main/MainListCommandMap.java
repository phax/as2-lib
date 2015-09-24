package com.helger.as2lib.supplementary.main;

import javax.activation.CommandInfo;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import com.helger.commons.collection.CollectionHelper;

public final class MainListCommandMap
{
  public static void main (final String [] args)
  {
    listCommandMap ();
  }

  public static void listCommandMap ()
  {
    final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    for (final String sMimeType : CollectionHelper.getSorted (aCommandMap.getMimeTypes ()))
    {
      System.out.println (sMimeType);
      for (final CommandInfo aCI : aCommandMap.getAllCommands (sMimeType))
      {
        System.out.println ("  CommandInfo:");
        System.out.println ("    " + aCI.getCommandClass ());
        System.out.println ("    " + aCI.getCommandName ());
      }
    }
  }
}
