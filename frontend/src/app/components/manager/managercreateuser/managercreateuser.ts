import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-create-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './managercreateuser.html',
  styleUrls: ['./managercreateuser.scss']
})
export class Managercreateuser {
  showModal = false;
  modalType: 'add' | 'delete' = 'add';

  formData: any = {
    username: '',
    email: '',
    password: '',
    role: 'RECEPTIONIST'
  };

  readonly baseUrl = (window as any).__env.apiBaseUrl;

  constructor(private http: HttpClient, private cdRef: ChangeDetectorRef, private authService: AuthService) {}

  openModal(type: typeof this.modalType) {
    this.modalType = type;
    this.showModal = true;
    this.cdRef.detectChanges();
  }

  closeModal() {
    this.showModal = false;
    this.formData = {
      username: '',
      email: '',
      password: '',
      role: 'RECEPTIONIST'
    };
    this.cdRef.detectChanges();
  }

  getAuthHeaders() {
    return this.authService.getAuthHeaders();
  }

  onAddUser() {
    this.http.post(`${this.baseUrl}/auth/register`, this.formData, {
      headers: this.getAuthHeaders(),
      responseType: 'text'
    }).subscribe({
      next: () => {
        alert('User added successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Add user failed:', err);
        alert('Failed to add user.');
      }
    });
  }

  onDeleteUser() {
    this.http.delete(`${this.baseUrl}/auth/delete/${this.formData.username}`, {
      headers: this.getAuthHeaders(),
      responseType: 'text'
    }).subscribe({
      next: () => {
        alert('User deleted successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Delete user failed:', err);
        alert('Failed to delete user.');
      }
    });
  }
}
