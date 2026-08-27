import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { forkJoin } from 'rxjs';

import { Admin } from '../admin';

import {
  LoginAttempt,
  LoginMetrics
} from '../../../../types';

@Component({

  selector: 'app-admin-dashboard',

  imports: [
    CommonModule
  ],

  templateUrl: './admin-dashboard.html',

  styleUrl: './admin-dashboard.css'

})
export class AdminDashboard implements OnInit {

  private readonly admin = inject(Admin);

  protected readonly metrics =
    signal<LoginMetrics | null>(null);

  protected readonly loginAttempts =
    signal<LoginAttempt[]>([]);

  protected readonly loading =
    signal(true);

  protected readonly errorMessage =
    signal('');

  ngOnInit(): void {

    this.loadDashboard();

  }

  private loadDashboard(): void {

    this.loading.set(true);

    this.errorMessage.set('');

    forkJoin({

      metrics:
        this.admin.getLoginMetrics(),

      attempts:
        this.admin.getLoginAttempts()

    }).subscribe({

      next: result => {

        this.metrics.set(
          result.metrics
        );

        this.loginAttempts.set(
          result.attempts
        );

        this.loading.set(false);

      },

      error: error => {

        console.error(
          'Admin dashboard load failed:',
          error
        );

        this.errorMessage.set(
          'Unable to load admin dashboard.'
        );

        this.loading.set(false);

      }

    });

  }

}