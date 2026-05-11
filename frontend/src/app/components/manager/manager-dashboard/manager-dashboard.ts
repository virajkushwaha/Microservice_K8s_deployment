import { Component } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './manager-dashboard.html',
  styleUrls: ['./manager-dashboard.scss']
})
export class ManagerDashboard {
  constructor(private router: Router, private authService: AuthService) {}

  navigateTo(path: string) {
    this.router.navigate([`/manager-dashboard/${path}`]);
  }

  hasActiveRoute(): boolean {
    const currentUrl = this.router.url;
    return currentUrl !== '/manager-dashboard' && currentUrl.includes('/manager-dashboard/');
  }

  logout(): void {
    this.authService.logout();
  }
}


 