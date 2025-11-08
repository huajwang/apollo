import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../service/auth-service';

@Component({
  selector: 'app-auth-callback',
  imports: [],
  templateUrl: './auth-callback-component.html',
  styleUrl: './auth-callback-component.scss',
})
export class AuthCallbackComponent implements OnInit {

  private route = inject(ActivatedRoute)
  private authService = inject(AuthService)

  loading = true;
  error: string | null = null;

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];
    const error = this.route.snapshot.queryParams['error'];

    if (error) {
      this.loading = false;
      this.error = 'Authentication failed. Please retry again.';
      return;
    }

    // Received token from backend
    if (token) {
      // Save token in localStorage and request the backend for 
      // the current user by the token
      this.authService.handleAuthCallback(token).subscribe({
        next: () => {
          this.loading = false;
          // Navigation handled by authService
        },
        error: (err) => {
          this.loading = false;
          this.error = 'Failed to complete authentication.';
          console.error('Auth callback error:', err);
        }
      })
    }
  }

}
