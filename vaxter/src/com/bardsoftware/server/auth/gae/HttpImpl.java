package com.bardsoftware.server.auth.gae;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.bardsoftware.server.HttpApi;

public class HttpImpl implements HttpApi {
  private final HttpServletRequest myRequest;
  private final HttpServletResponse myResponse;

  public HttpImpl(HttpServletRequest req, HttpServletResponse resp) {
    myRequest = req;
    myResponse = resp;
  }
  
  @Override
  public String getUsername() {
    HttpSession session = myRequest.getSession(false);
    return session == null ? null : (String)session.getAttribute("user_id");
  }

  @Override
  public void setUsername(String value) {
    myRequest.getSession().setAttribute("user_id", value);
  }

  @Override
  public void setCookie(String name, String domain, String path, int maxAge) {
    Cookie cookie = new Cookie(name, null);
    cookie.setDomain(domain);
    cookie.setPath(path);
    cookie.setMaxAge(maxAge);
    myResponse.addCookie(cookie);
  }

  @Override
  public String getRequestUrl() {
    return myRequest.getRequestURL().toString();
  }

  @Override
  public String getSessionId() {
    HttpSession session = myRequest.getSession(false);
    return session == null ? null : session.getId();
  }

  @Override
  public boolean hasSession() {
    return myRequest.getSession(false) != null;
  }

  @Override
  public void setRequestAttribute(String name, Object value) {
    myRequest.setAttribute(name, value);
  }

  @Override
  public void sendRedirect(String url) throws IOException {
    myResponse.sendRedirect(url);
  }

  @Override
  public String getPath() {
    return myRequest.getRequestURI();
  }

  @Override
  public void sendError(int code) throws IOException {
    myResponse.sendError(code);
  }

  @Override
  public String getUrlParameter(String name) {
    return myRequest.getParameter(name);
  }

  @Override
  public Object getSessionAttribute(String name) {
    return myRequest.getSession().getAttribute(name);
  }

  @Override
  public void setSessionAttribute(String name, Object value) {
    myRequest.getSession().setAttribute(name, value);
  }
}
