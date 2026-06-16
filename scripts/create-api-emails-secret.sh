NAMESPACE="${NAMESPACE:-hmpps-electronic-monitoring-crime-matching-dev}"
SECRET_NAME="hmpps-electronic-monitoring-crime-matching-api-valid-ingestion-emails"
DEPLOYMENT="deployment/hmpps-electronic-monitoring-crime-matching-api"

# Delete existing secret
kubectl -n $NAMESPACE \
  delete secret $SECRET_NAME --ignore-not-found

# Create new secret
kubectl -n $NAMESPACE \
  create secret generic $SECRET_NAME \
  --from-literal="avon-and-somerset"=xxx@justice.gov.uk \
  --from-literal="bedfordshire"=xxx@justice.gov.uk \
  --from-literal="cheshire"=xxx@justice.gov.uk \
  --from-literal="city-of-london"=xxx@justice.gov.uk \
  --from-literal="cumbrian"=xxx@justice.gov.uk \
  --from-literal="derbyshire"=xxx@justice.gov.uk \
  --from-literal="durham"=xxx@justice.gov.uk \
  --from-literal="essex"=xxx@justice.gov.uk \
  --from-literal="gloucestershire"=xxx@justice.gov.uk \
  --from-literal="gwent"=xxx@justice.gov.uk \
  --from-literal="hampshire"=xxx@justice.gov.uk \
  --from-literal="hertfordshire"=xxx@justice.gov.uk \
  --from-literal="humberside"=xxx@justice.gov.uk \
  --from-literal="kent"=xxx@justice.gov.uk \
  --from-literal="metropolitan"=xxx@justice.gov.uk \
  --from-literal="north-wales"=xxx@justice.gov.uk \
  --from-literal="nottinghamshire"=xxx@justice.gov.uk \
  --from-literal="sussex"=xxx@justice.gov.uk \
  --from-literal="west-midlands"=xxx@justice.gov.uk

# Refresh API pods (to pick up updated secret)
kubectl -n $NAMESPACE \
  rollout restart $DEPLOYMENT

