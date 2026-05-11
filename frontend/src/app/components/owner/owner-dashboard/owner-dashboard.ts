import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-owner-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './owner-dashboard.html',
  styleUrls: ['./owner-dashboard.scss']
})
export class OwnerDashboard implements OnInit {
  dashboardStats = {
    totalRooms: 0,
    activeBookings: 0,
    staffMembers: 0,
    monthlyRevenue: 0
  };

  private readonly baseUrl = (window as any).__env.apiBaseUrl;

  constructor(private router: Router, private http: HttpClient, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadDashboardStats();
    }
  }

  navigateTo(path: string) {
    this.router.navigate([`/${path}`]);
  }

  hasActiveRoute(): boolean {
    const currentUrl = this.router.url;
    return currentUrl !== '/owner-dashboard' && currentUrl.includes('/owner-dashboard/');
  }

  private getAuthHeaders() {
    const token = isPlatformBrowser(this.platformId) ? localStorage.getItem('jwt') : null;
    return {
      Authorization: `Bearer ${token}`
    };
  }

  loadDashboardStats() {
    // Load total rooms
    this.http.get(`${this.baseUrl}/room/allRooms`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (rooms: any) => {
        this.dashboardStats.totalRooms = rooms.length;
      },
      error: (err) => console.error('Failed to load rooms:', err)
    });

    // Load active bookings
    this.http.get(`${this.baseUrl}/reservation/all`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (reservations: any) => {
        this.dashboardStats.activeBookings = reservations.filter((r: any) => 
          r.status === 'CONFIRMED' || r.status === 'CHECKED_IN'
        ).length;
      },
      error: (err) => console.error('Failed to load reservations:', err)
    });

    // Load staff members (users with MANAGER or RECEPTIONIST roles)
    this.http.get(`${this.baseUrl}/user/all`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (users: any) => {
        this.dashboardStats.staffMembers = users.filter((u: any) => 
          u.role === 'MANAGER' || u.role === 'RECEPTIONIST'
        ).length;
      },
      error: (err) => console.error('Failed to load users:', err)
    });

    // Load monthly revenue from payments
    this.http.get(`${this.baseUrl}/payment/all`, {
      headers: this.getAuthHeaders()
    }).subscribe({
      next: (payments: any) => {
        const currentMonth = new Date().getMonth();
        const currentYear = new Date().getFullYear();
        
        this.dashboardStats.monthlyRevenue = payments
          .filter((p: any) => {
            const paymentDate = new Date(p.paymentDate);
            return paymentDate.getMonth() === currentMonth && 
                   paymentDate.getFullYear() === currentYear;
          })
          .reduce((total: number, p: any) => total + p.amount, 0);
      },
      error: (err) => console.error('Failed to load payments:', err)
    });
  }

  formatRevenue(amount: number): string {
    if (amount >= 100000) {
      return (amount / 100000).toFixed(1) + 'L';
    } else if (amount >= 1000) {
      return (amount / 1000).toFixed(1) + 'K';
    }
    return amount.toString();
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.clear();
    }
    this.router.navigate(['/login']);
  }
}
