package com.bardsoftware.server.auth;

public interface PrincipalExtent {
  Principal find(String id);
  Principal create(String id, String name);
}
