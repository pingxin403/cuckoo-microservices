package com.pingxin403.cuckoo.product.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 * 提供商品创建、查询和列表 REST API
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController extends BaseController {

    private final ProductService productService;

    /**
     * 创建商品
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody CreateProductRequest request) {
        logRequest("创建商品", request.getName(), request.getPrice());
        ProductDTO product = productService.createProduct(request);
        logResponse("创建商品", product.getId());
        return created(product);
    }

    /**
     * 根据 ID 查询商品
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        logRequest("查询商品", id);
        ProductDTO product = productService.getProductById(id);
        logResponse("查询商品", product.getId());
        return ok(product);
    }

    /**
     * 查询所有商品列表
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        logRequest("查询所有商品");
        List<ProductDTO> products = productService.getAllProducts();
        logResponse("查询所有商品", products.size() + " 个商品");
        return ok(products);
    }
}
