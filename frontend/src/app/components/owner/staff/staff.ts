import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff.html',
  styleUrls: ['./staff.scss']
})
export class AdminStaff {
  showModal = false;
  modalType: 'add' | 'getById' | 'delete' = 'add';

  staffForm = {
    name: '',
    address: '',
    salary: 0,
    age: 0,
    occupation: '',
    email: ''
  };

  staffId: number = 0;
  staffList: any[] = [];
  staffDetails: any = null;

  readonly baseUrl = `${(window as any).__env.apiBaseUrl}/staff`;

  constructor(private http: HttpClient, private cdRef: ChangeDetectorRef, private authService: AuthService) {}

  openModal(type: typeof this.modalType) {
    this.modalType = type;
    this.showModal = true;
    this.cdRef.detectChanges();
  }

  closeModal() {
    this.showModal = false;
    this.staffForm = {
      name: '',
      address: '',
      salary: 0,
      age: 0,
      occupation: '',
      email: ''
    };
    this.staffId = 0;
    this.cdRef.detectChanges();
  }

  getAuthHeaders() {
    return this.authService.getAuthHeaders();
  }

  onAddStaff() {
    this.http.post(`${this.baseUrl}/add`, this.staffForm, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Staff added successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Add staff failed:', err);
        alert('Failed to add staff.');
      }
    });
  }

  getAllStaff() {
    this.http.get(`${this.baseUrl}/allStaff`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.staffList = data;
        this.staffDetails = null;
      },
      error: (err) => {
        console.error('Fetch all staff failed:', err);
        alert('Failed to fetch staff.');
      }
    });
  }

  onGetById() {
    this.http.get(`${this.baseUrl}/${this.staffId}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.staffDetails = data;
        this.staffList = [];
        this.closeModal();
      },
      error: (err) => {
        console.error('Fetch staff by ID failed:', err);
        alert('Staff not found.');
      }
    });
  }

  onDeleteStaff() {
    this.http.delete(`${this.baseUrl}/${this.staffId}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Staff deleted successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Delete staff failed:', err);
        alert('Failed to delete staff.');
      }
    });
  }
}
