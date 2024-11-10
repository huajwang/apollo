package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.dto.ProductDto;
import com.goodfeel.nightgrass.serviceImpl.CartService;
import com.goodfeel.nightgrass.serviceImpl.ProductPhotoService;
import com.goodfeel.nightgrass.serviceImpl.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping(path = "/product")
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;
    private final ProductPhotoService productPhotoService;

    public ProductController(ProductService productService, CartService cartService, ProductPhotoService productPhotoService) {
        this.productService = productService;
        this.cartService = cartService;
        this.productPhotoService = productPhotoService;
    }

    @GetMapping("/all")
    public String listProducts(Model model) {
        Flux<ProductDto> products = productService.getAllProducts();
        Mono<Integer> count = cartService.getCartItemCount();
        model.addAttribute("products", products);
        model.addAttribute("cartItemCount", count);
        return "product-list";
    }

    @GetMapping("/detail")
    public String index(@RequestParam("id") Long productId, Model model) {
        model.addAttribute("cartItemCount", cartService.getCartItemCount());
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("productPhotos", productPhotoService.findProductImg(productId));
        return "product_detail";
    }
}
