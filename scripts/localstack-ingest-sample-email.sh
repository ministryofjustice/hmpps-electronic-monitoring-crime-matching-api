#!/usr/bin/env bash
set -euo pipefail

REGION=eu-west-2
BUCKET=police-emails
QUEUE_NAME=email
LOCALSTACK_ENDPOINT="http://localhost:4566"

EMAIL_FILE=./scripts/fixtures/email-file.eml
S3_KEY="samples/email-file.eml"

DB_CONTAINER="query-db"
DB_USER="postgres"
DB_NAME="postgres"

if [ ! -f "$EMAIL_FILE" ]; then
  echo "Sample Email file not found: $EMAIL_FILE" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  echo "Postgres container not running: $DB_CONTAINER" >&2
  echo "Run: docker ps  (and start it if needed)" >&2
  exit 1
fi

echo "Checking Localstack is reachable..."
curl -fsS "$LOCALSTACK_ENDPOINT/_localstack/health" >/dev/null

echo "Ensuring S3 bucket exists: $BUCKET"
if ! awslocal s3api head-bucket --bucket "$BUCKET" >/dev/null 2>/dev/null; then
  awslocal s3api create-bucket \
    --bucket "$BUCKET" \
    --region "$REGION" \
    --create-bucket-configuration "{\"LocationConstraint\": \"$REGION\"}" \
    >/dev/null
fi

echo "Ensuring SQS queue exists: $QUEUE_NAME"
if ! awslocal sqs get-queue-url --queue-name "$QUEUE_NAME" >/dev/null 2>/dev/null; then
  awslocal sqs create-queue --queue-name "$QUEUE_NAME" >/dev/null
fi

echo "Resolving SQS queue URL..."
QUEUE_URL=$(awslocal sqs get-queue-url \
  --queue-name "$QUEUE_NAME" \
  --query QueueUrl \
  --output text)

echo "Clearing application data from public schema tables in Postgres (keeping Flyway history)..."
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 >/dev/null <<'SQL'
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN (
    SELECT tablename
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename <> 'flyway_schema_history'
  )
  LOOP
    EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' CASCADE';
  END LOOP;
END $$;
SQL

echo "Uploading sample email to S3: s3://$BUCKET/$S3_KEY"
awslocal s3 cp "$EMAIL_FILE" "s3://$BUCKET/$S3_KEY" >/dev/null

echo "Sending SQS message to trigger ingestion..."
awslocal sqs send-message \
  --queue-url "$QUEUE_URL" \
  --message-body "{
    \"Type\": \"Notification\",
    \"MessageId\": \"$(uuidgen)\",
    \"Message\": \"{\\\"notificationType\\\":\\\"Received\\\",\\\"receipt\\\":{\\\"action\\\":{\\\"type\\\":\\\"S3\\\",\\\"bucketName\\\":\\\"$BUCKET\\\",\\\"objectKeyPrefix\\\":\\\"\\\",\\\"objectKey\\\":\\\"$S3_KEY\\\"}}}\"
  }" >/dev/null

echo "Waiting briefly for ingestion to complete..."
sleep 1

echo "Fetching IDs from Postgres to be used in Postman API requests..."
CRIME_BATCH_UUID=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
  "select id from crime_batch order by created_at desc limit 1;")

CRIME_BATCH_REF=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
  "select batch_id from crime_batch order by created_at desc limit 1;")

CRIME_VERSION_ID=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c \
  "select id from crime_version order by created_at desc limit 1;")

echo ""
echo "Ingested sample data"
echo "crimeBatchId (UUID): $CRIME_BATCH_UUID"
echo "batchId (ref):       $CRIME_BATCH_REF"
echo "crimeVersionId:      $CRIME_VERSION_ID"
echo ""
