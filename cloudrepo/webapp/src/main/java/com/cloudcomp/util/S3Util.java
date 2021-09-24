package com.cloudcomp.util;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3Util {

    public  static AmazonS3  createCredentials() {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new InstanceProfileCredentialsProvider(false))
                .build();
        return s3Client;
    }

    public static String productRetrieveFileFromS3( String fileName, String app_username , String bucketName) {
        AmazonS3 s3client = AmazonS3ClientBuilder.standard().build();
        S3Object attachment = null;


        String attachmentName = new String();
        for( S3ObjectSummary sumObj : S3Objects.inBucket(s3client, bucketName) ) {
            attachmentName = sumObj.getKey();
            if( attachmentName.equals(app_username) ) {
                attachment = s3client.getObject( bucketName, attachmentName );
                break;
            }
        }
        if( attachment != null ) {
            return attachment.getObjectContent().getHttpRequest().getURI().toString();
        }
        else {
            return null;
        }


    }


}
