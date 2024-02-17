# Notes for gothbuzz-signup
## Followed the steps in this article
[DEPLOY AN HTTP FUNCTION TO GOOGLE CLOUD FUNCTIONS](https://guides.micronaut.io/latest/micronaut-google-cloud-http-function-gradle-java.html)
## Created the base project here
[Micronaut launcher](https://micronaut.io/launch)
## Gcloud SSO
    gcloud auth login
## Gcloud select project
    gcloud config set project gothbuzz-signup
## Upload the function with
     gcloud functions deploy signup --entry-point io.micronaut.gcp.function.http.HttpFunction --runtime java21 --trigger-http --gen2 --allow-unauthenticated         
## Created a Google Cloud Storage bucket with:
    gcloud storage buckets create gs://<name of bucket> --location=europe-west1