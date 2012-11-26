package com.bardsoftware.server;

import java.io.IOException;

public interface HttpApi {
  String getRequestUrl();
  String getUrlParameter(String name);
  String getPath();

  String getSessionId();
  boolean hasSession();
  String getUsername();
  void setUsername(String value);
  Object getSessionAttribute(String name);
  void setSessionAttribute(String name, Object object);
  
  void setRequestAttribute(String name, Object value);
  
  void setCookie(String name, String domain, String path, int maxAge);
  void sendRedirect(String buildUrlFromPath) throws IOException;
  void sendError(int code) throws IOException;
}