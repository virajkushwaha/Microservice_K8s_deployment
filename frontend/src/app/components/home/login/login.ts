import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { jwtDecode } from 'jwt-decode';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class Login {
  username = '';
  password = '';
  readonly baseUrl = (window as any).__env.apiBaseUrl;

  constructor(private http: HttpClient, private router: Router, private authService: AuthService) {}


  onLogin() {
    const payload = {
      username: this.username,
      password: this.password
    };

    this.http.post(`${this.baseUrl}/auth/login`, payload).subscribe({
      next: (response: any) => {
        const token = response.token;
        if (!token) {
          alert('Login failed: No token received');
          return;
        }

        this.authService.setToken(token);

        try {
          const decoded: any = jwtDecode(token);
          const role = decoded.role;

          switch (role) {
            case 'OWNER':
              this.router.navigate(['/owner-dashboard']);
              break;
            case 'MANAGER':
              this.router.navigate(['/manager-dashboard']);
              break;
            case 'RECEPTIONIST':
              this.router.navigate(['/receptionist-dashboard']);
              break;
            default:
              this.router.navigate(['/']);
          }
        } catch (err) {
          console.error('Token decoding failed:', err);
          alert('Invalid token received. Please contact support.');
        }
      },
      error: (err) => {
        console.error('Login failed:', err);
        alert('Login failed. Please check your credentials.');
      }
    });
  }
}
