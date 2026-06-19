package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.CategoryNotFoundException;
import com.ecommerce.product.exception.DuplicateSkuException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)   // read-only by default; override on writes
public class ProductService {

    private final ProductRepository     productRepository;
    private final CategoryRepository    categoryRepository;
    private final ProductMapper         productMapper;
    private final ProductIndexingService indexingService;

    // ── Read operations (cached) ──────────────────────────────────

    /**
     * Get product by ID.
     * Result cached in Redis — subsequent calls skip the database entirely.
     * Cache is evicted when the product is updated or deleted.
     */
    @Cacheable(value = "products", key = "#id")
    public ProductDto.Response findById(Long id) {
        log.debug("Fetching product {} from database (cache miss)", id);
        return productRepository.findActiveById(id)
            .map(productMapper::toResponse)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Cacheable(value = "products-page", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDto.Summary> findAll(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(productMapper::toSummary);
    }

    // ── Write operations ──────────────────────────────────────────

    @Transactional
    public ProductDto.Response create(ProductDto.CreateRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Product product = Product.builder()
            .sku(request.getSku())
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stock(request.getStock())
            .imageUrl(request.getImageUrl())
            .active(true)
            .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);

        // Index in Elasticsearch asynchronously
        indexingService.index(saved);

        log.info("Product created: id={}, sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"products", "products-page"}, key = "#id")  // evict on update
    public ProductDto.Response update(Long id, ProductDto.UpdateRequest request) {
        Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        productMapper.updateFromRequest(request, product);   // null-safe partial update

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        indexingService.index(updated);   // re-index with new data

        log.info("Product updated: id={}", id);
        return productMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = {"products", "products-page"}, allEntries = true)
    public void delete(Long id) {
        Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);   // soft delete — preserves order history
        productRepository.save(product);
        indexingService.removeFromIndex(id);

        log.info("Product soft-deleted: id={}", id);
    }
}
