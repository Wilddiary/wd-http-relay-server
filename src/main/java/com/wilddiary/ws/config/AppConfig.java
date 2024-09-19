/*
 * Licensed to the Wilddiary.com under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 * Wilddiary.com licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.wilddiary.ws.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Configuration for the application. */
@Configuration
public class AppConfig {

  /**
   * Create a {@link RestTemplate} with a 10 second connect and read timeout.
   *
   * @param builder the {@link RestTemplateBuilder}
   * @param followRedirects whether to follow redirects
   * @return the {@link RestTemplate}
   */
  @Bean
  public RestTemplate httpClient(
      RestTemplateBuilder builder,
      @Value("${relay.follow-redirects:false}") boolean followRedirects) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(10))
        .requestFactory(() -> httpFactory(followRedirects))
        .build();
  }

  /**
   * Create a {@link HttpRequestFactory} with the given followRedirects setting.
   *
   * @param followRedirects whether to follow redirects
   * @return the {@link HttpRequestFactory}
   */
  private HttpRequestFactory httpFactory(boolean followRedirects) {
    return new HttpRequestFactory(followRedirects);
  }

  /** A {@link SimpleClientHttpRequestFactory} that allows setting the followRedirects property. */
  @RequiredArgsConstructor
  static class HttpRequestFactory extends SimpleClientHttpRequestFactory {
    private final boolean followRedirects;

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod)
        throws IOException {
      super.prepareConnection(connection, httpMethod);
      connection.setInstanceFollowRedirects(followRedirects);
    }
  }
}
