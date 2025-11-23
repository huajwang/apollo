import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CartItem } from './cart-item';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  
  private http = inject(HttpClient);
  readonly apiUrl = `${environment.apiUrl}/cart`;

  getCart(): Observable<CartItem[]> {
    return this.http.get<CartItem[]>(`${this.apiUrl}/my-cart`);
  }

  addToCart(cartItem: CartItem): Observable<CartItem> {
    return this.http.post<CartItem>(`${this.apiUrl}`, cartItem);
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clear`);
  }

  updateCart(items: CartItem[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/update`, { items });
  }
}
