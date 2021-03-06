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
package org.jclouds.cloudloadbalancers.features;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.jclouds.cloudloadbalancers.CloudLoadBalancersAsyncClient;
import org.jclouds.cloudloadbalancers.CloudLoadBalancersClient;
import org.jclouds.cloudloadbalancers.config.CloudLoadBalancersRestClientModule;
import org.jclouds.cloudloadbalancers.functions.ConvertLB;
import org.jclouds.cloudloadbalancers.reference.Region;
import org.jclouds.domain.Credentials;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.RequiresHttp;
import org.jclouds.internal.ClassMethodArgs;
import org.jclouds.json.config.GsonModule.DateAdapter;
import org.jclouds.json.config.GsonModule.Iso8601DateAdapter;
import org.jclouds.openstack.OpenStackAuthAsyncClient.AuthenticationResponse;
import org.jclouds.openstack.config.OpenStackAuthenticationModule;
import org.jclouds.openstack.filters.AuthenticateRequest;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.RestClientTest;
import org.jclouds.rest.RestContext;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.rest.RestContextSpec;
import org.jclouds.rest.internal.RestContextImpl;
import org.testng.annotations.BeforeClass;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Types;

/**
 * @author Adrian Cole
 */
public abstract class BaseCloudLoadBalancersAsyncClientTest<T> extends RestClientTest<T> {

   @RequiresHttp
   @ConfiguresRestClient
   public static class CloudLoadBalancersRestClientModuleExtension extends CloudLoadBalancersRestClientModule {

      protected void bindRegionsToProvider() {
         bindRegionsToProvider(Regions.class);
      }
      
      //TODO: replace this with Expect test
      @SuppressWarnings("unchecked")
      @Override
      protected void configure() {
         // following from CloudLoadBalancersRestClientModule, except we are hard-coding the auth response
         install(new OpenStackAuthenticationModule() {
            @Override
            protected Supplier<AuthenticationResponse> provideAuthenticationResponseSupplier(
                     LoadingCache<Credentials, AuthenticationResponse> cache, Credentials in) {
               return Suppliers.ofInstance(new AuthenticationResponse("token", ImmutableMap.<String, URI> of()));
            }
         });
         bind(DateAdapter.class).to(Iso8601DateAdapter.class);
         bindRegionsToProvider();
         install(new FactoryModuleBuilder().build(ConvertLB.Factory.class));

         // following from RestClientModule
         bind(new TypeLiteral<RestContext>() {
         }).to(
               (TypeLiteral) TypeLiteral.get(Types.newParameterizedType(
                     RestContextImpl.class, syncClientType, asyncClientType))).in(
               Scopes.SINGLETON);
         bind(TypeLiteral.get(Types.newParameterizedType(RestContext.class, syncClientType, asyncClientType))).to(
                  (TypeLiteral) TypeLiteral.get(Types.newParameterizedType(RestContextImpl.class, syncClientType,
                           asyncClientType))).in(Scopes.SINGLETON);
         bindAsyncClient();
         bindClient();
         bindErrorHandlers();
         bindRetryHandlers();
      }

      static class Regions implements javax.inject.Provider<Map<String, URI>> {
         @Override
         public Map<String, URI> get() {
            return ImmutableMap.<String, URI> of("DFW", URI
                     .create("https://dfw.loadbalancers.api.rackspacecloud.com/v1.0/1234"));
         }
      }

      @Override
      protected String accountID(Supplier<AuthenticationResponse> in) {
         return "1234";
      }
   }

   @Override
   protected void checkFilters(HttpRequest request) {
      assertEquals(request.getFilters().size(), 1);
      assertEquals(request.getFilters().get(0).getClass(), AuthenticateRequest.class);
   }

   @Override
   protected Module createModule() {
      return new CloudLoadBalancersRestClientModuleExtension();
   }

   @Override
   public RestContextSpec<CloudLoadBalancersClient, CloudLoadBalancersAsyncClient> createContextSpec() {
      Properties props = new Properties();
      return new RestContextFactory().createContextSpec("cloudloadbalancers", "email", "apikey", props);
   }

   @BeforeClass
   @Override
   protected void setupFactory() throws IOException {
      super.setupFactory();
      try {
         processor.setCaller(new ClassMethodArgs(CloudLoadBalancersAsyncClient.class,
                  CloudLoadBalancersAsyncClient.class.getMethod("getLoadBalancerClient", String.class),
                  new Object[] { Region.DFW }));
      } catch (Exception e) {
         Throwables.propagate(e);
      }
   }
}