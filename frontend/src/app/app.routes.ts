import { Routes } from '@angular/router';
import { HomePageComponent } from './components/home/home-page/home-page';
import {Contact} from './components/home/contact/contact';
import {RoomListComponent} from './components/home/rooms/rooms';
import {Eat} from './components/home/eat/eat';
import { Login } from './components/home/login/login';
import { Register } from './components/home/register/register';
import { OwnerDashboard } from './components/owner/owner-dashboard/owner-dashboard';
import { AdminCreateUser } from './components/owner/create-user/create-user';
import { AdminPayments } from './components/owner/payments/payments';
import { AdminReservations } from './components/owner/reservations/reservations';
import { AdminRooms } from './components/owner/rooms/rooms';
import { AdminStaff } from './components/owner/staff/staff';
import { ManagerDashboard } from './components/manager/manager-dashboard/manager-dashboard';
import { ReceptionistDashboard } from './components/receptionist/receptionist-dashboard/receptionist-dashboard';
import { Managercreateuser } from './components/manager/managercreateuser/managercreateuser';
import { Managerstaff } from './components/manager/managerstaff/managerstaff';
import { Managerrooms } from './components/manager/managerrooms/managerrooms';
import { Managerreservation } from './components/manager/managerreservation/managerreservation';
import { Managerpayments } from './components/manager/managerpayments/managerpayments';
import { Receptionistpayment } from './components/receptionist/receptionistpayment/receptionistpayment';
import { Receptionistreservation } from './components/receptionist/receptionistreservation/receptionistreservation';
import { Receptionistroom } from './components/receptionist/receptionistroom/receptionistroom';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  {path: 'contact', component:Contact},
  {path : 'rooms' , component:RoomListComponent},
  {path : 'eat' , component:Eat},
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  {
    path: 'owner-dashboard',
    component: OwnerDashboard,
    children: [
      { path: 'create-user', component: AdminCreateUser },
      { path: 'staff', component: AdminStaff },
      { path: 'rooms', component: AdminRooms },
      { path: 'reservations', component: AdminReservations },
      { path: 'payments', component: AdminPayments }
    ]
  },
  {
    path: 'manager-dashboard',
    component: ManagerDashboard,
   children: [
      { path: 'create-user', component: Managercreateuser },
      { path: 'staff', component: Managerstaff },
      { path: 'rooms', component: Managerrooms },
      { path: 'reservations', component: Managerreservation },
      { path: 'payments', component: Managerpayments }
    ]
  }
  ,
  { path: 'receptionist-dashboard', component: ReceptionistDashboard,
    children: [
      { path: 'rooms', component: Receptionistroom },
      { path: 'reservations', component: Receptionistreservation },
      { path: 'payments', component: Receptionistpayment }
    ] },
  { path: '**', redirectTo: '' }
];
