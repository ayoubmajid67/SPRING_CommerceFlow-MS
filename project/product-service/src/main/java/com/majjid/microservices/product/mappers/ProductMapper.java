package com.majjid.microservices.product.mappers;


import com.majjid.microservices.product.Dto.ProductRequestDto;
import com.majjid.microservices.product.Dto.ProductResponseDto;
import com.majjid.microservices.product.model.Product;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponseDto toDto(Product product);

    Product toObject(ProductRequestDto productRequestDto);
}
