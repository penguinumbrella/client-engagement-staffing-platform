import { Component, computed, effect, inject, signal } from '@angular/core';
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
  icon: string;
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

  private static readonly DARK_MODE_STORAGE_KEY = 'theme-dark-mode';

  protected readonly sidebarOpen = signal(true);
  protected readonly authPage = signal(false);
  protected readonly darkMode = signal(localStorage.getItem(App.DARK_MODE_STORAGE_KEY) === 'true');

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
          path: '/my-engagements',
          icon: 'pi-briefcase'
        }
      ];
    }

    if (user.role === 'ENGAGEMENT_MANAGER') {
      return [
        {
          label: 'Engagements',
          path: '/em/engagements',
          icon: 'pi-sitemap'
        },
        {
          label: 'Timeline',
          path: '/em/timeline',
          icon: 'pi-calendar'
        },
        {
          label: 'Clients',
          path: '/em/clients',
          icon: 'pi-building'
        },
        {
          label: 'Consultants',
          path: '/em/consultants',
          icon: 'pi-users'
        }
      ];
    }

    return [];
  });

  constructor() {
    effect(() => {
      document.documentElement.classList.toggle('app-dark', this.darkMode());
      localStorage.setItem(App.DARK_MODE_STORAGE_KEY, String(this.darkMode()));
    });

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

  protected toggleDarkMode(): void {
    this.darkMode.set(!this.darkMode());
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

      default:
        return user.role;
    }
  }
}