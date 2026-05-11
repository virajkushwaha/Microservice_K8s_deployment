// import { Component, ChangeDetectorRef } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { HttpClient } from '@angular/common/http';

// @Component({
//   selector: 'app-create-user',
//   standalone: true,
//   imports: [CommonModule, FormsModule],
//   templateUrl: './create-user.html',
//   styleUrls: ['./create-user.scss']
// })
// export class CreateUser {
//   showModal = false;
//   modalType: 'add' | 'updateUsername' | 'updatePassword' | 'delete' = 'add';

//   formData: any = {
//     username: '',
//     email: '',
//     password: '',
//     role: 'RECEPTIONIST',
//     newUsername: '',
//     newPassword: ''
//   };

//   readonly baseUrl = 'http://localhost:8080';

//   constructor(private http: HttpClient, private cdRef: ChangeDetectorRef) {}

//   openModal(type: typeof this.modalType) {
//     this.modalType = type;
//     this.showModal = true;
//     this.cdRef.detectChanges();
//   }

//   closeModal() {
//     this.showModal = false;
//     this.formData = {
//       username: '',
//       email: '',
//       password: '',
//       role: 'RECEPTIONIST',
//       newUsername: '',
//       newPassword: ''
//     };
//     this.cdRef.detectChanges();
//   }

//   getAuthHeaders() {
//     const token = localStorage.getItem('jwt');
//     return {
//       Authorization: `Bearer ${token}`
//     };
//   }

//   onAddUser() {
//     this.http.post(`${this.baseUrl}/auth/register`, this.formData, {
//       headers: this.getAuthHeaders(),
//       responseType: 'text'
//     }).subscribe({
//       next: (res) => {
//         alert('User added successfully!');
//         this.closeModal();
//       },
//       error: (err) => {
//         console.error('Add user failed:', err);
//         alert('Failed to add user.');
//       }
//     });
//   }

//   // onUpdateUsername() {
//   //   const payload = {
//   //     username: this.formData.newUsername,
//   //     email: this.formData.email,
//   //     password: this.formData.password,
//   //     role: this.formData.role
//   //   };

//   //   this.http.put(`${this.baseUrl}/auth/update-username/${this.formData.username}`, payload, {
//   //     headers: this.getAuthHeaders(),
//   //     responseType: 'text'
//   //   }).subscribe({
//   //     next: (res) => {
//   //       alert('Username updated successfully!');
//   //       this.closeModal();
//   //     },
//   //     error: (err) => {
//   //       console.error('Update username failed:', err);
//   //       alert('Failed to update username.');
//   //     }
//   //   });
//   // }

//   // onUpdatePassword() {
//   //   const payload = {
//   //     newPassword: this.formData.newPassword
//   //   };

//   //   this.http.put(`${this.baseUrl}/auth/update-password/${this.formData.username}`, payload, {
//   //     headers: this.getAuthHeaders(),
//   //     responseType: 'text'
//   //   }).subscribe({
//   //     next: (res) => {
//   //       alert('Password updated successfully!');
//   //       this.closeModal();
//   //     },
//   //     error: (err) => {
//   //       console.error('Update password failed:', err);
//   //       alert('Failed to update password.');
//   //     }
//   //   });
//   // }

//   onDeleteUser() {
//     this.http.delete(`${this.baseUrl}/auth/delete/${this.formData.username}`, {
//       headers: this.getAuthHeaders(),
//       responseType: 'text'
//     }).subscribe({
//       next: (res) => {
//         alert('User deleted successfully!');
//         this.closeModal();
//       },
//       error: (err) => {
//         console.error('Delete user failed:', err);
//         alert('Failed to delete user.');
//       }
//     });
//   }
// }


import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-create-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './create-user.html',
  styleUrls: ['./create-user.scss']
})
export class AdminCreateUser {
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
