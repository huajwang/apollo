import { Routes } from '@angular/router';
import { Home } from './home/home';
import { ProductDetail } from './product-detail/product-detail';
import { CartView } from './cart/cart-view/cart-view';

export const routes: Routes = [
  {
      path: '',
      component: Home,
      title: 'Yaojia Buy',
    },
    {
      path: 'product/detail/:id',
      component: ProductDetail,
      title: 'Product detail',
    },
    {
      path: 'cart', component: CartView, title: 'Cart'
    },
  ];
