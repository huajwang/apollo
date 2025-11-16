export interface Product {
    
    productId: number;
    productName: string;
    description: string;
    imageUrl: string;
    price: number;
    additionalInfo: string | null;
    additionalInfoMap: { [key: string]: any };
    category: string;
    discountType: string | null;
    discountValue: number | null;
    discountedPrice: number | null;
    showNewProductBadge: boolean;
    percentageDiscounted: boolean;
    flatDiscounted: boolean;

    // Extended properties for product detail page
    gallery?: string[];
    specifications?: { [key: string]: string | string[] };
}
