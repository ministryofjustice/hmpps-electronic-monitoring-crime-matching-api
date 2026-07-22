package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.sqs

import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor
import io.opentelemetry.api.trace.Span
import org.slf4j.MDC
import org.springframework.messaging.Message

class LoggingContextMessageInterceptor : MessageInterceptor<Any> {

  override fun intercept(message: Message<Any>): Message<Any> {
    val span = Span.current()
    val messageId = message.headers["id"]?.toString()

//    messageId?.let { MDC.put("messageId", it) }
//    messageId?.let { span.setAttribute("aws.sqs.message_id", it) }

    return message
  }

  override fun afterProcessing(message: Message<Any>, t: Throwable?) {
    MDC.remove("messageId")
  }
}
