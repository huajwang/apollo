import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../service/auth-service";
import { catchError, switchMap, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const token = authService.getAccessToken();
    const publicEndpoints = [
        '/oauth2', '/login', '/api/product',
    ];
    const isPublicEndpoint = publicEndpoints.some(endpoint => 
        req.url.includes(endpoint)
    );

    if (isPublicEndpoint) return next(req);

    // If request already has Authrization header, don't override it
    if (req.headers.has('Authorization')) {
        return next(req);
    }
    // Clone request with authorization header
    const authReq = token ?
        req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
    return next(authReq).pipe(
        catchError(error => {
            if (error.status == 401) {
                return authService.refreshTokenAndRetry(req, next);
            }
            return throwError(() => error);
        })
    );
    
}
