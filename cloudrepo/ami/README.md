# AWS AMI for CSYE 6225

## Validate Template

packer validate  ami.json

## Build AMI prod

sh
packer build \
    -var 'aws_access_key=REDACTED' \
    -var 'aws_secret_key=REDACTED' \
    -var 'aws_region=us-east-1' \
    -var 'subnet_id=REDACTED' \
    ami.json


or 


```
packer build -var-file=./vars.json ami.json