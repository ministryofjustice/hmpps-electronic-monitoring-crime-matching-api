# Observability

## Tooling

### Grafana Dashboards
- [Resource Usage](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload)
  - Filter by namespace & workload
    - e.g namespace = hmpps-electronic-monitoring-crime-matching-{environment}
    - workload = hmpps-electronic-monitoring-crime-matching-api
- [Kubernetes Pod View](https://grafana.live.cloud-platform.service.justice.gov.uk/d/k8s_views_pods/kubernetes-views-pods) 
  - Filter by namespace & pod
    - e.g namespace = hmpps-electronic-monitoring-crime-matching-{environment}
    - pod = hmpps-electronic-monitoring-crime-matching-api-*
- [Nginx Ingress Controller](https://grafana.live.cloud-platform.service.justice.gov.uk/d/nginx-ingress-controller/nginx-ingress-controller) 
  - Filter by namespace & service
    - e.g namespace = hmpps-electronic-monitoring-crime-matching-{environment}
    - service = hmpps-electronic-monitoring-crime-matching-api
- [AWS RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds)
  - Filter by instance identifier acquired from secrets
- [Application Alerts](https://grafana.live.cloud-platform.service.justice.gov.uk/d/application-alerts/application-alerts)
  - Filter by namespace 
    - e.g hmpps-electronic-monitoring-crime-matching-{environment}

### OpenSearch
- Dev - [OpenSearch Logs](https://app-logs.cloud-platform.service.justice.gov.uk/_dashboards/goto/e20d441cafa106b88469eb0699463c90?security_tenant=global)
- Preprod - [OpenSearch Logs](https://app-logs.cloud-platform.service.justice.gov.uk/_dashboards/goto/1b595aef5dcf08aa6f900433ebf85ef6?security_tenant=global)
- Prod - [OpenSearch Logs](https://app-logs.cloud-platform.service.justice.gov.uk/_dashboards/goto/982962531e7a4443d1d09b6f49389a45?security_tenant=global)

### Application Insights
Follow the steps to access Application Insights:
- [Application insights](https://portal.azure.com/#browse/microsoft.insights%2Fcomponents)
- Navigate to the correct environment
    - nomisapi-t3 (dev)
    - nomisapi-preprod
    - nomisapi-prod

#### Querying the logs
- Navigate to **Logs** within the **Monitoring** tab in the sidebar
- Query to retrieve requests logs
  - ```requests | where cloud_RoleName == 'hmpps-electronic-monitoring-crime-matching-api'```
- Query to retrieve exceptions logs
  - ```exceptions | where cloud_RoleName == 'hmpps-electronic-monitoring-crime-matching-api'```

#### Viewing end to end transactions or failures
- Navigate to **Performance** or **Failures** within the **Investigate** tab in the sidebar
- Filter **Roles** to 'hmpps-electronic-monitoring-crime-matching-api'
- Select a time window and search for a specific operation such as 'GET /ingestion-attempts'
- Select the operation then **Drill into…** and find the relevant sample, this will provide end to end transaction details of that operation, allowing visibility of the services that were interacted with and their status during the request.

## Police Data Ingestion

Observability tooling that can be used to monitor the status of police data ingestion.

### Grafana Dashboards
- [Police Data Ingestion](https://grafana.live.cloud-platform.service.justice.gov.uk/d/afsdv98od660we/police-data-ingestion-dashboard)
  - Filter by queue or DLQ
- [AWS S3](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSS31iWk/aws-s3) 
  - Filter by police-emails bucket
- [AWS SNS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSNS001/aws-sns)
  - Filter by topic
- [AWS SQS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs)
  - Filter by queue or DLQ

## Runbooks
- [Investigating Email Notifications DLQ Messages](email-notifications-dlq-runbook.md)
  (covers both `emailsend-dlq` DEAD rows and `FAILED` rows that bypass the DLQ)

## Design & Testing
- [Email Outbox: flow diagrams (happy & sad paths)](email-outbox-flow.md)
- [Email Outbox: local & dev testing guide](email-outbox-testing.md)
