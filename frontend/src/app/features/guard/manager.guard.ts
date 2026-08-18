import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router
} from '@angular/router';

import { Auth } from '../em/auth/auth';
import { UserRole } from '../../types';

export const managerGuard: CanActivateFn = () => {

  const auth = inject(Auth);
  const router = inject(Router);

  const user = auth.getUser();

  if (
    user?.role === UserRole.ENGAGEMENT_MANAGER
  ) {
    return true;
  }

  return router.createUrlTree([
    '/my-engagements'
  ]);
};