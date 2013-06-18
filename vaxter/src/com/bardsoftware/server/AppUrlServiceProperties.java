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
package com.bardsoftware.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppUrlServiceProperties implements AppUrlService {
  private static final Logger LOGGER = Logger.getLogger("Config");
  private static final Properties properties = new Properties();
  private final String host;
  private final String domain;
  private boolean myDevMode;

  @Override
  public String buildUrlFromPath(String path, HttpApi httpApi) {
    return "http://" + httpApi.getHost() + path;
  }

  public String getUrl(String urlName, HttpApi httpApi) {
    try {
      URL requestUrl = new URL(httpApi.getRequestUrl());
      String hostCallback = properties.getProperty(requestUrl.getHost() + "." + urlName + (myDevMode ? ".dev" : ".prod"));
      if (hostCallback == null) {
        hostCallback = properties.getProperty(urlName + (myDevMode ? ".dev" : ".prod"));
      }
      if (hostCallback != null) {
        hostCallback = hostCallback.trim();
      }
      return hostCallback;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getDomainName() {
    return domain;
  }

  static {
    loadProperties(properties, "/app.properties");
  }

  public AppUrlServiceProperties(boolean devMode) {
    myDevMode = devMode;
    host = "http://" + properties.getProperty(devMode ? "dev.host" : "prod.host");
    domain = properties.getProperty(devMode ? "dev.domain" : "prod.domain");
  }

  private static void loadProperties(Properties result, String resource) {
    URL url = AppUrlService.class.getResource(resource);
    if (url == null) {
      return;
    }
    try {
      result.load(url.openStream());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to load properties", e);
    }
  }
}
