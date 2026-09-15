# Create an example email file

1. Create an example CSV to attach to a test email, e.g. example.csv
1. cp email-file.eml example.eml
1. base64 -i example.csv
1. Copy the output from the clipboard and replace the base64 content in example.eml with it.
1. Run ./scripts/localstack-ingest-sample-email.sh with the API running locally to ingest it.