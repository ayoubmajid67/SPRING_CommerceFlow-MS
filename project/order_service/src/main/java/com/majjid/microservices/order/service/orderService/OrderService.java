package com.majjid.microservices.order.service.orderService;
import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderUpdateRequestDto;
import com.majjid.microservices.order.client.inventoryClient.InventoryClient;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryInStockResponse;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryResponseDto;
import com.majjid.microservices.order.config.CustomAppException;
import com.majjid.microservices.order.config.hanlders.feignHanlders.FeignClientHandler;
import com.majjid.microservices.order.mappers.CustomMapper;
import com.majjid.microservices.order.model.Order;
import com.majjid.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import com.majjid.microservices.order.client.inventoryClient.dto.SellDto;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements IOrderService{
  private final CustomMapper mapper;
  private  final OrderRepository orderRepository;
  private  final InventoryClient inventoryClient;

    @Override
    public ResponseDto<List<OrderResponseDto>> getOrders() {
        return ResponseDto.listed(orderRepository.findAll().stream().map(mapper::toDto).toList(),"orders");
    }

    @Override
    public ResponseDto<OrderResponseDto> getOrderById(Integer orderId) {
        Order  order = orderRepository.findById(orderId).orElseThrow( ()-> new CustomAppException(HttpStatus.NOT_FOUND,CustomAppException.buildNotFoundMsg(orderId,"order")));
        return ResponseDto.retrieved(mapper.toDto(order),"order");
    }

    @Override
    public ResponseDto<OrderResponseDto> placeAnOrder(OrderCreateRequestDto orderCreateRequestDto) {
/*
        InventoryInStockResponse inventoryResponseDtoResponseDto =
                FeignClientHandler.handleFeignCall(() ->
                        inventoryClient.isInStock(orderCreateRequestDto.skuCode(), new IsInStockRequestDto(orderCreateRequestDto.quantity())) ,InventoryClient.SERVICE_NAME);

       log.info("Inventory Service Response: {}", inventoryResponseDtoResponseDto);
*/
        InventoryInStockResponse sellInventoryResponse = FeignClientHandler.handleFeignCall(() ->inventoryClient.sellInventory(orderCreateRequestDto.skuCode(),
               new SellDto(orderCreateRequestDto.quantity())),InventoryClient.SERVICE_NAME);

       log.info("Sell Service Response: {}", sellInventoryResponse);
        Order order = mapper.toObject(orderCreateRequestDto);
        order= orderRepository.save(order);

        log.info("Order : {}", order);


        return ResponseDto.created(mapper.toDto(order),"order");

    }

    @Override
    public ResponseDto<OrderResponseDto> updateAnOrder(Integer orderId, OrderUpdateRequestDto orderUpdateRequestDto) {

        Order order = orderRepository.findById(orderId).orElseThrow(()-> new CustomAppException(HttpStatus.NOT_FOUND,CustomAppException.buildNotFoundMsg(orderId,"order")));


//        ToDo : check if the order id will get updated or not after using the mapper :
//        ToDo : check if the skuCode changes , if yes you have check it in the Inventory service
        order = mapper.toObject(orderUpdateRequestDto);
        order= orderRepository.save(order);
        return ResponseDto.retrieved(mapper.toDto(order),"order");
    }

    @Override
    public ResponseDto<OrderResponseDto> deleteAnOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new CustomAppException(HttpStatus.NOT_FOUND,CustomAppException.buildNotFoundMsg(orderId,"order")));

        orderRepository.delete(order);

//        ToDo: check if the saved order variable will be cleared after deleting the order :
        return ResponseDto.deleted(mapper.toDto(order),"order");


    }

    @Override
    public ResponseDto<OrderResponseDto> cancelAnOrder(Integer orderId) {
        return null;
    }


}
