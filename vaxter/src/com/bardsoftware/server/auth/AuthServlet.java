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

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppUrlService;
import com.bardsoftware.server.HttpApi;
import com.google.common.base.Strings;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

//import com.bardsoftware.server.auth.gae.PrincipalGae;

public class AuthServlet {
  private static final Logger LOGGER = Logger.getLogger("AuthService");
  private Properties properties;
  private final AppUrlService myUrlService;
  private final AuthService authService;
  private final boolean isDevMode;

  public AuthServlet(boolean devMode, PrincipalExtent principalExtent, AppCapabilitiesService capabilities, AppUrlService urlService) {
    isDevMode = devMode;
    myUrlService = urlService;
    authService = new AuthService(principalExtent, capabilities);
    this.properties = new Properties();
    loadProperties(this.properties, "/auth.properties");
    loadProperties(this.properties, "/auth.secret.properties");
  }

  public void doGet(HttpApi http) throws IOException {
    String uri = http.getPath();
    String[] components = uri.split("/");
    if (components.length != 3) {
      http.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    Properties props = getProperties();
    String authProvider = components[2];
    if ("logout".equals(authProvider)) {
      doLogout(http);
      return;
    }
    String keyIsEnabled = authProvider + ".enabled";
    if (!Boolean.TRUE.equals(Boolean.parseBoolean(props.getProperty(keyIsEnabled, "false")))) {
      http.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }


    try {
      String pluginClass = props.getProperty(authProvider + ".class.plugin", DefaultOAuthPlugin.class.getName());
      DefaultOAuthPlugin plugin = (DefaultOAuthPlugin) Class.forName(pluginClass)
          .getConstructor(String.class, Properties.class).newInstance(authProvider, props);

      final String userDataJson;
      if ("dev".equals(authProvider)) {
        userDataJson = isDevMode ? doDevAuth(http, plugin) : null;
      } else {
        userDataJson = doOauth(http, authProvider, plugin);
      }
      if (userDataJson != null) {

        authService.remember(http, authService.getUserFromLoginService(userDataJson, plugin));
        http.sendRedirect(myUrlService.getUrl("oauth.complete", http));
      }
    } catch (ClassNotFoundException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (InstantiationException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (IllegalAccessException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (InvocationTargetException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (NoSuchMethodException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (SecurityException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }

  private void doLogout(HttpApi http) throws IOException {
    Principal p = authService.setupUserAndMaintenance(http);
    if (p != Principal.ANONYMOUS) {
      authService.logout(p, http);
    }
    http.sendRedirect(myUrlService.getUrl("logout", http));
  }

  private String doOauth(HttpApi http, String authProvider, DefaultOAuthPlugin plugin)
      throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(plugin.getBuilderApiClass())
        .apiKey(plugin.getKey())
        .apiSecret(plugin.getSecret())
        .callback(myUrlService.getUrl("oauth.callback", http) + authProvider);
    if (plugin.getScope() != null) {
      serviceBuilder.scope(plugin.getScope());
    }
    OAuthService service = serviceBuilder.build();

    if (Strings.isNullOrEmpty(plugin.extractToken(http))) {
      // Obtain the Request Token
      if ("1.0".equals(service.getVersion())) {
        Token requestToken = service.getRequestToken();
        http.setSessionAttribute("request_token", requestToken);
        http.sendRedirect(service.getAuthorizationUrl(requestToken));
        return null;
      } else if ("2.0".equals(service.getVersion())) {
        http.sendRedirect(service.getAuthorizationUrl(null));
        return null;
      }
    }

    Verifier verifier = new Verifier(plugin.extractVerifier(http));
    Token requestToken = (Token) http.getSessionAttribute("request_token");
    if ("1.0".equals(service.getVersion())) {
      if (requestToken == null) {
        http.sendRedirect(myUrlService.getUrl("oauth.failure", http));
        return null;
      }
    }
    try {
      Token accessToken = service.getAccessToken(requestToken, verifier);
      OAuthRequest request = new OAuthRequest(Verb.GET, plugin.buildRequest(accessToken.getRawResponse()));
      service.signRequest(accessToken, request);
      return request.send().getBody();
    } catch (OAuthException e) {
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
  }

  private String doDevAuth(HttpApi http, DefaultOAuthPlugin plugin) throws IOException {
    String id = http.getUrlParameter("id");
    String name = http.getUrlParameter("name");
    if (Strings.isNullOrEmpty(id) || Strings.isNullOrEmpty(name)) {
      http.sendRedirect(myUrlService.buildUrlFromPath(plugin.getProperty(".redirect"), http));
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
