package com.bardsoftware.server;

import java.io.IOException;

public interface HttpApi {
  String getRequestUrl();
  String getHost();
  String getUrlParameter(String name);

  String getPath();
  String getSessionId();
  boolean hasSession();
  String getUsername();
  void setUsername(String value);
  Object getSessionAttribute(String name);
  void setSessionAttribute(String name, Object object);

  void clearSession();


  void setRequestAttribute(String name, Object value);
  void sendRedirect(String buildUrlFromPath) throws IOException;

  void sendError(int code) throws IOException;
}