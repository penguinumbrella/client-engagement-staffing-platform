import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { Auth } from './auth';
import { environment } from '../../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const auth = inject(Auth);
  const token = auth.getToken();

  const isApiRequest =
    req.url.startsWith(environment.apiGatewayUrl);

  const isPublicAuthEndpoint =
    req.url.endsWith('/api/auth/login') ||
    req.url.endsWith('/api/auth/register');

  if (!token || !isApiRequest || isPublicAuthEndpoint) {
    return next(req);
  }

  const authenticatedRequest = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authenticatedRequest);
};