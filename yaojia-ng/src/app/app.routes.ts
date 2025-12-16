import { Routes } from '@angular/router';
import { Home } from './home/home';
import { ProductDetail } from './product-detail/product-detail';
import { CartView } from './cart/cart-view/cart-view';
import { CheckoutComponent } from './checkout/checkout';
import { LoginComponent } from './auth/login-component/login-component';
import { AuthCallbackComponent } from './auth/auth-callback-component/auth-callback-component';
import { SearchComponent } from './search/search';
import { AuthGuard } from './guards/auth-guard';
import { OrderHistoryComponent } from './order-history/order-history';
import { AddressComponent } from './address/address';
import { PaymentComponent } from './payment/payment';

export const routes: Routes = [
  { path: '', component: Home, title: 'Yaojia Buy' },
  { path: 'search', component: SearchComponent, title: 'Search' },
  { path: 'product/detail/:productId', component: ProductDetail, title: 'Product detail' },
  { path: 'cart', component: CartView, title: 'Cart' }, // canActivate: [AuthGuard]
  { path: 'checkout', component: CheckoutComponent, title: 'Checkout' },
  { path: 'login', component: LoginComponent, title: 'OAuth2 Login'},
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: 'orders', component: OrderHistoryComponent, title: 'Order History', canActivate: [AuthGuard] },
  { path: 'address', component: AddressComponent, title: 'My Address', canActivate: [AuthGuard] },
  { path: 'payment/:orderId', component: PaymentComponent, title: 'Payment' },
];
