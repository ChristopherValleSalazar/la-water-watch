import { useEffect, useState } from "react";
import { MapContainer, TileLayer, GeoJSON, CircleMarker, Tooltip, useMap } from "react-leaflet";
import type { FeatureCollection, Geometry } from "geojson";
import "leaflet/dist/leaflet.css";

// These are the backend fields this map uses. pwsid identifies the same system in the panel API.
type SystemProperties = { pwsid: string; name: string };
type SystemMap = FeatureCollection<Geometry, SystemProperties>;

// Props are inputs from App. onSelectSystem is a function the map calls to report a selection.
type MapViewProps = {
  className?: string;
  marker?: [number, number] | null; // [lat, lon] from the address search
  selectedSystemId: string;
  onSelectSystem: (systemId: string) => void;
};

const palette = ["#e63946", "#2a9d8f", "#e9c46a", "#457b9d", "#f4a261", "#8338ec", "#06d6a0"];

// Turns a polygon's name into a stable palette index, so it keeps the same color on every render.
function pickColor(key: string): string {
  let hash = 0;
  for (const ch of key) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return palette[hash % palette.length];
}

// Helper component: useMap() only works INSIDE MapContainer, so this lives in there.
// It renders nothing; it just moves the map whenever position changes.
function FlyTo({ position }: { position: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo(position, 15);
  }, [map, position]);
  return null;
}

export default function MapView({ className, marker, selectedSystemId, onSelectSystem }: MapViewProps) {
  // The generic <SystemMap | null> tells TypeScript what data can hold.
  const [data, setData] = useState<SystemMap | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);

  // Fetch boundaries once, or on retry. Selecting a system does not download the map again.
  useEffect(() => {
    const controller = new AbortController();
    let current = true;
    setLoading(true);
    setError(false);

    async function load() {
      try {
        const response = await fetch("http://localhost:8080/api/systems/map", {
          signal: controller.signal,
        });
        if (!response.ok) throw new Error("Map data unavailable");
        const result: SystemMap = await response.json();
        if (result.type !== "FeatureCollection" || !Array.isArray(result.features)) {
          throw new Error("Unexpected map data");
        }
        if (current) setData(result);
      } catch {
        if (current) setError(true);
      } finally {
        if (current) setLoading(false);
      }
    }

    void load();
    return () => {
      current = false;
      controller.abort();
    };
  }, [attempt]);

  return (
    <>
      {loading && <p className="map-status" role="status">Loading water system boundaries…</p>}
      {error && (
        <div className="map-status">
          <p role="alert">Water system boundaries couldn’t be loaded.</p>
          <button onClick={() => setAttempt((value) => value + 1)}>Try loading the map again</button>
        </div>
      )}
      {!loading && !error && data?.features.length === 0 && (
        <p className="map-status" role="status">No water system boundaries are available.</p>
      )}
      <MapContainer
        className={className}
        center={[34.05, -118.24]}
        zoom={9}
        style={{ height: "500px", width: "100%" }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; OpenStreetMap contributors"
        />
        {/* One GeoJSON layer per system keeps every piece of a MultiPolygon selected together. */}
        {data?.features.map((feature) => {
          const { pwsid, name } = feature.properties;
          // Deriving styles from App's ID also restores the previous system's normal outline.
          const selected = pwsid === selectedSystemId;
          const color = pickColor(name);

          return (
            <GeoJSON
              key={pwsid}
              data={feature}
              style={{
                className: `water-system-boundary system-${pwsid}`,
                color,
                weight: selected ? 4 : 2,
                fillColor: color,
                fillOpacity: selected ? 0.55 : 0.4,
              }}
              // Send only the ID to App; the existing panel handles fetching the details.
              eventHandlers={{ click: () => onSelectSystem(pwsid) }}
            >
              <Tooltip sticky>{name}</Tooltip>
            </GeoJSON>
          );
        })}
        {/* CircleMarker avoids Leaflet's default pin image, which breaks with Vite bundling. */}
        {marker && (
          <>
            <CircleMarker center={marker} radius={8} pathOptions={{ color: "#111", fillColor: "#4a7dff", fillOpacity: 1 }} />
            <FlyTo position={marker} />
          </>
        )}
      </MapContainer>
    </>
  );
}
