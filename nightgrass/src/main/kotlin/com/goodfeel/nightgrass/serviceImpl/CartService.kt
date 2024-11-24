package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.*
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.repo.*
import com.goodfeel.nightgrass.service.ICartService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@Service
open class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository
) : ICartService {
    private val logger: Logger = LoggerFactory.getLogger(CartService::class.java)

    override fun getCartForUser(userId: String): Mono<Cart> {
        if (userId.equals("Guest", ignoreCase = true)) {
            // Handle guest cart logic (e.g., use session storage or a temporary in-memory cart)
            return Mono.just(Cart(
                total = BigDecimal.ZERO,
                userId = ""
            ))
        }
        return cartRepository.findByUserId(userId)
            .switchIfEmpty(
                cartRepository.save(Cart(null, BigDecimal.ZERO, userId))
            )
    }

    override fun addProductToCart(productId: Long): Mono<Cart> {
        return Utility.currentUserId
            .flatMap { userId: String -> this.getCartForUser(userId) }
            .flatMap { cart: Cart ->
                cartItemRepository.findByCartId(cart.cartId!!)
                    .filter { item -> item.productId == productId }
                    .next() // Take the first matching item, if any
                    .flatMap { existingItem: CartItem ->
                        // If product exists, increment quantity and save
                        existingItem.quantity += 1
                        cartItemRepository.save(existingItem).thenReturn(cart)
                    }
                    .switchIfEmpty( // If product does not exist in cart, add as new CartItem
                        productRepository.findById(productId)
                            .flatMap { product: Product ->
                                val propertiesMap = mapOf("color" to "blue", "size" to "large")
                                val newItem = CartItem(
                                    itemId = null,
                                    cartId =  cart.cartId,
                                    productId = productId,
                                    quantity =  1,
                                    isSelected = true
                                )
                                newItem.setPropertiesFromMap(map = propertiesMap)
                                cartItemRepository.save(newItem)
                                    .thenReturn(cart)
                            }
                    ) // After adding/updating the item, update the cart total
                    .flatMap { updatedCart: Cart ->
                        updateCartTotal(updatedCart.cartId!!).thenReturn(updatedCart)
                    }
            }
    }

    override fun getCartItemCount(): Mono<Int> {
        return Utility.currentUserId
            .flatMap { userId: String ->
                getCartForUser(userId)
            }
            .flatMap { cart: Cart ->

                cart.cartId?.let {
                    cartItemRepository.findByCartId(cart.cartId)
                        .map(CartItem::quantity) // Extract quantity of each item
                        .reduce(0) { total, quantity -> total + quantity }
                } ?: Mono.just(0)
            }
            .switchIfEmpty(Mono.just(0))
    }


    override fun removeCartItemFromCart(itemId: Long): Mono<Void> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItemRepository.delete(cartItem)
                    .then(updateCartTotal(cartItem.cartId))
            } // Update total after deletion
    }

    override fun getCartItems(): Flux<CartItemDto> {
        return Utility.currentUserId
            .flatMap { userId: String -> this.getCartForUser(userId) }
            .flatMapMany { cart: Cart -> cartItemRepository.findByCartId(cart.cartId!!) }
            .flatMap { cartItem: CartItem -> this.mapToCartItemDto(cartItem) }
    }

    private fun mapToCartItemDto(cartItem: CartItem): Mono<CartItemDto> {
        // Format price in Canadian dollars
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA)
        return productRepository.findById(cartItem.productId)
            .map { product: Product ->
                CartItemDto(
                    itemId = cartItem.itemId,
                    productId = cartItem.productId,
                    imageUrl = product.imageUrl,
                    productName = product.productName,
                    description = product.description,
                    quantity = cartItem.quantity,
                    properties = cartItem.properties,
                    price = product.price,
                    formattedPrice = currencyFormat.format(product.price),
                    isSelected = cartItem.isSelected
                )
            }
    }

    override fun getTotalPrice(): Mono<BigDecimal> {
        return Utility.currentUserId
            .flatMap { userId ->
                getCartForUser(userId) // Get the cart for the current user
            }
            .flatMap { cart ->
                cartItemRepository.findByCartId(cart.cartId!!) // Fetch all items in the cart
                    .filter(CartItem::isSelected) // Only include selected items
                    .flatMap { cartItem ->
                        productRepository.findById(cartItem.productId) // Fetch product details
                            .map { product ->
                                product.price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong())) // Calculate total for the item
                            }
                    }
                    .reduce(BigDecimal.ZERO, BigDecimal::add) // Sum up all item totals
            }
    }

    fun updateQuantity(itemId: Long, quantity: Int): Mono<CartItem> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItem.quantity = quantity
                cartItemRepository.save(cartItem) //  ensure that the total is only updated when a cart item is selected
                    .then(if (cartItem.isSelected) updateCartTotal(cartItem.cartId) else Mono.empty())
            }
            .then(cartItemRepository.findById(itemId)) // Return updated cart item
    }

    /**
     * This item is used to update the cart total when
     * 1. A product is added to the shopping cart;
     * 2. quantity is changed;
     * 3. cart item is deleted from the cart;
     *
     * @param cartId
     * @return
     */
    private fun updateCartTotal(cartId: Long): Mono<Void> {
        // Retrieve all items in the cart
        return cartItemRepository.findByCartId(cartId)
            .flatMap { cartItem ->
                productRepository.findById(cartItem.productId)
                    .map { product ->
                        product.price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
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
            .then()
    }

    /**
     * This function is used to update the cart total when user select/deselect the item by (un)check the checkbox
     *
     * @param itemId     - Cart item ID
     * @param isSelected Is the cart item selected or not
     * @return
     */
    fun updateCartTotal(itemId: Long, isSelected: Boolean): Mono<Void> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItem.isSelected = isSelected
                cartItemRepository.save(cartItem)
            }
            .flatMap { cartItem ->
                productRepository.findById(cartItem.productId)
                    .map { product: Product ->
                        val itemTotal: BigDecimal =
                            product.price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong()))
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
            .then()
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
    open fun createOrderAndCleanupShoppingCart(userId: String): Mono<Order> {
        return cartRepository.findByUserId(userId)
            .flatMap { cart: Cart ->
                val cartItemDtoFlux = cartItemRepository.findByCartId(cart.cartId!!)
                    .filter(CartItem::isSelected)
                    .flatMap { cartItem: CartItem ->
                        this.populateCartItemWithProductInfo(
                            cartItem
                        )
                    }
                createOrder(cart, cartItemDtoFlux)
                    .flatMap { order: Order ->
                        clearCartItemsAndTotal(
                            cart
                        ).thenReturn(order)
                    }
            }
    }


    private fun populateCartItemWithProductInfo(cartItem: CartItem): Mono<CartItemDto> {
        return productRepository.findById(cartItem.productId)
            .map{ product: Product ->
                CartItemDto(
                    itemId = cartItem.itemId,
                    productId = cartItem.productId,
                    productName = product.productName,
                    description = product.description,
                    imageUrl = product.imageUrl,
                    quantity = cartItem.quantity,
                    properties = cartItem.properties,
                    price = product.price
                )
            }
    }

    private fun createOrder(cart: Cart, cartItemDtoFlux: Flux<CartItemDto>): Mono<Order> {
        // Calculate total reactively from Flux<CartItemDto>
        val totalMono = cartItemDtoFlux
            .map{ item: CartItemDto ->
                item.price.multiply(BigDecimal.valueOf(item.quantity.toLong()))
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add)

        return userRepository.findByOauthId(cart.userId) // TODO - save userId or OauthId in cart/order table?
            .zipWith(totalMono) { user: User, total: BigDecimal ->
                // Create the order
                val order = Order(
                    orderNo =  generateOrderNo(),
                    userId = cart.userId,
                    createdAt = LocalDateTime.now(),
                    orderStatus = OrderStatus.CHECKOUT,
                    total = total,
                    // Copy user details to order
                    contactName = user.customerName,
                    contactPhone = user.phone,
                    deliveryAddress = user.address
                )
                logger.debug("Creating order for user ${order.userId}, orderNo = ${order.orderNo}, total = ${order.total}")
                order
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

    // Helper method to generate a human-readable order ID with date/time and a unique suffix
    private fun generateOrderNo(): String {
        // Format current date-time to a string
        val dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
        // Generate a random 4-digit number as a suffix to ensure uniqueness
        val randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999)
        return dateTimePart + randomSuffix
    }
}
