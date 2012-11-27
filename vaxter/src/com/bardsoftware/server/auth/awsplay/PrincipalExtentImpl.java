package com.bardsoftware.server.auth.awsplay;

import java.util.Map;

import com.bardsoftware.server.auth.Principal;
import com.bardsoftware.server.auth.PrincipalExtent;
import com.google.common.collect.Maps;

public class PrincipalExtentImpl implements PrincipalExtent {
  private final Map<String, Principal> myPrincipals = Maps.newConcurrentMap();
  
  @Override
  public Principal find(String id) {
    return myPrincipals.get(id);
  }

  @Override
  public Principal create(String id, String name) {
    PrincipalImpl result = new PrincipalImpl(id, name);
    myPrincipals.put(id, result);
    return result;
  }
}
