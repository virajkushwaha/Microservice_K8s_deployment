import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rooms.html',
  styleUrls: ['./rooms.scss']
})
export class AdminRooms {
  showModal = false;
  modalType: 'add' | 'getByRoomNumber' | 'delete' = 'add';

  roomForm = {
    roomNumber: '',
    roomType: 'STANDARD',
    ratePerNight: 0,
    status: 'AVAILABLE',
    capacity: 1
  };

  roomNumber: string = '';
  roomId: number = 0;
  roomList: any[] = [];
  roomDetails: any = null;

  roomTypes = ['STANDARD', 'DELUXE', 'LUXURY', 'SUITE', 'FAMILY'];
  roomStatuses = ['AVAILABLE', 'OCCUPIED', 'MAINTENANCE'];

  readonly baseUrl = `${(window as any).__env.apiBaseUrl}/room`;

  constructor(private http: HttpClient, private cdRef: ChangeDetectorRef, private authService: AuthService) {}

  openModal(type: typeof this.modalType) {
    this.modalType = type;
    this.showModal = true;
    this.cdRef.detectChanges();
  }

  closeModal() {
    this.showModal = false;
    this.roomForm = {
      roomNumber: '',
      roomType: 'STANDARD',
      ratePerNight: 0,
      status: 'AVAILABLE',
      capacity: 1
    };
    this.roomNumber = '';
    this.roomId = 0;
    this.cdRef.detectChanges();
  }

  getAuthHeaders() {
    return this.authService.getAuthHeaders();
  }

  onAddRoom() {
    this.http.post(`${this.baseUrl}/add`, this.roomForm, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Room added successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Add room failed:', err);
        alert('Failed to add room.');
      }
    });
  }

  getAllRooms() {
    this.http.get(`${this.baseUrl}/allRooms`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.roomList = data;
        this.roomDetails = null;
      },
      error: (err) => {
        console.error('Fetch all rooms failed:', err);
        alert('Failed to fetch rooms.');
      }
    });
  }

  onGetRoomByNumber() {
    this.http.get(`${this.baseUrl}/room-number/${this.roomNumber}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (data: any) => {
        this.roomDetails = data;
        this.roomList = [];
        this.closeModal();
      },
      error: (err) => {
        console.error('Fetch room failed:', err);
        alert('Room not found.');
      }
    });
  }

  onDeleteRoom() {
    this.http.delete(`${this.baseUrl}/${this.roomId}`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: () => {
        alert('Room deleted successfully!');
        this.closeModal();
      },
      error: (err) => {
        console.error('Delete room failed:', err);
        alert('Failed to delete room.');
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'available':
        return 'available';
      case 'occupied':
        return 'occupied';
      case 'maintenance':
        return 'maintenance';
      default:
        return 'available';
    }
  }
}
