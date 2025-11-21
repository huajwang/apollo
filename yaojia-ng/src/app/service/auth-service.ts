import { computed, inject, Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, Observable, of, tap, map, throwError, switchMap } from 'rxjs';
import { User } from '../auth/user';
import { HttpClient, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { decodeJwtPayload, getUserFromToken } from '../utils/jwt-utils';

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

  private userSubject = new BehaviorSubject<User | null>(null);
  private user = signal<User | null>(null);

  // Public observables and signals
  public readonly user$ = this.userSubject.asObservable();
  public readonly currentUser = this.user.asReadonly();
  public readonly isAuthenticated = computed(() => this.user() !== null);

  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  constructor() {
    this.loadUserFromToken()
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  getRefreshToken() {
    return this.refreshToken;
  }

  setRefreshToken(refreshToken: string) {
    this.refreshToken = refreshToken;
  }

  /**
   * Load user from stored token on app initialization
   */
  private loadUserFromToken(): void {
    const token = this.getToken();
    if (token) {
      this.getCurrentUser().subscribe({
        next: user => this.setUser(user),
        error: () => {
          // Token is invalid, clear it
          this.setAccessToken(null);
        }
      })
    }
  }
  
  isLoggedIn() {
    return this.getToken() !== null && this.user() !== null;
  }

  /**
   * Start OAuth2 login flow
   * Angular redirects to backend OAuth2 endpoint
   */
  loginWithProvider(provider: 'google' | 'tiktok' | 'wechat' | 'facebook'): void {
    const authUrl = `${this.baseUrl}/oauth2/authorization/${provider}`;
    window.location.href = authUrl;
  }

  /**
   * Handle OAuth2 callback with token from YaojiaBuy backend
   */
  handleAuthCallback(token: string): Observable<User> {
    this.setAccessToken(token);
    // Extract user info from token for immediate UI update (optimistic)
    const userFromToken = getUserFromToken(token);
    if (userFromToken) {
      this.setUser(userFromToken);
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
   */
  private refreshAccessToken(refreshToken: string): Observable<string> {
    return this.http.post<{accessToken: string, refreshToken?: string}>(`${this.apiUrl}/auth/refresh`, {
      refreshToken
    }).pipe(
      tap(response => {
        // Store new access token
        this.setAccessToken(response.accessToken);
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
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      // No refresh token available. redirect to login
      this.redirectToLogin();
      return throwError(() => new Error('No refresh token available'));
    }

    // Attempt to refresh the access token
    return this.refreshAccessToken(refreshToken).pipe(
      switchMap((newAccessToken: string) => {
        const retryReq = req.clone({
          setHeaders: { Authorization: `Bearer ${newAccessToken}` }
        });
        // Retry the original request
        return next(retryReq);
      }),
      catchError(refreshError => {
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


