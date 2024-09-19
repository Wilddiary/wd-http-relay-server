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

package com.wilddiary.ws.controllers;

import static com.wilddiary.ws.controllers.RelayController.CONTROLLER_CONTEXT;

import com.wilddiary.ws.services.RelayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Controller for relaying requests. */
@Controller
@RequestMapping(CONTROLLER_CONTEXT)
@Slf4j
public class RelayController {
  public static final String CONTROLLER_CONTEXT = "/relay";

  private final RelayService relayService;

  /**
   * Constructor for the RelayController.
   *
   * @param relayService the relay service
   */
  public RelayController(RelayService relayService) {
    this.relayService = relayService;
  }

  /**
   * Relays requests.
   *
   * @param downstreamRequest the downstream request
   * @param downstreamResponse the downstream response
   * @param downstreamRequestBody the downstream request body
   */
  @RequestMapping(value = "/**")
  public void relayRequests(
      HttpServletRequest downstreamRequest,
      HttpServletResponse downstreamResponse,
      @RequestBody(required = false) byte[] downstreamRequestBody) {
    HttpMethod downstreamRequestMethod = HttpMethod.valueOf(downstreamRequest.getMethod());
    this.relayService.relayRequest(
        downstreamRequest, downstreamResponse, downstreamRequestMethod, downstreamRequestBody);
  }
}
