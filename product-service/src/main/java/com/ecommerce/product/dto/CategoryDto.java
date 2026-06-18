package com.ecommerce.product.dto;

import lombok.*;

public class CategoryDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
    }
}
