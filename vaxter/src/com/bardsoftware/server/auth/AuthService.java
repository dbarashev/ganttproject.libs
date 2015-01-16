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

import com.google.common.collect.ImmutableSet;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppCapability;
import com.bardsoftware.server.HttpApi;

public class AuthService {
  private static final Logger LOGGER = Logger.getLogger("AuthService");
//  private final String authDomain;
  private final AppCapabilitiesService capabilities;
  private final PrincipalExtent principalExtent;

  public AuthService(PrincipalExtent principalExtent, AppCapabilitiesService capabilities) {
    this.principalExtent = principalExtent;
    this.capabilities = capabilities;
  }
  
  private Principal getUser(final HttpApi http) {
    String userId = http.getUsername();
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(String.format("req url=%s session_id=%s user_id=%s", http.getRequestUrl(), http.getSessionId(), userId));
    }
    return (userId == null) ? null : principalExtent.find(String.valueOf(userId));
  }
  
  public void logout(Principal user, HttpApi http) {
    if (http.hasSession()) {
      http.setUsername(null);
    }
    http.clearSession();
  }
        
  public Principal remember(HttpApi http, Principal user) {
    if (user == null) {
      user = Principal.ANONYMOUS;
    } else {
        http.setUsername(user.getID());
        http.setRequestAttribute("user_id", user.getID());
    }
    return user;
  }
  
  public Principal getUserFromLoginService(String jsonText, OAuthPlugin loginService) {
    try {
      if (jsonText == null) {
        return null;
      }
      LOGGER.log(Level.FINE, "json from login service=" + jsonText);
      JSONObject json = new JSONObject(jsonText);
      if (loginService.isResponseOk(json)) {
        String userId = loginService.createUserId(json); 
        Principal result = principalExtent.find(userId);
        if (result == null) {
          String name = loginService.createUserName(json);
          result = principalExtent.create(userId, name);
        }
        result.save();
        return result;
      }
    } catch (JSONException e) {
      LOGGER.log(Level.SEVERE, "", e);
    }
    return null;
  }
  
  public Principal setupUserAndMaintenance(HttpApi http) {
    Principal user = Principal.ANONYMOUS;
    AppCapability writeCapability = capabilities.getWriteCapability();
    switch (writeCapability.status) {
    case ENABLED:
      user = getUser(http);
      if (user == null) {
        user = Principal.ANONYMOUS;
      }
      http.setRequestAttribute("nickname", user.getNickname());
      if (user != Principal.ANONYMOUS) {
        http.setRequestAttribute("user_id", user.getID());
      }
      break;
    case DISABLED:
      http.setRequestAttribute("maintenance", true);
      http.setRequestAttribute("maintenance_", writeCapability.message);
      break;
    }
    return user;
  }

  private static final Set<String> VALID_PROVIDERS = ImmutableSet.of("email", "google", "facebook", "twitter", "vkontakte");

  public static boolean isValidAuthProvider(String provider) {
    return VALID_PROVIDERS.contains(provider);
  }
}
