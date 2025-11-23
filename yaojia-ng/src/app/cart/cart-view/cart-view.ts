import { Component, inject, ChangeDetectionStrategy, DestroyRef, signal, computed, effect } from '@angular/core';
import { CartService } from '../cart-service';
import { CartItem } from '../cart-item';
import { CartStore } from '../cart-store';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../service/auth-service';

@Component({
  selector: 'app-cart-view',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule, MatTooltipModule, RouterLink],
  templateUrl: './cart-view.html',
  styleUrl: './cart-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CartView {
  cartStore = inject(CartStore);
  cartService = inject(CartService);
  authService = inject(AuthService);
  router = inject(Router);
  destroyRef = inject(DestroyRef);

  isLoading = signal(true);
  cartItems = this.cartStore.cartItems;

  subtotal = computed(() => {
    return this.cartItems().reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  });

  tax = computed(() => this.subtotal() * 0.08);

  total = computed(() => this.subtotal() + this.tax());

  cartEmpty = computed(() => this.cartItems().length === 0);

  constructor() {
    this.loadCart();
  }

  private loadCart(): void {
    // Only load from backend if user is logged in
    // Guests use localStorage which is already loaded in CartStore
    if (!this.authService.isLoggedIn()) {
      this.isLoading.set(false);
      return;
    }

    this.cartService
      .getCart()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.isLoading.set(false),
        error: (err) => {
          console.error('Error loading cart from backend:', err);
          this.isLoading.set(false);
        },
      });
  }

  increaseQuantity(productId: number): void {
    const items = this.cartItems();
    const currentItem = items.find((item) => item.product.productId === productId);
    if (currentItem && currentItem.quantity < 99) {
      this.cartStore.updateQuantity(productId, currentItem.quantity + 1);
    }
  }

  decreaseQuantity(productId: number): void {
    const items = this.cartItems();
    const currentItem = items.find((item) => item.product.productId === productId);
    if (currentItem && currentItem.quantity > 1) {
      this.cartStore.updateQuantity(productId, currentItem.quantity - 1);
    }
  }

  removeItem(productId: number): void {
    this.cartStore.removeItem(productId);
    // Only sync to backend if logged in
    if (!this.authService.isLoggedIn()) {
      return;
    }
    // Sync the updated cart items to backend by setting quantity to 0 for removed item
    const updatedItems = this.cartItems().map(item => ({
      ...item,
      quantity: item.product.productId === productId ? 0 : item.quantity
    }));
    this.cartService
      .updateCart(updatedItems)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err) => console.error('Error removing item:', err),
      });
  }

  clearCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.cartStore.clearCart();
      // Only sync to backend if logged in
      if (this.authService.isLoggedIn()) {
        this.cartService
          .clearCart()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            error: (err) => console.error('Error clearing cart:', err),
          });
      }
    }
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

}
