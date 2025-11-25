import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../service/auth-service";
import { catchError, switchMap, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const token = authService.getAccessToken();
    
    // Public endpoints that allow unauthenticated access
    // But authenticated users can still send their token
    const publicEndpoints = [
        '/oauth2', '/login', '/api/product', '/auth/refresh', '/api/cart', '/api/orders'
    ];
    const isPublicEndpoint = publicEndpoints.some(endpoint => 
        req.url.includes(endpoint)
    );

    // If request already has Authorization header, don't override it
    if (req.headers.has('Authorization')) {
        return next(req);
    }

    // Add token if available, regardless of whether endpoint is public or private
    if (token) {
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

    // No token available - send request as-is
    // Public endpoints will work without auth, private endpoints will get 401
    return next(req).pipe(
        catchError(error => {
            if (error.status == 401 && !isPublicEndpoint) {
                return authService.refreshTokenAndRetry(req, next);
            }
            return throwError(() => error);
        })
    );
};
