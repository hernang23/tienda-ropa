import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => cleanup());

// Recharts exige canvas en jsdom; se neutraliza para pruebas de renderizado.
class CanvasRenderingContextStub {
  [key: string]: unknown;
  constructor() { return new Proxy(this, { get: () => () => undefined }) as unknown as CanvasRenderingContextStub; }
}

HTMLCanvasElement.prototype.getContext = (() => new CanvasRenderingContextStub()) as never;
HTMLCanvasElement.prototype.toDataURL = (() => '') as never;

if (!window.matchMedia) {
  window.matchMedia = ((query: string) => ({
    matches: false, media: query, onchange: null,
    addListener: () => undefined, removeListener: () => undefined,
    addEventListener: () => undefined, removeEventListener: () => undefined,
    dispatchEvent: () => false,
  })) as never;
}

const realConsoleError = console.error.bind(console);
console.error = (...args: unknown[]) => {
  const msg = String(args[0] ?? '');
  // jsdom reporta "Could not parse CSS" por Tailwind v4 y ruido de recharts; se ignora.
  if (msg.includes('Could not parse CSS')) return;
  realConsoleError(...args);
};
