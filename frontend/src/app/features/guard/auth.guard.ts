import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { Auth } from '../em/auth/auth';

export const authGuard: CanActivateFn = () => {

  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }else{
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    return router.createUrlTree(['/login']);
  }
};