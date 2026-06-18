package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductDocument;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "category", source = "category")
    ProductDto.Response toResponse(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    ProductDto.Summary toSummary(Product product);

    // Map JPA entity → Elasticsearch document for indexing
    @Mapping(target = "id",           source = "id", qualifiedByName = "longToString")
    @Mapping(target = "categoryName", source = "category.name")
    ProductDocument toDocument(Product product);

    @Named("longToString")
    default String longToString(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(ProductDto.UpdateRequest request, @MappingTarget Product product);
}
