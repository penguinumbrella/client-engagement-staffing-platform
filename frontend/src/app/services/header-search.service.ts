import { Injectable, signal } from '@angular/core';

/**
 * The header search box means two different things depending on role:
 * - EM: a global lookup across clients/engagements/consultants (handled by
 *   GlobalSearchService + the dropdown in app.html).
 * - Consultant: they have no roster to browse, only their own engagements
 *   (`/my-engagements`), so the same box instead acts as a live filter over
 *   that page. This service is just the shared channel for that query - the
 *   header writes to it, `MyEngagements` reads it.
 */
@Injectable({ providedIn: 'root' })
export class HeaderSearchService {
  readonly query = signal('');
}
