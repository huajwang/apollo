import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CartService } from '../cart-service';
import { CartItem } from '../cart-item';
import { Subscription } from 'rxjs';
import { CartStore } from '../cart-store';
import { Product } from '../../model/product';

@Component({
  selector: 'app-cart-view',
  imports: [],
  templateUrl: './cart-view.html',
  styleUrl: './cart-view.scss',
})
export class CartView implements OnInit, OnDestroy {

  cartStore = inject(CartStore)
  cartService = inject(CartService)
  cartItems: CartItem[] = []
  private subscription = new Subscription()

  addProduct(product: Product) {
    this.cartStore.addItem(product.productId, product.productName, product.price)
  }

  increaseQuantity(id: number) {
    const items = this.cartStore.cartItems()
    const currentItem: CartItem | undefined = items.find((item) => item.id == id)
    if (currentItem) {
      this.cartStore.updateQuantity(id, currentItem.quantity + 1)
    }
  }

  decreaseQuantity(id: number) {
    const items = this.cartStore.cartItems()
    const currentItem: CartItem | undefined = items.find((item) => item.id == id)
    if (currentItem) {
      this.cartStore.updateQuantity(id, currentItem.quantity -1)
    }
  }

  removeItem(id: number) {
    this.cartStore.removeItem(id)
    this.subscription.add(
      this.cartService.clearCart().subscribe({
        next: () => console.log("Remove item successfully: " + id),
        error: (err) => {
          console.log("Remove item failed: " + err)
        }
      })
    )
  }

  clearCart() {
    this.cartStore.clearCart()
    
    this.subscription.add(
      this.cartService.clearCart().subscribe({
        next: () => console.log("Clear shopping cart successfully"),
        error: (err) => {
          console.log("Error occurs while clear cart: " + err)
          // TODO - set the original cart items back to cartStore?
        }
      })
    )
      
  }

  ngOnInit(): void {
    this.subscription.add(
      this.cartService.getCart().subscribe({
      next: (cartItems) => {
        this.cartItems = cartItems
      },
      error: (err) => {
        console.log("Error get shopping cart: " + err)
      }
    }))
    
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe()
  }

}
