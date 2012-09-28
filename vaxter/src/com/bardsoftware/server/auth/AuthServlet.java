/**
Copyright 2012 Dmitry Barashev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.bardsoftware.server.auth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.bardsoftware.server.AppUrlService;
import com.bardsoftware.server.auth.DefaultOAuthPlugin;
import com.google.common.base.Strings;

public class AuthServlet extends HttpServlet {
  private static final Logger LOGGER = Logger.getLogger("AuthService");
  private Properties properties;
  private final AppUrlService myUrlService;
  private final AuthService authService;
  private final boolean isDevMode;
  
  public AuthServlet(boolean devMode) {
    isDevMode = devMode;
    myUrlService = new AppUrlService(devMode);
    authService = new AuthService(myUrlService.getDomainName());
    this.properties = new Properties();
    loadProperties(this.properties, "/auth.properties");
    loadProperties(this.properties, "/auth.secret.properties");    
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String uri = req.getRequestURI();
    String[] components = uri.split("/");
    if (components.length != 3) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    Properties props = getProperties();
    String authProvider = components[2];
    if ("logout".equals(authProvider)) {
      doLogout(req, resp);
      return;
    }
    String keyIsEnabled = authProvider + ".enabled";
    if (!Boolean.TRUE.equals(Boolean.parseBoolean(props.getProperty(keyIsEnabled, "false")))) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    
    try {
      String pluginClass = props.getProperty(authProvider + ".class.plugin", DefaultOAuthPlugin.class.getName());
      DefaultOAuthPlugin plugin = (DefaultOAuthPlugin) Class.forName(pluginClass)
          .getConstructor(String.class, Properties.class).newInstance(authProvider, props);
      
      final String userDataJson;
      if ("dev".equals(authProvider)) {
        userDataJson = isDevMode ? doDevAuth(req, resp, plugin) : null;
      } else {
        userDataJson = doOauth(req, resp, authProvider, plugin);
      }
      if (userDataJson != null) {
        authService.remember(req, authService.getUserFromLoginService(userDataJson, plugin));
        resp.sendRedirect(myUrlService.buildUrlFromPath(""));
      }
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (InstantiationException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (IllegalAccessException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (InvocationTargetException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (NoSuchMethodException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (SecurityException e) {
      LOGGER.log(Level.SEVERE, "", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }
  
  private void doLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Principal p = authService.setupUserAndMaintenance(req);
    if (p != Principal.ANONYMOUS) {
      authService.logout(p, req, resp);
    }
    resp.sendRedirect(myUrlService.buildUrlFromPath(""));
  }

  private String doOauth(HttpServletRequest req, HttpServletResponse resp, String authProvider, DefaultOAuthPlugin plugin) 
      throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(plugin.getBuilderApiClass())
        .apiKey(plugin.getKey())
        .apiSecret(plugin.getSecret())
        .callback(myUrlService.buildUrlFromPath(req.getRequestURI()));
    if (plugin.getScope() != null) {
      serviceBuilder.scope(plugin.getScope());
    }
    OAuthService service = serviceBuilder.build();

    if (Strings.isNullOrEmpty(plugin.extractToken(req))) {
      // Obtain the Request Token
      if ("1.0".equals(service.getVersion())) { 
        Token requestToken = service.getRequestToken();
        req.getSession().setAttribute("request_token", requestToken);
        resp.sendRedirect(service.getAuthorizationUrl(requestToken));
        return null;
      } else if ("2.0".equals(service.getVersion())) {
        resp.sendRedirect(service.getAuthorizationUrl(null));
        return null;
      }
    }

    Verifier verifier = new Verifier(plugin.extractVerifier(req));
    Token requestToken = (Token) req.getSession().getAttribute("request_token");
    if ("1.0".equals(service.getVersion())) { 
      if (requestToken == null) {
        resp.sendRedirect(myUrlService.buildUrlFromPath(""));
        return null;
      }
    }
    try {
      Token accessToken = service.getAccessToken(requestToken, verifier);
      OAuthRequest request = new OAuthRequest(Verb.GET, plugin.buildRequest(accessToken.getRawResponse()));
      service.signRequest(accessToken, request);
      return request.send().getBody();      
    } catch (OAuthException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Problems connecting to " + authProvider);
      return null;
    }
  }

  private String doDevAuth(HttpServletRequest req, HttpServletResponse resp, DefaultOAuthPlugin plugin) throws IOException {
    String id = req.getParameter("id");
    String name = req.getParameter("name");
    if (Strings.isNullOrEmpty(id) || Strings.isNullOrEmpty(name)) {
      resp.sendRedirect(myUrlService.buildUrlFromPath(plugin.getProperty(".redirect")));
      return null;
    }
    return MessageFormat.format("'{'\"id\" : \"{0}\", \"name\" : \"{1}\"'}'", id, name);
  }
  
  private Properties getProperties() {
    return this.properties;
  }
  
  private static void loadProperties(Properties result, String resource) {
    URL url = AuthServlet.class.getResource(resource);
    if (url == null) {
      return;
    }
    try {
      result.load(url.openStream());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to load properties", e);
    }
  }
}
