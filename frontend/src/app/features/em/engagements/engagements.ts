import { Component } from '@angular/core';
import { KanbanBoard } from './kanban-board/kanban-board';

@Component({
  selector: 'app-engagements',
  imports: [KanbanBoard],
  templateUrl: './engagements.html',
  styleUrl: './engagements.css',
})
export class Engagements {}
