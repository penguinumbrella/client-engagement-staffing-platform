import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClientCard } from './client-card';
import { RelationshipStatus } from '../../../../types/client.types';

describe('ClientCard', () => {
  let component: ClientCard;
  let fixture: ComponentFixture<ClientCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientCard);
    fixture.componentRef.setInput('client', {
      id: 1,
      companyName: 'Acme Corporation',
      industry: 'Technology',
      primaryContactName: 'Jane Doe',
      primaryContactEmail: 'jane@acme.com',
      relationshipStatus: RelationshipStatus.ACTIVE,
    });
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
