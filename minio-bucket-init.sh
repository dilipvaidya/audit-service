#!/bin/bash
BUCKET_NAME="${MINIO_BUCKET:-dev-s3-bucket}"
ACCESS_KEY="minioadmin"
SECRET_KEY="minioadmin"
ENDPOINT="http://minio:9000"

until curl -s -o /dev/null $ENDPOINT/minio/health/ready; do
  echo "Waiting for MinIO to be ready..."
  sleep 2
done

mc alias set local $ENDPOINT $ACCESS_KEY $SECRET_KEY
mc mb -q --ignore-existing local/$BUCKET_NAME
