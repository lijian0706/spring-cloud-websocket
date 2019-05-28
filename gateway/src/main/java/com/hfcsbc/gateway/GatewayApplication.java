package com.hfcsbc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

//	@Bean
//	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//		//@formatter:off
//		return builder.routes()
//				.route("uaa", r -> r.path("/**")
//						.uri("http://localhost:9999"))
//				.route("order", r -> r.host("/order")
//						.uri("http://parking-lot-order-service:8080"))
//				.route("vehicle", r -> r.host("/vehicle")
//						.uri("http://parking-lot-vehicle-service:8081"))
//				.route("ws", r -> r.path("/ws")
//						.uri("ws://parking-lot-order-service:8080"))
//				.build();
//		//@formatter:on
//	}
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
