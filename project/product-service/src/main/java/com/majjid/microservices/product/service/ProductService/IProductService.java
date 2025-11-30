package com.majjid.microservices.product.service.ProductService;

import com.majjid.microservices.product.Dto.ProductRequestDto;
import com.majjid.microservices.product.Dto.ProductResponseDto;
import com.majjid.microservices.product.Dto.ResponseDto;

import java.util.List;

public interface IProductService {

    ResponseDto<ProductResponseDto> createProduct(ProductRequestDto productRequestDto);

    ResponseDto<List<ProductResponseDto>> getProducts();

    ResponseDto<Boolean> deleteProduct(String productId);

    ResponseDto<ProductResponseDto> getProduct(String productId);

    ResponseDto<ProductResponseDto> updateProduct(String productId, ProductRequestDto productRequestDto);
}