/*
Copyright (C) 2012 BarD Software

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
package com.bardsoftware.server.auth.provider;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

/**
 * Provides OAuth 2.0 configuration for Google's authentication.
 *
 * @author dbarashev@bardsoftware.com
 */
public class Google2Api extends DefaultApi20 {
  private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&response_type=code";
  private static final String SCOPED_AUTHORIZE_URL = String.format("%s&scope=%%s", AUTHORIZE_URL);

  @Override
  public String getAccessTokenEndpoint() {
    return "https://accounts.google.com/o/oauth2/token";
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig config) {
    Preconditions.checkValidUrl(config.getCallback(), "Valid url is required for a callback");
    if (config.hasScope()) {
      return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(config.getScope()));
    } else {
      return String.format(AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
    }
  }

  @Override
  public AccessTokenExtractor getAccessTokenExtractor() {
    return new JsonAccessTokenExtractor();
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  public OAuthService createService(final OAuthConfig config) {
    // Google OAuth uses POST requests and thus requires more hacks
    return new OAuth20ServiceImpl(this, config) {
      @Override
      public Token getAccessToken(Token requestToken, Verifier verifier) {
        OAuthRequest request = new OAuthRequest(getAccessTokenVerb(), getAccessTokenEndpoint());
        request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
        request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
        request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
        request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
        request.addBodyParameter("grant_type", "authorization_code");

        if (config.hasScope()) {
          request.addBodyParameter(OAuthConstants.SCOPE, config.getScope());
        }

        Response response = request.send();
        return getAccessTokenExtractor().extract(response.getBody());
      }
    };
  }
}
