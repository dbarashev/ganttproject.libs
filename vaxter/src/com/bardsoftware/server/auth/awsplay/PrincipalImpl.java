package com.bardsoftware.server.auth.awsplay;

import com.bardsoftware.server.auth.Principal;

public class PrincipalImpl implements Principal {
  private String myId;
  private String myName;

  PrincipalImpl(String id, String name) {
    myId = id;
    myName = name;
  }
  
  @Override
  public String getID() {
    return myId;
  }

  @Override
  public void save() {
  }

  @Override
  public String getNickname() {
    return myName;
  }
}
