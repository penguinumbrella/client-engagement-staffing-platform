import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Engagements } from './engagements';

describe('Engagements', () => {
  let component: Engagements;
  let fixture: ComponentFixture<Engagements>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Engagements],
    }).compileComponents();

    fixture = TestBed.createComponent(Engagements);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
