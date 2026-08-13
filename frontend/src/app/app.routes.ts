import { Routes } from '@angular/router';
import { Engagements } from './features/em/engagements/engagements';
import { Clients } from './features/em/clients/clients';
import { Consultants } from './features/em/consultants/consultants';
import { Login } from './features/em/auth/login/login';
import { Register } from './features/em/auth/register/register';

export const routes: Routes = [
  { path: 'em/engagements', component: Engagements },
  { path: 'em/clients', component: Clients },
  { path: 'em/consultants', component: Consultants },
  { path: 'login', component: Login},
  { path: 'register',component: Register}
];
