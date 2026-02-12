package com.majjid.microservices.order.config.hanlders.restClient;

@FunctionalInterface
public interface RestClientCall<T> {
    T execute();
}
