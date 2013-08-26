// Copyright (C) 2013 BarD Software
package com.bardsoftware.server.auth;

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppUrlService;
import com.bardsoftware.server.HttpApi;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailGetter extends AuthServlet {
  private static final String AUTH_SCOPE_KEY_FORMAT = "%s.auth.scope";
  private static final String EMAIL_SCOPE_KEY_FORMAT = "%s.email.scope";
  private static final Logger LOGGER = Logger.getLogger("EmailGetter");

  public EmailGetter(PrincipalExtent principalExtent, AppCapabilitiesService capabilities, AppUrlService urlService) {
    super(false, principalExtent, capabilities, urlService);
  }
  
  public String getEmail(String authProvider, HttpApi http, String callback) throws IOException {
    Properties props = getProperties();
    String keyIsEnabled = authProvider + ".enabled";
    if (!Boolean.TRUE.equals(Boolean.parseBoolean(props.getProperty(keyIsEnabled, "false")))) {
      http.sendError(HttpServletResponse.SC_FORBIDDEN);
      return null;
    }

    try {
      String pluginClass = props.getProperty(authProvider + ".class.plugin", DefaultOAuthPlugin.class.getName());
      Properties properties = new Properties(props);
      String emailScope = props.getProperty(String.format(EMAIL_SCOPE_KEY_FORMAT, authProvider));
      if (emailScope == null) {
        throw new UnsupportedOperationException("There is no email scope for this provider");
      }
      // override auth scope
      properties.setProperty(String.format(AUTH_SCOPE_KEY_FORMAT, authProvider), emailScope);
      DefaultOAuthPlugin plugin = (DefaultOAuthPlugin) Class.forName(pluginClass)
          .getConstructor(String.class, Properties.class).newInstance(authProvider, properties);

      final String userDataJson = doOauthWithCallback(http, plugin, callback);
      if (userDataJson != null) {
        LOGGER.log(Level.FINE, "json from login service=" + userDataJson);
        JSONObject json = new JSONObject(userDataJson);
        if (plugin.isResponseOk(json)) {
          return plugin.getEmail(json);
        }
      }
      return null;
    } catch (JSONException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | 
        InvocationTargetException | NoSuchMethodException | SecurityException e) {
      LOGGER.log(Level.SEVERE, "", e);
      http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
  }
}
