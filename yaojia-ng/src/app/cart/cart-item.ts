import { Product } from "../model/product";

export interface CartItem {
    itemId?: number;  // Backend ID for database record
    product: Product;
    quantity: number;
    properties: ProductVariantProperties;
}

export type ProductVariantProperties = { [key: string]: string };
