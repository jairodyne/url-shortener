import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule],
  template: `
    <main>
      <h1>Encurtador de URL</h1>

      <form (ngSubmit)="shorten()">
        <input
          type="text"
          name="url"
          [(ngModel)]="url"
          placeholder="Cole a URL original aqui (ex.: https://exemplo.com/…)"
          required
        />
        <input
          type="text"
          name="alias"
          [(ngModel)]="alias"
          placeholder="Alias opcional (ex.: meu-link)"
        />
        <button type="submit" [disabled]="loading">Encurtar</button>
      </form>

      @if (error) {
        <p class="error">{{ error }}</p>
      }

      @if (shortUrl) {
        <div class="result">
          <a [href]="shortUrl" target="_blank" rel="noopener">{{ shortUrl }}</a>
          <button type="button" (click)="copy()" [disabled]="!copied">
            {{ copied ? 'Copiado!' : 'Copiar' }}
          </button>
        </div>
      }
    </main>
  `,
  styles: [`
    main { font-family: system-ui, Arial, sans-serif; max-width: 640px; margin: 4rem auto; padding: 0 1rem; }
    h1 { margin-bottom: 1.5rem; }
    form { display: flex; flex-direction: column; gap: 0.75rem; }
    input { padding: 0.625rem; border: 1px solid #ccc; border-radius: 6px; font-size: 1rem; }
    button { padding: 0.625rem 1rem; border: none; border-radius: 6px; background: #1565c0; color: #fff; font-size: 1rem; cursor: pointer; }
    button:disabled { opacity: 0.6; cursor: default; }
    .error { color: #b71c1c; }
    .result { margin-top: 1.5rem; display: flex; gap: 0.75rem; align-items: center; }
    .result a { font-size: 1.125rem; word-break: break-all; }
  `],
})
export class AppComponent {
  private http = inject(HttpClient);

  url = '';
  alias = '';
  shortUrl = '';
  error = '';
  loading = false;
  copied = false;

  shorten(): void {
    if (!this.url.trim()) {
      return;
    }
    this.error = '';
    this.shortUrl = '';
    this.copied = false;
    this.loading = true;

    const body = {
      url: this.url.trim(),
      alias: this.alias.trim() || null,
    };

    this.http.post<{ shortUrl: string }>('api/urls', body).subscribe({
      next: (res) => (this.shortUrl = res.shortUrl),
      error: (err) => (this.error = err.error?.message ?? 'Falha inesperada ao encurtar a URL'),
    }).add(() => (this.loading = false));
  }

  copy(): void {
    navigator.clipboard.writeText(this.shortUrl).then(() => (this.copied = true));
  }
}