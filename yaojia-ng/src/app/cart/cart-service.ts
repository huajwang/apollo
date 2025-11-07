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
  readonly apiUrl = environment.apiUrl;

  getCart(): Observable<CartItem[]> {
    return this.http.get<CartItem[]>(this.apiUrl);
  }

  addToCart(cartItem: CartItem): Observable<CartItem> {
    return this.http.post<CartItem>(this.apiUrl, cartItem);
  }

  getCartItems(): Observable<CartItem[]> {
    return this.http.get<CartItem[]>(this.apiUrl)
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl)
  }

  updateCart(items: CartItem[]) {
    return this.http.post(`${this.apiUrl}/update`, { items })
  }
}
