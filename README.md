# hmpps-electronic-monitoring-crime-matching-api

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-electronic-monitoring-crime-matching-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-electronic-monitoring-crime-matching-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-electronic-monitoring-crime-matching-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-electronic-monitoring-crime-matching-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

# Contents
- [About this project](#about-this-project)
- [Running the application locally](#running-the-application-locally)
  - [Running the application in intellij](#running-the-application-in-intellij)
  - [Running the application with connection to Athena](#running-the-application-with-connection-to-dev-athena)
  - [Running the application with mocked Athena](#running-the-application-with-mocked-athena)

# About this project

An API used by the Electronic Monitoring Crime Matching UI, a service that allows users to track subject location data, 
breach data and matched crime data.

It is built using [Spring Boot](https://spring.io/projects/spring-boot/) and [Kotlin](https://kotlinlang.org/) as well as the following technologies for its infrastructure:
- [AWS](https://aws.amazon.com/) - Services utilise AWS features through Cloud Platform.
- [Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/#cloud-platform-user-guide) - Ministry of
  Justice's (MOJ) cloud hosting platform built on top of AWS which offers numerous tools such as logging, monitoring and
  alerting for our services.
- [Docker](https://www.docker.com/) - The API is built into docker images which are deployed to our containers.
- [Kubernetes](https://kubernetes.io/docs/home/) - Creates 'pods' to host our environment. Manages auto-scaling, load
  balancing and networking to our application.

## Running the application locally

The application comes with a `local` spring profile that includes default settings for running locally.

There is also a `docker-compose.yml` that can be used to run a local instance in docker and also an
instance of HMPPS Auth.

```bash
docker compose pull && docker compose up
```

will build the application and run it and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-electronic-monitoring-crime-matching-api=0
```
will just start a docker instance of HMPPS Auth. The application should then be started with a `local` active profile
in Intellij.

### Running the application with connection to dev Athena

#### Acquiring local credentials
1. Configure KubeCtl to let you connect to the Cloud Platform Kubernetes cluster - [follow this guide](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html)
2. Set the kubectl context to the dev environment: `kubectl config set-context --current --namespace=hmpps-electronic-monitoring-crime-matching-dev`
3. Get details for the service pod that you can use to query AWS: `kubectl get pods`. One should have a name indicating it's a service account similar to `hmpps-em-crime-matching-dev-service-pod-#Z###ZZZ##-Z####`.
4. Ssh into this service pod: `kubectl exec --stdin --tty YOUR_SERVICE_POD_NAME_FROM_THE_PREV_STEP -- /bin/bash`
   > Confirm you've signed in correctly by running `aws sts get-caller-identity` - this should return a response with an ARN matching the pattern `arn:aws:sts::############:assumed-role/cloud-platform-irsa-abc123xyz-live/botocore-session-##########`
5. Assume the correct role ([AWS docs](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/sts/assume-role.html)): `aws sts assume-role --role-arn YOUR_ATHENA_ROLE_ARN --role-session-name cli-session`
   > This will return AWS credentials including  a SessionToken, which will last around an hour

_You now have Athena credentials_ - they will last for 1 hour.

#### Run configuration setup
1. Edit your Spring Boot configuration file to include the following environment variables you retrieved in [acquiring local credentials](#acquiring-local-credentials):
  - `AWS_ACCESS_KEY_ID` = value you retrieved (no quotes)
  - `AWS_SECRET_ACCESS_KEY` = value you retrieved (no quotes)
  - `AWS_SESSION_TOKEN` = value you retrieved (no quotes)
  - `FLAG_USE_LOCAL_CREDS` = `true`
2. The [EmDatastoreRoleProvider](src/main/kotlin/uk/gov/justice/digital/hmpps/electronicmonitoringdatastoreapi/client/EmDatastoreRoleProvider.kt)`.getRole()` method will now use these values to create the athena connection at runtime.
3. To disable this, just set `FLAG_USE_LOCAL_CREDS` to `false`

This should pick up the values you set in your environment variables as per the [AWS Java SDK docs](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

### Running the application with mocked Athena
It's possible to run the application without a connection to Athena and mock responses using the `mocking` spring profile.

### Code coverage
This project has Jacoco integrated and this will run after each test run. The generated report can be found [here](build/reports/jacoco/test/html/index.html) and can be opened in your browser.
