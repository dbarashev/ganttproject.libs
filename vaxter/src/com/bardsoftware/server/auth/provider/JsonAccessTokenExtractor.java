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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.Token;

/**
 * Token extractor which really parses response as JSON, rather than applying regexp, as
 * Scribe's json token extractor does.
 *
 * @author dbarashev@bardsoftware.com
 */
public class JsonAccessTokenExtractor implements AccessTokenExtractor {
  private static final Logger LOGGER = Logger.getLogger("AuthService");

  @Override
  public Token extract(String response) {
    try {
      JSONObject json = new JSONObject(response);
      return new Token(json.getString("access_token"), "", response);
    } catch (JSONException e) {
      LOGGER.log(Level.SEVERE, "Failed to parse JSON:\n" + response, e);
    }
    return null;
  }
}
