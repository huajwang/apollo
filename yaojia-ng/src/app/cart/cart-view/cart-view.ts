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
  errorMessage = signal<string | null>(null);
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
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // Only load from backend if user is logged in
    // Guests use localStorage which is already loaded in CartStore
    if (!this.authService.isLoggedIn()) {
      this.isLoading.set(false);
      return;
    }

    this.cartStore.refresh()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.isLoading.set(false),
        error: (err) => {
          console.error('Error loading cart from backend:', err);
          this.errorMessage.set('Failed to load cart. Please try again.');
          this.isLoading.set(false);
        },
      });
  }

  onRetry(): void {
    this.loadCart();
  }

  increaseQuantity(item: CartItem): void {
    if (item.quantity < 99) {
      this.cartStore.updateQuantity(item.product.productId, item.quantity + 1, item.properties);
    }
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.cartStore.updateQuantity(item.product.productId, item.quantity - 1, item.properties);
    }
  }

  removeItem(item: CartItem): void {
    this.cartStore.removeItem(item.product.productId, item.properties);
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
