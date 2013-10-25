package com.bardsoftware.server.auth.awsplay;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import play.cache.Cache;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Results;

import com.bardsoftware.server.HttpApi;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

public class HttpApiImpl implements HttpApi {
  private static final Logger LOGGER = Logger.getLogger("HttpApiImpl");
  private static final String SESSION_ID_KEY = "JSESSIONID";

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
    String scheme = Objects.firstNonNull(myRequest.getHeader("X-Scheme"), "http");
    return String.format("%s://%s%s", scheme, myRequest.host(), myRequest.uri());
  }

  @Override
  public String getUrlParameter(String name) {
    String[] values = myRequest.queryString().get(name);
    return values == null ? null : Joiner.on(',').join(values);
  }

  @Override
  public String getHost() {
    return myRequest.host();
  }

  @Override
  public String getPath() {
    return myRequest.path();
  }

  @Override
  public String getSessionId() {
    return mySession.get(SESSION_ID_KEY);
  }

  @Override
  public boolean hasSession() {
    return mySession.get(SESSION_ID_KEY) != null;
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
    String sessionId = mySession.get(SESSION_ID_KEY);
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(String.format("getting attribute: session_id=%s", sessionId));
    }
    if (sessionId == null) {
      return null;
    }
    String attrKey = sessionId + "." + name;
    Object attrVal = Cache.get(attrKey);
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(String.format("getting attribute: got key=%s value=%s", attrKey, attrVal));
    }
    return attrVal;
  }

  @Override
  public void setSessionAttribute(String name, Object object) {
    String sessionId = mySession.get(SESSION_ID_KEY);
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      mySession.put(SESSION_ID_KEY, sessionId);
    }
    String attrKey = sessionId + "." + name;
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(String.format("setting attribute: key=%s value=%s", attrKey, object));
    }
    Cache.set(attrKey, object);
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
