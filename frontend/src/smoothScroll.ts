// Gently scrolls the page so an element is in view. The browser's own smooth scroll has a fixed speed and
// starts abruptly; this one eases in and out over a longer time, so it reads as a calm pan, not a jump.

// Space left above the element when it's scrolled to the top of the screen
const TOP_MARGIN = 16;

// Slow at the start, faster in the middle, slow at the end
const easeInOutCubic = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2);

let cancelCurrent: (() => void) | null = null;

export function smoothScrollIntoView(element: HTMLElement): void {
  cancelCurrent?.();

  const rect = element.getBoundingClientRect();
  const viewport = window.innerHeight;

  // Like "nearest": do nothing if it's already fully visible; otherwise bring its top (or, when it's
  // below and fits on screen, its bottom) into view
  let delta = 0;
  if (rect.top < TOP_MARGIN) {
    delta = rect.top - TOP_MARGIN;
  } else if (rect.bottom > viewport) {
    delta = rect.height + TOP_MARGIN <= viewport ? rect.bottom - viewport + TOP_MARGIN : rect.top - TOP_MARGIN;
  }
  if (Math.abs(delta) < 1) return;

  const startY = window.scrollY;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduceMotion) {
    window.scrollTo(0, startY + delta);
    return;
  }

  // Longer distances get a bit more time: 700ms to 1300ms
  const duration = Math.min(1300, Math.max(700, 500 + Math.abs(delta) * 0.5));
  const startTime = performance.now();
  let frame = 0;

  const step = (now: number) => {
    const t = Math.min(1, (now - startTime) / duration);
    window.scrollTo(0, startY + delta * easeInOutCubic(t));
    if (t < 1) frame = requestAnimationFrame(step);
    else cancelCurrent = null;
  };

  // If the user scrolls or touches the page themselves, stop and let them take over
  const stop = () => {
    cancelAnimationFrame(frame);
    window.removeEventListener("wheel", stop);
    window.removeEventListener("touchstart", stop);
    window.removeEventListener("keydown", stop);
    cancelCurrent = null;
  };
  window.addEventListener("wheel", stop, { passive: true, once: true });
  window.addEventListener("touchstart", stop, { passive: true, once: true });
  window.addEventListener("keydown", stop, { once: true });
  cancelCurrent = stop;

  frame = requestAnimationFrame(step);
}
