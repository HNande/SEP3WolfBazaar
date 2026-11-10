package org.example.application.config;

import generated.ItemListingServiceGrpc;
import generated.MemberServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    ItemListingServiceGrpc.ItemListingServiceBlockingStub itemListingStub(
            GrpcChannelFactory channels,
            @Value("${wolfbazaar.grpc.target:localhost:9090}") String target,
            @Value("${wolfbazaar.grpc.max-message-bytes:16777216}") int maxMessageBytes) {
        return ItemListingServiceGrpc.newBlockingStub(channels.createChannel(target))
                .withMaxInboundMessageSize(maxMessageBytes)
                .withMaxOutboundMessageSize(maxMessageBytes);
    }

    @Bean
    MemberServiceGrpc.MemberServiceBlockingStub memberStub(
            GrpcChannelFactory channels,
            @Value("${wolfbazaar.grpc.target:localhost:9090}") String target) {
        return MemberServiceGrpc.newBlockingStub(channels.createChannel(target));
    }
}
