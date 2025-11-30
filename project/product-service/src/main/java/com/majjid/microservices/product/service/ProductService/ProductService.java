package com.majjid.microservices.product.service.ProductService;

import com.majjid.microservices.product.Dto.ProductRequestDto;
import com.majjid.microservices.product.Dto.ProductResponseDto;
import com.majjid.microservices.product.Dto.ResponseDto;
import com.majjid.microservices.product.config.CustomAppException;
import com.majjid.microservices.product.mappers.CustomMapper;
import com.majjid.microservices.product.model.Product;
import com.majjid.microservices.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CustomMapper mapper;
    private final String OBJECT_TYPE = "Product";

    @Override
    public ResponseDto<ProductResponseDto> createProduct(ProductRequestDto productRequestDto) {
        log.info("Creating new product: {}", productRequestDto.name());


        Product product = mapper.toObject(productRequestDto);
        product = productRepository.save(product);
        log.info("Product {} is saved", product.getId());

        ProductResponseDto responseDto = mapper.toDto(product);
        return ResponseDto.created(responseDto, "product");
    }

    @Override
    public ResponseDto<List<ProductResponseDto>> getProducts() {
        log.info("Fetching all products");

        List<ProductResponseDto> products = productRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseDto.listed(products, "products");
    }

    @Override
    public ResponseDto<Boolean> deleteProduct(String productId) {
        log.info("Deleting product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, CustomAppException.buildNotFoundMsg(productId,OBJECT_TYPE)));

        productRepository.deleteById(productId);
        log.info("Product {} deleted successfully", productId);

        return ResponseDto.deleted(true, "product");
    }

    @Override
    public ResponseDto<ProductResponseDto> getProduct(String productId) {
        log.info("Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, "The Product with id = " + productId + " is Not Found"));

        ProductResponseDto responseDto = mapper.toDto(product);
        return ResponseDto.retrieved(responseDto, "product");
    }

    @Override
    public ResponseDto<ProductResponseDto> updateProduct(String productId, ProductRequestDto productRequestDto) {
        log.info("Updating product with ID: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, "The Product with id = " + productId + " is Not Found"));

        // Update the existing product with new data
        Product updatedProduct = mapper.toObject(productRequestDto);
        updatedProduct.setId(existingProduct.getId()); // Preserve the ID

        updatedProduct = productRepository.save(updatedProduct);
        log.info("Product {} updated successfully", productId);

        ProductResponseDto responseDto = mapper.toDto(updatedProduct);
        return ResponseDto.updated(responseDto, "product");
    }
}