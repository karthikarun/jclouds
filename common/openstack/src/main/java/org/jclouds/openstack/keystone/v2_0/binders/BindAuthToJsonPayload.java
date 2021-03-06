/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.openstack.keystone.v2_0.binders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.http.HttpRequest;
import org.jclouds.json.Json;
import org.jclouds.openstack.keystone.v2_0.domain.ApiAccessKeyCredentials;
import org.jclouds.openstack.keystone.v2_0.domain.PasswordCredentials;
import org.jclouds.rest.MapBinder;
import org.jclouds.rest.binders.BindToJsonPayload;
import org.jclouds.rest.internal.GeneratedHttpRequest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * 
 * @author Adrian Cole
 * 
 */
@Singleton
public class BindAuthToJsonPayload extends BindToJsonPayload implements MapBinder {
   @Inject
   public BindAuthToJsonPayload(Json jsonBinder) {
      super(jsonBinder);
   }

   @Override
   public <R extends HttpRequest> R bindToRequest(R request, Object toBind) {
      throw new IllegalStateException("BindAuthToJsonPayload needs parameters");
   }

   protected void addCredentialsInArgsOrNull(GeneratedHttpRequest<?> gRequest, Builder<String, Object> builder) {
      for (Object arg : gRequest.getArgs()) {
         if (arg instanceof PasswordCredentials) {
            builder.put("auth", ImmutableMap.of("passwordCredentials", PasswordCredentials.class.cast(arg)));
         } else if (arg instanceof ApiAccessKeyCredentials) {
            builder.put("auth", ImmutableMap.of("apiAccessKeyCredentials", ApiAccessKeyCredentials.class.cast(arg)));
         }
      }
   }

   @Override
   public <R extends HttpRequest> R bindToRequest(R request, Map<String, String> postParams) {
      checkArgument(checkNotNull(request, "request") instanceof GeneratedHttpRequest<?>,
               "this binder is only valid for GeneratedHttpRequests!");
      GeneratedHttpRequest<?> gRequest = (GeneratedHttpRequest<?>) request;
      checkState(gRequest.getArgs() != null, "args should be initialized at this point");

      Builder<String, Object> builder = ImmutableMap.<String, Object> builder();
      builder.put("tenantId", postParams.get("tenantId"));
      addCredentialsInArgsOrNull(gRequest, builder);
      return super.bindToRequest(request, builder.build());
   }

}
