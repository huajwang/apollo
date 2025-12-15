import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CartItem } from './cart-item';
import { environment } from '../../environments/environment';
import { map, Observable } from 'rxjs';
import { AuthService } from '../service/auth-service';

interface CartResponse {
  items: CartItem[];
}

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
    return this.http.get<CartResponse>(`${this.apiUrl}/my-cart`, { headers })
      .pipe(map(response => response.items.map(item => this.mapDtoToCartItem(item))));
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
    return this.http.post<CartResponse>(`${this.apiUrl}/update`, { items: dtoItems }, { headers })
      .pipe(map(response => response.items.map(item => this.mapDtoToCartItem(item))));
  }

  mergeGuestCart(guestSessionId: string): Observable<CartItem[]> {
    // Call merge endpoint with guest session ID
    // Backend will merge guest cart items into authenticated user's cart
    return this.http.post<CartResponse>(`${this.apiUrl}/merge`, { guestSessionId })
      .pipe(map(response => response.items.map(item => this.mapDtoToCartItem(item))));
  }

  private mapDtoToCartItem(dto: any): CartItem {
    return {
      itemId: dto.itemId,
      quantity: dto.quantity,
      properties: dto.properties ? JSON.parse(dto.properties) : {},
      product: {
        productId: dto.productId,
        productName: dto.productName,
        imageUrl: dto.imageUrl,
        description: dto.description,
        price: dto.price,
        discountedPrice: dto.discountedPrice,
        // Default values for fields not present in CartItemDto
        additionalInfo: null,
        additionalInfoMap: {},
        category: '',
        discountType: null,
        discountValue: null,
        showNewProductBadge: false,
        percentageDiscounted: false,
        flatDiscounted: false
      }
    };
  }
}
