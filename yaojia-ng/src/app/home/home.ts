import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { Product } from '../model/product';
import { ProductService } from '../service/product-service';
import { ProductCard } from '../product-card/product-card';

@Component({
  selector: 'app-home',
  imports: [ProductCard],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit, OnDestroy {

  private subscription = new Subscription();

  products: Product[] = [];
  filteredProducts: Product[] = [];

  productService = inject(ProductService);

  filterResults(searchText: string) {
    this.filteredProducts = this.products.filter((product: Product) => {
      const searchTextLower = searchText.toLowerCase();
      return product.productName.toLowerCase().includes(searchTextLower) ||
             product.description.toLowerCase().includes(searchTextLower) ||
             product.category.toLowerCase().includes(searchTextLower);
    });
  }

  ngOnInit() {
    this.subscription.add(
      this.productService.getAllProducts().subscribe({
        next: (products) => {
          this.products = products;
          this.filteredProducts = this.products;
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

