import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reservations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './receptionistreservation.html',
  styleUrls: ['./receptionistreservation.scss']
})
export class Receptionistreservation {
  showModal = false;
  modalType: 'create' | 'search' | 'cancel' | 'checkout' = 'create';

  reservationForm = {
    guestName: '',
    phoneNumber: '',
    emailId: '',
    roomType: '',
    checkIn: '',
    checkOut: '',
    numChildren: 0,
    numAdults: 0,
    totalAmount: 0
  };

  searchForm = {
    guestName: '',
    phoneNumber: ''
  };

  reservationId: number = 0;
  reservationCode: string = '';
  reservationList: any[] = [];
  searchResults: any[] = [];

  readonly baseUrl = `${(window as any).__env.apiBaseUrl}/reservations`;

  constructor(private http: HttpClient, private cdRef: ChangeDetectorRef, private authService: AuthService) {}

  openModal(type: typeof this.modalType) {
    this.modalType = type;
    this.showModal = true;
    this.cdRef.detectChanges();
  }

  closeModal() {
    this.showModal = false;
    this.reservationForm = {
      guestName: '',
      phoneNumber: '',
      emailId: '',
      roomType: '',
      checkIn: '',
      checkOut: '',
      numChildren: 0,
      numAdults: 0,
      totalAmount: 0
    };
    this.searchForm = {
      guestName: '',
      phoneNumber: ''
    };
    this.reservationId = 0;
    this.reservationCode = '';
    this.cdRef.detectChanges();
  }

  getAuthHeaders() {
    return this.authService.getAuthHeaders();
  }

  onCreateReservation() {
    this.initiateRazorpayPayment();
  }

  initiateRazorpayPayment() {
    const options = {
      key: 'rzp_test_your_key_here',
      amount: this.reservationForm.totalAmount * 100,
      currency: 'INR',
      name: 'Baratie Hotel',
      description: 'Hotel Reservation Payment',
      handler: (response: any) => {
        this.createReservationAfterPayment(response.razorpay_payment_id);
      },
      prefill: {
        name: this.reservationForm.guestName,
        email: this.reservationForm.emailId,
        contact: this.reservationForm.phoneNumber
      },
      theme: {
        color: '#10B981'
      }
    };

    const rzp = new (window as any).Razorpay(options);
    rzp.open();
  }

  createReservationAfterPayment(paymentId: string) {
    const reservationData = {
      ...this.reservationForm,
      paymentId: paymentId,
      paymentStatus: 'PAID'
    };

    this.http.post(`${this.baseUrl}/create`, reservationData, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Reservation created and payment successful!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Create reservation failed:', err);
        alert('Failed to create reservation.');
      }
    });
  }

  getAllReservations() {
    this.http.get(`${this.baseUrl}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.reservationList = data;
        this.searchResults = [];
      },
      error: (err) => {
        console.error('Fetch reservations failed:', err);
        alert('Failed to fetch reservations.');
      }
    });
  }

  onSearchReservation() {
    const { guestName, phoneNumber } = this.searchForm;
    this.http.get(`${this.baseUrl}/search?guestName=${guestName}&phoneNumber=${phoneNumber}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.searchResults = data;
        this.reservationList = [];
        this.closeModal();
      },
      error: (err) => {
        console.error('Search failed:', err);
        alert('No reservations found.');
      }
    });
  }

  onCancelReservation() {
    this.http.put(`${this.baseUrl}/cancel/${this.reservationId}`, {}, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Reservation cancelled successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Cancel failed:', err);
        alert('Failed to cancel reservation.');
      }
    });
  }

  onCheckoutReservation() {
    this.http.put(`${this.baseUrl}/checkout?reservationCode=${this.reservationCode}`, {}, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Checkout successful!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Checkout failed:', err);
        alert('Failed to checkout reservation.');
      }
    });
  }
}
