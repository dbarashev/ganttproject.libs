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
  private static final Properties ourProperties = new Properties();
  private final AppUrlService myUrlService;
  private final AuthService authService;
  private final Properties myProperties;

  static {
    loadProperties(ourProperties, "/auth.properties");
    loadProperties(ourProperties, "/auth.secret.properties");
  }

  public static Properties getDefaultProperties() {
    return ourProperties;
  }

  public AuthServlet(PrincipalExtent principalExtent, AppCapabilitiesService capabilities, AppUrlService urlService, Properties properties) {
    myUrlService = urlService;
    authService = new AuthService(principalExtent, capabilities);
    myProperties = properties;
  }

  protected DefaultOAuthPlugin getOauthPlugin(String authProvider) {
    return getOauthPlugin(authProvider, getProperties());
  }

  protected static DefaultOAuthPlugin getOauthPlugin(String authProvider, Properties props) {
    try {
      String keyIsEnabled = authProvider + ".enabled";
      if (!Boolean.TRUE.equals(Boolean.parseBoolean(props.getProperty(keyIsEnabled, "false")))) {
        return null;
      }
      String pluginClass = props.getProperty(authProvider + ".class.plugin", DefaultOAuthPlugin.class.getName());
      return (DefaultOAuthPlugin) Class.forName(pluginClass).getConstructor(String.class, Properties.class).newInstance(authProvider, props);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      LOGGER.log(Level.SEVERE, "Failed to create OAuth plugin", e);
      return null;
    }
  }

  public void doGet(HttpApi http) throws IOException {
    String uri = http.getPath();
    String[] components = uri.split("/");
    if (components.length != 3) {
      http.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String authProvider = components[2];
    if ("logout".equals(authProvider)) {
      doLogout(http);
      return;
    }

    DefaultOAuthPlugin plugin = getOauthPlugin(authProvider);
    if (plugin == null) {
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
    try {

      final String userDataJson;
      if ("dev".equals(authProvider)) {
        userDataJson = doDevAuth(http, plugin);
      } else {
        userDataJson = doOauth(http, authProvider, plugin);
      }
      if (userDataJson != null) {
        authService.remember(http, authService.getUserFromLoginService(userDataJson, plugin));
        http.sendRedirect(myUrlService.getUrl("oauth.complete", http));
      }
    } catch (ClassNotFoundException e) {
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      LOGGER.log(Level.SEVERE, "", e);
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
    return doOauthWithCallback(http, plugin, myUrlService.getUrl("oauth.callback", http) + authProvider);
  }

  protected String doOauthWithCallback(HttpApi http, DefaultOAuthPlugin plugin, String callback)
      throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    ServiceBuilder serviceBuilder = new ServiceBuilder()
        .provider(plugin.getBuilderApiClass())
        .apiKey(plugin.getKey())
        .apiSecret(plugin.getSecret())
        .callback(callback);
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

  protected Properties getProperties() {
    return myProperties;
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
