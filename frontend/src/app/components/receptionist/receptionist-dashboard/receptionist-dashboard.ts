import { Component } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-receptionist-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './receptionist-dashboard.html',
  styleUrls: ['./receptionist-dashboard.scss']
})
export class ReceptionistDashboard {
  constructor(private router: Router, private authService: AuthService) {}

  navigateTo(path: string) {
    this.router.navigate([`/receptionist-dashboard/${path}`]);
  }

  hasActiveRoute(): boolean {
    const currentUrl = this.router.url;
    return currentUrl !== '/receptionist-dashboard' && currentUrl.includes('/receptionist-dashboard/');
  }

  logout(): void {
    this.authService.logout();
  }
}
