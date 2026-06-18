package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Keeps the Elasticsearch index in sync with PostgreSQL.
 *
 * Indexing is done asynchronously (@Async) so it never slows down
 * the HTTP response — if Elasticsearch is slow or temporarily down,
 * the write to PostgreSQL still succeeds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexingService {

    private final ProductSearchRepository searchRepository;
    private final ProductMapper           productMapper;

    @Async
    public void index(Product product) {
        try {
            ProductDocument doc = productMapper.toDocument(product);
            searchRepository.save(doc);
            log.debug("Indexed product {} in Elasticsearch", product.getId());
        } catch (Exception e) {
            // Don't let Elasticsearch failure break the main flow
            log.error("Failed to index product {}: {}", product.getId(), e.getMessage());
        }
    }

    @Async
    public void removeFromIndex(Long productId) {
        try {
            searchRepository.deleteById(String.valueOf(productId));
            log.debug("Removed product {} from Elasticsearch index", productId);
        } catch (Exception e) {
            log.error("Failed to remove product {} from index: {}", productId, e.getMessage());
        }
    }
}
