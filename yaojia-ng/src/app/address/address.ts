import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../service/auth-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-address',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './address.html',
  styleUrl: './address.scss'
})
export class AddressComponent implements OnInit {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  isLoading = signal(false);
  isSaving = signal(false);
  hasSaved = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  addressForm = this.fb.group({
    customerName: ['', [Validators.required, Validators.minLength(2)]],
    phone: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-\(\)]{10,}$/)]],
    address: ['', [Validators.required, Validators.minLength(5)]],
    city: ['', [Validators.required]],
    postalCode: ['', [Validators.required]]
  });

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    this.isLoading.set(true);
    this.authService.getUserProfile().subscribe({
      next: (user) => {
        if (user) {
          this.addressForm.patchValue({
            customerName: user.customerName || user.name || '',
            phone: user.phone || '',
            address: user.address || '',
            city: user.city || '',
            postalCode: user.postalCode || ''
          });
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load user profile', err);
        this.errorMessage.set('Failed to load address information.');
        this.isLoading.set(false);
      }
    });
  }

  onSubmit() {
    if (this.addressForm.invalid) {
      return;
    }

    this.isSaving.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    const formData = this.addressForm.value;

    this.authService.updateAddress(formData).subscribe({
      next: (updatedUser) => {
        this.isSaving.set(false);
        this.hasSaved.set(true);
        this.addressForm.markAsPristine();
        this.successMessage.set('Address updated successfully!');
        // Clear success message after 3 seconds
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (err) => {
        console.error('Failed to update address', err);
        this.isSaving.set(false);
        this.errorMessage.set('Failed to update address. Please try again.');
      }
    });
  }

  onCancel() {
    this.router.navigate(['/']);
  }
}
