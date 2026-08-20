import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { EngagementDetail } from './engagement-detail';

describe('EngagementDetail', () => {
  let component: EngagementDetail;
  let fixture: ComponentFixture<EngagementDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EngagementDetail],
      providers: [MessageService],
    }).compileComponents();

    fixture = TestBed.createComponent(EngagementDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
