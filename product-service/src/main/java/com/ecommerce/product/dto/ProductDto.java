package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    // ── Request DTOs ──────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        @NotBlank(message = "SKU is required")
        @Size(max = 50)
        private String sku;

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        private String name;

        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal price;

        @NotNull
        @Min(value = 0, message = "Stock cannot be negative")
        private Integer stock;

        private String imageUrl;

        private Long categoryId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        @Size(max = 255)
        private String name;

        private String description;

        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 8, fraction = 2)
        private BigDecimal price;

        @Min(0)
        private Integer stock;

        private String imageUrl;

        private Long categoryId;
    }

    // ── Response DTOs ─────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String sku;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String imageUrl;
        private Boolean active;
        private CategoryDto.Response category;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // Lightweight response for list views (no full description)
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Summary {
        private Long id;
        private String sku;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private String imageUrl;
        private String categoryName;
    }
}
