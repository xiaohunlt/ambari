/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.common;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.util.SSLUtil;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * Layer to send REST request to External server using jersey client
 */
@Named
public class ExternalServerClient {

  private static Logger LOG = Logger.getLogger(ExternalServerClient.class);
  private static final ThreadLocal<JerseyClient> localJerseyClient = new ThreadLocal<JerseyClient>() {
    @Override
    protected JerseyClient initialValue() {
      return SSLUtil.isKeyStoreSpecified() ?
          new JerseyClientBuilder().sslContext(SSLUtil.getSSLContext()).build() :
          JerseyClientBuilder.createClient();
    }
  };

  @Inject
  private AuthPropsConfig authPropsConfig;

  /**
   * Send GET request to an external server
   */
  public Object sendGETRequest(String loginUrl, Class<?> klass, String username, String password) throws Exception {
    String url = authPropsConfig.getExternalAuthHostUrl() + loginUrl;
    JerseyClient client = localJerseyClient.get();
    HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
      .credentials(username, password)
      .build();
    client.register(authFeature);

    WebTarget target = client.target(url);
    LOG.debug("URL: " + url);
    
    Invocation.Builder invocationBuilder =  target.request(MediaType.APPLICATION_JSON_TYPE);
    try {
      return invocationBuilder.get().readEntity(klass);
    } catch (Exception e) {
      throw new Exception(e.getCause());
    } finally {
      localJerseyClient.remove();
    }
  }
}