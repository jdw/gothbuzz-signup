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
    gcloud config set project <project id>

or

    gcloud config get project
### Enable secrets and cloud functions API for project
    gcloud services enable secretmanager.googleapis.com cloudfunctions.googleapis.com
    gcloud secrets create <secret name> --replication-policy="automatic"
    gcloud secrets versions add <secret name> --data-file <filename>
    gcloud iam service-accounts create <project name>-identity
    gcloud secrets add-iam-policy-binding <secret name> \
        --member serviceAccount:<project name>-identity@<project id>.iam.gserviceaccount.com \
        --role roles/secretmanager.secretAccessor
### Compile and upload
    ./gradlew shadowJar
    cd build/libs
    (remove all JARs except the latest)
     gcloud functions deploy <gradle project.name> --entry-point io.micronaut.gcp.function.http.HttpFunction --runtime java17 --trigger-http --gen2 --allow-unauthenticated

to expose environment variables or secrets to the funciton, add
    
    --set-env-vars "<nane>=<value>"
    --set-secrets "<name to expose as envvar>=<secret name>:latest"
### Created a Google Cloud Storage bucket with:
    gcloud storage buckets create gs://<name of bucket> --location=europe-west1
## Service accounts and their keys
### List all service accounts for project
    gcloud iam service-accounts list
### Create service account and make sure it can read and write to bucket
#### Create account
    gcloud iam service-accounts create SERVICE_ACCOUNT_NAME \
    --description="DESCRIPTION" \
    --display-name="DISPLAY_NAME"
#### Create service account key
    gcloud iam service-accounts keys create <filename> \                      
    --iam-account=<service account name>@gothbuzz-signup.iam.gserviceaccount.com
#### Make service account have read and write permissions to the bucket
    gsutil iam ch serviceAccount:<service account email>:objectAdmin gs://<bucket name>
    gsutil iam ch serviceAccount:<service accout email>:legacyBucketReader gs://<bucket name>

## Domain mapping
### Verify domain
    gcloud domains verify <domain> --project=<project id> --location=<region>
## Create load balancer and SSL certificate
### Create externally accessible IP
    gcloud compute addresses create gothbuzz-loadbalancer-ip \
    --network-tier=PREMIUM \
    --ip-version=IPV4 \
    --global
### Find IP
    gcloud compute addresses describe gothbuzz-loadbalancer-ip \
    --format="get(address)" \
    --global
### Create a network endpoint group
    gcloud compute network-endpoint-groups create gothbuzz-neg-name \  
    --region=europe-west1 \
    --network-endpoint-type=serverless  \
    --cloud-run-service=signup     
### Create backend rule set
    gcloud compute backend-services create gothbuzz-backend \    
    --load-balancing-scheme=EXTERNAL \
    --global
### Associate backend rule set and NEG
    gcloud compute backend-services add-backend gothbuzz-backend \    
    --global \
    --network-endpoint-group=gothbuzz-neg-name \  
    --network-endpoint-group-region=europe-west1
### Create a URL map
    gcloud compute url-maps create gothbuzz-url-map \
    --default-service gothbuzz-backend 
### Create HTTPS proxy
    gcloud compute target-https-proxies create gothbuzz-https-proxy \   
    --ssl-certificates=gothbuzz \            
    --url-map=gothbuzz-url-map
### Create forwarding rule set
    gcloud compute forwarding-rules create gothbuzz-https-forwarding \
    --load-balancing-scheme=EXTERNAL \
    --network-tier=PREMIUM \
    --address=gothbuzz-loadbalancer-ip \
    --target-https-proxy=gothbuzz-https-proxy \
    --global \
    --ports=443
### Check status of SSL certificate
    gcloud compute ssl-certificates describe gothbuzz \
    --global \
    --format="get(name,managed.status)"