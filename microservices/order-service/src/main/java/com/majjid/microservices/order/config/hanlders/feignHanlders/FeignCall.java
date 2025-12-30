package com.majjid.microservices.order.config.hanlders.feignHanlders;

import feign.FeignException;

@FunctionalInterface
public interface FeignCall<T> {
    T execute() throws FeignException;
}
