package com.bardsoftware.server.auth.gae;

import com.bardsoftware.server.auth.Principal;
import com.bardsoftware.server.auth.PrincipalExtent;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class PrincipalExtentImpl implements PrincipalExtent {
  static {
    ObjectifyService.register(PrincipalEntity.class);
  }

  @Override
  public Principal find(String id) {
    Objectify ofy = ObjectifyService.ofy();
    PrincipalEntity entity = ofy.load().type(PrincipalEntity.class).id(id).now();
    return entity == null ? null : new PrincipalGae(entity);
  }

  @Override
  public Principal create(String id, String name) {
    return new PrincipalGae(id, name);
  }

}
