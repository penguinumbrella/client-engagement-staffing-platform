import { Routes } from '@angular/router';
import { Engagements } from './features/em/engagements/engagements';
import { Clients } from './features/em/clients/clients';
import { Consultants } from './features/em/consultants/consultants';

export const routes: Routes = [
  { path: 'em/engagements', component: Engagements },
  { path: 'em/clients', component: Clients },
  { path: 'em/consultants', component: Consultants }
];
