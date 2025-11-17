import { Product } from './product';

export interface ProductDetailResponse {
  product: Product;
  gallery?: Array<{ photoUrl: string }>;
  specifications?: Array<{
    propertyId: number;
    productId: number;
    propertyName: string;
    propertyValue: string;
  }>;
  reviews?: Array<{
    reviewId: number;
    productId: number;
    reviewer: string;
    content: string;
    createdAt: string;
  }>;
}
