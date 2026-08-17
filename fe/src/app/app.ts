import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { GlobalLoadingDialogComponent } from './shared/ui/global-loading-dialog/global-loading-dialog';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobalLoadingDialogComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
