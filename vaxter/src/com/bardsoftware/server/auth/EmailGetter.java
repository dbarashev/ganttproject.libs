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

/**
 * This servlet will run OAuth process similar to one that AuthServlet runs,
 * but with additional email scope. It assumes that user is already authenticated
 * with OAuth, and the only thing which is needed is to get his email.
 *
 * @author gkalabin@bardsoftware.com
 * @author dbarashev@bardsoftware.com
 */
public class EmailGetter extends AuthServlet {
  private static final String EMAIL_SCOPE_KEY_FORMAT = "%s.email.scope";
  private static final Logger LOGGER = Logger.getLogger("EmailGetter");

  public EmailGetter(PrincipalExtent principalExtent, AppCapabilitiesService capabilities, AppUrlService urlService, Properties props) {
    super(principalExtent, capabilities, urlService, props);
  }

  public String getEmail(String authProvider, HttpApi http, String callback) throws IOException {
    try {
      Properties props = getProperties();
      String emailScope = props.getProperty(String.format(EMAIL_SCOPE_KEY_FORMAT, authProvider));
      if (emailScope == null) {
        throw new UnsupportedOperationException("There is no email scope for this provider");
      }
      DefaultOAuthPlugin plugin = getOauthPlugin(authProvider);
      if (plugin == null) {
        http.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return null;
      }
      plugin.setScope(emailScope);

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

  public static boolean isEmailSupported(String authProvider, Properties props) {
    String emailScope = props.getProperty(String.format(EMAIL_SCOPE_KEY_FORMAT, authProvider));
    return emailScope != null && getOauthPlugin(authProvider, props).isEmailSupported();
  }
}
