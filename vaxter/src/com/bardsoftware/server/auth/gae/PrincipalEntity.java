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

import javax.persistence.Embedded;
import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;

@Cached(expirationSeconds = 3600)
public class PrincipalEntity {
  @Id String id;
  String displayName;
  String token;
  @Embedded ContactEntity contacts = new ContactEntity();
  @Embedded NotificationEntity notifications = new NotificationEntity();
  Integer experiments;
  
  public static class ContactEntity {
    public String email;
    String xmpp;
    public String emailVerificationToken;
    
    public ContactEntity() {}
    public ContactEntity(String email, String xmpp, String token) {
      this.email = email;
      this.xmpp = xmpp;
      this.emailVerificationToken = token;
    }
  }

  public static class NotificationEntity {
    int replies;
    int news;
    
    public NotificationEntity() {}
    public NotificationEntity(int replies, int news) {
      this.replies = replies;
      this.news = news;
    }
  }
  
  public PrincipalEntity() {
  }
  
  PrincipalEntity(String id, String displayName, String refID) {
    this.id = id;
    this.displayName = displayName;
  }
}
