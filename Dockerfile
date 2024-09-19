#
# Licensed to the Wilddiary.com under one or more contributor license
# agreements.  See the NOTICE file distributed with this work for
# additional information regarding copyright ownership.
# Wilddiary.com licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#
FROM amazoncorretto:17.0.12-al2023-headless
LABEL authors="Dr0na"
ARG VERSION
WORKDIR /home/wilddiary

COPY target/wd-http-relay-server-$VERSION.jar service.jar

ENTRYPOINT [ "java", "-jar", "service.jar" ]