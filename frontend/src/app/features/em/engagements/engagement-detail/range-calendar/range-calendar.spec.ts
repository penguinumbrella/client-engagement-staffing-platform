import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RangeCalendar } from './range-calendar';

describe('RangeCalendar', () => {
  let component: RangeCalendar;
  let fixture: ComponentFixture<RangeCalendar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RangeCalendar],
    }).compileComponents();

    fixture = TestBed.createComponent(RangeCalendar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
