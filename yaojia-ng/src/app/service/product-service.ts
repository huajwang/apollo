import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Product } from '../model/product';
import { ProductDetailResponse } from '../model/product-detail-response';
import { environment } from '../../environments/environment';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private http = inject(HttpClient)

  readonly API_URL = environment.apiUrl;

  getAllProducts() {
    return this.http.get<Product[]>(`${this.API_URL}/product/all`);
  }

  getProduct(productId: number) {
    return this.http.get<ProductDetailResponse>(`${this.API_URL}/product/detail/${productId}`)
      .pipe(
        map(response => {
          // Merge the product data with gallery and specifications
          const product = response.product;
          return {
            ...product,
            gallery: response.gallery,
            specifications: this.parseSpecifications(response.specifications)
          } as Product;
        })
      );
  }

  private parseSpecifications(specs: any[] | undefined): { [key: string]: string | string[] } | undefined {
    if (!specs || specs.length === 0) return undefined;

    const result: { [key: string]: string | string[] } = {};
    specs.forEach(spec => {
      const values = spec.propertyValue.split(',').map((v: string) => v.trim());
      result[spec.propertyName] = values.length > 1 ? values : values[0];
    });
    return result;
  }

  getRelatedProducts(productId: number) {
    return this.http.get<Product[]>(`${this.API_URL}/product/related/${productId}`);
  }
}
