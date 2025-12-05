package com.majjid.microservices.order.controller;


import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.Dto.order.OrderUpdateRequestDto;
import com.majjid.microservices.order.service.orderService.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("orders")
public class OrderController {
final  private IOrderService orderService;

    @GetMapping
    ResponseEntity<ResponseDto<List<OrderResponseDto>>> getOrders() {
        ResponseDto<List<OrderResponseDto>>  ordersResponseDto= orderService.getOrders();
        return ResponseEntity.status(ordersResponseDto.getStatus()).body(ordersResponseDto);

    }
@GetMapping("{orderId}")
ResponseEntity<ResponseDto<OrderResponseDto>> getOrderById(@PathVariable Integer orderId) {

    ResponseDto<OrderResponseDto>  orderResponseDto= orderService.getOrderById(orderId);
    return ResponseEntity.status(orderResponseDto.getStatus()).body(orderResponseDto);

}
@PostMapping
ResponseEntity<ResponseDto<OrderResponseDto>> placeAnOrder(@Valid @RequestBody OrderCreateRequestDto orderCreateRequestDto) {
        ResponseDto<OrderResponseDto> orderResponseDto = orderService.placeAnOrder(orderCreateRequestDto);
        return  ResponseEntity.status(orderResponseDto.getStatus()).body(orderResponseDto);

}

    @PutMapping("{orderId}")
    ResponseEntity<ResponseDto<OrderResponseDto>> updateAnOrder(@Valid @PathVariable Integer orderId, OrderUpdateRequestDto orderUpdateRequestDto) {
        ResponseDto<OrderResponseDto> orderResponseDto = orderService.updateAnOrder(orderId, orderUpdateRequestDto);

        return ResponseEntity.status(orderResponseDto.getStatus()).body(orderResponseDto);
    }

    @DeleteMapping("{orderId}")
    ResponseEntity<ResponseDto<OrderResponseDto>> deleteAnOrder(@PathVariable Integer orderId) {
        ResponseDto<OrderResponseDto> orderResponseDto = orderService.deleteAnOrder(orderId);

        return ResponseEntity.status(orderResponseDto.getStatus()).body(orderResponseDto);
    }


}
