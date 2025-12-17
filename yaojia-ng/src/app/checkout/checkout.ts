import { Component, inject, computed, signal, DestroyRef, OnInit } from '@angular/core';
import { CartStore } from '../cart/cart-store';
import { CartService } from '../cart/cart-service';
import { OrderService, PlaceOrderRequest } from '../service/order-service';
import { AuthService } from '../service/auth-service';
import { User } from '../auth/user';
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
export class CheckoutComponent implements OnInit {
  cartStore = inject(CartStore);
  cartService = inject(CartService);
  orderService = inject(OrderService);
  authService = inject(AuthService);
  router = inject(Router);
  destroyRef = inject(DestroyRef);
  fb = inject(FormBuilder);

  // Make Math available in template
  Math = Math;

  isProcessing = signal(false);
  orderPlaced = signal(false);
  savedAddress = signal<User | null>(null);
  showAddressPrompt = signal(false);

  cartItems = this.cartStore.cartItems;

  subtotal = computed(() => {
    const items = this.cartItems();
    const sum = items.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
    // Round to 2 decimal places to avoid floating point precision issues
    return Math.round(sum * 100) / 100;
  });

  tax = computed(() => {
    const subtotalValue = this.subtotal();
    const taxAmount = subtotalValue * 0.08;
    // Round to 2 decimal places
    return Math.round(taxAmount * 100) / 100;
  });

  total = computed(() => {
    const subtotalValue = this.subtotal();
    const taxValue = this.tax();
    // Round to 2 decimal places
    return Math.round((subtotalValue + taxValue) * 100) / 100;
  });

  // Simple checkout form
  checkoutForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-\(\)]{10,}$/)]],
    address: ['', [Validators.required, Validators.minLength(5)]],
    city: ['', [Validators.required]],
    postalCode: ['', [Validators.required]],
    paymentMethod: ['credit-card', Validators.required],
  });

  ngOnInit() {
    this.authService.getUserProfile().subscribe(user => {
      if (user && user.address) {
        this.savedAddress.set(user);
        this.showAddressPrompt.set(true);
      }
    });
  }

  useSavedAddress() {
    const user = this.savedAddress();
    if (user) {
      this.checkoutForm.patchValue({
        fullName: user.customerName || user.name,
        phone: user.phone,
        address: user.address,
        city: user.city,
        postalCode: user.postalCode
      });
      this.showAddressPrompt.set(false);
    }
  }

  useNewAddress() {
    this.showAddressPrompt.set(false);
    // Optionally clear the form or leave it empty
    // this.checkoutForm.reset();
  }

  placeOrder(): void {
    if (!this.checkoutForm.valid) {
      alert('Please fill in all required fields correctly');
      return;
    }

    this.isProcessing.set(true);

    // Prepare order request
    const formValue = this.checkoutForm.value;
    const orderRequest: PlaceOrderRequest = {
      items: this.cartItems().map(item => ({
        productId: item.product.productId,
        productName: item.product.productName,
        imageUrl: item.product.imageUrl,
        quantity: item.quantity,
        price: item.product.price,
        properties: Object.keys(item.properties).length > 0 ? JSON.stringify(item.properties) : undefined
      })),
      subtotal: this.subtotal(),
      tax: this.tax(),
      total: this.total(),
      shippingFee: 0, // Can be calculated based on address if needed
      fullName: formValue.fullName || '',
      phone: formValue.phone || '',
      address: formValue.address || '',
      city: formValue.city || '',
      postalCode: formValue.postalCode || '',
      paymentMethod: formValue.paymentMethod || 'credit-card'
    };

    // Send order to backend
    const submitOrder = () => {
      this.orderService
        .placeOrder(orderRequest)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (order) => {
            console.log(`Order placed successfully: ${order.orderNo}`);
            this.orderPlaced.set(true);
            this.isProcessing.set(false);

            // Clear cart after successful order
            this.cartStore.clearCart();

            // Only clear backend cart if user is logged in
            if (this.authService.isLoggedIn()) {
              this.cartService
                .clearCart()
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                  error: (err) => console.error('Error clearing cart after checkout:', err),
                });
            }

            // Navigate to payment page
            this.router.navigate(['/payment', order.orderId]);
          },
          error: (err) => {
            console.error('Error placing order:', err);
            this.isProcessing.set(false);
            alert('Failed to place order. Please try again.');
          }
        });
    };

    if (this.authService.isLoggedIn()) {
      const updateAddressRequest = {
        customerName: formValue.fullName,
        phone: formValue.phone,
        address: formValue.address,
        city: formValue.city,
        postalCode: formValue.postalCode
      };

      this.authService.updateAddress(updateAddressRequest)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            console.log('Address updated successfully during checkout');
            submitOrder();
          },
          error: (err) => {
            console.warn('Failed to update address during checkout, proceeding with order', err);
            submitOrder();
          }
        });
    } else {
      submitOrder();
    }
  }

  continueShoppingAfterOrder(): void {
    this.router.navigate(['/']);
  }

  goBackToCart(): void {
    this.router.navigate(['/cart']);
  }
}
