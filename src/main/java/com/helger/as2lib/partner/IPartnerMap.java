package com.helger.as2lib.partner;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.util.StringMap;
import com.helger.commons.annotations.ReturnsMutableCopy;

public interface IPartnerMap
{
  @Nullable
  StringMap getPartnerOfName (@Nullable String sPartnerName);

  @Nonnull
  @ReturnsMutableCopy
  Set <String> getAllPartnerNames ();

  @Nonnull
  @ReturnsMutableCopy
  List <StringMap> getAllPartners ();
}
