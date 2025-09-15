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

