# Logging

This file is to store helpful logging snippets / configs to aid in development. 

## AWS

To log AWS SDK requests to the console, this snippet can be added to the `logback-spring.xml` file.

```xml
<logger name="software.amazon.awssdk.request" additivity="false" level="DEBUG">
  <appender-ref ref="consoleAppender"/>
</logger>
```

The output from the SDK logs is fairly limited, it might tell you the name of the headers sent with a request but 
doesn't show the values that were sent. This can make it difficult to construct wiremock responses, especially when most 
AWS requests are differentiated by the `X-Amz-Target` header.
```xml
<logger name="org.apache.http.wire" level="DEBUG">
  <appender-ref ref="consoleAppender"/>
</logger>
```

## Athena Queries

By default the application is configured with a log level of `DEBUG` for any class path within 
`uk.gov.justice.digital.hmpps`. To change the log level of the Athena query logging, the following snippet can be added 
to `logback-spring.xml`.

```xml
<logger name="uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient" additivity="false" level="INFO">
  <appender-ref ref="consoleAppender"/>
</logger>
```