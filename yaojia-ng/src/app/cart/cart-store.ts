import { computed, inject, Injectable, signal } from "@angular/core";
import { CartItem } from "./cart-item";
import { AuthService } from "../service/auth-service";
import { CartService } from "./cart-service";
import { catchError, of, Subscribable, Subscription } from "rxjs";

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
        this.items().reduce((total, item) => total + item.price * item.quantity, 0)

    );

    addItem(id: number, name: string, price: number) {
        this.items.update( (currentItems) => {
            const existingItem = currentItems.find((item) => item.id == id)

            let updatedItems: CartItem[];
            if (existingItem) {
                updatedItems = currentItems.map((item) => 
                    item.id == id ? {...item, quantity: item.quantity + 1 } : item)
            } else {
                updatedItems = [...currentItems, { id, price, quantity: 1, properties: "" }]
            }
            // Sync to backend and save to localStorage
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);

            return updatedItems;
        })
    }

    removeItem(id: number) {
        this.items.update((currentItems) => {
            const updatedItems = currentItems.filter((item) => item.id != id)
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);

            return updatedItems;
        })
    }

    updateQuantity(id: number, quantity: number) {
        if (quantity <= 0) {
            this.removeItem(id)
            return;
        }

        this.items.update((currentItems) => {
            const updatedItems = currentItems.map((item) => 
            item.id == id ? {...item, quantity} : item)
            this.syncToBackend(updatedItems);
            this.saveToLocalStorage(updatedItems);
            return updatedItems;
        } )
    }

    clearCart() {
        const emptyCart: CartItem[] = [];
        this.items.set(emptyCart);

        // Sync to backend and save to localStorage
        this.syncToBackend(emptyCart);
        this.saveToLocalStorage(emptyCart);

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
            merged.set(item.id, item)
        });

        // override with backend items (backend wins conflicts)
        backendCart.forEach(item => {
            const existingItem = merged.get(item.id)
            if (existingItem) {
                // combine quantities or use backend data
                merged.set(item.id, {
                    ...item, quantity: Math.max(item.quantity, existingItem.quantity)
                })
            } else {
                merged.set(item.id, item)
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

}
