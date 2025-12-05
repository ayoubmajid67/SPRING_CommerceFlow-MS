package com.majjid.microservices.order.mappers;


import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.Dto.order.OrderUpdateRequestDto;
import com.majjid.microservices.order.model.Order;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface OrderMapper {

   OrderResponseDto toDto(Order order);

   Order toObject(OrderCreateRequestDto productRequestDto);

   Order toObject(OrderUpdateRequestDto productRequestDto);
}

