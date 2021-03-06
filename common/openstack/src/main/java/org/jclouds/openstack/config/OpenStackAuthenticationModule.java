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
package org.jclouds.openstack.config;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static com.google.common.base.Throwables.propagate;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.Constants;
import org.jclouds.concurrent.RetryOnTimeOutExceptionFunction;
import org.jclouds.date.TimeStamp;
import org.jclouds.domain.Credentials;
import org.jclouds.http.RequiresHttp;
import org.jclouds.location.Provider;
import org.jclouds.openstack.Authentication;
import org.jclouds.openstack.OpenStackAuthAsyncClient;
import org.jclouds.openstack.OpenStackAuthAsyncClient.AuthenticationResponse;
import org.jclouds.rest.AsyncClientFactory;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

/**
 * Configures the Rackspace authentication service connection, including logging and http transport.
 * 
 * @author Adrian Cole
 */
@RequiresHttp
public class OpenStackAuthenticationModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(new TypeLiteral<Function<Credentials, AuthenticationResponse>>() {
      }).to(GetAuthenticationResponse.class);
   }

   /**
    * borrowing concurrency code to ensure that caching takes place properly
    */
   @Provides
   @Singleton
   @Authentication
   protected Supplier<String> provideAuthenticationTokenCache(final Supplier<AuthenticationResponse> supplier)
            throws InterruptedException, ExecutionException, TimeoutException {
      return new Supplier<String>() {
         public String get() {
            return supplier.get().getAuthToken();
         }
      };
   }

   @Provides
   @Provider
   protected Credentials provideAuthenticationCredentials(@Named(Constants.PROPERTY_IDENTITY) String user,
            @Named(Constants.PROPERTY_CREDENTIAL) String key) {
      return new Credentials(user, key);
   }

   @Singleton
   public static class GetAuthenticationResponse extends
            RetryOnTimeOutExceptionFunction<Credentials, AuthenticationResponse> {

      @Inject
      public GetAuthenticationResponse(final AsyncClientFactory factory) {
         super(new Function<Credentials, AuthenticationResponse>() {

            @Override
            public AuthenticationResponse apply(Credentials input) {
               try {
                  Future<AuthenticationResponse> response = factory.create(OpenStackAuthAsyncClient.class)
                           .authenticate(input.identity, input.credential);
                  return response.get(30, TimeUnit.SECONDS);
               } catch (Exception e) {
                  throw propagate(e);
               }
            }

            @Override
            public String toString() {
               return "authenticate()";
            }
         });

      }
   }

   @Provides
   @Singleton
   public LoadingCache<Credentials, AuthenticationResponse> provideAuthenticationResponseCache2(
            Function<Credentials, AuthenticationResponse> getAuthenticationResponse) {
      return CacheBuilder.newBuilder().expireAfterWrite(23, TimeUnit.HOURS).build(
               CacheLoader.from(getAuthenticationResponse));
   }

   @Provides
   @Singleton
   protected Supplier<AuthenticationResponse> provideAuthenticationResponseSupplier(
            final LoadingCache<Credentials, AuthenticationResponse> cache, @Provider final Credentials creds) {
      return new Supplier<AuthenticationResponse>() {
         @Override
         public AuthenticationResponse get() {
            try {
               return cache.get(creds);
            } catch (ExecutionException e) {
               throw propagate(e.getCause());
            }
         }
      };
   }

   @Provides
   @Singleton
   @TimeStamp
   protected Supplier<Date> provideCacheBusterDate() {
      return memoizeWithExpiration(new Supplier<Date>() {
         public Date get() {
            return new Date();
         }
      }, 1, TimeUnit.SECONDS);
   }

   @Provides
   @Singleton
   protected AuthenticationResponse provideAuthenticationResponse(Supplier<AuthenticationResponse> supplier)
            throws InterruptedException, ExecutionException, TimeoutException {
      return supplier.get();
   }

}