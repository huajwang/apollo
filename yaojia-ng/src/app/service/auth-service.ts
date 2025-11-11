import { computed, inject, Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, Observable, of, tap, map } from 'rxjs';
import { User } from '../auth/user';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private baseUrl = environment.baseUrl;
  private apiUrl = environment.apiUrl;
  private readonly TOKEN_KEY = 'auth_token';

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
    return this.getCurrentUser().pipe(
      tap(user => {
        this.setUser(user);
        this.router.navigate(['/']); // TODO - navigate to the intended target page?
      })
    );
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

  ensureValidToken(): Observable<string | null> {
    const currentToken = this.getAccessToken();
    console.log('locally stored token:', currentToken);
    if (currentToken) {
      return this.verifyToken(currentToken).pipe(
        catchError(() => {
          return this.attemptTokenRefresh();
        })
      )
    } else return this.attemptTokenRefresh();
  }

  private verifyToken(token: string): Observable<string> {
    console.log('verify token with backend:', token);
    return this.http.get<{valid: boolean}>(`${this.apiUrl}/auth/verify`).pipe(
      tap(response => {
        if (!response.valid) {
          throw new Error('Token invalid');
        }
        console.log('token verified:', response.valid);
      }),
      catchError(() => {
        throw new Error('Token verification failed');
      }),
      map(() => token) // Return the token if valid
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
