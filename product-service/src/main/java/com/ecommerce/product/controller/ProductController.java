package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ProductSearchService;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalogue management and search")
public class ProductController {

    private final ProductService       productService;
    private final ProductSearchService searchService;

    // ── Public endpoints ──────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all products (paginated, from PostgreSQL)")
    public Page<ProductDto.Summary> list(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id") String sort
    ) {
        return productService.findAll(PageRequest.of(page, size, Sort.by(sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (Redis cached)")
    public ProductDto.Response getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search with fuzzy matching (Elasticsearch)")
    public Page<ProductDto.Summary> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.search(q, page, size);
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "Filter by category (Elasticsearch)")
    public Page<ProductDto.Summary> byCategory(
        @PathVariable String categoryName,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.findByCategory(categoryName, page, size);
    }

    // ── Admin endpoints (require ADMIN role) ──────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductDto.Response> create(@Valid @RequestBody ProductDto.CreateRequest request) {
        ProductDto.Response created = productService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing product (partial update supported)")
    public ProductDto.Response update(
        @PathVariable Long id,
        @Valid @RequestBody ProductDto.UpdateRequest request
    ) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a product (preserves order history)")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
