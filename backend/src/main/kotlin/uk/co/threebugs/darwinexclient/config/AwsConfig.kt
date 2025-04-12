package uk.co.threebugs.darwinexclient.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class AwsConfig {

    @Value("\${aws.region}")
    private lateinit var awsRegion: String

    @Value("\${aws.s3.bucket.name}")
    private lateinit var bucketName: String

    @Value("\${broker.name}")
    private lateinit var brokerName: String

    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.of(awsRegion))
            .build()
    }

    @Bean
    fun s3BucketName(): String {
        return bucketName
    }

    @Bean
    fun brokerName(): String {
        return brokerName
    }
}