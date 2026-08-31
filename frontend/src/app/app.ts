import { Component, computed, inject, signal } from '@angular/core';

import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import { filter } from 'rxjs';

import { Toast } from 'primeng/toast';

import { Auth } from './features/em/auth/auth';

interface NavItem {

  label: string;

  path: string;

}

@Component({

  selector: 'app-root',

  imports: [RouterOutlet, RouterLink, RouterLinkActive, Toast],

  templateUrl: './app.html',

  styleUrl: './app.css',

})

export class App {

  private readonly router = inject(Router);

  private readonly auth = inject(Auth);

  protected readonly sidebarOpen = signal(true);

  protected readonly authPage = signal(false);

  protected readonly currentUser = this.auth.currentUser;

  protected readonly navItems = computed<NavItem[]>(() => {

    const user = this.currentUser();

    if (!user) {

      return [];

    }

    if (user.role === 'CONSULTANT') {

      return [

        {

          label: 'My Engagements',

          path: '/my-engagements'

        }

      ];

    }

    if (user.role === 'ENGAGEMENT_MANAGER') {

      return [

        {

          label: 'Engagements',

          path: '/em/engagements'

        },

        {

          label: 'Clients',

          path: '/em/clients'

        },

        {

          label: 'Consultants',

          path: '/em/consultants'

        }

      ];

    }

    if (user.role === 'ADMIN') {

      return [

        {

          label: 'Admin Dashboard',

          path: '/admin'

        },

        {

          label: 'Notification Logs',

          path: '/admin/notifications'

        }

      ];

    }

    return [];

  });

  constructor() {

    this.updateLayout(this.router.url);

    this.router.events

      .pipe(

        filter(

          (event): event is NavigationEnd =>
            event instanceof NavigationEnd

        )

      )

      .subscribe(event => {

        this.updateLayout(event.urlAfterRedirects);

      });

  }

  protected toggleSidebar(): void {

    this.sidebarOpen.set(!this.sidebarOpen());

  }

  private updateLayout(url: string): void {

    this.authPage.set(

      url.startsWith('/login') ||
      url.startsWith('/register')

    );

  }

  logout(): void {

    this.auth.logout();

    this.router.navigate(['/login']);

  }

  getUserInitials(): string {

    const user = this.currentUser();

    if (!user) {

      return '';

    }

    return (

      user.firstName.charAt(0) +
      user.lastName.charAt(0)

    ).toUpperCase();

  }

  getRoleDisplayName(): string {

    const user = this.currentUser();

    if (!user) {

      return '';

    }

    switch (user.role) {

      case 'ENGAGEMENT_MANAGER':

        return 'Engagement Manager';

      case 'CONSULTANT':

        return 'Consultant';

      case 'ADMIN':

        return 'Administrator';

      default:

        return user.role;

    }

  }

}