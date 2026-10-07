NAMESPACE="${NAMESPACE:-hmpps-electronic-monitoring-crime-matching-dev}"
POLICE_FORCE_EMAILS_SECRET_NAME="hmpps-electronic-monitoring-crime-matching-api-police-force-recipient-emails"
KENT_EMAIL_SECRET_NAME="hmpps-electronic-monitoring-crime-matching-api-kent-email-address"
DEPLOYMENT="deployment/hmpps-electronic-monitoring-crime-matching-api"

SPRING_APPLICATION_JSON='{
  "notify": {
    "police-force-recipient-emails": {
      "AVON_AND_SOMERSET": ["xxx@justice.gov.uk"],
      "BEDFORDSHIRE": ["xxx@justice.gov.uk"],
      "CHESHIRE": ["xxx@justice.gov.uk"],
      "CITY_OF_LONDON": ["xxx@justice.gov.uk"],
      "CUMBRIA": ["xxx@justice.gov.uk"],
      "DERBYSHIRE": ["xxx@justice.gov.uk"],
      "DURHAM": ["xxx@justice.gov.uk"],
      "ESSEX": ["xxx@justice.gov.uk"],
      "GLOUCESTERSHIRE": ["xxx@justice.gov.uk"],
      "GWENT": ["xxx@justice.gov.uk"],
      "HAMPSHIRE": ["xxx@justice.gov.uk"],
      "HERTFORDSHIRE": ["xxx@justice.gov.uk"],
      "HUMBERSIDE": ["xxx@justice.gov.uk"],
      "KENT": ["xxx@justice.gov.uk"],
      "METROPOLITAN": ["xxx@justice.gov.uk"],
      "NORTH_WALES": ["xxx@justice.gov.uk"],
      "NOTTINGHAMSHIRE": ["xxx@justice.gov.uk"],
      "SUSSEX": ["xxx@justice.gov.uk"],
      "WEST_MIDLANDS": ["xxx@justice.gov.uk"]
    }
  }
}'

# Delete existing secrets
kubectl -n "$NAMESPACE" \
  delete secret "$POLICE_FORCE_EMAILS_SECRET_NAME" --ignore-not-found

kubectl -n "$NAMESPACE" \
  delete secret "$KENT_EMAIL_SECRET_NAME" --ignore-not-found

# Create new secrets
kubectl -n "$NAMESPACE" \
  create secret generic "$POLICE_FORCE_EMAILS_SECRET_NAME" \
  --from-literal="SPRING_APPLICATION_JSON=${SPRING_APPLICATION_JSON}"

kubectl -n "$NAMESPACE" \
  create secret generic "$KENT_EMAIL_SECRET_NAME" \
  --from-literal="kent_email_address=xxx@justice.gov.uk"

# Refresh API pods (to pick up updated secrets)
kubectl -n "$NAMESPACE" \
  rollout restart "$DEPLOYMENT"