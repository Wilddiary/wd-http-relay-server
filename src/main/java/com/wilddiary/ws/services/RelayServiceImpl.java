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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Service implementation for relay server. */
@Service
@Slf4j
class RelayServiceImpl implements RelayService {

  private final String contextPath;
  private final String pathPrefix;
  private final Map<String, String> contextMap;
  private final Map<String, String> invertedContextMap;

  // Set of mime types to rewrite URLs in the response body
  private final Set<MediaType> mimeTypes =
      new HashSet<>(
          Set.of(
              MediaType.parseMediaType("text/html"),
              MediaType.parseMediaType("text/xml"),
              MediaType.parseMediaType("application/xml"),
              MediaType.parseMediaType("application/xhtml+xml"),
              MediaType.parseMediaType("text/plain"),
              MediaType.parseMediaType("text/css"),
              MediaType.parseMediaType("application/javascript"),
              MediaType.parseMediaType("application/json"),
              MediaType.parseMediaType("application/rss+xml"),
              MediaType.parseMediaType("application/atom+xml"),
              MediaType.parseMediaType("application/rdf+xml"),
              MediaType.parseMediaType("application/xml+rss"),
              MediaType.parseMediaType("application/xml+atom"),
              MediaType.parseMediaType("application/xml+rdf"),
              MediaType.parseMediaType("application/xml+xml"),
              MediaType.parseMediaType("application/xslt+xml")));

  // Set of headers to carry over from the upstream response to the downstream response
  private static final Set<String> CARRY_OVER_RESPONSE_HEADERS =
      Set.of(
          HttpHeaders.ACCEPT_RANGES.toLowerCase(),
          HttpHeaders.CACHE_CONTROL.toLowerCase(),
          HttpHeaders.CONTENT_DISPOSITION.toLowerCase(),
          HttpHeaders.CONTENT_ENCODING.toLowerCase(),
          HttpHeaders.CONTENT_TYPE.toLowerCase(),
          HttpHeaders.CONTENT_RANGE.toLowerCase(),
          HttpHeaders.DATE.toLowerCase(),
          HttpHeaders.ETAG.toLowerCase(),
          HttpHeaders.EXPIRES.toLowerCase(),
          HttpHeaders.LAST_MODIFIED.toLowerCase(),
          "Strict-Transport-Security".toLowerCase(),
          "Referrer-Policy".toLowerCase());

  private final RestTemplate restTemplate;

  /**
   * Constructor for the RelayServiceImpl.
   *
   * @param contextMap the context mapping
   * @param contextPath the context path
   * @param strippablePathPrefix the strippable path prefix
   * @param excludeMimeTypes the exclude mime types
   * @param restTemplate the rest template
   */
  public RelayServiceImpl(
      @Value("#{${relay.context.mapping}}") Map<String, String> contextMap,
      @Value("#{servletContext.contextPath}") String contextPath,
      @Value("#{T(com.wilddiary.ws.controllers.RelayController).CONTROLLER_CONTEXT}")
          String strippablePathPrefix,
      @Value(
              "#{'${relay.rewrite-urls.exclude.mime-types:}'.trim().isEmpty() ? new String[] {} : "
                  + "'${relay.rewrite-urls.exclude.mime-types:}'.split(',')}")
          Set<MediaType> excludeMimeTypes,
      RestTemplate restTemplate) {
    this.contextMap = contextMap;
    this.pathPrefix = strippablePathPrefix;
    this.restTemplate = restTemplate;
    this.invertedContextMap = invertMap(contextMap);
    this.contextPath = contextPath;
    this.mimeTypes.removeAll(excludeMimeTypes);
  }

