import { Component, inject } from '@angular/core';
import { AuthService } from '../../service/auth-service';

@Component({
  selector: 'app-login-component',
  imports: [],
  templateUrl: './login-component.html',
  styleUrl: './login-component.scss',
})
export class LoginComponent {

  private authService = inject(AuthService)

  loading = false;

  loginWith(provider: 'google' | 'tiktok' | 'wechat'): void {
    this.loading = true;
    this.authService.loginWithProvider(provider);
  }
}
