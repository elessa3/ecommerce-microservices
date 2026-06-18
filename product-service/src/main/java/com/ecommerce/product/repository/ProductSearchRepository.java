package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    // Full-text search across name and description
    Page<ProductDocument> findByNameContainingOrDescriptionContaining(
        String name, String description, Pageable pageable
    );

    // Filter by category
    Page<ProductDocument> findByCategoryNameAndActiveTrue(String categoryName, Pageable pageable);

    // Autocomplete on product name
    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": ["name^3", "description"],
            "type": "best_fields",
            "fuzziness": "AUTO"
          }
        }
        """)
    Page<ProductDocument> searchWithFuzziness(String query, Pageable pageable);
}
