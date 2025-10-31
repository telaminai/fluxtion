# fluxtion-aws-generator-client

Java client library for the fluxtion-aws-generator REST API.

- Simple HTTP client wrapper (AwsGeneratorClient)
- SourceGenerator implementation (FluxtionSourceGeneratorClient) that posts RemoteGenerationRequest to /generation
- SLF4J logging

## Usage
Add dependency:
```
<dependency>
  <groupId>com.telamin.fluxtion</groupId>
  <artifactId>fluxtion-aws-generator-client</artifactId>
  <version>${project.version}</version>
</dependency>
```

When using Fluxtion, select the SourceGenerator by id "aws-http" and pass system properties:
- fluxtion.sourceGeneratorId: aws-http (or set programmatically via EventProcessorGenerator.SOURCE_GENERATOR_ID_PROPERTY)
- fluxtion.aws.apiBaseUrl: e.g. http://127.0.0.1:3000 (SAM local) or API Gateway base URL
- fluxtion.aws.apiKey: token for Authorization header (optional for local)
- fluxtion.aws.zip: true to request zipped output

Note: This module includes a META-INF/services descriptor so the implementation is auto-discovered via ServiceLoader.

## Example
```
System.setProperty("fluxtion.sourceGeneratorId", "aws-http");
System.setProperty("fluxtion.aws.apiBaseUrl", "http://127.0.0.1:3000");
System.setProperty("fluxtion.aws.apiKey", "TEST");
System.setProperty("fluxtion.aws.zip", "false");
```


## License
This module is distributed under the Telamin Fluxtion Commercial License v1.0. See the LICENSE file at the repository root for details.

Distributed JARs include META-INF/LICENSE.txt and META-INF/NOTICE.txt.

Unauthorized use, copying, modification, redistribution/resale, or offering the Software as a service is prohibited without a commercial agreement with Telamin Limited.
