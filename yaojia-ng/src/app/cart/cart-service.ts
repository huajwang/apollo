import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CartItem } from './cart-item';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { AuthService } from '../service/auth-service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  readonly apiUrl = `${environment.apiUrl}/cart`;
  
  // Generate or retrieve guest session ID
  private getGuestSessionId(): string {
    const key = 'guest-session-id';
    let sessionId = sessionStorage.getItem(key);
    if (!sessionId) {
      sessionId = `guest-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      sessionStorage.setItem(key, sessionId);
    }
    return sessionId;
  }
  
  // Helper to add guest session ID header for guest requests only
  private addGuestHeaderIfNeeded(headers?: HttpHeaders): HttpHeaders {
    if (!headers) {
      headers = new HttpHeaders();
    }
    // Only add guest session ID if user is NOT authenticated
    if (!this.authService.isLoggedIn()) {
      const guestSessionId = this.getGuestSessionId();
      return headers.append('X-Guest-Session-Id', guestSessionId);
    }
    return headers;
  }

  getCart(): Observable<CartItem[]> {
    const headers = this.addGuestHeaderIfNeeded();
    return this.http.get<CartItem[]>(`${this.apiUrl}/my-cart`, { headers });
  }

  addToCart(cartItem: CartItem): Observable<CartItem> {
    const headers = this.addGuestHeaderIfNeeded();
    return this.http.post<CartItem>(`${this.apiUrl}`, cartItem, { headers });
  }

  clearCart(): Observable<void> {
    const headers = this.addGuestHeaderIfNeeded();
    return this.http.delete<void>(`${this.apiUrl}/clear`, { headers });
  }

  updateCart(items: CartItem[]): Observable<CartItem[]> {
    // Convert CartItem to CartItemDto format expected by backend
    const dtoItems = items.map(item => ({
      itemId: item.itemId ?? null,  // Include backend ID if available
      productId: item.product.productId,
      imageUrl: item.product.imageUrl,
      productName: item.product.productName,
      description: item.product.description,
      quantity: item.quantity,
      properties: item.properties ? JSON.stringify(item.properties) : null,
      price: item.product.price,
      discountedPrice: item.product.discountedPrice ?? item.product.price  // Use price if discountedPrice is null
    }));
    const headers = this.addGuestHeaderIfNeeded();
    return this.http.post<CartItem[]>(`${this.apiUrl}/update`, { items: dtoItems }, { headers });
  }

  mergeGuestCart(guestSessionId: string): Observable<CartItem[]> {
    // Call merge endpoint with guest session ID
    // Backend will merge guest cart items into authenticated user's cart
    return this.http.post<CartItem[]>(`${this.apiUrl}/merge`, { guestSessionId });
  }
}
