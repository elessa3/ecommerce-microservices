package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;

    /**
     * Full-text search with fuzzy matching.
     * Tolerates typos: "headphnes" will still find "Wireless Headphones".
     */
    public Page<ProductDto.Summary> search(String query, int page, int size) {
        log.debug("Searching Elasticsearch for: '{}'", query);
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());

        return searchRepository
            .searchWithFuzziness(query, pageable)
            .map(this::toSummary);
    }

    /**
     * Filter by category — useful for browsing (no text query needed).
     */
    public Page<ProductDto.Summary> findByCategory(String categoryName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository
            .findByCategoryNameAndActiveTrue(categoryName, pageable)
            .map(this::toSummary);
    }

    private ProductDto.Summary toSummary(ProductDocument doc) {
        return ProductDto.Summary.builder()
            .id(Long.parseLong(doc.getId()))
            .sku(doc.getSku())
            .name(doc.getName())
            .price(doc.getPrice())
            .stock(doc.getStock())
            .categoryName(doc.getCategoryName())
            .build();
    }
}
