import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, of, switchMap } from 'rxjs';
import { Toast } from 'primeng/toast';
import { Auth } from './features/em/auth/auth';
import { NotificationsModal } from './shared/notifications-modal/notifications-modal';
import { NotificationService } from './services/notification.service';
import { GlobalSearchService, GroupedSearchResults, SearchResult } from './services/global-search.service';
import { HeaderSearchService } from './services/header-search.service';

const SEARCH_MIN_LENGTH = 2;

const EMPTY_RESULTS: GroupedSearchResults = { clients: [], engagements: [], consultants: [] };

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Toast, NotificationsModal],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {

  private readonly router = inject(Router);
  private readonly mainContent = viewChild<ElementRef<HTMLElement>>('mainContent');
  private readonly auth = inject(Auth);
  private readonly notificationService = inject(NotificationService);
  private readonly globalSearchService = inject(GlobalSearchService);
  private readonly headerSearchService = inject(HeaderSearchService);

  private static readonly DARK_MODE_STORAGE_KEY = 'theme-dark-mode';

  protected readonly sidebarOpen = signal(true);
  protected readonly authPage = signal(false);
  protected readonly darkMode = signal(localStorage.getItem(App.DARK_MODE_STORAGE_KEY) === 'true');
  protected readonly notificationsOpen = signal(false);
  protected readonly unreadNotificationCount = signal(0);

  protected readonly searchQuery = signal('');
  protected readonly searchOpen = signal(false);
  protected readonly searchLoading = signal(false);
  protected readonly searchResults = signal<GroupedSearchResults>(EMPTY_RESULTS);

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

  private readonly scrollIdleTimers = new WeakMap<EventTarget, ReturnType<typeof setTimeout>>();

  constructor() {
    effect(() => {
      document.documentElement.classList.toggle('app-dark', this.darkMode());
      localStorage.setItem(App.DARK_MODE_STORAGE_KEY, String(this.darkMode()));
    });

    document.addEventListener('scroll', (event) => this.onScroll(event), { capture: true, passive: true });

    effect(() => {
      const user = this.currentUser();

      if (!user) {
        this.unreadNotificationCount.set(0);
        return;
      }

      this.notificationService.getForRecipient(user.id).subscribe(notifications => {
        this.unreadNotificationCount.set(notifications.filter(n => !n.read).length);
      });
    });

    toObservable(this.searchQuery)
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((query) => {
          const trimmed = query.trim();
          const role = this.currentUser()?.role;

          // Consultants have no client/consultant roster to browse - their
          // search box just filters their own engagements list instead of
          // querying the backend, so there's nothing to fetch here.
          if (!role || role === 'CONSULTANT' || trimmed.length < SEARCH_MIN_LENGTH) {
            this.searchLoading.set(false);
            return of(EMPTY_RESULTS);
          }

          this.searchLoading.set(true);
          return this.globalSearchService.search(trimmed);
        }),
      )
      .subscribe((results) => {
        this.searchResults.set(results);
        this.searchLoading.set(false);
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
        this.mainContent()?.nativeElement.scrollTo({ top: 0 });
      });
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.set(!this.sidebarOpen());
  }

  protected toggleDarkMode(): void {
    this.darkMode.set(!this.darkMode());
  }

  protected toggleNotifications(): void {
    this.notificationsOpen.set(!this.notificationsOpen());
  }

  private onScroll(event: Event): void {
    const target = event.target;
    if (!(target instanceof Element)) return;

    target.classList.add('is-scrolling');

    clearTimeout(this.scrollIdleTimers.get(target));
    this.scrollIdleTimers.set(
      target,
      setTimeout(() => target.classList.remove('is-scrolling'), 600),
    );
  }

  protected onSearchInput(query: string): void {
    this.searchQuery.set(query);

    if (this.currentUser()?.role === 'CONSULTANT') {
      // Live filter, applied immediately - no debounce needed for a local list.
      this.headerSearchService.query.set(query.trim());
      this.searchOpen.set(false);
      return;
    }

    this.searchOpen.set(query.trim().length >= SEARCH_MIN_LENGTH);
  }

  protected onSearchFocus(): void {
    if (this.currentUser()?.role === 'CONSULTANT') {
      return;
    }

    if (this.searchQuery().trim().length >= SEARCH_MIN_LENGTH) {
      this.searchOpen.set(true);
    }
  }

  protected closeSearch(): void {
    this.searchOpen.set(false);
  }

  protected hasAnySearchResults(): boolean {
    const results = this.searchResults();
    return results.clients.length > 0 || results.engagements.length > 0 || results.consultants.length > 0;
  }

  protected selectSearchResult(result: SearchResult): void {
    this.closeSearch();
    this.searchQuery.set('');

    const routesByType: Record<SearchResult['type'], string> = {
      client: '/em/clients',
      engagement: '/em/engagements',
      consultant: '/em/consultants',
    };

    this.router.navigate([routesByType[result.type]], { queryParams: { openId: result.id } });
  }

  private updateLayout(url: string): void {
    this.authPage.set(
      url.startsWith('/login') ||
      url.startsWith('/register') ||
      url.startsWith('/auth/callback') ||
      url.startsWith('/onboarding')
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