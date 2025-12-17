package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.*
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.repo.*
import com.goodfeel.nightgrass.service.ICartService
import com.goodfeel.nightgrass.service.ProcessedProductService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

@Service
open class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository,
    private val processedProductService: ProcessedProductService,
    private val addressRepository: AddressRepository
) : ICartService {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CartService::class.java)
    }

    /**
     * If shopping cart is not yet in DB, insert one.
     * Supports both authenticated users and guests.
     * - Authenticated users: cart associated with userId
     * - Guests: cart associated with guestId (session ID from frontend)
     * 
     * @param user User object (can be null for guests)
     * @param guestSessionId Session ID for guest carts (required if user is null)
     */
    override fun getCartForUserOrGuest(user: User?, guestSessionId: String?): Mono<Cart> {
        // Determine which identifier to use
        val cartIdentifier = if (user != null && user.oauthId != null) {
            user.oauthId
        } else if (!guestSessionId.isNullOrBlank()) {
            guestSessionId
        } else {
            return Mono.error(IllegalArgumentException("Either user or guestSessionId must be provided"))
        }
        
        return cartRepository.findByUserId(cartIdentifier)
            .switchIfEmpty(
                Mono.defer {
                    val newCart =
                        Cart(cartId = null, userId = cartIdentifier, guestId = null, total = BigDecimal.ZERO)
                    cartRepository.save(newCart)
                        .onErrorResume {
                            if (it is DuplicateKeyException) {
                                logger.debug("The cart already exists for: $cartIdentifier")
                                cartRepository.findByUserId(cartIdentifier)
                            } else {
                                Mono.error(it)
                            }
                        }
                        .doOnError {
                            logger.error("Failed to save or get cart for: $cartIdentifier")
                        }
                }.doOnSuccess {
                    logger.debug("New cart saved for: $cartIdentifier")
                }
            )
    }


    override fun getCartItemCount(user: User): Mono<Int> {
        return getCartForUserOrGuest(user, null)
            .flatMap { cart: Cart ->
                cart.cartId?.let {
                    cartItemRepository.findByCartId(cart.cartId)
                        .filter { it.isSelected }
                        .map(CartItem::quantity) // Extract quantity of each item
                        .reduce(0) { total, quantity -> total + quantity }
                } ?: Mono.just(0)
            }
            .switchIfEmpty(Mono.just(0))

    }

    override fun removeCartItemFromCart(itemId: Long): Mono<Long> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItemRepository.delete(cartItem)
                    .then(updateCartTotalByCartId(cartItem.cartId))
                    .then(notifyCartUpdate(cartItem.cartId))
                    .thenReturn(cartItem.cartId)
            }
    }

    override fun getCartItemsForCart(cartId: Long): Flux<CartItemDto> {
        return cartItemRepository.findByCartId(cartId)
            .flatMap { cartItem -> this.mapToCartItemDto(cartItem) }
    }

    private fun mapToCartItemDto(cartItem: CartItem): Mono<CartItemDto> {
        // Format price in Canadian dollars
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA)
        return processedProductService.findAndProcessProductByProductId(cartItem.productId)
            .map { productDto: ProductDto ->
                CartItemDto(
                    itemId = cartItem.itemId,
                    productId = cartItem.productId,
                    imageUrl = productDto.imageUrl,
                    productName = productDto.productName,
                    description = productDto.description,
                    quantity = cartItem.quantity,
                    properties = cartItem.properties,
                    price = productDto.price,
                    discountedPrice = productDto.discountedPrice ?: productDto.price,
                    formattedPrice = currencyFormat.format(productDto.discountedPrice ?: productDto.price),
                    isSelected = cartItem.isSelected
                )
            }
    }

    override fun getSubtotal(cartId: Long): Mono<BigDecimal> {
        return calculateCartValue(cartId) { productDto, cartItem ->
            val price = productDto.price
            price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
        }
    }

    override fun getTotalAfterDiscount(cartId: Long): Mono<BigDecimal> {
        return calculateCartValue(cartId) { productDto, cartItem ->
            val salePrice = productDto.discountedPrice ?: productDto.price
            salePrice.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
        }
    }

    /**
     * productDto.price is the original price;
     * productDto.discountedPrice is not null if there is no discount associated with this product;
     */
    override fun getSavings(cartId: Long): Mono<BigDecimal> {
        return calculateCartValue(cartId) { productDto, cartItem ->
            val savings = if (productDto.discountedPrice != null) {
                productDto.price.subtract(productDto.discountedPrice)
            } else {
                BigDecimal.ZERO
            }
            savings.multiply(BigDecimal.valueOf(cartItem.quantity.toLong())) // Calculate total saved
        }
    }

    /**
     * The 2nd parameter of this function is a lambda. The caller of this function passes its own
     * implementation of the lambda to achieve its specific calculation.
     */
    private fun calculateCartValue(
        cartId: Long,
        calculation: (productDto: ProductDto, cartItem: CartItem) -> BigDecimal
    ): Mono<BigDecimal> {
        // Fetch all cart items once and share the result
        val cartItemsMono = cartItemRepository.findByCartId(cartId) // Fetch all items in the cart
            .filter(CartItem::isSelected) // Only include selected items
            .cache() // catch the steam to prevent duplicate database queries

        return cartItemsMono
            .flatMap { cartItem ->
                processedProductService.findAndProcessProductByProductId(cartItem.productId)
                    .cache() // Cache the result for the same product ID
                    .map { productDto ->
                        calculation(productDto, cartItem)
                    }
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add)
    }

    /**
     * If cart item with itemID does not exist, emit an error downstream
     */
    override fun updateQuantity(itemId: Long, quantity: Int): Mono<CartItem> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItem.quantity = quantity
                cartItemRepository.save(cartItem)
            }
            .switchIfEmpty(Mono.error(IllegalArgumentException("Cart item with ID $itemId not found")))
    }

    /**
     * Add a new cart item from CartItemDto (used by updateCart endpoint)
     */
    open fun addCartItemToCart(cart: Cart, cartItemDto: CartItemDto): Mono<CartItem> {
        val newItem = CartItem(
            itemId = null,
            cartId = cart.cartId!!,
            productId = cartItemDto.productId,
            quantity = cartItemDto.quantity,
            properties = cartItemDto.properties,
            isSelected = true
        )
        return cartItemRepository.save(newItem)
            .flatMap { savedItem ->
                updateCartTotalByCartId(cart.cartId)
                    .thenReturn(savedItem)
            }
    }

    /**
     * Update cart total by calculating sum of all items and their prices.
     * Used when: product added, quantity changed, or item deleted from cart.
     *
     * @param cartId The cart ID to update
     * @return Mono<Void> when complete
     */
    open fun updateCartTotalByCartId(cartId: Long): Mono<Void> {
        return cartItemRepository.findByCartId(cartId)
            .flatMap { cartItem ->
                processedProductService.findAndProcessProductByProductId(cartItem.productId)
                    .map { productDto ->
                        val salePrice = productDto.discountedPrice ?: productDto.price
                        salePrice.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
                    }
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .flatMap { total ->
                cartRepository.findById(cartId)
                    .flatMap { cart ->
                        cart.total = total
                        cartRepository.save(cart)
                    }
            }
            .flatMap { savedCart ->
                notifyCartUpdate(savedCart.cartId!!)
            }
            .then()
    }

    /**
     * Update cart total when user selects/deselects an item via checkbox.
     * Recalculates total based on selection state.
     *
     * @param itemId     Cart item ID to update
     * @param isSelected Whether the item is now selected
     * @return Mono<Cart> with updated cart
     */
    open fun updateCartTotalByItemSelection(itemId: Long, isSelected: Boolean): Mono<Cart> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItem.isSelected = isSelected
                cartItemRepository.save(cartItem)
            }
            .flatMap { cartItem ->
                processedProductService.findAndProcessProductByProductId(cartItem.productId)
                    .map { productDto: ProductDto ->
                        val salePrice = productDto.discountedPrice ?: productDto.price
                        val itemTotal: BigDecimal =
                            salePrice.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
                        CartTotalUpdate(cartItem.cartId, itemTotal)
                    }
            }
            .flatMap { cartTotalUpdate: CartTotalUpdate ->
                cartRepository.findById(cartTotalUpdate.cartId)
                    .flatMap { cart: Cart ->
                        val newTotal: BigDecimal = if (isSelected)
                            cart.total.add(cartTotalUpdate.itemTotal)
                        else
                            cart.total.subtract(cartTotalUpdate.itemTotal)
                        cart.total = newTotal
                        cartRepository.save(cart)
                    }
            }
    }

    // Helper class to carry cart ID and item total to avoid recalculations
    private data class CartTotalUpdate(val cartId: Long, val itemTotal: BigDecimal)

    /**
     * When something is wrong when doing checkout the shopping cart, it needs to perform DB rollback.
     * Important: Method annotated with @Transactional must be open!!!
     * @param userId
     * @return
     */
    @Transactional
    open fun createOrderAndCleanupShoppingCart(userId: String?, guestId: String?): Mono<Order> {
        val cartMono = if (userId != null) {
            cartRepository.findByUserId(userId)
        } else if (guestId != null) {
            cartRepository.findByGuestId(guestId)
        } else {
            throw IllegalArgumentException("Both userId and guestId are null")
        }
        return cartMono
            .flatMap { cart: Cart ->
                val cartItemDtoFlux = cartItemRepository.findByCartId(cart.cartId!!)
                    .filter(CartItem::isSelected)
                    .flatMap { cartItem: CartItem ->
                        this.populateCartItemWithProductInfo(cartItem)
                    }

                createOrder(cart, cartItemDtoFlux)
                    .flatMap { order: Order ->
                        clearCartItemsAndTotal(cart)
                            .thenReturn(order)
                    }
            }
    }

    override fun mergeCart(userId: String, guestId: String): Mono<Void> {
        return cartRepository.findByGuestId(guestId)
            .doOnSuccess {
                logger.debug("Found Guest cart: $it")
            }
            .zipWith(cartRepository.findByUserId(userId)
                .doOnSuccess {
                    logger.debug("Found user cart: $it")
                }
                .defaultIfEmpty(Cart(userId = userId, total = BigDecimal.ZERO))
                .flatMap { userCart ->
                    if (userCart.cartId == null) {
                        cartRepository.save(userCart)
                            .doOnSuccess { savedCart ->
                                logger.debug("Saved new user cart: $savedCart")
                            }
                    } else {
                        Mono.just(userCart)
                    }
                }
            )
            .flatMap { tuple ->
                val guestCart = tuple.t1
                val userCart = tuple.t2
                mergeCartItems(guestCart, userCart)
                    .then(
                        cartRepository.deleteByCartId(guestCart.cartId!!)
                            .doOnSuccess {
                                logger.debug("Delete the guest cart after merging guest cardId: ${guestCart.cartId}")
                            }
                    )
                    .then(
                        getTotalAfterDiscount(userCart.cartId!!).flatMap { cartTotal ->
                            cartRepository.updateTotal(userCart.cartId, cartTotal)
                        }
                    )
                    .then()
            }
    }

    private fun mergeCartItems(guestCart: Cart, userCart: Cart): Mono<Void> {
        return cartItemRepository.findByCartId(guestCart.cartId!!)
            .flatMap { guestItem ->
                // Ensure user cart ID is not null
                if (userCart.cartId == null) {
                    return@flatMap Mono.error<Void>(
                        IllegalStateException("User cart ID is null during item merge")
                    )
                }
                cartItemRepository.findByCartIdAndProductId(userCart.cartId, guestItem.productId)
                    .defaultIfEmpty(guestItem.copy(itemId = null, cartId = userCart.cartId))
                    .flatMap { existingItem ->
                        if (existingItem.itemId == null) {
                            // Save the guest item into the user cart
                            cartItemRepository.save(guestItem.copy(itemId = null, cartId = userCart.cartId))
                                .doOnSuccess { savedItem ->
                                    logger.debug("Saved new item in user cart: $savedItem")
                                }
                        } else {
                            // Update the quantity of the existing item in the user cart
                            cartItemRepository.updateQuantity(
                                existingItem.itemId, existingItem.quantity + guestItem.quantity
                            )
                                .doOnSuccess {
                                    logger.debug("Updated quantity for existing item: $existingItem")
                                }
                        }
                    }
            }
            .then() // Convert Flux to Mono<Void>
    }


    private fun populateCartItemWithProductInfo(cartItem: CartItem): Mono<CartItemDto> {
        return processedProductService.findAndProcessProductByProductId(cartItem.productId)
            .map { productDto: ProductDto ->
                CartItemDto(
                    itemId = cartItem.itemId,
                    productId = cartItem.productId,
                    productName = productDto.productName,
                    description = productDto.description,
                    imageUrl = productDto.imageUrl,
                    quantity = cartItem.quantity,
                    properties = cartItem.properties,
                    price = productDto.price,
                    discountedPrice = productDto.discountedPrice ?: productDto.price
                )
            }
    }

    private fun createOrder(
        cart: Cart,
        cartItemDtoFlux: Flux<CartItemDto>
    ): Mono<Order> {
        // Calculate total reactively
        val discountedTotalMono = cartItemDtoFlux
            .map { item: CartItemDto ->
                val price = item.discountedPrice
                price.multiply(BigDecimal.valueOf(item.quantity.toLong()))
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add)

        val originalTotalMono = cartItemDtoFlux
            .map { item: CartItemDto ->
                item.price.multiply(BigDecimal.valueOf(item.quantity.toLong()))
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add)

        val userAddressMono = if (cart.userId != null) {
            userRepository.findByOauthId(cart.userId)
                .flatMap { user ->
                    addressRepository.findByUserIdAndIsDefaultTrue(user.id!!)
                        .defaultIfEmpty(Address(userId = user.id, addressLine = "No Address"))
                        .map { address -> user to address }
                }
        } else if (cart.guestId != null) {
            userRepository.findByGuestId(cart.guestId)
                .switchIfEmpty(
                    Mono.defer {
                        val guest = User(guestId = cart.guestId)
                        userRepository.save(guest)
                    }.doOnSuccess {
                        logger.debug("should not see this one --- create order. save user....")
                    }
                )
                .map { user -> user to Address(userId = user.id ?: 0, addressLine = "Guest Address") }
        } else {
            Mono.error(IllegalArgumentException("Both userId and guestId are null"))
        }

        return Mono.zip(discountedTotalMono, originalTotalMono, userAddressMono).map { tuple ->
            val discountedTotal = tuple.t1
            val originalTotal = tuple.t2
            val address = tuple.t3.second

            val estimatedHst = discountedTotal.multiply(BigDecimal.valueOf(0.13))
                .setScale(2, RoundingMode.HALF_UP)
            val shippingFee = BigDecimal.ZERO // TODO - calculate shipping fee
            val finalTotal = discountedTotal.add(estimatedHst).add(shippingFee)

            Order(
                orderNo = Utility.generateOrderNo(),
                userId = cart.userId ?: cart.guestId ?: throw IllegalArgumentException("userId and guestId is null"),
                createdAt = LocalDateTime.now(),
                orderStatus = OrderStatus.PENDING,
                originalTotal = originalTotal,
                discountedTotal = discountedTotal,
                hst = estimatedHst,
                shippingFee = BigDecimal.ZERO,
                finalTotal = finalTotal,
                contactName = address.customerName,
                contactPhone = address.phone,
                deliveryAddress = address.addressLine
            )
        }
            .flatMap { order: Order ->  // Save the order and create order items in a reactive chain
                orderRepository.save(order)
                    .flatMap { savedOrder: Order ->
                        cartItemDtoFlux
                            .flatMap { cartItemDto: CartItemDto ->
                                createOrderItem(
                                    savedOrder,
                                    cartItemDto
                                )
                            }
                            .then(Mono.just(savedOrder))
                    }
            }
    }

    private fun createOrderItem(order: Order, cartItemDto: CartItemDto): Mono<OrderItem> {
        // Map CartItem to OrderItem
        val orderItem = OrderItem(
            orderId = order.orderId!!,
            productName = cartItemDto.productName,
            imageUrl = cartItemDto.imageUrl,
            quantity = cartItemDto.quantity,
            properties = cartItemDto.properties,
            unitPrice = cartItemDto.price
        )
        // Save OrderItem
        return orderItemRepository.save(orderItem)
    }

    /**
     * After user press Checkout button, all selected cart items are all deleted. The rest of cart items, if there is
     * any, are 'unselected'. So, just write cart total as zero.
     *
     * @param cart
     * @return
     */
    private fun clearCartItemsAndTotal(cart: Cart): Mono<Void> {
        // Delete selected cart items and reset cart total
        return cartItemRepository.deleteByCartIdAndIsSelected(cart.cartId!!, true)
            .then(cartRepository.updateTotal(cart.cartId, BigDecimal.ZERO))
    }

    private fun notifyCartUpdate(cartId: Long): Mono<Int> {
        return cartItemRepository.findByCartId(cartId)
            .reduce(0) { total, cartItem -> total + cartItem.quantity }
    }

    @Transactional
    override fun clearCart(user: User?, guestSessionId: String?): Mono<Void> {
        return getCartForUserOrGuest(user, guestSessionId)
            .flatMap { cart ->
                cartItemRepository.findByCartId(cart.cartId!!)
                    .collectList()
                    .flatMap { items ->
                        cartItemRepository.deleteAll(items)
                            .then(cartRepository.updateTotal(cart.cartId, BigDecimal.ZERO))
                            .then(notifyCartUpdate(cart.cartId).then())
                    }
            }
            .doOnNext {
                val userInfo = if (user != null) "user: ${user.oauthId}" else "guest: $guestSessionId"
                logger.info("Cart cleared for $userInfo")
            }
            .onErrorResume { error ->
                val userInfo = if (user != null) "user: ${user.oauthId}" else "guest: $guestSessionId"
                logger.error("Error clearing cart for $userInfo", error)
                Mono.error(error)
            }
    }

}
