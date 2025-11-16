import { Routes } from '@angular/router';
import { Home } from './home/home';
import { ProductDetail } from './product-detail/product-detail';
import { CartView } from './cart/cart-view/cart-view';
import { LoginComponent } from './auth/login-component/login-component';
import { AuthCallbackComponent } from './auth/auth-callback-component/auth-callback-component';
import { AuthGuard } from './guards/auth-guard';

export const routes: Routes = [
  { path: '', component: Home, title: 'Yaojia Buy' },
  { path: 'product/detail/:productId', component: ProductDetail, title: 'Product detail' },
  { path: 'cart', component: CartView, title: 'Cart', canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent, title: 'OAuth2 Login'},
  { path: 'auth/callback', component: AuthCallbackComponent },
];
