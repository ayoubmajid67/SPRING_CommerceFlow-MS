package com.majjid.microservices.product.controller;

import com.majjid.microservices.product.Dto.ProductRequestDto;
import com.majjid.microservices.product.Dto.ProductResponseDto;
import com.majjid.microservices.product.Dto.ResponseDto;
import com.majjid.microservices.product.service.ProductService.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @PostMapping
    public ResponseEntity<ResponseDto<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto productRequestDto) {
        ResponseDto<ProductResponseDto> response = productService.createProduct(productRequestDto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProducts() {
        ResponseDto<List<ProductResponseDto>> response = productService.getProducts();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(@PathVariable String productId) {
        ResponseDto<ProductResponseDto> response = productService.getProduct(productId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<Boolean>> deleteProduct(@PathVariable String productId) {
        ResponseDto<Boolean> response = productService.deleteProduct(productId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        ResponseDto<ProductResponseDto> response = productService.updateProduct(productId, productRequestDto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}