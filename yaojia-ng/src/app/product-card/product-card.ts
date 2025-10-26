import { Component, input } from '@angular/core';
import { Product } from '../model/product';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'product-card',
  imports: [RouterLink],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss'
})
export class ProductCard {
  product = input.required<Product>();
}
