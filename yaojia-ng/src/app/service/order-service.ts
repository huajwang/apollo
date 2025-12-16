import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { ProductVariantProperties } from '../cart/cart-item';

export interface OrderItem {
  productId: number;
  productName: string;
  imageUrl: string;
  quantity: number;
  price: number;
  properties?: string; // JSON string of variant properties
}

export interface PlaceOrderRequest {
  items: OrderItem[];
  subtotal: number;
  tax: number;
  total: number;
  shippingFee: number;
  fullName: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  postalCode: string;
  paymentMethod: string;
}

export interface OrderResponse {
  orderId: number;
  orderNo: string;
  userId: string;
  deliveryAddress: string;
  originalTotal: number;
  discountedTotal: number;
  hst: number;
  shippingFee: number;
  orderTotal: number;
  createdAt: string;
  orderStatus: string;
  items: OrderItem[];
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  
  private http = inject(HttpClient);
  readonly apiUrl = `${environment.apiUrl}/orders`;

  /**
   * Place an order.
   * Works for both authenticated users and guests.
   * Authenticated users: Order is linked to their account
   * Guests: A guest user record is created and order is linked to it
   */
  placeOrder(request: PlaceOrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.apiUrl}/place`, request);
  }

  getMyOrders(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`${this.apiUrl}/my-orders`);
  }
}
