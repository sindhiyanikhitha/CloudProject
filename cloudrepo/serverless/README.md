# serverless

## Email Service Using AWS Lambda Function
As a user,
> You will be able to request your due bills in x days

## Getting Started
Clone the repository on your local machine

# Task 1: AWS CLI Command For CloudFormation

#### CREATE CLOUDFORMATION STACK

aws cloudformation create-stack \
  --stack-name csye6225demo \
  --parameters ParameterKey=InstanceTypeParameter,ParameterValue=t2.micro \
  --template-body file://application.json

#### DELETE CLOUDFORMATION STACK

aws cloudformation delete-stack --stack-name csye6225demo

WAIT FOR CLOUDFORMATION STACK DELETION

aws cloudformation wait stack-delete-complete --stack-name csye6225demo


# Task 2: Trigger Circle CI for EmailLambda.jar for CI/CD

 aws lambda update-function-code --function-name  EmailLambda  --s3-bucket ${BUCKET_NAME} --s3-key EmailLambda.jar --region ${AWS_REGION}