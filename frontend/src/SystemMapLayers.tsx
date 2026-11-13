import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { GeoJSON, Marker, Polyline, useMap, useMapEvents } from "react-leaflet";
import { divIcon, point as pixelPoint, tooltip, type LatLng, type LatLngExpression, type Marker as LeafletMarker, type Point } from "leaflet";
import type { Feature, FeatureCollection, Geometry } from "geojson";
import { scoreColor } from "./scoreColor";

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
  selectedSystemId: string | null;
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
  badgePosition: LatLng | null;
  showLeader: boolean;
  onSelectSystem: (systemId: string) => void;
};

const BADGE_GAP = 4;
const LEADER_MIN_PX = 6;

type Badge = { pwsid: string; anchor: Point; point: Point; radius: number; fixed: boolean };

// Pushes overlapping badges apart in screen pixels; the selected (fixed) badge stays put.
function spreadBadges(badges: Badge[]): void {
  for (let round = 0; round < 80; round++) {
    let moved = false;
    for (let i = 0; i < badges.length; i++) {
      for (let j = i + 1; j < badges.length; j++) {
        const a = badges[i];
        const b = badges[j];
        const need = a.radius + b.radius + BADGE_GAP;
        let dx = b.point.x - a.point.x;
        let dy = b.point.y - a.point.y;
        let dist = Math.hypot(dx, dy);
        if (dist >= need - 0.5) continue;
        if (dist < 0.01) {
          dx = 1;
          dy = 0;
          dist = 1;
        }
        const push = need - dist;
        const ux = dx / dist;
        const uy = dy / dist;
        const shareA = a.fixed ? 0 : b.fixed ? 1 : 0.5;
        const shareB = 1 - shareA;
        a.point = pixelPoint(a.point.x - ux * push * shareA, a.point.y - uy * push * shareA);
        b.point = pixelPoint(b.point.x + ux * push * shareB, b.point.y + uy * push * shareB);
        moved = true;
      }
    }
    if (!moved) break;
  }
}

// This type guard checks coordinates and tells TypeScript they are numbers inside the guarded block.
function hasLabel(system: SystemProperties): system is SystemProperties & { labelLat: number; labelLng: number } {
  return system.labelLat != null && system.labelLng != null
    && Number.isFinite(system.labelLat) && Number.isFinite(system.labelLng);
}

// A small component keeps Leaflet's marker/DOM details out of MapView and App.
function SystemRegion({
  feature, selected, badgeSize, showBadge, badgePosition, showLeader, onSelectSystem, showName, hideName,
}: SystemRegionProps) {
  const system = feature.properties;
  // Share main's score palette with the legend, panel, and leaderboard.
  const color = scoreColor(system.waterScore);
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
      {showBadge && showLeader && badgePosition && hasLabel(system) && (
        <Polyline
          positions={[[system.labelLat, system.labelLng], badgePosition]}
          pathOptions={{ color: "#24352f", weight: 1.5, opacity: 0.7, interactive: false }}
        />
      )}
      {showBadge && badgePosition && hasLabel(system) && (
        <Marker
          ref={markerRef}
          position={badgePosition}
          icon={icon}
          // The inner button handles keyboard input; avoid a second tab stop on its wrapper.
          keyboard={false}
          zIndexOffset={selected ? 1000 : 0}
          eventHandlers={{
            click: () => onSelectSystem(system.pwsid),
            mouseover: () => showName(system.name, badgePosition),
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

  const { zoom, bounds } = view;
  const size = Math.min(36, Math.max(26, 26 + (zoom - 9) * 2));
  const badges: Badge[] = data.features
    .map(({ properties: system }) => {
      if (!hasLabel(system)) return null;
      const anchor = map.project([system.labelLat, system.labelLng], zoom);
      const selected = system.pwsid === selectedSystemId;
      return { pwsid: system.pwsid, anchor, point: anchor, radius: (selected ? size + 4 : size) / 2, fixed: selected };
    })
    .filter((badge): badge is Badge => badge != null);
  spreadBadges(badges);
  const layout = new Map(badges.map((badge) => [badge.pwsid, badge]));

  return (
    <>
      {data.features.map((feature) => {
        const badge = layout.get(feature.properties.pwsid);
        const badgePosition = badge ? map.unproject(badge.point, zoom) : null;
        return (
        <SystemRegion
          key={feature.properties.pwsid}
          feature={feature}
          selected={feature.properties.pwsid === selectedSystemId}
          badgeSize={feature.properties.pwsid === selectedSystemId ? size + 4 : size}
          showBadge={badgePosition != null && bounds.contains(badgePosition)}
          badgePosition={badgePosition}
          showLeader={badge != null && badge.point.distanceTo(badge.anchor) > LEADER_MIN_PX}
          onSelectSystem={onSelectSystem}
          showName={showName}
          hideName={hideName}
        />
        );
      })}
    </>
  );
}
