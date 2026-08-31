import { Routes } from '@angular/router';
import { Engagements } from './features/em/engagements/engagements';
import { Timeline } from './features/em/engagements/timeline/timeline';
import { Clients } from './features/em/clients/clients';
import { Consultants } from './features/em/consultants/consultants';
import { Login } from './features/em/auth/login/login';
import { Register } from './features/em/auth/register/register';
import { MyEngagements } from './features/my-engagements/my-engagements';
import { managerGuard } from './features/guard/manager.guard';
import { authGuard } from './features/guard/auth.guard';
import { adminGuard } from './features/guard/admin.guard';
import {AdminDashboard} from './features/em/admin/admin-dashboard/admin-dashboard';
import { NotificationLogs } from './features/em/admin/notification-logs/notification-logs';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full'},

  { path: 'em/engagements', component: Engagements, canActivate: [authGuard]},
  { path: 'em/timeline', component: Timeline, canActivate: [authGuard]},
  { path: 'em/clients', component: Clients, canActivate: [authGuard]},
  { path: 'em/consultants', component: Consultants, canActivate: [authGuard, managerGuard]},
  { path: 'login', component: Login},
  { path: 'register',component: Register},
  { path: 'my-engagements', component: MyEngagements, canActivate: [authGuard]},
  { path: 'admin', component: AdminDashboard, canActivate: [authGuard, adminGuard]},
  { path: 'admin/notifications', component: NotificationLogs, canActivate: [authGuard,adminGuard]}
];
