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
package com.bardsoftware.server.auth.provider;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.bardsoftware.server.auth.DefaultOAuthPlugin;

public class VkontaktePlugin extends DefaultOAuthPlugin {

  public VkontaktePlugin(String authService, Properties config) {
    super(authService, config);
  }
  
  @Override
  public String createUserId(JSONObject json) throws JSONException {
    return super.createUserId(json.optJSONArray("response").optJSONObject(0));
  }

  @Override
  public boolean isResponseOk(JSONObject json) {
    return json.has("response") && super.isResponseOk(json.optJSONArray("response").optJSONObject(0));
  }

  @Override
  public String createUserName(JSONObject json) throws JSONException {
    return super.createUserName(json.optJSONArray("response").optJSONObject(0));
  }

  @Override
  public String buildRequest(String accessResponse) {
    try {
      JSONObject json = new JSONObject(accessResponse);
      return String.format(super.buildRequest(accessResponse), json.getString("user_id"));
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }
}
