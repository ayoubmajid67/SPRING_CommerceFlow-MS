package com.majjid.microservices.inventory.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomMapper extends InventoryMapper {


}
