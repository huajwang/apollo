package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.*
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.repo.*
import com.goodfeel.nightgrass.service.ICartService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.AddCartRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CartService::class.java)
        private val cartUpdateSink = Sinks.many().replay().latest<Int>()
    }


    private fun sendCartUpdate(event: Int) {
        val result = cartUpdateSink.tryEmitNext(event)
        logger.debug("cartUpdateSink emit result: $result")
    }

    fun getCartUpdateStream(): Flux<Int> {
        return cartUpdateSink.asFlux()
            .share()
            .doOnSubscribe {
                logger.debug("New subscriber connected to cartUpdateSink")
            }
            .doOnCancel {
                logger.debug("Subscriber disconnected from cartUpdateSink")
            }
    }

    override fun addProductToCart(addCartRequest: AddCartRequest, userId: String?, guestId: String?): Mono<Cart> {
        return getCartForUserOrGuest(userId, guestId)
            .flatMap { cart -> findOrAddCartItem(cart, addCartRequest) }
            .flatMap { cart -> updateCartTotalAndSelected(cart.cartId!!).thenReturn(cart) }
            .flatMap { cart -> notifyCartUpdate(cart.cartId!!).thenReturn(cart) }
    }

    override fun getCartForUserOrGuest(userId: String?, guestId: String?): Mono<Cart> {
        logger.debug("userId = $userId, guestId = $guestId")
        return when {
            userId != null -> {
                cartRepository.findByUserId(userId)
                    .switchIfEmpty(
                        Mono.defer {
                            val newCart = Cart(cartId = null, userId = userId, guestId = null, total = BigDecimal.ZERO)
                            cartRepository.save(newCart)
                        }.doOnSuccess {
                            logger.debug("new cart is saved for userId: $it")
                        }
                    )
            }
            guestId != null -> {
                cartRepository.findByGuestId(guestId)
                    .switchIfEmpty(
                        Mono.defer {
                            val newCart = Cart(cartId = null, userId = null, guestId = guestId, total = BigDecimal.ZERO)
                            cartRepository.save(newCart)
                        }.doOnSuccess {
                            logger.debug("new cart is saved for guest: $it")
                        }
                    )
            }
            else -> Mono.error(IllegalArgumentException("Either userId or guestId must be provided"))
        }
    }

    private fun findOrAddCartItem(cart: Cart, addCartRequest: AddCartRequest): Mono<Cart> {
        return cartItemRepository.findByCartId(cart.cartId!!)
            .filter { item ->
                item.productId == addCartRequest.productId &&
                        item.getPropertiesAsMap() == addCartRequest.properties
            }
            .next()
            .flatMap { existingItem ->
                logger.debug("Updating quantity for existing item in cart: ${cart.cartId}")
                existingItem.quantity += 1
                cartItemRepository.save(existingItem).thenReturn(cart)
            }
            .switchIfEmpty(
                Mono.defer {
                    logger.debug("Adding new item to cart: ${cart.cartId}")
                    val newItem = CartItem(
                        itemId = null,
                        cartId = cart.cartId,
                        productId = addCartRequest.productId,
                        quantity = 1,
                        isSelected = true
                    )
                    newItem.setPropertiesFromMap(map = addCartRequest.properties)
                    cartItemRepository.save(newItem).thenReturn(cart)
                }
            )
    }


    override fun getCartItemCount(userId: String?, guestId: String?): Mono<Int> {
        return getCartForUserOrGuest(userId, guestId)
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
                    .then(updateCartTotalAndSelected(cartItem.cartId))
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

    override fun getTotalPriceForCart(cartId: Long): Mono<BigDecimal> {
        return cartItemRepository.findByCartId(cartId) // Fetch all items in the cart
            .filter(CartItem::isSelected) // Only include selected items
            .flatMap { cartItem ->
                productRepository.findById(cartItem.productId) // Fetch product details
                    .map { product ->
                        product.price.multiply(BigDecimal.valueOf(cartItem.quantity.toLong())) // Calculate total for the item
                    }
            }
            .reduce(BigDecimal.ZERO, BigDecimal::add) // Sum up all item totals
    }

    open fun updateQuantity(itemId: Long, quantity: Int): Mono<CartItem> {
        return cartItemRepository.findById(itemId)
            .flatMap { cartItem: CartItem ->
                cartItem.quantity = quantity
                cartItemRepository.save(cartItem)
                    //  ensure that the total is only updated when a cart item is selected
                    .then(if (cartItem.isSelected) updateCartTotalAndSelected(cartItem.cartId) else Mono.empty())
            }
            .then(cartItemRepository.findById(itemId))
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
    private fun updateCartTotalAndSelected(cartId: Long): Mono<Void> {
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
            .flatMap { savedCart ->
                notifyCartUpdate(savedCart.cartId!!)
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
    open fun updateCartTotalAndSelected(itemId: Long, isSelected: Boolean): Mono<Cart> {
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
                                logger.debug("Delete the guest cart after merging: ${guestCart.cartId}")
                            }
                    )
                    .then(
                        getTotalPriceForCart(userCart.cartId!!).flatMap { cartTotal ->
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
                                existingItem.itemId, existingItem.quantity + guestItem.quantity)
                                .doOnSuccess {
                                    logger.debug("Updated quantity for existing item: $existingItem")
                                }
                        }
                    }
            }
            .then() // Convert Flux to Mono<Void>
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

        return userRepository.findByOauthId(cart.userId!!)
            .zipWith(totalMono) { user: User, total: BigDecimal ->
                // Create the order
                val order = Order(
                    orderNo =  generateOrderNo(),
                    userId = cart.userId,
                    createdAt = LocalDateTime.now(),
                    orderStatus = OrderStatus.PENDING,
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


    private fun notifyCartUpdate(cartId: Long): Mono<Int> {
        return cartItemRepository.findByCartId(cartId)
            .reduce(0) { total, cartItem -> total + cartItem.quantity }
            .doOnNext { totalQuantity ->
                logger.debug("Centralized notification: Cart item count is now $totalQuantity")
                sendCartUpdate(totalQuantity)
            }
    }

}
