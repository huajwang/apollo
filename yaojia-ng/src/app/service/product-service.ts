import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Product } from '../model/product';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private http = inject(HttpClient)

  readonly url = 'https://yaojiabuy.com/api';

  getAllProducts() {
    return this.http.get<Product[]>(`${this.url}/product/all`);
  }

  getProductById(id: number) {
    return this.http.get<Product>(`${this.url}/product/detail/${id}`);
  }
}
