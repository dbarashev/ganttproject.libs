package com.bardsoftware.server.auth;

public interface Principal {

  Principal ANONYMOUS = new Principal() {
    @Override
    public String getID() {
      return "@anonymous";
    }
    @Override
    public void save() {
    }
    @Override
    public String getNickname() {
      return "Anonymous";
    }
  };

  String getID();

  void save();

  String getNickname();
}
