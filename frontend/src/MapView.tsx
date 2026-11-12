import { useEffect, useState } from "react";
import { MapContainer, TileLayer, GeoJSON, CircleMarker, useMap } from "react-leaflet";
import type { Feature, GeoJsonObject } from "geojson";
import type { PathOptions } from "leaflet";
import "leaflet/dist/leaflet.css";

// Props type: describes what <MapView /> accepts. The ? makes each one optional.
type MapViewProps = {
  className?: string;
  marker?: [number, number] | null; // [lat, lon] from the address search
  onSelectSystem?: (pwsid: string) => void; // called with the clicked system's PWSID
};

const palette = ["#e63946", "#2a9d8f", "#e9c46a", "#457b9d", "#f4a261", "#8338ec", "#06d6a0"];

// Turns a polygon's name into a stable palette index, so it keeps the same color on every render.
function pickColor(key: string): string {
  let hash = 0;
  for (const ch of key) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return palette[hash % palette.length];
}

function styleFeature(feature?: Feature): PathOptions {
  const key = String(feature?.properties?.name ?? feature?.id ?? "");
  const color = pickColor(key);
  return {
    color, // outline
    weight: 2, // outline thickness
    fillColor: color, // inside
    fillOpacity: 0.4,
  };
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

export default function MapView({ className, marker, onSelectSystem }: MapViewProps) {
  // data is either GeoJSON from the backend or null before the fetch finishes.
  const [data, setData] = useState<GeoJsonObject | null>(null);

  useEffect(() => {
    fetch("http://localhost:8080/api/systems/map")
      .then((res) => res.json())
      .then((json: GeoJsonObject) => setData(json))
      .catch((err) => console.error("Map data fetch failed:", err));
  }, []);

  return (
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
      {data && (
        <GeoJSON
          data={data}
          style={styleFeature}
          eventHandlers={{
            // propagatedFrom is the polygon that was clicked; its feature carries the backend's properties.
            click: (e) => {
              const pwsid = e.propagatedFrom?.feature?.properties?.pwsid;
              if (pwsid) onSelectSystem?.(pwsid);
            },
          }}
        />
      )}
      {/* CircleMarker avoids Leaflet's default pin image, which breaks with Vite bundling. */}
      {marker && (
        <>
          <CircleMarker center={marker} radius={8} pathOptions={{ color: "#111", fillColor: "#4a7dff", fillOpacity: 1 }} />
          <FlyTo position={marker} />
        </>
      )}
    </MapContainer>
  );
}