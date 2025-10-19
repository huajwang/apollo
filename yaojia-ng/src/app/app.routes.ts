import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Detail } from './product/detail/detail';

export const routes: Routes = [
  {
      path: '',
      component: Home,
      title: 'Home page',
    },
    {
      path: 'detail/:id',
      component: Detail,
      title: 'Home details',
    },
  ];
