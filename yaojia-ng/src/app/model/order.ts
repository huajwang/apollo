export interface Order {
    orderId: number;
    orderNo: string;
    userId: string;
    deliveryAddress?: string;
    originalTotal: number;
    discountedTotal: number;
    hst: number;
    shippingFee: number;
    orderTotal: number;
    createdAt: string;
    orderStatus: string;
}
