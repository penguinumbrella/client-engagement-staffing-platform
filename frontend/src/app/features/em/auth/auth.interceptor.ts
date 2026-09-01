import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';

import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
  catchError,
  throwError
} from 'rxjs';

import { Auth } from './auth';

import { environment } from '../../../../environments/environment';


let sessionExpiredHandled = false;


export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const auth = inject(Auth);

  const router = inject(Router);

  const messageService = inject(MessageService);

  const token = auth.getToken();

  const isApiRequest =
    req.url.startsWith(environment.apiGatewayUrl);

  const isPublicAuthEndpoint =
    req.url.endsWith('/api/auth/login') ||
    req.url.endsWith('/api/auth/register');


  if (!token || !isApiRequest || isPublicAuthEndpoint) {

    return next(req);

  }


  /*
   * Catch an expired token before even sending
   * the request to the backend.
   */
  if (isTokenExpired(token)) {

    handleExpiredSession(
      auth,
      router,
      messageService
    );

    return throwError(() =>
      new Error('JWT token has expired')
    );

  }


  const authenticatedRequest = req.clone({

    setHeaders: {

      Authorization: `Bearer ${token}`

    }

  });


  return next(authenticatedRequest).pipe(

    catchError((error: HttpErrorResponse) => {

      /*
       * Safety net:
       *
       * The token may expire between the frontend
       * checking it and the backend processing it.
       */
      if (
        error.status === 401 &&
        isTokenExpired(token)
      ) {

        handleExpiredSession(
          auth,
          router,
          messageService
        );

      }

      return throwError(() => error);

    })

  );

};


function handleExpiredSession(
  auth: Auth,
  router: Router,
  messageService: MessageService
): void {

  if (sessionExpiredHandled) {

    return;

  }


  sessionExpiredHandled = true;


  auth.logout();


  router.navigate(['/login']).then(() => {

    messageService.add({

      severity: 'warn',

      summary: 'Session Expired',

      detail: 'Your session has expired. Please log in again.',

      life: 5000

    });


    setTimeout(() => {

      sessionExpiredHandled = false;

    }, 1000);

  });

}


function isTokenExpired(token: string): boolean {

  try {

    const tokenParts = token.split('.');

    if (tokenParts.length !== 3) {

      return false;

    }


    let payload = tokenParts[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');


    while (payload.length % 4) {

      payload += '=';

    }


    const decodedPayload = JSON.parse(
      atob(payload)
    );


    if (!decodedPayload.exp) {

      return false;

    }


    return Date.now() >= decodedPayload.exp * 1000;

  } catch {

    return false;

  }

}