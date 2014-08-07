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

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;

@Cache(expirationSeconds = 3600)
@Entity
public class PrincipalEntity {
  @Id String id;
  public String displayName;
  ContactEntity contacts = new ContactEntity();
  public Integer experiments;
  
  @Embed
  public static class ContactEntity {
    public String email;
    public String xmpp;
    public String emailVerificationToken;
    
    public ContactEntity() {}
    public ContactEntity(String email, String xmpp, String token) {
      this.email = email;
      this.xmpp = xmpp;
      this.emailVerificationToken = token;
    }
  }

  public PrincipalEntity() {
  }
  
  protected PrincipalEntity(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public ContactEntity getContacts() {
    return this.contacts;
  }

  public String getId() {
    return id;
  }
}
