echo "AWS Account Name "

read account

echo "Create Stack Name: "

read stack_name

echo "Key Pair: "

read KeyPair

echo "VPC Name: "

read vpcName

echo "AWS Region: "

read vpcRegion

echo "VPC CIDR: "

read vpcCidr


echo "-------------Public Subnets------------"

echo "Public Subnet 1:"

echo "Name of Public Subnet 1:"

read pubSubnet1

echo "Enter CIDR for Public Subnet 1: "

read pubSubnet1CIDR

echo "Public Subnet 2"

echo "Name of Public Subnet 2: "

read pubSubnet2

echo "Enter CIDR for Public Subnet 2:"

read pubSubnet2CIDR

echo "Public Subnet 3"

echo "Name of Public Subnet 3: "

read pubSubnet3

echo "Enter CIDR for Public Subnet 3: "

read pubSubnet3CIDR

echo "Database User: "

read dbUser

echo "Database Password: "

read dbPassword

echo " "



echo "-------------Private Subnets------------"

echo "Private Subnet 1"

echo "Name of Private Subnet 1: "

read pvtSubnet1

echo "Enter CIDR for Private Subnet 1: "

read pvtSubnet1CIDR

echo "Private Subnet 2"

echo "Name of Private Subnet 2: "

read pvtSubnet2

echo "Enter CIDR for Private Subnet 2: "

read pvtSubnet2CIDR

echo "Private Subnet 3"

echo "Name of Private Subnet 3: "

read pvtSubnet3

echo "Enter CIDR for Private Subnet 3: "

read pvtSubnet3CIDR

echo "AMI ID: "

read ImageID

echo "SIZE OF EC2 INSTANCE "

read EC2Size

echo "SIZE OF RDS VOLUME  "

read RDSSize

echo "Code Deploy Bucket Name "

read CodeDeployS3Bucket

echo "Hosted zone ID"

read hostedZoneID

echo "Domain Name"

read domainName

echo "Certificate ARN"

read certificateARN

echo "***********************************************************"

echo "Stack creation in process.................................."

echo "***********************************************************"

echo ""

stackId=$(aws cloudformation create-stack --stack-name $stack_name --template-body \
 file://lambda-application.json --profile $account --region $vpcRegion  --capabilities CAPABILITY_NAMED_IAM --parameters \
ParameterKey=EC2Size,ParameterValue=$EC2Size \
ParameterKey=RDSSize,ParameterValue=$RDSSize \
ParameterKey=vpcName,ParameterValue=$vpcName \
ParameterKey=ImageID,ParameterValue=$ImageID \
ParameterKey=vpcCidr,ParameterValue=$vpcCidr \
ParameterKey=KeyPair,ParameterValue=$KeyPair \
ParameterKey=pubSubnet1,ParameterValue=$stack_name$pubSubnet1 \
ParameterKey=pubSubnet2,ParameterValue=$stack_name$pubSubnet2 \
ParameterKey=pubSubnet3,ParameterValue=$stack_name$pubSubnet3 \
ParameterKey=pubSubnet1CIDR,ParameterValue=$pubSubnet1CIDR \
ParameterKey=pubSubnet2CIDR,ParameterValue=$pubSubnet2CIDR \
ParameterKey=pubSubnet3CIDR,ParameterValue=$pubSubnet3CIDR \
ParameterKey=pvtSubnet1CIDR,ParameterValue=$pvtSubnet1CIDR \
ParameterKey=pvtSubnet2CIDR,ParameterValue=$pvtSubnet2CIDR \
ParameterKey=pvtSubnet3CIDR,ParameterValue=$pvtSubnet3CIDR \
ParameterKey=pvtSubnet1,ParameterValue=$stack_name$pvtSubnet1 \
ParameterKey=pvtSubnet2,ParameterValue=$stack_name$pvtSubnet2 \
ParameterKey=pvtSubnet3,ParameterValue=$stack_name$pvtSubnet3 \
ParameterKey=vpcIdUnique,ParameterValue=vpcId$stack_name \
ParameterKey=dbUser,ParameterValue=$dbUser \
ParameterKey=dbPassword,ParameterValue=$dbPassword \
ParameterKey=CodeDeployS3Bucket,ParameterValue=$CodeDeployS3Bucket \
ParameterKey=hostedZoneID,ParameterValue=$hostedZoneID \
ParameterKey=certificateARN,ParameterValue=$certificateARN \
ParameterKey=domainName,ParameterValue=$domainName \
--query [StackId] --output text)

echo "###################################################################################################"
echo 'Your Stack Id: '$stackId
echo "###################################################################################################"

if [ -z $stackId ]; then
    echo 'Stack creation failed!!!!!!!!!!!!!!!!!!!'
else
    aws cloudformation wait stack-create-complete --stack-name $stackId --profile $account --region $vpcRegion
    echo "Stack Created "
fi