package com.cloudcomp.metrics;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("aws")
public class ConfigSQS {

    @Bean
    public AmazonSQS sqsClient() {
        return AmazonSQSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
    }
}
