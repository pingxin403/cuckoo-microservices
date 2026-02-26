package com.pingxin403.cuckoo.product.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import org.springframework.stereotype.Component;

/**
 * 商品映射器
 * 实现 DTOMapper 接口，提供 Product 实体和 ProductDTO 之间的转换
 */
@Component
public class ProductMapper implements DTOMapper<Product, ProductDTO> {

    /**
     * 将 Product 实体转换为 ProductDTO
     * 
     * @param entity Product 实体对象
     * @return ProductDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public ProductDTO toDTO(Product entity) {
        if (entity == null) {
            return null;
        }
        
        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 ProductDTO 转换为 Product 实体
     * 注意：不设置 createdAt 和 updatedAt，这些字段由 JPA 自动管理
     * 
     * @param dto ProductDTO 对象
     * @return Product 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .build();
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
