// Copyright (C) 2013 BarD Software
package com.bardsoftware.server;

/**
 * @author dbarashev@bardsoftware.com
 */
public interface AppUrlService {
  String buildUrlFromPath(String path, HttpApi httpApi);
  String getUrl(String urlName, HttpApi httpApi);
  String getDomainName(HttpApi httpApi);
}
