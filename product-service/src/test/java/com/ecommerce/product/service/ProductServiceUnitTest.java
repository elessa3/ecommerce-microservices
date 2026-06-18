package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test — pure Mockito, no Spring context, no Docker containers.
 * Fast: runs in milliseconds. Tests business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock ProductRepository      productRepository;
    @Mock CategoryRepository     categoryRepository;
    @Mock ProductMapper          productMapper;
    @Mock ProductIndexingService indexingService;

    @InjectMocks
    ProductService productService;

    @Test
    @DisplayName("findById: should throw when product does not exist")
    void findById_notFound_throwsException() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
            .hasMessageContaining("99");

        verify(productRepository).findActiveById(99L);
        verifyNoInteractions(productMapper);
    }

    @Test
    @DisplayName("create: should throw when SKU already exists")
    void create_duplicateSku_throwsException() {
        when(productRepository.existsBySku("ELEC-001")).thenReturn(true);

        ProductDto.CreateRequest request = ProductDto.CreateRequest.builder()
            .sku("ELEC-001")
            .name("Duplicate")
            .price(BigDecimal.TEN)
            .stock(5)
            .build();

        assertThatThrownBy(() -> productService.create(request))
            .hasMessageContaining("ELEC-001");

        verify(productRepository, never()).save(any());
        verifyNoInteractions(indexingService);
    }

    @Test
    @DisplayName("create: should save product and trigger Elasticsearch indexing")
    void create_validRequest_savesAndIndexes() {
        when(productRepository.existsBySku("NEW-001")).thenReturn(false);

        Product saved = Product.builder().id(1L).sku("NEW-001").name("New Product")
            .price(BigDecimal.TEN).stock(10).active(true).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDto.Response expectedResponse = ProductDto.Response.builder()
            .id(1L).sku("NEW-001").build();
        when(productMapper.toResponse(saved)).thenReturn(expectedResponse);

        ProductDto.CreateRequest request = ProductDto.CreateRequest.builder()
            .sku("NEW-001").name("New Product").price(BigDecimal.TEN).stock(10).build();

        ProductDto.Response result = productService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
        verify(indexingService).index(saved);   // Elasticsearch indexing was triggered
    }

    @Test
    @DisplayName("delete: should set active=false, not remove from database")
    void delete_existingProduct_softDeletes() {
        Product product = Product.builder().id(5L).active(true).build();
        when(productRepository.findActiveById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.delete(5L);

        assertThat(product.getActive()).isFalse();   // soft delete
        verify(productRepository, never()).delete(any());   // never hard deleted
        verify(indexingService).removeFromIndex(5L);
    }
}
