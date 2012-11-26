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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppCapability;
import com.bardsoftware.server.HttpApi;
//import com.bardsoftware.server.auth.gae.AppCapabilitiesServiceImpl;
//import com.bardsoftware.server.auth.gae.PrincipalGae;
import com.google.common.base.Charsets;

public class AuthService {
  private static final Logger LOGGER = Logger.getLogger("AuthService");
  private final String authDomain;
  private final AppCapabilitiesService capabilities;
  private final PrincipalExtent principalExtent;

  public AuthService(String authDomain, PrincipalExtent principalExtent, AppCapabilitiesService capabilities) {
    this.principalExtent = principalExtent;
    this.authDomain = authDomain;
    this.capabilities = capabilities;
  }
  
  private Principal getUser(final HttpApi http) {
    String userId = http.getUsername();
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("req url=" + http.getRequestUrl() + "session id=" + http.getSessionId() + "user id=" + userId);
    }
    return (userId == null) ? null : principalExtent.find(String.valueOf(userId));
  }
  
  public void logout(Principal user, HttpApi http) {
    if (http.hasSession()) {
      http.setUsername(null);
    }
    http.setCookie("JSESSIONID", authDomain, "/", 0);
  }
    
    public static String getMD5(String src) throws NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(src.getBytes(Charsets.UTF_8));
      byte[] digest = md.digest();
      StringBuffer hexDigest = new StringBuffer();
      for (byte b: digest) {
        String hexByte = Integer.toHexString(0xFF & b);
        if (hexByte.length() == 1) {
          hexDigest.append("0");
        }
        hexDigest.append(hexByte);
      }
      return hexDigest.toString();
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
        JSONObject json = new JSONObject(jsonText.toString());
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
}
