import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterModule, Router, ActivatedRoute } from '@angular/router';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatBadgeModule} from '@angular/material/badge';
import {MatMenuModule} from '@angular/material/menu';
import {MatDividerModule} from '@angular/material/divider';
import { CartStore } from './cart/cart-store';
import { AuthService } from './service/auth-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatBadgeModule, MatMenuModule, MatDividerModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  cartStore = inject(CartStore);
  authService = inject(AuthService);
  router = inject(Router);
  activatedRoute = inject(ActivatedRoute);
  
  cartItemCount = computed(() => {
    const items = this.cartStore.cartItems();
    if (!Array.isArray(items)) {
      return 0;
    }
    return items.reduce((sum, item) => sum + item.quantity, 0);
  });

  isLoggedIn = this.authService.isAuthenticated;
  currentUser = this.authService.currentUser;

  /**
   * Get the share URL based on current route
   * - Product detail page: share product URL
   * - Other pages: share app URL
   */
  getShareUrl(): string {
    const currentUrl = this.router.url;
    let targetUrl = '/';
    
    // Check if we're on a product detail page
    if (currentUrl.includes('/product/detail/')) {
      targetUrl = currentUrl;
    }
    
    const user = this.currentUser();
    if (user && user.referralCode) {
      return `https://yaojiabuy.com/referral/${user.referralCode}?target=${encodeURIComponent(targetUrl)}`;
    }
    
    return `https://yaojiabuy.com${targetUrl === '/' ? '' : targetUrl}`;
  }

  /**
   * Get the share text based on current route
   */
  getShareText(): string {
    const currentUrl = this.router.url;
    
    if (currentUrl.includes('/product/detail/')) {
      return 'Check out this product on Yaojia Buy!';
    }
    
    return 'Join me on Yaojia Buy - the best online shopping platform!';
  }

  /**
   * Share to WeChat
   */
  shareToWeChat() {
    const shareUrl = this.getShareUrl();
    const shareText = this.getShareText();
    
    // WeChat share implementation
    // In production, this would use WeChat SDK
    // For now, we'll copy to clipboard as a fallback
    const fullText = `${shareText}\n${shareUrl}`;
    navigator.clipboard.writeText(fullText).then(() => {
      alert('Link copied to clipboard! You can now share it on WeChat.');
    });
  }

  /**
   * Share to TikTok
   */
  shareToTikTok() {
    const shareUrl = this.getShareUrl();
    
    // TikTok share implementation
    // Open a modal or dialog for TikTok sharing
    const tiktokShareUrl = `https://www.tiktok.com/share?url=${encodeURIComponent(shareUrl)}`;
    window.open(tiktokShareUrl, '_blank', 'width=600,height=400');
  }

  /**
   * Share using native share API (fallback for all platforms)
   */
  shareNative() {
    const shareUrl = this.getShareUrl();
    const shareText = this.getShareText();
    
    if (navigator.share) {
      navigator.share({
        title: 'Yaojia Buy',
        text: shareText,
        url: shareUrl,
      }).catch(err => console.log('Share failed:', err));
    } else {
      // Fallback: copy to clipboard
      navigator.clipboard.writeText(`${shareText}\n${shareUrl}`).then(() => {
        alert('Link copied to clipboard!');
      });
    }
  }
}
