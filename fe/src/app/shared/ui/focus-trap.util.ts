/**
 * Shared focus-trap helpers extracted from ModalDialogComponent so raw shell
 * overlays (quick-nav, help, logout, action-center, push prompt) get identical
 * trapping semantics (BUG-3/BUG-8 of the shortcut audit).
 */
const FOCUSABLE_SELECTOR = [
  'a[href]',
  'area[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[contenteditable="true"]',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

export function getFocusableElements(container: HTMLElement | null | undefined): HTMLElement[] {
  if (!container) return [];
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (element) => element.getAttribute('aria-hidden') !== 'true' && isRendered(element),
  );
}

/**
 * Real browsers take the fast layout path; the ancestor-walk fallback keeps
 * semantics correct in jsdom/tests and SSR where layout metrics are absent.
 */
function isRendered(element: HTMLElement): boolean {
  if (element.getClientRects().length > 0) return true;
  let node: HTMLElement | null = element;
  while (node) {
    if (node.hidden || node.style.display === 'none' || node.getAttribute('aria-hidden') === 'true') {
      return false;
    }
    node = node.parentElement;
  }
  return true;
}

/** Traps Tab inside `container`. Returns true when the event was consumed. */
export function trapFocusWithin(container: HTMLElement | null | undefined, event: KeyboardEvent): boolean {
  const dialog = container;
  if (!dialog) return false;

  const focusable = getFocusableElements(dialog);
  if (focusable.length === 0) {
    event.preventDefault();
    dialog.focus({ preventScroll: true });
    return true;
  }

  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  const active = document.activeElement;
  const activeInside = active instanceof Node && dialog.contains(active);

  if (event.shiftKey && (!activeInside || activeElementIs(active, first))) {
    event.preventDefault();
    last.focus({ preventScroll: true });
    return true;
  }

  if (!event.shiftKey && (!activeInside || activeElementIs(active, last))) {
    event.preventDefault();
    first.focus({ preventScroll: true });
    return true;
  }

  return false;
}

function activeElementIs(active: Element | null, element: HTMLElement): boolean {
  return active === element;
}
