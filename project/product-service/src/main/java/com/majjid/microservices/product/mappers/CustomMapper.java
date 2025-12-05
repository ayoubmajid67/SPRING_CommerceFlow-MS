package com.majjid.microservices.product.mappers;


import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;


@Mapper(componentModel = "spring")
public interface CustomMapper extends ProductMapper {



}
