package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.repo.ProductRepo;
import com.goodfeel.nightgrass.service.ICartService;
import com.goodfeel.nightgrass.dto.CartItemDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class CartService implements ICartService {

    private final ProductRepo productRepo;
    private final Map<Long, CartItemDto> cart = new HashMap<>();

    public CartService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public Mono<Void> addProductToCart(Long productId, int quantity) {
        return productRepo.findById(productId)
                .flatMap(product -> {
                    System.out.println("Adding product to shopping cart: " + product);
                    cart.compute(productId, (_, existingItem) -> {
                        if (existingItem == null) {
                            return new CartItemDto(
                                    product.getId(),
                                    product.getName(),
                                    product.getDescription(),
                                    product.getPrice(),
                                    quantity
                            );
                        } else {
                            existingItem.setQuantity(existingItem.getQuantity() + quantity);
                            return existingItem;
                        }
                    });
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Integer> getCartItemCount() {
        Integer totalCount = cart.values().stream() // TODO blocking?
                .mapToInt(CartItemDto::getQuantity)
                .sum();
        return Mono.just(totalCount);
    }

    @Override
    public Mono<Void> removeProductFromCart(Long productId) {
        return Mono.fromRunnable(() -> {
            cart.remove(productId);
        });
    }

    @Override
    public Flux<CartItemDto> getCartItems() {
        return Flux.fromIterable(cart.values());
    }

    @Override
    public Mono<Double> getTotalPrice() {
        return getCartItems()
                .map(item -> item.getPrice() * item.getQuantity())
                .reduce(0.00, Double::sum);
    }
}
