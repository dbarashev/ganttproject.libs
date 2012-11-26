package com.bardsoftware.server;

public class AppCapability {
  public static enum Status {ENABLED, SCHEDULED_MAINTENANCE, DISABLED, UNKNOWN};
  
  public final AppCapability.Status status;
  public final String message;
  
  public AppCapability(AppCapability.Status status, String message) {
    this.status = status;
    this.message = message;
  }
}