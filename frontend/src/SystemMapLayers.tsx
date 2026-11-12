import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { GeoJSON, Marker, useMap, useMapEvents } from "react-leaflet";
import { divIcon, tooltip, type LatLngExpression, type Marker as LeafletMarker, type Point } from "leaflet";
import type { Feature, FeatureCollection, Geometry } from "geojson";

// These fields come from /api/systems/map. Both polygons and badges use the same pwsid.
type SystemProperties = {
  pwsid: string;
  name: string;
  waterScore: number | null;
  labelLat: number | null;
  labelLng: number | null;
};
type SystemFeature = Feature<Geometry, SystemProperties>;
export type SystemMap = FeatureCollection<Geometry, SystemProperties>;

type SelectionProps = {
  selectedSystemId: string;
  onSelectSystem: (systemId: string) => void;
};
type HoverProps = {
  showName: (name: string, position: LatLngExpression) => void;
  hideName: () => void;
};
// The & combines shared hover inputs with the inputs needed for one system's shapes/badge.
type SystemRegionProps = HoverProps & {
  feature: SystemFeature;
  selected: boolean;
  badgeSize: number;
  showBadge: boolean;
  onSelectSystem: (systemId: string) => void;
};

const palette = ["#e63946", "#2a9d8f", "#e9c46a", "#457b9d", "#f4a261", "#8338ec", "#06d6a0"];

function regionColor(name: string) {
  // A stable name-to-color mapping keeps badges and regions in the same palette.
  let hash = 0;
  for (const character of name) hash = (hash * 31 + character.charCodeAt(0)) >>> 0;
  return palette[hash % palette.length];
}

// This type guard checks coordinates and tells TypeScript they are numbers inside the guarded block.
function hasLabel(system: SystemProperties): system is SystemProperties & { labelLat: number; labelLng: number } {
  return system.labelLat != null && system.labelLng != null
    && Number.isFinite(system.labelLat) && Number.isFinite(system.labelLng);
}

// A small component keeps Leaflet's marker/DOM details out of MapView and App.
function SystemRegion({
  feature, selected, badgeSize, showBadge, onSelectSystem, showName, hideName,
}: SystemRegionProps) {
  const system = feature.properties;
  const color = regionColor(system.name);
  const markerRef = useRef<LeafletMarker>(null);

  // Leaflet displays this native button. textContent safely inserts API values as text.
  // Keep the icon stable on selection so keyboard focus isn't lost when its appearance changes.
  const icon = useMemo(() => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "system-score-button";
    button.textContent = String(system.waterScore ?? "—");
    const scoreDescription = system.waterScore == null ? "score unavailable" : `water score ${system.waterScore} out of 100`;
    button.setAttribute("aria-label", `${system.name}, ${scoreDescription}`);
    button.style.setProperty("--region-color", color);
    button.addEventListener("focus", () => {
      if (hasLabel(system)) showName(system.name, [system.labelLat, system.labelLng]);
    });
    button.addEventListener("blur", hideName);
    button.addEventListener("keydown", (event) => {
      if (event.key === "Escape") hideName();
    });
    return divIcon({ className: "system-score-marker", html: button, iconSize: [0, 0], iconAnchor: [0, 0] });
  }, [system, color, showName, hideName]);

  // Update the existing button rather than replacing it. App remains the only selection owner.
  useEffect(() => {
    const button = markerRef.current?.getElement()?.querySelector("button");
    if (!button) return;
    button.classList.toggle("is-selected", selected);
    button.setAttribute("aria-pressed", String(selected));
    button.style.setProperty("--badge-size", `${badgeSize}px`);
  }, [selected, badgeSize, showBadge]);

  return (
    <>
      <GeoJSON
        data={feature}
        style={{
          className: `water-system-boundary system-${system.pwsid}`,
          color,
          weight: selected ? 4 : 2,
          fillColor: color,
          fillOpacity: selected ? 0.55 : 0.4,
        }}
        eventHandlers={{
          click: () => onSelectSystem(system.pwsid),
          mouseover: (event) => showName(system.name, event.latlng),
          mousemove: (event) => showName(system.name, event.latlng),
          mouseout: hideName,
        }}
      />
      {showBadge && hasLabel(system) && (
        <Marker
          ref={markerRef}
          position={[system.labelLat, system.labelLng]}
          icon={icon}
          // The inner button handles keyboard input; avoid a second tab stop on its wrapper.
          keyboard={false}
          zIndexOffset={selected ? 1000 : 0}
          eventHandlers={{
            click: () => onSelectSystem(system.pwsid),
            mouseover: () => showName(system.name, [system.labelLat, system.labelLng]),
            mouseout: hideName,
          }}
        />
      )}
    </>
  );
}

