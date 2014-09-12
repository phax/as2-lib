package com.helger.as2lib.partner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.collections.ContainerHelper;
import com.helger.commons.state.EChange;

public final class PartnerMap implements IPartnerMap
{
  private final Map <String, StringMap> m_aMap = new LinkedHashMap <String, StringMap> ();

  public void add (@Nonnull final StringMap aNewPartner) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aNewPartner, "NewPartner");
    final String sName = aNewPartner.getAttributeAsString ("name");
    if (m_aMap.containsKey (sName))
      throw new OpenAS2Exception ("Partner is defined more than once: '" + sName + "'");

    m_aMap.put (sName, aNewPartner);
  }

  public void set (@Nonnull final PartnerMap aPartners)
  {
    m_aMap.clear ();
    m_aMap.putAll (aPartners.m_aMap);
  }

  @Nonnull
  public EChange removePartner (@Nullable final String sPartnerName)
  {
    return EChange.valueOf (m_aMap.remove (sPartnerName) != null);
  }

  @Nullable
  public StringMap getPartnerOfName (@Nullable final String sPartnerName)
  {
    return m_aMap.get (sPartnerName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPartnerNames ()
  {
    return ContainerHelper.newOrderedSet (m_aMap.keySet ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <StringMap> getAllPartners ()
  {
    return ContainerHelper.newList (m_aMap.values ());
  }
}
