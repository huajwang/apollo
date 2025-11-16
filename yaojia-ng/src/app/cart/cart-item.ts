import { Product } from "../model/product";

export interface CartItem {
    product: Product;
    quantity: number;
    properties: ProductVariantProperties;
}

export type ProductVariantProperties = { [key: string]: string };
