package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.*
import com.goodfeel.nightgrass.repo.*
import com.goodfeel.nightgrass.web.util.AddCartRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.math.BigDecimal

class CartServiceTest {

    private lateinit var cartRepository: CartRepository
    private lateinit var cartItemRepository: CartItemRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var orderItemRepository: OrderItemRepository
    private lateinit var userRepository: UserRepository
    private lateinit var cartService: CartService

    @BeforeEach
    fun setup() {
        cartRepository = mock(CartRepository::class.java)
        cartItemRepository = mock(CartItemRepository::class.java)
        productRepository = mock(ProductRepository::class.java)
        orderRepository = mock(OrderRepository::class.java)
        orderItemRepository = mock(OrderItemRepository::class.java)
        userRepository = mock(UserRepository::class.java)

        cartService = CartService(
            cartRepository,
            cartItemRepository,
            productRepository,
            orderRepository,
            orderItemRepository,
            userRepository
        )
    }

    @Test
    fun `addProductToCart should add a new product to the cart of a logged-in user`() {
        val user = User(oauthId = "user123", guestId = null)
        val addCartRequest = AddCartRequest(productId = 1L, properties = mutableMapOf("color" to "red"))
        val cartId = 1L
        val cart = Cart(cartId = cartId, userId = user.oauthId, total = BigDecimal.ZERO)
        val newCartItem = CartItem(
            itemId = null,
            cartId = cartId,
            productId = addCartRequest.productId,
            quantity = 1,
            isSelected = true
        )

        `when`(cartRepository.findByUserId(user.oauthId!!)).thenReturn(Mono.just(cart))
        `when`(cartItemRepository.findByCartId(cartId)).thenReturn(Flux.empty())
        `when`(cartItemRepository.save(any(CartItem::class.java))).thenReturn(Mono.just(newCartItem))
        `when`(cartRepository.save(any(Cart::class.java))).thenReturn(Mono.just(cart))
        `when`(cartRepository.findById(cartId)).thenReturn(Mono.just(cart))

        val result = cartService.addProductToCart(addCartRequest, user)

        StepVerifier.create(result)
            .expectNext(cart)
            .verifyComplete()

        verify(cartItemRepository).save(argThat { it.productId == addCartRequest.productId && it.quantity == 1 })
    }


    @Test
    fun `addProductToCart should add an existing product to the cart`() {
        val cartId = 1L
        val user = User(oauthId = "user123", guestId = null)
        val productId = 1L
        val addCartRequest = AddCartRequest(productId = productId, properties = mutableMapOf("color" to "red"))
        val cart = Cart(cartId = cartId, userId = user.oauthId, total = BigDecimal.ZERO)
        val product = Product(
            productId = productId,
            productName = "Drone",
            description = "E99",
            imageUrl = "",
            price = BigDecimal.valueOf(100)
        )

        val existingCartItem = CartItem(
            itemId = 10L,
            cartId = cartId,
            productId = addCartRequest.productId,
            quantity = 1,
            isSelected = true
        )
        val propertyMap = mapOf("color" to "red")
        existingCartItem.setPropertiesFromMap(propertyMap)
        val updatedCartItem = existingCartItem.copy(quantity = 2)
        val updatedCart = cart.copy(total = BigDecimal(200))

        `when`(cartRepository.findByUserId(user.oauthId!!)).thenReturn(Mono.just(cart))
        `when`(cartItemRepository.findByCartId(cartId)).thenReturn(Flux.just(existingCartItem))
        `when`(cartItemRepository.save(any(CartItem::class.java))).thenReturn(Mono.just(updatedCartItem))
        `when`(cartRepository.save(any(Cart::class.java))).thenReturn(Mono.just(updatedCart))
        `when`(productRepository.findById(1L)).thenReturn(Mono.just(product))

        val result = cartService.addProductToCart(addCartRequest, user)

        StepVerifier.create(result)
            .expectNext(cart)
            .verifyComplete()

        verify(cartItemRepository).save(argThat { it.itemId == existingCartItem.itemId && it.quantity == 2 })
        verify(cartRepository).save(argThat { it.cartId == cart.cartId && it.total == BigDecimal(200) })
    }

    @Test
    fun `addProductToCart should add a new product to the cart of a Guest`() {
        val guestId = "guest123"
        val user = User(oauthId = null, guestId = guestId)
        val addCartRequest = AddCartRequest(productId = 1L, properties = mutableMapOf("color" to "red"))
        val cartId = 1L
        val cart = Cart(cartId = cartId, guestId = user.guestId, total = BigDecimal.ZERO)
        val newCartItem = CartItem(
            itemId = null,
            cartId = cartId,
            productId = addCartRequest.productId,
            quantity = 1,
            isSelected = true
        )

        `when`(cartRepository.findByGuestId(user.guestId!!)).thenReturn(Mono.just(cart))
        `when`(cartItemRepository.findByCartId(cartId)).thenReturn(Flux.empty())
        `when`(cartItemRepository.save(any(CartItem::class.java))).thenReturn(Mono.just(newCartItem))
        `when`(cartRepository.save(any(Cart::class.java))).thenReturn(Mono.just(cart))
        `when`(cartRepository.findById(cartId)).thenReturn(Mono.just(cart))

        val result = cartService.addProductToCart(addCartRequest, user)

        StepVerifier.create(result)
            .expectNext(cart)
            .verifyComplete()

        verify(cartItemRepository).save(argThat { it.productId == addCartRequest.productId && it.quantity == 1 })
        verify(cartRepository).save(argThat { it.guestId == guestId && it.userId == null })
    }


