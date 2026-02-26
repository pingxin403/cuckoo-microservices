package com.pingxin403.cuckoo.common.mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic DTO mapper interface for entity-DTO conversions.
 * All service mappers should implement this interface for consistency.
 * 
 * <p>This interface provides a standardized contract for converting between entity objects
 * and Data Transfer Objects (DTOs). It includes both single-object and batch conversion methods,
 * with built-in null-safety guarantees.</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Standardized method names across all services</li>
 *   <li>Null-safe conversions (returns null for null input)</li>
 *   <li>Default batch conversion implementations</li>
 *   <li>Flexible implementation strategy (manual, MapStruct, ModelMapper, etc.)</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Component
 * public class ProductMapper implements DTOMapper<Product, ProductDTO> {
 * 
 *     @Override
 *     public ProductDTO toDTO(Product entity) {
 *         if (entity == null) {
 *             return null;
 *         }
 *         
 *         ProductDTO dto = new ProductDTO();
 *         dto.setId(entity.getId());
 *         dto.setName(entity.getName());
 *         dto.setPrice(entity.getPrice());
 *         dto.setStock(entity.getStock());
 *         dto.setCreatedAt(entity.getCreatedAt());
 *         return dto;
 *     }
 * 
 *     @Override
 *     public Product toEntity(ProductDTO dto) {
 *         if (dto == null) {
 *             return null;
 *         }
 *         
 *         Product entity = new Product();
 *         entity.setId(dto.getId());
 *         entity.setName(dto.getName());
 *         entity.setPrice(dto.getPrice());
 *         entity.setStock(dto.getStock());
 *         return entity;
 *     }
 *     
 *     // toDTOList() and toEntityList() are inherited with default implementations
 * }
 * 
 * // Usage in service layer
 * @Service
 * @RequiredArgsConstructor
 * public class ProductService {
 * 
 *     private final ProductRepository productRepository;
 *     private final ProductMapper productMapper;
 * 
 *     public ProductDTO getProductById(Long id) {
 *         Product product = productRepository.findById(id)
 *             .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
 *         return productMapper.toDTO(product);
 *     }
 * 
 *     public List<ProductDTO> getAllProducts() {
 *         List<Product> products = productRepository.findAll();
 *         return productMapper.toDTOList(products);  // Batch conversion
 *     }
 * 
 *     public ProductDTO createProduct(CreateProductRequest request) {
 *         Product product = new Product();
 *         product.setName(request.getName());
 *         product.setPrice(request.getPrice());
 *         product.setStock(request.getStock());
 *         
 *         Product saved = productRepository.save(product);
 *         return productMapper.toDTO(saved);
 *     }
 * }
 * }</pre>
 * 
 * <h2>MapStruct Implementation Example:</h2>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface OrderMapper extends DTOMapper<Order, OrderDTO> {
 * 
 *     @Override
 *     @Mapping(target = "customerName", source = "customer.name")
 *     @Mapping(target = "itemCount", expression = "java(entity.getItems().size())")
 *     OrderDTO toDTO(Order entity);
 * 
 *     @Override
 *     @Mapping(target = "items", ignore = true)
 *     @Mapping(target = "createdAt", ignore = true)
 *     Order toEntity(OrderDTO dto);
 *     
 *     // Batch methods inherited from DTOMapper interface
 * }
 * }</pre>
 * 
 * <h2>Null Safety:</h2>
 * <p>All methods in this interface must handle null inputs gracefully by returning null.
 * This prevents NullPointerException and allows for clean optional chaining:</p>
 * <pre>{@code
 * // Safe null handling
 * Product product = null;
 * ProductDTO dto = mapper.toDTO(product);  // Returns null, no exception
 * 
 * // Safe with Optional
 * Optional<ProductDTO> dto = Optional.ofNullable(product)
 *     .map(mapper::toDTO);
 * }</pre>
 * 
 * @param <E> Entity type
 * @param <D> DTO type
 * 
 * @author cuckoo-team
 * @since 1.0.0
 */
public interface DTOMapper<E, D> {

    /**
     * Converts an entity object to a DTO object.
     * 
     * <p>This method must be implemented by all concrete mapper classes.
     * The implementation should handle all necessary field mappings from
     * the entity to the DTO.</p>
     * 
     * <p><strong>Null Safety:</strong> This method must return null when
     * the input entity is null, rather than throwing an exception.</p>
     * 
     * @param entity Entity object to convert
     * @return DTO object with data from the entity, or null if entity is null
     */
    D toDTO(E entity);

    /**
     * Converts a DTO object to an entity object.
     * 
     * <p>This method must be implemented by all concrete mapper classes.
     * The implementation should handle all necessary field mappings from
     * the DTO to the entity.</p>
     * 
     * <p><strong>Note:</strong> This method typically does not set auto-generated
     * fields like ID, createdAt, updatedAt, etc. These fields are usually managed
     * by the persistence layer.</p>
     * 
     * <p><strong>Null Safety:</strong> This method must return null when
     * the input DTO is null, rather than throwing an exception.</p>
     * 
     * @param dto DTO object to convert
     * @return Entity object with data from the DTO, or null if dto is null
     */
    E toEntity(D dto);

    /**
     * Converts a list of entity objects to a list of DTO objects.
     * 
     * <p>This method provides a default implementation that uses the {@link #toDTO(Object)}
     * method to convert each entity in the list. Implementations can override this method
     * if a more efficient batch conversion strategy is available.</p>
     * 
     * <p><strong>Null Safety:</strong> This method returns null when the input list is null.
     * Individual null elements in the list are converted to null DTOs.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * List<Product> products = productRepository.findAll();
     * List<ProductDTO> dtos = productMapper.toDTOList(products);
     * }</pre>
     * 
     * @param entities List of entity objects to convert
     * @return List of DTO objects, or null if entities is null
     */
    default List<D> toDTOList(List<E> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Converts a list of DTO objects to a list of entity objects.
     * 
     * <p>This method provides a default implementation that uses the {@link #toEntity(Object)}
     * method to convert each DTO in the list. Implementations can override this method
     * if a more efficient batch conversion strategy is available.</p>
     * 
     * <p><strong>Null Safety:</strong> This method returns null when the input list is null.
     * Individual null elements in the list are converted to null entities.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * List<ProductDTO> dtos = request.getProducts();
     * List<Product> products = productMapper.toEntityList(dtos);
     * productRepository.saveAll(products);
     * }</pre>
     * 
     * @param dtos List of DTO objects to convert
     * @return List of entity objects, or null if dtos is null
     */
    default List<E> toEntityList(List<D> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }
}
