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

import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppUrlServiceProperties implements AppUrlService {
  private static final Logger LOGGER = Logger.getLogger("Config");
  private static final Properties properties = new Properties();

  @Override
  public String buildUrlFromPath(String path, HttpApi httpApi) {
    return "http://" + httpApi.getHost() + path;
  }

  public String getUrl(String urlName, HttpApi httpApi) {
    try {
      URL requestUrl = new URL(httpApi.getRequestUrl());
      int port = requestUrl.getPort();
      String hostCallback = properties.getProperty(requestUrl.getHost() + "." + urlName);
      if (hostCallback == null) {
        hostCallback = properties.getProperty(urlName);
      }
      if (hostCallback != null) {
        hostCallback = hostCallback.trim();
        List<String> arguments = Lists.newArrayList();
        if (!hostCallback.startsWith("http")) {
          arguments.add(requestUrl.getProtocol() + "://");
        }
        if (port > 0) {
          arguments.add(":" + port);
        } else {
          arguments.add("");
        }
        Object[] varargs = arguments.toArray(new String[0]);
        hostCallback = MessageFormat.format(hostCallback, varargs);
      }
      return hostCallback;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getDomainName(HttpApi httpApi) {
    String hostname = httpApi.getHost();
    return properties.getProperty(hostname + ".cookieDomain", hostname);
  }

  static {
    loadProperties(properties, "/app.properties");
  }

  public AppUrlServiceProperties() {
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
