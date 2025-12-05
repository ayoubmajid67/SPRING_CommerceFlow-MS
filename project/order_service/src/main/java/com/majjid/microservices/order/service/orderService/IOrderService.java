package com.majjid.microservices.order.service.orderService;

import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderUpdateRequestDto;


import java.util.List;


public interface IOrderService {

    ResponseDto<List<OrderResponseDto>> getOrders();
    ResponseDto<OrderResponseDto> getOrderById(Integer orderId);

    ResponseDto<OrderResponseDto> placeAnOrder(OrderCreateRequestDto orderCreateRequestDto);
    ResponseDto<OrderResponseDto> updateAnOrder(Integer orderId, OrderUpdateRequestDto orderCreateRequestDto);
    ResponseDto<OrderResponseDto> deleteAnOrder(Integer orderId);

    ResponseDto<OrderResponseDto>  cancelAnOrder(Integer orderId);

}
