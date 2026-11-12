// The address pin, shared by the map marker (MapView) and the "Back to your system" button (InfoPanel),
// so both are the exact same drawing. Blue stands out against the red -> green score colors.

export const PIN_COLOR = "#2563eb";
export const PIN_VIEWBOX = "0 0 30 42";
export const PIN_PATH = "M15 1C7.3 1 1 7.2 1 14.9 1 25.3 15 41 15 41s14-15.7 14-26.1C29 7.2 22.7 1 15 1z";
export const PIN_DOT = { cx: 15, cy: 15, r: 5.5 };

// The same pin as an HTML string, for Leaflet's divIcon on the map. A white outline keeps it readable on any fill.
export function pinSvg(width: number, height: number): string {
  return (
    `<svg viewBox="${PIN_VIEWBOX}" width="${width}" height="${height}" aria-hidden="true">` +
    `<path d="${PIN_PATH}" fill="${PIN_COLOR}" stroke="#fff" stroke-width="2.5"/>` +
    `<circle cx="${PIN_DOT.cx}" cy="${PIN_DOT.cy}" r="${PIN_DOT.r}" fill="#fff"/>` +
    `</svg>`
  );
}
