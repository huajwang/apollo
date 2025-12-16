import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderService, OrderResponse } from '../service/order-service';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './payment.html',
  styleUrl: './payment.scss'
})
export class PaymentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);

  orderId = signal<string | null>(null);
  order = signal<OrderResponse | null>(null);
  isLoading = signal(true);
  isProcessingPayment = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('orderId');
      if (id) {
        this.orderId.set(id);
        this.loadOrder(id);
      } else {
        this.errorMessage.set('Invalid Order ID');
        this.isLoading.set(false);
      }
    });
  }

  loadOrder(orderId: string) {
    this.isLoading.set(true);
    this.orderService.getOrderById(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load order', err);
        this.errorMessage.set('Failed to load order details.');
        this.isLoading.set(false);
      }
    });
  }

  proceedToPayment() {
    const currentOrder = this.order();
    if (!currentOrder) return;

    this.isProcessingPayment.set(true);
    
    // Construct the payment request URL
    // We are using the existing backend endpoint which expects form data or query params
    // But since it returns a redirect URL, we might need to call it via API and then redirect
    
    // For now, let's assume we have a service method to initiate payment
    this.orderService.initiatePayment(currentOrder).subscribe({
      next: (redirectUrl) => {
        window.location.href = redirectUrl;
      },
      error: (err) => {
        console.error('Payment initiation failed', err);
        this.errorMessage.set('Failed to initiate payment. Please try again.');
        this.isProcessingPayment.set(false);
      }
    });
  }
}
