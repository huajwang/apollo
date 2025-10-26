import { RenderMode, ServerRoute } from '@angular/ssr';
import { Product } from './model/product';

export const serverRoutes: ServerRoute[] = [
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'product/detail/:id',
    renderMode: RenderMode.Prerender,
    getPrerenderParams: async () => {
      try {
        // Fetch product IDs from backend API
      const response = await fetch('/api/product/all');
      const products = await response.json();
      // Extrac IDs and return in the format Angular expects
      return products.map((product: Product) => ({ id: product.productId.toString() }));
      } catch( error ) {
        console.warn('Failed to fetch products for prerendering:', error);
        // Fall back to some default product IDs
        return [ { id: '1' }, { id: '5' }, { id: '17' }]; // TODO - update preferred product IDs
      }
   }
  },
  {
    path: '**',
    renderMode: RenderMode.Server // Use server rending for other routes
  }
];
