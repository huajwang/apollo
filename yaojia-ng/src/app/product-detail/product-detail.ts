import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Product } from '../model/product';
import { ActivatedRoute } from '@angular/router';
import { ProductService } from '../service/product-service';
import { CartStore } from '../cart/cart-store';
import { CommonModule, KeyValuePipe, SlicePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips'
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { ProductCard } from '../product-card/product-card';

@Component({
  selector: 'app-product-detail',
  imports: [
    CommonModule,
    KeyValuePipe,
    SlicePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatListModule,
    ProductCard
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss'
})
export class ProductDetail implements OnInit {

  // Signals for component state
  product = signal<Product | null>(null);
  selectedImageIndex = signal<number>(0);
  selectedSpecifications = signal<{ [key: string]: string }>({});
  quantity = signal<number>(1);
  isInWishlist = signal<boolean>(false);
  relatedProducts = signal<Product[]>([]);

  // Computed properties
  productLoaded = computed(() => this.product() !== null);

  // Computed properties
  selectedImage = computed(() => {
    const prod = this.product();
    const index = this.selectedImageIndex();
    const galleryItem = prod?.gallery?.[index];
    // Gallery items are objects with photoUrl property
    const galleryUrl = galleryItem?.photoUrl;
    return galleryUrl || prod?.imageUrl;
  });

  variantSpecifications = computed(() => {
    const specs = this.product()?.specifications;
    if (!specs) return {};

    const variants: { [key: string]: string[] } = {};
    Object.entries(specs).forEach(([key, value]) => {
      if (Array.isArray(value)) variants[key] = value;
    });
    return variants;
  });

  technicalSpecifications = computed(() => {
    const specs = this.product()?.specifications;
    if (!specs) return {};

    const filtered: { [key: string]: string | string[] } = {};
    Object.entries(specs).forEach(([key, value]) => {
      if (!Array.isArray(value)) filtered[key] = value;
    });
    return filtered;
  });

  variantKeys = computed(() => Object.keys(this.variantSpecifications()));

  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartStore = inject(CartStore);

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const productId = Number(params['productId']);
      if (productId) {
        this.loadProduct(productId);
        this.loadRelatedProducts(productId);
      }
    });
  }

  private loadProduct(productId: number) {
    this.productService.getProduct(productId).subscribe({
      next: (product) => {
        this.product.set(product);
        const variants = this.variantSpecifications();
        const defaultSelections: { [key: string]: string } = {};
        Object.entries(variants).forEach(([key, values]) => {
          if (values.length > 0) defaultSelections[key] = values[0];
        });
        this.selectedSpecifications.set(defaultSelections);
      },
      error: (error) => console.error('Error loading product:', error)
    });
  }

  private loadRelatedProducts(productId: number) {
    this.productService.getRelatedProducts(productId).subscribe({
      next: (products) => {
        this.relatedProducts.set(products);
      },
      error: (error) => console.error('Error loading related products:', error)
    });
  }

  selectImage(index: number) {
    this.selectedImageIndex.set(index);
  }

  openImageDialog() {
    console.log('Opening image dialog');
  }

  onSpecificationSelect(specKey: string, value: string) {
    this.selectedSpecifications.update(current => ({ ...current, [specKey]: value }));
  }

  variantOptions(specKey: string): string[] {
    return this.variantSpecifications()[specKey] || [];
  }

  increaseQuantity() {
    this.quantity.update(qty => qty + 1);
  }

  decreaseQuantity() {
    const currentQty = this.quantity();
    if (currentQty > 1) this.quantity.set(currentQty - 1);
  }

  onQuantityChange(event: any) {
    const value = parseInt(event.target.value);
    if (value && value > 0) this.quantity.set(value);
  }

  addToCart() {
    const product = this.product();
    if (product) {
      const properties = this.selectedSpecifications();
      this.cartStore.addItem(product, this.quantity(), properties);
      console.log(`Added ${this.quantity()} x ${product.productName} to cart`);
      this.quantity.set(1);
    }
  }

  toggleWishlist() {
    this.isInWishlist.update(inWishlist => !inWishlist);
  }
}
