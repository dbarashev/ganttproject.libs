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

import com.bardsoftware.server.HttpApi;
import com.google.common.base.Function;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.api.Api;

import java.util.Properties;

public class DefaultOAuthPlugin implements OAuthPlugin {
  /**
   * Separator between authentication provider and provider's user id in formatted user id
   */
  private static final String ID_SEPARATOR = ":::";

  public static Function<JSONObject, String> createIdFormatter(final String authProvider, final String idAttr) {
    return new Function<JSONObject, String>() {
      @Override
      public String apply(JSONObject json) {
        try {
          return buildId(authProvider, json.getString(idAttr));
        } catch (JSONException e) {
          return null;
        }
      }
    };
  }

  /**
   * Create id by authentication service name and user user id in that service
   *
   * @param authService name of the authentication service
   * @param userId      id of the user in that service
   * @return formatted id for the user
   */
  public static String buildId(String authService, String userId) {
    return authService + ID_SEPARATOR + userId;
  }

  /**
   * Parse formatted id by provider's name and user id in provider's service
   *
   * @param rawId id which should be parsed
   * @return array with two elements - first is provider's name, second - user id
   * @throws IllegalArgumentException if id has wrong format
   * @see DefaultOAuthPlugin#isIdValid(String)
   */
  public static String[] parseId(String rawId) {
    if (!isIdValid(rawId)) {
      throw new IllegalArgumentException("Id has invalid format");
    }
    int separatorIdx = rawId.indexOf(ID_SEPARATOR);
    int idStartIdx = separatorIdx + ID_SEPARATOR.length();
    String provider = rawId.substring(0, separatorIdx);
    String id = rawId.substring(idStartIdx);
    return new String[] {provider, id};
  }

  /**
   * Check is provided id has correct format
   * @param id to test
   * @return true, if id has correct format, false otherwise
   */
  public static boolean isIdValid(String id) {
    if (id == null) {
      return false;
    }
    int separatorIdx = id.indexOf(ID_SEPARATOR);
    int idStartIdx = separatorIdx + ID_SEPARATOR.length();
    return separatorIdx > 0 && idStartIdx < id.length();
  }

  private final Function<JSONObject, String> myIdFormatter;
  private final String myIdAttr;
  private final String myFullNameAttr;
  private final String myFirstNameAttr;
  private final String myLastNameAttr;
  private final Properties myConfig;
  private final String myAuthService;
  // nullable. If null then this plugin can't provide user email
  private final String myEmailAttr;
  private final String myEmailValidAttr;

  public DefaultOAuthPlugin(String authService, Properties config) {
    myAuthService = authService;
    myConfig = new Properties(config);
    myIdAttr = myConfig.getProperty(authService + ".json.id");
    myFullNameAttr = myConfig.getProperty(authService + ".json.full_name");
    myFirstNameAttr = myConfig.getProperty(authService + ".json.first_name");
    myLastNameAttr = myConfig.getProperty(authService + ".json.last_name");
    myEmailAttr = myConfig.getProperty(authService + ".json.email");
    myEmailValidAttr = myConfig.getProperty(authService + ".json.email_valid");
    myIdFormatter = createIdFormatter(authService, myIdAttr);
  }

  @Override
  public String createUserId(JSONObject json) throws JSONException {
    return myIdFormatter.apply(json);
  }

  public String getEmail(JSONObject json) throws JSONException {
    if (!isEmailSupported()) {
      throw new UnsupportedOperationException("Email acquiring is not supported");
    }
    if (myEmailValidAttr == null || json.getBoolean(myEmailValidAttr)) {
      return json.getString(myEmailAttr);
    } else {
      return null;
    }
  }

  public boolean isEmailSupported() {
    return myEmailAttr != null;
  }

  @Override
  public boolean isResponseOk(JSONObject json) {
    return json.has(myIdAttr);
  }

  @Override
  public String createUserName(JSONObject json) throws JSONException {
    StringBuilder name = new StringBuilder("");
    if (json.has(myFirstNameAttr) || json.has(myLastNameAttr)) {
      if (json.has(myFirstNameAttr)) {
        name.append(json.getString(myFirstNameAttr));
      }
      if (json.has(myLastNameAttr)) {
        name.append(" ").append(json.getString(myLastNameAttr));
      }
    }
    if (name.length() == 0) {
      if (json.has(myFullNameAttr)) {
        name.append(json.getString(myFullNameAttr));
      }
    }
    if (name.length() == 0) {
      name.append(createUserId(json));
    }
    return name.toString();
  }

  public String buildRequest(String accessResponse) {
    return getProperty(".method");
  }

  public Class<Api> getBuilderApiClass() throws ClassNotFoundException {
    return (Class<Api>)Class.forName(getProperty(".class"));
  }

  public String getKey() {
    return getProperty(".auth.key");
  }

  public String getSecret() {
    return getProperty(".auth.secret");
  }

  public String getScope() {
    return getProperty(".auth.scope");
  }

  public String extractToken(HttpApi req) {
    return req.getUrlParameter(getProperty(".param.request_token"));
  }

  public String extractVerifier(HttpApi req) {
    return req.getUrlParameter(getProperty(".param.verifier"));
  }

  public String getProperty(String suffix) {
    return myConfig.getProperty(myAuthService + suffix);
  }

  private void setProperty(String suffix, String value) {
    myConfig.setProperty(myAuthService + suffix, value);
  }

  public void setScope(String scope) {
    setProperty(".auth.scope", scope);
  }
}