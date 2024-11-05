package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.serviceImpl.CartService;
import com.goodfeel.nightgrass.serviceImpl.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;

    public ProductController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    @GetMapping("/")
    public String listProducts(Model model) {
        Flux<ProductDto> products = productService.getAllProducts();
        Mono<Integer> count = cartService.getCartItemCount();
        model.addAttribute("products", products);
        model.addAttribute("cartItemCount", count);
        // Return the name of the Thymeleaf template (product-list.html)
        return "product-list";
    }

    @PostMapping(path = "/cart/add")
    public Mono<String> addToCart(@ModelAttribute CartRequest cartRequest, Model model) {
        return cartService.addProductToCart(cartRequest.getProductId(), 1)
                .thenReturn("redirect:/product_detail?id=" + cartRequest.getProductId());
    }

    @PostMapping(path = "/cart/remove")
    public Mono<String> removeFromCart(@ModelAttribute CartRequest cartRequest, Model model) {
        return cartService.removeProductFromCart(cartRequest.getProductId())
                .then(Mono.fromRunnable(() ->
                        model.addAttribute(
                                "message",
                                "Item removed from the cart successfully!")
                ))
                .thenReturn("redirect:/cart");  // Redirect to the cart view after removal
    }

    // View cart contents
    @GetMapping("/cart")
    public String viewCart(Model model) {
        Flux<CartItemDto> cartItems = cartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        return "cart"; // Thymeleaf template for viewing the cart
    }
}