    @Test
    fun `getCartForUserOrGuest should return an existing cart for a user`() {
        val user = User(oauthId = "user123", guestId = null)
        val cart = Cart(cartId = 1L, userId = user.oauthId, total = BigDecimal.ZERO)

        `when`(cartRepository.findByUserId(user.oauthId!!)).thenReturn(Mono.just(cart))

        val result = cartService.getCartForUserOrGuest(user)

        StepVerifier.create(result)
            .expectNext(cart)
            .verifyComplete()

        verify(cartRepository).findByUserId(user.oauthId!!)
    }

    @Test
    fun `getCartForUserOrGuest should return an existing cart for a Guest`() {
        val guestId = "guest123"
        val user = User(oauthId = null, guestId = guestId)
        val cart = Cart(cartId = 1L, guestId = guestId, total = BigDecimal.ZERO)

        `when`(cartRepository.findByGuestId(user.guestId!!)).thenReturn(Mono.just(cart))

        val result = cartService.getCartForUserOrGuest(user)

        StepVerifier.create(result)
            .expectNext(cart)
            .verifyComplete()

        verify(cartRepository).findByGuestId(user.guestId!!)
    }

    @Test
    fun `getCartForUserOrGuest should create a new cart for a user if not found`() {
        val user = User(oauthId = "user123", guestId = null)
        val newCart = Cart(cartId = 1L, userId = user.oauthId, total = BigDecimal.ZERO)

        `when`(cartRepository.findByUserId(user.oauthId!!)).thenReturn(Mono.empty())
        `when`(cartRepository.save(any(Cart::class.java))).thenReturn(Mono.just(newCart))

        val result = cartService.getCartForUserOrGuest(user)

        StepVerifier.create(result)
            .expectNext(newCart)
            .verifyComplete()

        verify(cartRepository).save(argThat { it.userId == user.oauthId && it.total == BigDecimal.ZERO })
    }

    @Test
    fun `getCartForUserOrGuest should create a new cart for a Guest if not found`() {
        val guestId = "guest123"
        val user = User(oauthId = null, guestId = guestId)
        val newCart = Cart(cartId = 1L, guestId = guestId, total = BigDecimal.ZERO)

        `when`(cartRepository.findByGuestId(user.guestId!!)).thenReturn(Mono.empty())
        `when`(cartRepository.save(any(Cart::class.java))).thenReturn(Mono.just(newCart))

        val result = cartService.getCartForUserOrGuest(user)

        StepVerifier.create(result)
            .expectNext(newCart)
            .verifyComplete()

        verify(cartRepository).save(argThat { it.guestId == user.guestId && it.total == BigDecimal.ZERO })
    }

    @Test
    fun `updateQuantity should update quantity and return updated cart item when item exists`() {
        val cartItemId = 1L
        val cartId = 100L
        val newQuantity = 5
        val existingCartItem = CartItem(
            itemId = cartItemId,
            cartId = cartId,
            productId = 200L,
            quantity = 2,
            isSelected = true
        )
        val productId = 1L
        val productPrice = BigDecimal.valueOf(100)
        val product = Product(productId = productId, productName = "Drone",
            description = "Good Stuff", imageUrl = "", price = productPrice)

        val updatedCartItem = existingCartItem.copy(quantity = newQuantity)


        `when`(cartItemRepository.findById(existingCartItem.itemId!!)).thenReturn(Mono.just(existingCartItem))
        `when`(cartItemRepository.save(any(CartItem::class.java))).thenReturn(Mono.just(updatedCartItem))
        `when`(cartItemRepository.findByCartId(cartId)).thenReturn(Flux.just(updatedCartItem))
        `when`(productRepository.findById(updatedCartItem.productId)).thenReturn(Mono.just(product))
        val updatedCart = Cart(cartId = cartId, total = BigDecimal.valueOf(500))
        `when`(cartRepository.findById(cartId)).thenReturn(Mono.just(updatedCart))
        `when`(cartRepository.save(updatedCart)).thenReturn(Mono.just(updatedCart))

        val result = cartService.updateQuantity(cartItemId, newQuantity)

        StepVerifier.create(result)
            .expectNext(updatedCartItem)
            .verifyComplete()

        verify(cartItemRepository).save(argThat { it.itemId == cartItemId && it.quantity == newQuantity })
        verify(cartRepository).save(argThat { it.cartId == cartId && it.total == BigDecimal.valueOf(500) })
    }

    @Test
    fun `updateQuantity should emit error when cart item does not exist`() {
        val cartItemId = 1L
        val newQuantity = 5

        `when`(cartItemRepository.findById(cartItemId)).thenReturn(Mono.empty())

        val result = cartService.updateQuantity(cartItemId, newQuantity)

        StepVerifier.create(result)
            .expectErrorMatches { it is IllegalArgumentException && it.message == "Cart item with ID $cartItemId not found" }
            .verify()
        // verify that save() method is not invoked on cartItemRepository
        verify(cartItemRepository, never()).save(any())
    }

}
