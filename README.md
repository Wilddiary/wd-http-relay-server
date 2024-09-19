<!--
Licensed to the Wilddiary.com under one or more contributor license
agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.
Wilddiary.com licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Wilddiary HTTP Relay Server
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/Wilddiary/wd-http-relay-server)](https://github.com/Wilddiary/wd-http-relay-server/graphs/contributors)
[![Build](https://github.com/Wilddiary/wd-http-relay-server/actions/workflows/maven-build.yml/badge.svg)](https://github.com/Wilddiary/wd-http-relay-server/actions/workflows/maven-build.yml)
[![GitHub last commit](https://img.shields.io/github/last-commit/Wilddiary/wd-http-relay-server)](https://github.com/Wilddiary/wd-http-relay-server/commits/main/)
[![GitHub Release](https://img.shields.io/github/v/release/Wilddiary/wd-http-relay-server)](https://github.com/Wilddiary/wd-http-relay-server/releases)
[![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/Wilddiary/wd-http-relay-server)](https://github.com/Wilddiary/wd-http-relay-server/issues)

This project is a Spring-based HTTP relay server that forwards incoming HTTP requests to external servers based on servlet context mapping. It supports relaying headers, query parameters, HTTP body, and handling responses including string, binary, and compressed formats (e.g., GZIP, Brotli). The server also handles rewriting URLs in the upstream HTTP response to point to the relay server for all mapped domains. This project is useful for scenarios where you need to relay requests to external services through your domain where routing through a proxy server is not possible.

## Features

1. Relays requests based on URL context mapping to external hosts.
2. Relays request headers, query parameters, and body.
3. Handles any type of response (e.g., string, binary).
4. Supports response compression (GZIP, Brotli, Deflate).
5. Decompresses incoming responses if needed.
6. Streams chunked responses and handles large payloads.
7. Filters headers before returning the response.
8. Follows redirects from the external host. Enabled by default. Configurable through the property `relay.follow-redirects`.
9. Rewrites URLs in the response to point to the relay server.
10. Supports all common HTTP methods (GET, POST, PUT, DELETE).
11. Handles errors from unreachable external hosts or response errors.
12. Easy to configure and deploy.
13. Open-source and licensed under Apache 2.0 License.

## Prerequisites

Before running this project, ensure you have the following installed:

1. Java 17+
2. Maven 3.6+

## Getting Started

### Clone the Repository
```bash
git clone https://github.com/Wilddiary/wd-http-relay-server.git
cd wd-http-relay-server
```

### Build the Project
Use Maven to build the project:

```bash
mvn clean install
```

### Configuration
Configure the mappings between servlet contexts and external hosts in the [application.properties](./src/main/resources/application.properties) file.

```application.yml
relay.context.mapping={\
  "/gh" : "https://github.com", \
  "/nz" : "https://www.stats.govt.nz" \
}
```

This configuration defines that all requests with `/gh` context will be relayed to https://github.com and `/nz` to https://www.stats.govt.nz.

### Run the Project
You can run the Spring Boot application directly using Maven:

```bash
mvn spring-boot:run
```

Or package it as a JAR and run it:

```bash
java -jar target/wd-http-relay-server-1.0.0.jar
```

### Usage

Once the server is running, you can make requests to the relay server, and it will forward them to the corresponding external servers based on the context mapping.

For example, with the following configuration:

```yaml
relay.context.mapping={\
  "/gh" : "https://github.com", \
  "/nz" : "https://www.stats.govt.nz" \
}
```

If you make a request to:

```bash
GET http://localhost:8080/relay/gh/emn178/js-sha256/archive/refs/tags/v0.11.0.tar.gz
```
The relay server will forward the request to:

```
GET http://github.com/emn178/js-sha256/archive/refs/tags/v0.11.0.tar.gz
```

### Compression
The relay server will compress the response if the client can handle compressed responses. If the client does not support compression, the response will pass through as uncompressed.

### Supported Request Types
The relay server supports all common HTTP methods including GET, POST, PUT, DELETE.

### Advanced Configuration

#### Header Filtering
Certain headers can be filtered out from the response before returning it to the client. You can modify the list of headers to be filtered by adjusting the `CARRY_OVER_RESPONSE_HEADERS` list in [RelayServiceImpl.java](./src/main/java/com/wilddiary/ws/services/RelayServiceImpl.java).


### Error Handling
In case the external host is unreachable or responds with an error, the relay server will propagate the error back to the client. For requests received for contexts that are not mapped, the relay server responds with a `404 Not Found` error.

### Testing

You can test the relay functionality using tools like curl or Postman.

**Example**:

```bash
curl -X GET 'http://localhost:8080/relay/gh/emn178/js-sha256/archive/refs/tags/v0.11.0.tar.gz' \
-H 'Accept-Encoding: gzip'
```
This request will be forwarded to the mapped external host for `/gh`, and the response will be returned with GZIP compression if supported.

### License

This project is licensed under the Apache 2.0 License. See the LICENSE file for more details.

### Contributing

Contributions are welcome! Please feel free to submit a Pull Request for any enhancements, fixes, or improvements. Refer to [CONTRIBUTING.md](./CONTRIBUTING.md) for more details on how to contribute.
