export interface JwtPayload {
    sub: string; // User ID
    name: string;
    email?: string;
    avatarUrl?: string;
    provider?: string;
    iat: number;
    exp: number;
    iss: string;
    type: 'access' | 'refresh';
}

/**
 * Decode JWT token payload without cryptographic verification
 * Use ONLY for client-side UI state and UX improvements
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
    // Basic format validation
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1];
    // Add padding if necessary for base64 decoding
    const paddedPayload = payload.padEnd(payload.length + (4 - (payload.length % 4)) % 4, '=');
    try {
        const decoded = atob(paddedPayload);
        return JSON.parse(decoded) as JwtPayload;
    } catch (e) {
        console.error('Failed to decode JWT payload:', e);
        return null;
    }
}

/**
 * Extract user information from JWT token payload for display purposes
 */
export function getUserFromToken(token: string) {
    const payload = decodeJwtPayload(token);
    if (!payload) return null;
    return {
        id: payload.sub,
        name: payload.name,
        email: payload.email || undefined,
        avatar: payload.avatarUrl || undefined,
        provider: payload.provider || undefined
    };
}
