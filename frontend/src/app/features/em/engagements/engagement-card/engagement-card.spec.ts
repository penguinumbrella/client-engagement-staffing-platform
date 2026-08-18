import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EngagementCard } from './engagement-card';
import { EngagementCard as EngagementCardModel } from '../engagement-detail/engagement.model';

const mockEngagement = {
  id: 1,
  engagementName: 'Test Engagement',
  engagementType: 'Advisory',
  client: { companyName: 'Acme Corp', initials: 'AC', color: '#000000' },
  consultants: [],
} as unknown as EngagementCardModel;

describe('EngagementCard', () => {
  let component: EngagementCard;
  let fixture: ComponentFixture<EngagementCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EngagementCard],
    }).compileComponents();

    fixture = TestBed.createComponent(EngagementCard);
    fixture.componentRef.setInput('engagement', mockEngagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