// This component must sit inside MapContainer to access Leaflet's map and zoom events.
export default function SystemMapLayers({ data, selectedSystemId, onSelectSystem }: { data: SystemMap } & SelectionProps) {
  const map = useMap();
  const [view, setView] = useState(() => ({ zoom: map.getZoom(), bounds: map.getBounds() }));
  const tooltipText = useMemo(() => document.createElement("span"), []);
  const [nameTooltip] = useState(() => tooltip({
    // We open/close it ourselves; this prevents touch-capable browsers closing it mid-click.
    permanent: true,
    direction: "top", offset: [0, -26], opacity: 1, className: "system-name-tooltip",
  }).setContent(tooltipText));

  // One tooltip instance prevents duplicate polygon/icon labels. Hover never selects a system.
  const showName = useCallback((name: string, position: LatLngExpression) => {
    if (tooltipText.textContent !== name) tooltipText.textContent = name;
    nameTooltip.setLatLng(position);
    if (!nameTooltip.isOpen()) nameTooltip.openOn(map);
  }, [map, nameTooltip, tooltipText]);
  const hideName = useCallback(() => { map.closeTooltip(nameTooltip); }, [map, nameTooltip]);
  const updateView = useCallback(() => {
    setView({ zoom: map.getZoom(), bounds: map.getBounds() });
  }, [map]);

  useMapEvents({
    // Leaflet also fires moveend after zooming. Recalculate only once movement finishes.
    moveend: updateView,
    resize: updateView,
    movestart: hideName,
    mouseout: hideName,
  });
  useEffect(() => {
    // Close the shared label when leaving this map, including during development refreshes.
    return () => hideName();
  }, [hideName]);

  // Offscreen badges aren't tab stops. Polygons remain available even when their label is offscreen.
  const { zoom, bounds } = view;
  const candidates = data.features.filter(({ properties }) =>
    hasLabel(properties) && bounds.contains([properties.labelLat, properties.labelLng]));
  // Only reconsider sizes/spacing after movement or selection changes, never during panning.
  const size = Math.min(36, Math.max(26, 26 + (zoom - 9) * 2));
  const placed: { point: Point; radius: number }[] = [];
  const visibleBadges = new Set<string>();
  const ordered = candidates.sort((a, b) =>
    Number(b.properties.pwsid === selectedSystemId) - Number(a.properties.pwsid === selectedSystemId));

  // Selected first: nearby badges yield to it. Zooming in separates the other labels.
  for (const { properties: system } of ordered) {
    if (!hasLabel(system)) continue;
    const point = map.project([system.labelLat, system.labelLng], zoom);
    const radius = (system.pwsid === selectedSystemId ? size + 4 : size) / 2;
    if (placed.some((other) => point.distanceTo(other.point) < radius + other.radius + 4)) continue;
    placed.push({ point, radius });
    visibleBadges.add(system.pwsid);
  }

  const hasHiddenBadges = visibleBadges.size < candidates.length;

  return (
    <>
      {hasHiddenBadges && <div className="score-map-hint">Zoom in to see nearby scores.</div>}
      {data.features.map((feature) => (
        <SystemRegion
          key={feature.properties.pwsid}
          feature={feature}
          selected={feature.properties.pwsid === selectedSystemId}
          badgeSize={feature.properties.pwsid === selectedSystemId ? size + 4 : size}
          showBadge={visibleBadges.has(feature.properties.pwsid)}
          onSelectSystem={onSelectSystem}
          showName={showName}
          hideName={hideName}
        />
      ))}
    </>
  );
}
