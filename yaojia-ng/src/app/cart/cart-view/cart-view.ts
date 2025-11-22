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
    this.cartService
      .clearCart()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err) => console.error('Error removing item:', err),
      });
  }

  clearCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.cartStore.clearCart();
      this.cartService
        .clearCart()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          error: (err) => console.error('Error clearing cart:', err),
        });
    }
  }

  private loadCart(): void {
    this.cartService
      .getCart()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.isLoading.set(false),
        error: (err) => {
          console.error('Error loading cart:', err);
          this.isLoading.set(false);
        },
      });
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

}
