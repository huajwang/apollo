import { computed, inject, Injectable, signal } from "@angular/core";
import { CartItem, ProductVariantProperties } from "./cart-item";
import { AuthService } from "../service/auth-service";
import { CartService } from "./cart-service";
import { catchError, of, Subscription } from "rxjs";
import { Product } from "../model/product";

@Injectable({
    providedIn: 'root',
})
export class CartStore {

    authService = inject(AuthService)
    cartService = inject(CartService)

    private syncSubscription?: Subscription;

    private items = signal<CartItem[]>([]);
    readonly cartItems = this.items.asReadonly();

    constructor() {
        this.loadCartFromStorage();
        this.syncWithBackend();
    }

    readonly totalQuantity = computed(() =>
        this.items().reduce((total, item) => total + item.quantity, 0)
    );

    readonly totalPrice = computed(() =>
        this.items().reduce((total, item) => total + item.product.price * item.quantity, 0)

    );

    // Add item to cart, or increase quantity if already exists
    addItem(product: Product, quantity: number = 1, properties: ProductVariantProperties = {}) {
        this.items.update( (currentItems) => {
            const existingItem = currentItems.find((item) => 
                item.product.productId == product.productId && this.propertiesMatch(item.properties, properties)

        );

            let updatedItems: CartItem[];
            if (existingItem) {
                updatedItems = currentItems.map((item) => 
                    item.product.productId == product.productId && this.propertiesMatch(item.properties, properties) 
                ? {...item, quantity: item.quantity + quantity } : item)
            } else {
                updatedItems = [...currentItems, { product, quantity, properties }]
            }
            console.log('Cart updated items: ', updatedItems);
            // Sync to backend and save to localStorage
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);

            return updatedItems;
        })
    }

    removeItem(productId: number, properties?: ProductVariantProperties) {
        this.items.update((currentItems) => {
            const updatedItems = currentItems.filter((item) => {
                if (item.product.productId !== productId) return true;
                if (!properties) return false;
                return !this.propertiesMatch(item.properties, properties);
            })
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);

            return updatedItems;
        })
    }

    updateQuantity(productId: number, quantity: number, properties?: ProductVariantProperties) {
        if (quantity <= 0) {
            this.removeItem(productId, properties);
            return;
        }

        this.items.update((currentItems) => {
            const updatedItems = currentItems.map((item) => {
                if (item.product.productId === productId) {
                    if (!properties || this.propertiesMatch(item.properties, properties))
                         return {...item, quantity}
                }
                return item;
            })
            
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);
            return updatedItems;
        } )
    }

    clearCart() {
        const emptyCart: CartItem[] = [];
        this.items.set(emptyCart);

        // Clear from backend (if logged in) and localStorage
        if (this.authService.isLoggedIn()) {
            this.cartService.clearCart().pipe(
                catchError(error => {
                    console.error('Failed to clear cart on backend:', error);
                    return of(void 0);
                })
            ).subscribe(() => {
                this.saveToLocalStorage(emptyCart);
            });
        } else {
            this.saveToLocalStorage(emptyCart);
        }
    }

    /**
     * Initial sync shopping cart on app loading
     */
    private syncWithBackend() {
        if (this.authService.isLoggedIn()) {
            this.cartService.getCart().pipe(
                catchError(error => {
                    return of([])
                })
            ).subscribe(backendCart => {
                const localCart = this.items()
                const mergedCart = this.mergeCartItems(backendCart, localCart)
                this.items.set(mergedCart)
                this.saveToLocalStorage(mergedCart)
            })
        }
    }

    private mergeCartItems(backendCart: CartItem[], localCart: CartItem[]) {
        // simple merge strategy: backend takes precedence
        const merged = new Map<number, CartItem>()

        // add local item first
        localCart.forEach(item => {
            merged.set(item.product.productId, item)
        });

        // override with backend items (backend wins conflicts)
        backendCart.forEach(item => {
            const existingItem = merged.get(item.product.productId);
            if (existingItem) {
                // combine quantities
                merged.set(item.product.productId, {
                    ...item, quantity: Math.max(item.quantity, existingItem.quantity)
                })
            } else {
                merged.set(item.product.productId, item)
            }
        });

        return Array.from(merged.values())
    }

    private saveToLocalStorage(items: CartItem[]) {
        localStorage.setItem('cart', JSON.stringify(items));
    }

    private loadCartFromStorage() {
        const savedCart = localStorage.getItem('cart');
        if (savedCart) {
            try {
                const parsedCart: CartItem[] = JSON.parse(savedCart);
                this.items.set(parsedCart)
            } catch (error) {
                console.warn('Failed to load cart from localStorage: ', error);
                localStorage.removeItem('cart'); // Remove corrupted data
            }
        }
    }

    /**
     * Sync with backend whenever shopping cart changes
     * 
     * Prevents race condition caused by user rapid interactions. The previous pending
     * HTTP request will be cancelled so that no multiple requests are in flight. Also reduce
     * uneccessary server/network loads.
     * 
     * Only syncs when user is authenticated. Guest/unauthenticated users rely on localStorage only.
     */
    private syncToBackend(items: CartItem[]) {
        if (this.authService.isLoggedIn()) {
            // Cancel previous sync if still pending (prevents race condition)
            this.syncSubscription?.unsubscribe();

            this.syncSubscription = this.cartService.updateCart(items).pipe(
                catchError(error => {
                    console.warn('Failed to sync cart to backend:', error);
                    return of(null); // continue with local operation on error
                })
            ).subscribe({
                complete: () => {
                    // clean up reference
                    this.syncSubscription = undefined
                }
            });
        }
    }

    private propertiesMatch(pros1: ProductVariantProperties, pros2: ProductVariantProperties) {
        const keys1 = Object.keys(pros1);
        const keys2 = Object.keys(pros2);
        if (keys1.length !== keys2.length) return false;
        return keys1.every(key => pros1[key] === pros2[key]);
    }

}
