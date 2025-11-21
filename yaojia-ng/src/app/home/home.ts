import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { Product } from '../model/product';
import { ProductService } from '../service/product-service';
import { ProductCard } from '../product-card/product-card';
import { MatFormField, MatLabel } from "@angular/material/form-field";
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-home',
  imports: [ProductCard, MatFormField, MatLabel, MatInputModule],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit, OnDestroy {

  private subscription = new Subscription();

  products: Product[] = [];
  filteredProducts: Product[] = [];

  productService = inject(ProductService);

  applyFilter(event: Event) {
    const inputElement = event.target as HTMLInputElement;
    const searchText = inputElement.value.trim();
    const searchTextLower = searchText.toLowerCase();
    this.filteredProducts = this.products.filter(product => {
      console.log('Checking product:', product.productName);
      return product.productName.toLowerCase().includes(searchTextLower) ||
      product.description.toLowerCase().includes(searchTextLower) ||
      product.category.toLowerCase().includes(searchTextLower)
    });
  }

  ngOnInit() {
    this.subscription.add(
      this.productService.getAllProducts().subscribe({
        next: (products) => {
          this.products = products;
          this.filteredProducts = products;
        },
        error: (err) => {
          console.error('Error fetching products:', err);
        }
      })
    )
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
  
}

