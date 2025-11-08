import { computed, inject, Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, Observable, of, tap } from 'rxjs';
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

  constructor() {
    this.loadUserFromToken()
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
          localStorage.removeItem(this.TOKEN_KEY);
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
  loginWithProvider(provider: 'google' | 'tiktok' | 'wechat'): void {
    const authUrl = `${this.baseUrl}/oauth2/authorization/${provider}`;
    window.location.href = authUrl;
  }

  /**
   * Handle OAuth2 callback with token from YaojiaBuy backend
   */
  handleAuthCallback(token: string): Observable<User> {
    localStorage.setItem(this.TOKEN_KEY, token);
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
    return this.http.get<User>(`${this.apiUrl}/auth/user`, {
      headers: this.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Failed to get current user:', error);
        this.logout();
        return of(null as any)
      })
    );
  }

  /**
   * Get authorization headers
   */
  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return token
      ? new HttpHeaders().set('Authorization', `Bearer ${token}`)
      : new HttpHeaders();
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setUser(user: User | null): void {
    
  }

  private logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {}, {
      headers: this.getAuthHeaders()
    }).pipe(
      tap(() => {
        localStorage.removeItem(this.TOKEN_KEY);
        this.setUser(null);
        this.router.navigate(['/login']);
      }),
      catchError(error => {
        // Even if backend logout fails, clear local state
        localStorage.removeItem(this.TOKEN_KEY);
        this.setUser(null);
        this.router.navigate(['/login']);
        return of(null);
      })
    );
  }
}
