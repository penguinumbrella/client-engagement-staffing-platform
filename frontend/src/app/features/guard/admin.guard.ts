import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { Auth } from '../em/auth/auth';
import { UserRole } from '../../types';

export const adminGuard: CanActivateFn = () => {

  const auth = inject(Auth);
  const router = inject(Router);

  const user = auth.getUser();

  if (
    auth.isLoggedIn() &&
    user?.role === UserRole.ADMIN
  ) {
    return true;
  }

  return router.createUrlTree(['/login']);
};