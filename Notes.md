# Notes for gothbuzz-signup
## Prerequisites
### Expected environment variables
* GOTHBUZZ_BUCKET_NAME
* GOTHBUZZ_ENVIRONMENT_NAME
## How to build and run
### Followed the steps in this article
[DEPLOY AN HTTP FUNCTION TO GOOGLE CLOUD FUNCTIONS](https://guides.micronaut.io/latest/micronaut-google-cloud-http-function-gradle-java.html)
### Created the base project here
[Micronaut launcher](https://micronaut.io/launch)
### Run the project locally
    ./gradlew run -t
### Gcloud SSO
    gcloud auth login
### Gcloud select project
    gcloud config set project gothbuzz-signup
### Compile and upload
    ./gradlew shadowJar
    cd build/libs
    (remove all JARs except the latest)
     gcloud functions deploy signup --entry-point io.micronaut.gcp.function.http.HttpFunction --runtime java17 --trigger-http --gen2 --allow-unauthenticated         
### Created a Google Cloud Storage bucket with:
    gcloud storage buckets create gs://<name of bucket> --location=europe-west1