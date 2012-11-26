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
package com.bardsoftware.server.auth.gae;

import java.util.Map;

import com.bardsoftware.server.AppCapabilitiesService;
import com.bardsoftware.server.AppCapability;
import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.CapabilityState;
import com.google.appengine.api.capabilities.CapabilityStatus;
import com.google.common.collect.ImmutableMap;

public class AppCapabilitiesServiceImpl implements AppCapabilitiesService {
  private static final CapabilitiesService SERVICE = CapabilitiesServiceFactory.getCapabilitiesService();
  private final Map<CapabilityStatus, AppCapability.Status> MAPPING = ImmutableMap.of(
      CapabilityStatus.DISABLED, AppCapability.Status.DISABLED,
      CapabilityStatus.ENABLED, AppCapability.Status.ENABLED,
      CapabilityStatus.SCHEDULED_MAINTENANCE, AppCapability.Status.SCHEDULED_MAINTENANCE,
      CapabilityStatus.UNKNOWN, AppCapability.Status.UNKNOWN);
  
  public AppCapability getWriteCapability() {
    CapabilityState status = SERVICE.getStatus(com.google.appengine.api.capabilities.Capability.DATASTORE_WRITE);
    if (status.getStatus() == CapabilityStatus.DISABLED) {
      return new AppCapability(MAPPING.get(status.getStatus()), "It appears that Google App Engine is having problems");
    }
    return new AppCapability(MAPPING.get(status.getStatus()), null);
  }
}
