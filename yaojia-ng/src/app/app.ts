import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatBadgeModule} from '@angular/material/badge';
import { CartStore } from './cart/cart-store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('yaojia-ng');
  cartStore = inject(CartStore);
  
  cartItemCount = computed(() => {
    return this.cartStore.cartItems().reduce((sum, item) => sum + item.quantity, 0);
  });
}
