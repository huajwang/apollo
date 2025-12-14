import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../service/auth-service";
import { catchError, switchMap, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const token = authService.getAccessToken();
    
    // Public endpoints that allow unauthenticated access
    // Do NOT add Authorization header for these endpoints
    const publicEndpoints = [
        '/oauth2', '/login', '/api/product', '/auth/refresh'
    ];
    const isPublicEndpoint = publicEndpoints.some(endpoint => 
        req.url.includes(endpoint)
    );

    // If request already has Authorization header, don't override it
    if (req.headers.has('Authorization')) {
        console.warn('Request already has Authorization header, skipping auth interceptor.');
        return next(req);
    }

    // For public endpoints, don't add token even if available
    // This ensures public APIs work without authentication
    if (isPublicEndpoint) {
        return next(req).pipe(
            catchError(error => {
                // For public endpoints, don't attempt token refresh on 401
                return throwError(() => error);
            })
        );
    }

    // For private endpoints, add token if available
    if (token) {
        console.log('Adding Authorization header with token:', token);
        const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
        return next(authReq).pipe(
            catchError(error => {
                if (error.status == 401) {
                    return authService.refreshTokenAndRetry(req, next);
                }
                return throwError(() => error);
            })
        );
    }

    // No token available for private endpoint - send request as-is
    // Will get 401 which may trigger redirect to login
    return next(req).pipe(
        catchError(error => {
            if (error.status == 401) {
                return authService.refreshTokenAndRetry(req, next);
            }
            return throwError(() => error);
        })
    );
};
