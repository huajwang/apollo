import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatBadgeModule} from '@angular/material/badge';
import {MatMenuModule} from '@angular/material/menu';
import {MatDividerModule} from '@angular/material/divider';
import { CartStore } from './cart/cart-store';
import { AuthService } from './service/auth-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule, MatDividerModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  cartStore = inject(CartStore);
  authService = inject(AuthService);
  
  cartItemCount = computed(() => {
    return this.cartStore.cartItems().reduce((sum, item) => sum + item.quantity, 0);
  });

  isLoggedIn = computed(() => this.authService.isLoggedIn());
}
