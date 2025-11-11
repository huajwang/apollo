import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Product } from '../model/product';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private http = inject(HttpClient)

  readonly API_URL = environment.apiUrl;

  getAllProducts() {
    return this.http.get<Product[]>(`${this.API_URL}/product/all`);
  }

  getProductById(id: number) {
    return this.http.get<Product>(`${this.API_URL}/product/detail/${id}`);
  }
}
