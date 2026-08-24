import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { Consultants } from './consultants';

describe('Consultants', () => {
  let component: Consultants;
  let fixture: ComponentFixture<Consultants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Consultants],
      providers: [MessageService],
    }).compileComponents();

    fixture = TestBed.createComponent(Consultants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
