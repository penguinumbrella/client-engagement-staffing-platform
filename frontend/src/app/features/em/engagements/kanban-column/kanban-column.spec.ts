import { ComponentFixture, TestBed } from '@angular/core/testing';

import { KanbanColumn } from './kanban-column';
import { EngagementColumn } from '../engagement-detail/engagement.model';

const mockColumn = {
  status: 'In Progress',
  title: 'In Progress',
  engagements: [],
} as unknown as EngagementColumn;

describe('KanbanColumn', () => {
  let component: KanbanColumn;
  let fixture: ComponentFixture<KanbanColumn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KanbanColumn],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanColumn);
    fixture.componentRef.setInput('column', mockColumn);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
