import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  provideHttpClient,
  withFetch,
  withInterceptors
} from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { MessageService } from 'primeng/api';
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

import { routes } from './app.routes';
import { authInterceptor } from './features/em/auth/auth.interceptor';

const HoneyHive = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fef9e7',
      100: '#fdf0c3',
      200: '#fbe28c',
      300: '#f9d24f',
      400: '#f5b942',
      500: '#e8a422',
      600: '#c8871a',
      700: '#a06b16',
      800: '#7a5212',
      900: '#573b0d',
      950: '#3a2708',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#efe9df',
          100: '#d8cdb9',
          200: '#b6a181',
          300: '#8c7a5f',
          400: '#5c4f3d',
          500: '#453a2d',
          600: '#372e24',
          700: '#2b241d',
          800: '#211c16',
          900: '#1a1611',
          950: '#12100c',
        },
        primary: {
          color: '#f5b942',
          contrastColor: '#1a1611',
          hoverColor: '#f9d24f',
          activeColor: '#e8a422',
        },
        highlight: {
          background: 'rgba(245, 185, 66, 0.16)',
          focusBackground: 'rgba(245, 185, 66, 0.24)',
          color: '#f5b942',
          focusColor: '#f5b942',
        },
      },
    },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor])
    ),

    provideAnimationsAsync(),

    MessageService,

    providePrimeNG({
      theme: {
        preset: HoneyHive,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
    }),
  ]
};