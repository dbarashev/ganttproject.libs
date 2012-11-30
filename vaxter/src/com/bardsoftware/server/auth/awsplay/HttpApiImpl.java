package com.bardsoftware.server.auth.awsplay;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import play.cache.Cache;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Results;

import com.bardsoftware.server.HttpApi;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class HttpApiImpl implements HttpApi {
  private final Request myRequest;
  private final Response myResponse;
  private final Map<String, Object> myRequestData = Maps.newHashMap();
  private Result myResult;
  private final Session mySession;
  
  public HttpApiImpl(Http.Request req, Http.Response resp, Http.Session session) {
    myRequest = req;
    myResponse = resp;
    mySession = session;
  }
  @Override
  public String getRequestUrl() {
    return myRequest.uri();
  }

  @Override
  public String getUrlParameter(String name) {
    String[] values = myRequest.queryString().get(name);
    return values == null ? null : Joiner.on(',').join(values);
  }

  @Override
  public String getPath() {
    return myRequest.path();
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public boolean hasSession() {
    return mySession.get("JSESSIONID") != null;
  }

  @Override
  public String getUsername() {
    Object uidAttribute = getSessionAttribute("uid");
    return uidAttribute == null ? null : String.valueOf(uidAttribute);
  }

  @Override
  public void setUsername(String value) {
    setSessionAttribute("uid", value);
  }

  @Override
  public Object getSessionAttribute(String name) {
    String sessionId = mySession.get("JSESSIONID");
    if (sessionId == null) {
      return null;
    }
    return Cache.get(sessionId + "." + name);
  }

  @Override
  public void setSessionAttribute(String name, Object object) {
    String sessionId = mySession.get("JSESSIONID");
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      mySession.put("JSESSIONID", sessionId);
    }
    Cache.set(sessionId + "." + name, object);
  }

  @Override
  public void clearSession() {
    mySession.clear();
  }

  @Override
  public void setRequestAttribute(String name, Object value) {
    myRequestData.put(name, value);
  }

  @Override
  public void sendRedirect(String url) throws IOException {
    myResult = Results.redirect(url);
  }

  @Override
  public void sendError(int code) throws IOException {
    myResult = Results.status(code);
  }
  
  public Result getResult() {
    return myResult == null ? Results.ok() : myResult;
  }
}