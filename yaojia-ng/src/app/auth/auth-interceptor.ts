import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../service/auth-service";
import { catchError, switchMap } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const publicEndpoints = [
        '/oauth2', '/login', '/api/product',
    ];
    const isPublicEndpoint = publicEndpoints.some(endpoint => 
        req.url.includes(endpoint)
    );

    if (isPublicEndpoint) return next(req);

    return authService.ensureValidToken().pipe(
        switchMap(token => {
            if (token) {
                // Clone request with authorization header
                const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
                return next(authReq);
            } else {
                // No token available
                return next(req);
            }
        }),
        catchError(error => {
            console.error('Auth interceptor error:', error);
            return next(req);
        })
    );
}
