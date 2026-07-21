package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.sqs

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.sqs.HmppsErrorVisibilityHandler
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueDestinationContainerFactory
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.telemetry.TraceExtractingMessageInterceptor

@Configuration
class SqsConfig {

  @Bean("email-sqs-listener-factory")
  fun emailSqsListenerContainerFactory(
    @Qualifier("email-sqs-client") sqsClient: SqsAsyncClient,
    @Qualifier("email-sqs-dlq-client") sqsDlqClient: SqsAsyncClient,
    hmppsSqsProperties: HmppsSqsProperties,
    hmppsErrorVisibilityHandler: HmppsErrorVisibilityHandler,
    jsonMapper: JsonMapper,
  ): HmppsQueueDestinationContainerFactory {
    val queueConfig = hmppsSqsProperties.queues["email"]
      ?: throw IllegalStateException("Queue 'email' not found in HmppsSqsProperties")

    val hmppsQueue = HmppsQueue(
      id = "email",
      sqsClient = sqsClient,
      queueName = queueConfig.queueName,
      sqsDlqClient = sqsDlqClient,
      dlqName = queueConfig.dlqName.ifEmpty { null },
    )

    val factory = SqsMessageListenerContainerFactory.builder<Any>()
      .sqsAsyncClient(sqsClient)
      .messageInterceptor(TraceExtractingMessageInterceptor(jsonMapper, queueConfig.propagateTracing))
      .messageInterceptor(LoggingContextMessageInterceptor())
      .errorHandler(
        object : ErrorHandler<Any> {
          override fun handle(message: Message<Any>, t: Throwable) {
            hmppsErrorVisibilityHandler.setErrorVisibilityTimeout(message, hmppsQueue)
            throw t
          }
        },
      )
      .build()

    return HmppsQueueDestinationContainerFactory("email", factory)
  }
}
