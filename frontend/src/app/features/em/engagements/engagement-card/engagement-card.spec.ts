import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EngagementCard } from './engagement-card';

describe('EngagementCard', () => {
  let component: EngagementCard;
  let fixture: ComponentFixture<EngagementCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EngagementCard],
    }).compileComponents();

    fixture = TestBed.createComponent(EngagementCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
