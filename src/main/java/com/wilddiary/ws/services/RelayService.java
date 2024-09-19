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

package com.wilddiary.ws.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

/** Service interface for relay server. */
public interface RelayService {

  /**
   * Relays a request.
   *
   * @param downstreamRequest the downstream request
   * @param downstreamResponse the downstream response
   * @param method the HTTP method
   * @param body the request body
   */
  void relayRequest(
      HttpServletRequest downstreamRequest,
      HttpServletResponse downstreamResponse,
      HttpMethod method,
      byte[] body);
}
