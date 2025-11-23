import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { CartStore } from '../cart/cart-store';
import { CartService } from '../cart/cart-service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class CheckoutComponent {
  cartStore = inject(CartStore);
  cartService = inject(CartService);
  router = inject(Router);
  destroyRef = inject(DestroyRef);
  fb = inject(FormBuilder);

  // Make Math available in template
  Math = Math;

  isProcessing = signal(false);
  orderPlaced = signal(false);

  cartItems = this.cartStore.cartItems;

  subtotal = computed(() => {
    return this.cartItems().reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
  });

  tax = computed(() => this.subtotal() * 0.08);

  total = computed(() => this.subtotal() + this.tax());

  // Simple checkout form
  checkoutForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-\(\)]{10,}$/)]],
    address: ['', [Validators.required, Validators.minLength(5)]],
    city: ['', [Validators.required]],
    postalCode: ['', [Validators.required]],
    paymentMethod: ['credit-card', Validators.required],
  });

  placeOrder(): void {
    if (!this.checkoutForm.valid) {
      alert('Please fill in all required fields correctly');
      return;
    }

    this.isProcessing.set(true);

    // Simulate order placement
    setTimeout(() => {
      this.orderPlaced.set(true);
      this.isProcessing.set(false);

      // Clear cart after successful order
      this.cartStore.clearCart();

      // Optionally clear backend cart
      this.cartService
        .clearCart()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          error: (err) => console.error('Error clearing cart after checkout:', err),
        });

      // Redirect to home after 10 seconds
      setTimeout(() => {
        this.router.navigate(['/']);
      }, 10000);
    }, 1500);
  }

  continueShoppingAfterOrder(): void {
    this.router.navigate(['/']);
  }

  goBackToCart(): void {
    this.router.navigate(['/cart']);
  }
}
