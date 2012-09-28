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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppCapabilitiesService.AppCapability;
import com.google.common.base.Charsets;

public class AuthService {
  private static final Logger LOGGER = Logger.getLogger("AuthService");
  private final String authDomain;
  private final AppCapabilitiesService capabilities;

  public AuthService(String authDomain) {
    this(authDomain, new AppCapabilitiesService());
  }
  
  public AuthService(String authDomain, AppCapabilitiesService capabilitiesService) {
    this.authDomain = authDomain;
    this.capabilities = capabilitiesService;
  }
  
  private Principal getUser(final HttpServletRequest req) {
    Object userId = req.getSession().getAttribute("user_id");
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("req url=" + req.getRequestURL() + "session id=" + req.getSession().getId() + "user id=" + userId);
    }
    return (userId == null) ? null : Principal.find(String.valueOf(userId));
  }
  
  public void logout(Principal user, HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession(false);
    if (session != null) {
      session.setAttribute("user_id", null);
    }
    Cookie cookie = new Cookie("JSESSIONID", null);
    cookie.setDomain(authDomain);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    resp.addCookie(cookie);
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
        
    public Principal remember(HttpServletRequest req, Principal user) {
      if (user == null) {
        user = Principal.ANONYMOUS;
      } else {
          req.getSession().setAttribute("user_id", user.getID());
          req.setAttribute("user_id", user.getID());
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
          Principal result = Principal.find(userId);
          if (result == null) {
            String name = loginService.createUserName(json);
            result = new Principal(userId, name);
          }
          result.save();
          return result;
        }
      } catch (JSONException e) {
        LOGGER.log(Level.SEVERE, "", e);
      }
      return null;
    }
    
    public Principal setupUserAndMaintenance(HttpServletRequest req) {
      Principal user = Principal.ANONYMOUS;
      AppCapability writeCapability = capabilities.getWriteCapability();
      switch (writeCapability.status) {
      case ENABLED:
        user = getUser(req);
        if (user == null) {
          user = Principal.ANONYMOUS;
        }
        req.setAttribute("nickname", user.getNickname());
        if (user != Principal.ANONYMOUS) {
          req.setAttribute("user_id", user.getID());
        }
        break;
      case DISABLED:
        req.setAttribute("maintenance", true);
        req.setAttribute("maintenance_", writeCapability.message);
        break;
      }
      return user;
    }
}
