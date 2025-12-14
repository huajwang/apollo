import { computed, inject, Injectable, signal, Injector } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, Observable, of, tap, map, throwError, switchMap, from } from 'rxjs';
import { User } from '../auth/user';
import { HttpClient, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { decodeJwtPayload, getUserFromToken } from '../utils/jwt-utils';
import { CartStore } from '../cart/cart-store';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private baseUrl = environment.baseUrl;
  private apiUrl = environment.apiUrl;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly RETURN_URL_KEY = 'auth_return_url';

  private http = inject(HttpClient)
  private router = inject(Router)
  private injector = inject(Injector)

  private userSubject = new BehaviorSubject<User | null>(null);
  private user = signal<User | null>(null);

  // Public observables and signals
  public readonly user$ = this.userSubject.asObservable();
  public readonly currentUser = this.user.asReadonly();
  public readonly isAuthenticated = computed(() => this.user() !== null);

  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  constructor() {
    // Initialize user from stored token on app startup
    // This allows the user to remain logged in after page refresh
    const storedToken = localStorage.getItem(this.TOKEN_KEY);
    if (storedToken) {
      this.accessToken = storedToken;
      const userFromToken = getUserFromToken(storedToken);
      if (userFromToken) {
        this.setUser(userFromToken);
        console.log('User loaded from stored token on app initialization:', userFromToken);
      }
    }
  }

  getAccessToken(): string | null {
    // Try to get from memory first, then fall back to localStorage
    if (this.accessToken) {
      return this.accessToken;
    }
    const storedToken = localStorage.getItem(this.TOKEN_KEY);
    if (storedToken) {
      this.accessToken = storedToken;
    }
    return this.accessToken;
  }

  setAccessToken(token: string | null) {
    console.log('setAccessToken called with:', token ? token.substring(0, 20) + '...' : 'null');
    this.accessToken = token;
    if (token) {
      localStorage.setItem(this.TOKEN_KEY, token);
      console.log('Token saved to localStorage, verify:', localStorage.getItem(this.TOKEN_KEY)?.substring(0, 20) + '...');
    } else {
      localStorage.removeItem(this.TOKEN_KEY);
    }
  }

  getRefreshToken() {
    if (this.refreshToken) {
      return this.refreshToken;
    }
    const storedRefresh = localStorage.getItem('refresh_token');
    if (storedRefresh) {
      this.refreshToken = storedRefresh;
    }
    return this.refreshToken;
  }

  setRefreshToken(refreshToken: string) {
    this.refreshToken = refreshToken;
    localStorage.setItem('refresh_token', refreshToken);
  }

  /**
   * Load user from stored token on app initialization
   */
  isLoggedIn() {
    return this.getToken() !== null && this.user() !== null;
  }

  /**
   * Start OAuth2 login flow
   * Angular redirects to backend OAuth2 endpoint
   */
  loginWithProvider(provider: 'google' | 'tiktok' | 'wechat' | 'facebook' | 'github'): void {
    const authUrl = `${this.baseUrl}/oauth2/authorization/${provider}`;
    window.location.href = authUrl;
  }

  /**
   * Handle OAuth2 callback with token from YaojiaBuy backend
   */
  handleAuthCallback(token: string): Observable<User> {
    console.log('handleAuthCallback called with token:', token.substring(0, 20) + '...');
    this.setAccessToken(token);
    console.log('Token stored. getAccessToken() now returns:', this.getAccessToken()?.substring(0, 20) + '...');
    
    // Extract user info from token for immediate UI update (optimistic)
    const userFromToken = getUserFromToken(token);
    console.log('User extracted from token:', userFromToken);
    
    if (userFromToken) {
      this.setUser(userFromToken);
      // Sync cart after login to merge guest items with backend cart
      // Use lazy injection to avoid circular dependency
      this.injector.get(CartStore).syncCartAfterLogin();
      
      // Navigate to the intended destination or home after login
      const returnUrl = this.getAndClearReturnUrl();
      this.router.navigateByUrl(returnUrl);

      // Verify token with backend in background and update if different
      // This provides security while maintaining good UX
      this.getCurrentUser().pipe(
        catchError(error => {
          console.warn('Server verification failed, using token-based user info:', error);
          return of(userFromToken)
        })
      ).subscribe({
        next: (serverUser) => {
          if (JSON.stringify(serverUser) !== JSON.stringify(userFromToken)) {
            this.setUser(serverUser);
          }
        }, 
        error: (err) => {
          console.error('Error verifying user after login:', err);
        }});

        return of(userFromToken);
    }
    // Fallback to fetching user from backend
    return this.getCurrentUser().pipe(
      tap(user => {
        this.setUser(user);
        // Sync cart after login to merge guest items with backend cart
        // Use lazy injection to avoid circular dependency
        this.injector.get(CartStore).syncCartAfterLogin();
        const returnUrl = this.getAndClearReturnUrl();
        this.router.navigateByUrl(returnUrl);
      })
    );
}

  /**
   * Get the stored return URL and clear it
   * Returns '/' if not valid URL is stored
   */
  private getAndClearReturnUrl() {
    const storeUrl = sessionStorage.getItem(this.RETURN_URL_KEY);
    sessionStorage.removeItem(this.RETURN_URL_KEY);
    if (storeUrl) return storeUrl;
    return '/';
  }

  /**
   * Get current user from backend
   */
  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/auth/user`).pipe(
      catchError(error => {
        console.error('Failed to get current user:', error);
        this.logout();
        return of(null as any)
      })
    );
  }

  getToken(): string | null {
    return this.getAccessToken();
  }

  private setUser(user: User | null): void {
    this.user.set(user);
    this.userSubject.next(user);
  }

  private logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {}).pipe(
      tap(() => {
        this.setAccessToken(null);
        this.setUser(null);
        this.router.navigate(['/login']);
      }),
      catchError(error => {
        // Even if backend logout fails, clear local state
        this.setAccessToken(null);
        this.setUser(null);
        this.router.navigate(['/login']);
        return of(null);
      })
    );
  }

  /**
   * Attempt to refresh token or redirect to login
   */
  private attemptTokenRefresh(): Observable<string | null> {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.refreshAccessToken(refreshToken).pipe(
        catchError(() => {
          return this.redirectToLogin();
        })
      );
    }
    return this.redirectToLogin();
  }

  /**
   * Refresh access token using refresh token
   * The refresh token is stored in an HttpOnly cookie by the backend,
   * so the browser will automatically send it with this request.
   * We just send an empty object to trigger the endpoint.
   * 
   * IMPORTANT: withCredentials: true is required for browser to send HttpOnly cookies
   */
  private refreshAccessToken(refreshToken: string): Observable<string> {
    return this.http.post<{accessToken: string, refreshToken?: string}>(`${this.apiUrl}/auth/refresh`, {
      refreshToken: "" // Backend will read from HttpOnly cookie
    }, {
      withCredentials: true  // CRITICAL: allows browser to send HttpOnly cookies
    }).pipe(
      tap(response => {
        // Store new access token
        this.setAccessToken(response.accessToken);
        // Backend returns new refresh token in response body (for backup/sync with cookie)
        if (response.refreshToken) {
          this.setRefreshToken(response.refreshToken);
        }
      }),
      map(response => response.accessToken),
      catchError(error => {
        console.error('Token refresh failed:', error);
        this.clearAllTokens();
        throw error;
      })
    );
  }

  refreshTokenAndRetry(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<any> {
    console.log('Token expired, attempting to refresh...');

    // Attempt to refresh the access token
    return this.refreshAccessToken("").pipe(
      switchMap((newAccessToken: string) => {
        const retryReq = req.clone({
          setHeaders: { Authorization: `Bearer ${newAccessToken}` }
        });
        console.log('Token refreshed successfully, retrying original request');
        return next(retryReq);
      }),
      catchError(refreshError => {
        console.error('Token refresh failed:', refreshError);
        this.clearAllTokens();
        this.redirectToLogin();
        return throwError(() => refreshError);
      })
    );
  }


  private clearAllTokens() {
    this.accessToken = null;
    this.refreshToken = null;
  }

  /**
   * Redirect to login when no token available
   */
  private redirectToLogin(): Observable<null> {
    console.log('No valid token available, redirecting to login');
    this.router.navigate(['/login']);
    return of(null);
  }

}


