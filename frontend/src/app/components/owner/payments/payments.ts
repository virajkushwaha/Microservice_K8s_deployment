import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payments.html',
  styleUrls: ['./payments.scss']
})
export class AdminPayments {
  showModal = false;
  modalType: 'add' | 'search' = 'add';

  paymentForm = {
    reservationCode: '',
    amount: 0,
    paymentMethod: ''
  };

  searchCode: string = '';
  paymentList: any[] = [];
  searchResults: any[] = [];

  readonly baseUrl = `${(window as any).__env.apiBaseUrl}/payment`;

  constructor(private http: HttpClient, private cdRef: ChangeDetectorRef, private authService: AuthService) {}

  openModal(type: typeof this.modalType) {
    this.modalType = type;
    this.showModal = true;
    this.cdRef.detectChanges();
  }

  closeModal() {
    this.showModal = false;
    this.paymentForm = {
      reservationCode: '',
      amount: 0,
      paymentMethod: ''
    };
    this.searchCode = '';
    this.cdRef.detectChanges();
  }

  getAuthHeaders() {
    return this.authService.getAuthHeaders();
  }

  onAddPayment() {
    this.http.post(`${this.baseUrl}`, this.paymentForm, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Payment added successfully!');
        this.closeModal();
        this.getAllPayments();
      },
      error: (err) => {
        console.error('Add payment failed:', err);
        alert('Failed to add payment.');
      }
    });
  }

  getAllPayments() {
    this.http.get(`${this.baseUrl}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.paymentList = data;
        this.searchResults = [];
      },
      error: (err) => {
        console.error('Fetch payments failed:', err);
        alert('Failed to fetch payments.');
      }
    });
  }

  onSearchPayment() {
    this.http.get(`${this.baseUrl}/${this.searchCode}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.searchResults = Array.isArray(data) ? data : [data];
        this.paymentList = [];
        this.closeModal();
      },
      error: (err) => {
        console.error('Search payment failed:', err);
        alert('No payments found for this reservation.');
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}