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

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.api.Api;

import com.bardsoftware.server.HttpApi;
import com.google.common.base.Function;

public class DefaultOAuthPlugin implements OAuthPlugin {
  public static Function<JSONObject, String> createIdFormatter(final String providerPrefix, final String idAttr) {
    return new Function<JSONObject, String>() {
      @Override
      public String apply(JSONObject json) {
        try {
          return providerPrefix + json.getString(idAttr);
        } catch (JSONException e) {
          return null;
        }
      }
    };
  }
  private final Function<JSONObject, String> myIdFormatter;
  private final String myIdAttr;
  private final String myFullNameAttr;
  private final String myFirstNameAttr;
  private final String myLastNameAttr;
  private final Properties myConfig;
  private final String myAuthService;
  
  public DefaultOAuthPlugin(String authService, Properties config) {
    myAuthService = authService;
    myConfig = config;
    myIdAttr = config.getProperty(authService + ".json.id");
    myFullNameAttr = config.getProperty(authService + ".json.full_name");
    myFirstNameAttr = config.getProperty(authService + ".json.first_name");
    myLastNameAttr = config.getProperty(authService + ".json.last_name");
    myIdFormatter = createIdFormatter(config.getProperty(authService + ".principal.prefix"), myIdAttr);    
  }
    
  @Override
  public String createUserId(JSONObject json) throws JSONException {
    return myIdFormatter.apply(json);
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
}