  /**
   * Relays a request. The request is relayed to the upstream server.
   *
   * @param downstreamRequest the downstream request
   * @param downstreamResponse the downstream response
   * @param downstreamRequestMethod the downstream request method
   * @param downstreamBody the downstream body
   */
  @Override
  public void relayRequest(
      HttpServletRequest downstreamRequest,
      HttpServletResponse downstreamResponse,
      HttpMethod downstreamRequestMethod,
      byte[] downstreamBody) {
    log.debug(
        "Received downstream request: {} {}",
        downstreamRequestMethod,
        downstreamRequest.getRequestURI());

    // Build the upstream URL
    String upstreamUrl =
        buildUpstreamUrl(downstreamRequest)
            .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND));

    // Relay the request to the upstream server
    log.debug("Relaying downstream request to {}", upstreamUrl);
    restTemplate.execute(
        upstreamUrl,
        downstreamRequestMethod,
        requestCallback(buildUpstreamRequest(downstreamRequest, downstreamBody)),
        responseExtractor(downstreamRequest, downstreamResponse));
  }

  /**
   * Builds the upstream request. The upstream request is the request that is relayed to the
   * upstream server.
   *
   * @param downstreamRequest the downstream request
   * @param downstreamRequestBody the downstream request body
   * @return the upstream request
   */
  private HttpEntity<byte[]> buildUpstreamRequest(
      HttpServletRequest downstreamRequest, byte[] downstreamRequestBody) {

    // Copy headers from the downstream request
    HttpHeaders headers = copyRequestHeaders(downstreamRequest);
    return new HttpEntity<>(downstreamRequestBody, headers);
  }

  /**
   * Builds the upstream URL. The upstream URL is the URL of the upstream server.
   *
   * @param downstreamRequest the downstream request
   * @return the upstream URL
   */
  private Optional<String> buildUpstreamUrl(HttpServletRequest downstreamRequest) {
    String mappedContext = getContextPath(downstreamRequest);
    Optional<String> mappedUrl = Optional.ofNullable(contextMap.get(mappedContext));

    if (mappedUrl.isEmpty()) {
      log.debug("No mapping found for context {}", mappedContext);
      return Optional.empty();
    }

    log.debug("Found mapping for context {}: {}", mappedContext, mappedUrl.get());

    return Optional.of(
        UriComponentsBuilder.fromHttpUrl(mappedUrl.get() + stripMappedContext(downstreamRequest))
            .query(downstreamRequest.getQueryString())
            .build()
            .toUriString());
  }

  /**
   * Creates a request callback. The request callback relays the request to the upstream server.
   *
   * @param requestEntity the request entity
   * @return the request callback
   */
  private RequestCallback requestCallback(HttpEntity<byte[]> requestEntity) {
    return clientHttpRequest -> {
      clientHttpRequest.getHeaders().putAll(requestEntity.getHeaders());
      if (requestEntity.getBody() != null) {
        StreamUtils.copy(requestEntity.getBody(), clientHttpRequest.getBody());
      }
    };
  }

  /**
   * Creates a response extractor. The response extractor relays the response to the downstream
   * server.
   *
   * @param downstreamRequest the downstream request
   * @param downstreamResponse the downstream response
   * @return the response extractor
   */
  private ResponseExtractor<Void> responseExtractor(
      HttpServletRequest downstreamRequest, HttpServletResponse downstreamResponse) {
    return upstreamResponse -> {
      log.debug(
          "Received upstream response with status {}", upstreamResponse.getStatusCode().value());
      downstreamResponse.setStatus(upstreamResponse.getStatusCode().value());

      // Filter headers
      HttpHeaders downstreamResponseHeaders = filterResponseHeaders(upstreamResponse.getHeaders());

      // rewrite redirection url, if mapped
      handleRedirection(downstreamRequest, upstreamResponse, downstreamResponseHeaders);

      // Apply headers to the downstream response
      applyHeaders(downstreamResponseHeaders, downstreamResponse);

      log.debug("Relaying to downstream with headers {}", downstreamResponseHeaders);

      // Rewrite URLs in the response body
      InputStream upstreamResponseBodyInputStream =
          rewriteUrlsInResponseBody(downstreamRequest, upstreamResponse);

      // Stream the response
      streamResponse(downstreamResponse, upstreamResponseBodyInputStream);

      return null;
    };
  }

  /**
   * Applies headers to the servlet response. The headers are copied from the upstream response.
   *
   * @param headers the headers
   * @param servletResponse the servlet response
   */
  private void applyHeaders(HttpHeaders headers, HttpServletResponse servletResponse) {
    headers.forEach(
        (key, values) -> {
          for (String value : values) {
            servletResponse.addHeader(key, value);
          }
        });
  }

  /**
   * Strips the path prefixes. The path prefixes are the context path and the controller context.
   *
   * @param request the request
   * @return the stripped path prefixes
   */
  private String stripPathPrefixes(HttpServletRequest request) {
    String path = request.getServletPath();

    // If the path is empty or just "/", return an empty string
    if (path == null || path.isEmpty() || path.equals("/")) {
      return "";
    }

    // strip the controller context from the path
    return path.substring(path.indexOf(pathPrefix) + pathPrefix.length());
  }

  /**
   * Gets the context path. The context path is the first segment of the path.
   *
   * @param request the request
   * @return the context path
   */
  private String getContextPath(HttpServletRequest request) {

    String path = stripPathPrefixes(request);

    // Split the path into segments
    String[] segments = path.split("/");

    // The context path is typically the first segment
    if (segments.length > 1) {
      return "/" + segments[1];
    } else {
      return "";
    }
  }

  /**
   * Strips the mapped context. The mapped context is the context path that is mapped to the
   * upstream server.
   *
   * @param request the request
   * @return the stripped mapped context
   */
  private String stripMappedContext(HttpServletRequest request) {
    String path = stripPathPrefixes(request);

    String[] segments = path.split("/");
    if (segments.length > 1) {
      return path.substring(segments[1].length() + 1);
    } else {
      return path;
    }
  }

  /**
   * Copies request headers. The request headers are copied from the downstream request.
   *
   * @param request the request
   * @return the headers
   */
  private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();

    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      Enumeration<String> headerValues = request.getHeaders(headerName);

      while (headerValues.hasMoreElements()) {
        String headerValue = headerValues.nextElement();
        headers.add(headerName, headerValue);
      }
    }

    return headers;
  }

  /**
   * Filters response headers. The response headers are filtered to include only the headers that
   * are to be carried over.
   *
   * @param headers the headers
   * @return the filtered headers
   */
  private HttpHeaders filterResponseHeaders(HttpHeaders headers) {
    HttpHeaders filteredHeaders = new HttpHeaders();

    headers.forEach(
        (key, values) -> {
          if (CARRY_OVER_RESPONSE_HEADERS.contains(key.toLowerCase())) {
            filteredHeaders.put(key, values);
          }
        });

    return filteredHeaders;
  }

  /**
   * Gets the relay URL for a URL. The relay URL is the URL that is relayed to the downstream
   * server.
   *
   * @param baseUrl the base URL
   * @param location the location
   * @return the relay URL
   */
  private String getRelayUrlForUrl(String baseUrl, String location) {
    for (Map.Entry<String, String> entry : invertedContextMap.entrySet()) {
      if (location.startsWith(entry.getKey())) {
        String strippedUrl = location.substring(entry.getKey().length());
        return baseUrl + contextPath + pathPrefix + entry.getValue() + strippedUrl;
      }
    }
    return location;
  }

  /**
   * Inverts a map. The map is inverted such that the keys become the values and the values become
   * the keys.
   *
   * @param sourceMap the source map
   * @param <K> the key type
   * @param <V> the value type
   * @return the inverted map
   */
  private static <K, V> Map<V, K> invertMap(Map<K, V> sourceMap) {
    return sourceMap.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getValue, Map.Entry::getKey, (oldValue, newValue) -> oldValue));
  }

  /**
   * Handles redirection. The redirection is handled by rewriting the URL.
   *
   * @param downstreamRequest the downstream request
   * @param upstreamHttpResponse the upstream HTTP response
   * @param downstreamResponseHeaders the downstream response headers
   * @throws IOException if an I/O error occurs
   */
  private void handleRedirection(
      HttpServletRequest downstreamRequest,
      ClientHttpResponse upstreamHttpResponse,
      HttpHeaders downstreamResponseHeaders)
      throws IOException {
    if (upstreamHttpResponse.getStatusCode().is3xxRedirection()) {
      Optional.ofNullable(upstreamHttpResponse.getHeaders().getLocation())
          .ifPresent(
              location -> {
                String relayLocation =
                    getRelayUrlForUrl(getBaseUrl(downstreamRequest), location.toString());
                downstreamResponseHeaders.put(HttpHeaders.LOCATION, List.of(relayLocation));
              });
    }
  }

  /**
   * Gets the base URL. The base URL is the URL of the server.
   *
   * @param request the request
   * @return the base URL
   */
  private String getBaseUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  /**
   * Rewrites URLs in the response body. The URLs are rewritten to point to the relay server.
   *
   * @param downstreamRequest the downstream request
   * @param upstreamResponse the upstream response
   * @return the response body
   * @throws IOException if an I/O error occurs
   */
  private InputStream rewriteUrlsInResponseBody(
      HttpServletRequest downstreamRequest, ClientHttpResponse upstreamResponse)
      throws IOException {
    Optional<MediaType> contentType =
        Optional.ofNullable(upstreamResponse.getHeaders().getContentType());
    if (contentType.isEmpty()) {
      return upstreamResponse.getBody();
    } else if (!contentType.get().isPresentIn(mimeTypes)) {
      return upstreamResponse.getBody();
    }

    // Read the response body
    Charset charset =
        Optional.ofNullable(contentType.get().getCharset()).orElse(StandardCharsets.UTF_8);
    AtomicReference<InputStream> responseBodyStream =
        new AtomicReference<>(upstreamResponse.getBody());
    Optional.ofNullable(upstreamResponse.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))
        .ifPresent(
            encoding -> {
              try {
                responseBodyStream.set(
                    decompressResponseBody(upstreamResponse.getBody(), encoding));
              } catch (CompressorException | IOException e) {
                log.error("Failed to decompress response body", e);
              }
            });

    String responseBody =
        StreamUtils.copyToString(responseBodyStream.get(), Objects.requireNonNull(charset));

    String relayUrl = getBaseUrl(downstreamRequest) + contextPath + pathPrefix;

    for (Map.Entry<String, String> entry : invertedContextMap.entrySet()) {
      responseBody = responseBody.replace(entry.getKey(), relayUrl + entry.getValue());
    }

    // Replace all occurrences of the external host URL with the context path
    return new ByteArrayInputStream(responseBody.getBytes(charset));
  }

  /**
   * Decompresses the response body. The response body is decompressed if it is compressed.
   *
   * @param responseBodyStream the response body stream
   * @param contentEncoding the content encoding
   * @return the decompressed response body
   * @throws CompressorException if a compressor exception occurs
   * @throws IOException if an I/O error occurs
   */
  private InputStream decompressResponseBody(InputStream responseBodyStream, String contentEncoding)
      throws CompressorException, IOException {
    CompressorInputStream decompressedStream;
    switch (contentEncoding.toLowerCase()) {
      case "gzip":
      case "br":
      case "deflate":
      case "zstd":
        decompressedStream =
            new CompressorStreamFactory()
                .createCompressorInputStream(contentEncoding, responseBodyStream);
        break;
      default:
        // If the encoding is not supported, return the original stream
        log.warn(
            "Upstream response encoding <{}> is not supported."
                + " Response content will pass through as received.",
            contentEncoding);
        return responseBodyStream;
    }
    return decompressedStream;
  }

  /**
   * Streams the response. The response is streamed to the downstream server.
   *
   * @param servletResponse the servlet response
   * @param responseBodyStream the response body stream
   * @throws IOException if an I/O error occurs
   */
  private void streamResponse(HttpServletResponse servletResponse, InputStream responseBodyStream)
      throws IOException {
    StreamUtils.copy(responseBodyStream, servletResponse.getOutputStream());
    log.debug("Response streamed successfully.");
  }
}
