import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { Auth } from './auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const auth = inject(Auth);
  const token = auth.getToken();

  const isApiRequest =
    req.url.startsWith('http://localhost:8125');

  if (!token || !isApiRequest) {
    return next(req);
  }

  const authenticatedRequest = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authenticatedRequest);
};