package com.majjid.microservices.order.service.orderService;

import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderUpdateRequestDto;
import com.majjid.microservices.order.client.inventoryClient.InventoryClient;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryInStockResponse;
import com.majjid.microservices.order.client.inventoryClient.dto.PurchaseDto;
import com.majjid.microservices.order.config.CustomAppException;
import com.majjid.microservices.order.config.hanlders.restClient.RestClientHandler;
import com.majjid.microservices.order.mappers.CustomMapper;
import com.majjid.microservices.order.model.Order;
import com.majjid.microservices.order.model.enums.OrderStatus;
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
public class OrderService implements IOrderService {
        private final CustomMapper mapper;
        private final OrderRepository orderRepository;
        private final InventoryClient inventoryClient;
        private final String SERVICE_UNIT_NAME = "order";
        private final String SERVICE_LIST_NAME = "orders";

        @Override
        public ResponseDto<List<OrderResponseDto>> getOrders() {
                return ResponseDto.listed(orderRepository.findAll().stream().map(mapper::toDto).toList(),
                                SERVICE_LIST_NAME);
        }

        @Override
        public ResponseDto<OrderResponseDto> getOrderById(Integer orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                                                CustomAppException.buildNotFoundMsg(orderId, "SERVICE_UNIT_NAME")));
                return ResponseDto.retrieved(mapper.toDto(order), "order");
        }

        @Override
        public ResponseDto<OrderResponseDto> placeAnOrder(OrderCreateRequestDto orderCreateRequestDto) {

                InventoryInStockResponse sellInventoryResponse = RestClientHandler.handleCall(
                                () -> inventoryClient.sellInventory(orderCreateRequestDto.skuCode(),
                                                new SellDto(orderCreateRequestDto.quantity())),
                                InventoryClient.SERVICE_NAME);

                log.info("Sell Service Response: {}", sellInventoryResponse);
                Order order = mapper.toObject(orderCreateRequestDto);
                order.setOrderStatus(OrderStatus.UNDER_PROCESS);
                order = orderRepository.save(order);

                log.info("Order : {}", order);

                return ResponseDto.created(mapper.toDto(order), "order");

        }

        @Override
        public ResponseDto<OrderResponseDto> deleteAnOrder(Integer orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                                                CustomAppException.buildNotFoundMsg(orderId, SERVICE_UNIT_NAME)));

                orderRepository.delete(order);

                // ToDo: check if the saved order variable will be cleared after deleting the
                // order :
                return ResponseDto.deleted(mapper.toDto(order), SERVICE_UNIT_NAME);

        }

        @Override
        public ResponseDto<OrderResponseDto> cancelAnOrder(Integer orderId) {

                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new CustomAppException(
                                                HttpStatus.NOT_FOUND,
                                                CustomAppException.buildNotFoundMsg(orderId, SERVICE_UNIT_NAME)));

                if (order.getOrderStatus() != OrderStatus.UNDER_PROCESS) {
                        throw new CustomAppException(
                                        HttpStatus.CONFLICT,
                                        "The order with the id " + orderId +
                                                        " cannot be cancelled because its status is "
                                                        + order.getOrderStatus());
                }

                // ✅ Extract immutable values
                final String skuCode = order.getSkuCode();
                final Integer quantity = order.getQuantity();

                InventoryInStockResponse purchaseInventoryResponse = RestClientHandler.handleCall(
                                () -> inventoryClient.purchaseInventory(
                                                skuCode,
                                                new PurchaseDto(quantity)),
                                InventoryClient.SERVICE_NAME);

                order.setOrderStatus(OrderStatus.CANCELLED);

                log.info("Purchase Service Response: {}", purchaseInventoryResponse);

                order = orderRepository.save(order);

                return ResponseDto.success(
                                mapper.toDto(order),
                                "The order with the id " + orderId + " has been cancelled successfully");
        }
}
