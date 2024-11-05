package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.serviceImpl.CartService;
import com.goodfeel.nightgrass.serviceImpl.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductDetailController {

    private final ProductService productService;
    private final CartService cartService;

    public ProductDetailController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }


    @GetMapping("/product_detail")
    public String index(@RequestParam("id") Long productId, Model model) {
        model.addAttribute("cartItemCount", cartService.getCartItemCount());
        model.addAttribute("product", productService.getProductById(productId));
        return "product_detail";
    }
}
