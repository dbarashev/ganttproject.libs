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
package com.bardsoftware.server.auth;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class Principal {
  private static final Logger LOGGER = Logger.getLogger(Principal.class.getName());

  public static final Principal ANONYMOUS = new Principal("@anonymous", "Anonymous");

  private final PrincipalEntity myEntity;

  public static void registerEntities() {
  }
  static {
    ObjectifyService.register(PrincipalEntity.class);
  }

  
  public Principal(String id, String displayName) {
    myEntity = new PrincipalEntity(id, displayName, null);
  }

  public Principal(PrincipalEntity entity) {
    myEntity = entity;
  }

  public String getID() {
    return myEntity.id;
  }
  
  public String getDisplayName() {
    return myEntity.displayName;
  }

  public String getNickname() {
      return getDisplayName();
  }

  public void setExperiments(int experiments) {
    myEntity.experiments = experiments; 
  }
      
  public com.googlecode.objectify.Key<PrincipalEntity> getKey() {
    return new com.googlecode.objectify.Key<PrincipalEntity>(PrincipalEntity.class, myEntity.id);
  }

  @Override
  public String toString() {
      return getDisplayName();
  }

  public static Collection<Principal> find(Collection<String> ids) {
    Objectify ofy = ObjectifyService.begin();
    Map<String, PrincipalEntity> map = ofy.get(PrincipalEntity.class, ids);
    return Collections2.transform(map.values(), new Function<PrincipalEntity, Principal>() {
      @Override
      public Principal apply(PrincipalEntity entity) {
        return new Principal(entity);
      }
    });
  }

  public boolean canWrite() {
      return this != Principal.ANONYMOUS;
  }

  public void setToken(String token) {
    myEntity.token = token;
  }
  
  public String getToken() {
    return myEntity.token;
  }

  public static Principal find(String id) {
    Objectify ofy = ObjectifyService.begin();
    PrincipalEntity entity = ofy.find(PrincipalEntity.class, id);
    return entity == null ? null : new Principal(entity);
  }
  
  public static Principal findByEmail(String email) {
    Objectify ofy = ObjectifyService.begin();
    PrincipalEntity entity = ofy.query(PrincipalEntity.class).filter("contacts.email =", email).get();
    return entity == null ? null : new Principal(entity);
  }
  
  public static Principal find(com.googlecode.objectify.Key<PrincipalEntity> key) {
    Objectify ofy = ObjectifyService.begin();
    PrincipalEntity entity = ofy.get(key);
    return entity == null ? null : new Principal(entity);
  }

  public void save() {
    ObjectifyService.begin().put(myEntity);
  }
}
