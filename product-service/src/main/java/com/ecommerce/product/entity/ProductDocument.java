package com.ecommerce.product.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elasticsearch document — mirrors the Product JPA entity but optimised for search.
 * Stored separately from PostgreSQL; kept in sync by ProductIndexingService.
 */
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/settings.json")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;             // Elasticsearch uses String IDs

    @Field(type = FieldType.Keyword)
    private String sku;

    // Text field for full-text search + keyword for exact match/sorting
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Boolean)
    private Boolean active;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;
}
