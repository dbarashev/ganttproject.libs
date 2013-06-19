package com.bardsoftware.server.auth.gae;

import com.bardsoftware.server.AppUrlService;
import com.bardsoftware.server.HttpApi;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpImpl implements HttpApi {
  private final HttpServletRequest myRequest;
  private final HttpServletResponse myResponse;
  private final AppUrlService myUrlService;

  public HttpImpl(HttpServletRequest req, HttpServletResponse resp, AppUrlService urlService) {
    myRequest = req;
    myResponse = resp;
    myUrlService = urlService;
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
  public String getHost() {
    try {
      return new URL(myRequest.getRequestURL().toString()).getHost();
    } catch (MalformedURLException e) {
      return null;
    }
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

  @Override
  public void clearSession() {
    Cookie cookie = new Cookie("JSESSIONID", null);
    cookie.setDomain(myUrlService.getDomainName(this));
    cookie.setPath("/");
    cookie.setMaxAge(0);
    myResponse.addCookie(cookie);
  }
}
