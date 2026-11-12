import { useEffect, useState } from "react";
import { MapContainer, TileLayer, GeoJSON, Marker, useMap } from "react-leaflet";
import type { Feature, GeoJsonObject } from "geojson";
import { Control, DomEvent, DomUtil, divIcon, latLngBounds, type PathOptions } from "leaflet";
import "leaflet/dist/leaflet.css";
import "./MapView.css";
import { NO_SCORE_COLOR, scoreColor, scoreGradient } from "./scoreColor";
import { PIN_COLOR, pinSvg } from "./pinIcon";

// Props type: describes what <MapView /> accepts. The ? makes each one optional.
type MapViewProps = {
  className?: string;
  marker?: [number, number] | null; // [lat, lon] from the address search
  onSelectSystem?: (pwsid: string) => void; // called with the clicked system's PWSID
};

// Where the map starts, and where the recenter button brings it back to
const START_CENTER: [number, number] = [34.05, -118.24];
const START_ZOOM = 9;

// LA County as [south, west] / [north, east], the same box AddressSearch uses for suggestions,
// padded 5% so the edges of the county aren't pressed against the map border.
const LA_COUNTY = latLngBounds([33.7036, -118.9448], [34.8233, -117.6463]).pad(0.05);

// The address pin. A divIcon is plain HTML on Leaflet's marker layer, which is moved but never scaled
// during zoom animations, so the pin stays the same size and sharp at every zoom (a CircleMarker sits on
// the polygons' SVG layer, which is stretched mid-zoom). Anchored at the tip, so the tip marks the address.
const PIN_WIDTH = 24;
const PIN_HEIGHT = 34;
const addressPin = divIcon({
  html: pinSvg(PIN_WIDTH, PIN_HEIGHT),
  className: "address-pin", // replaces Leaflet's default white box
  iconSize: [PIN_WIDTH, PIN_HEIGHT],
  iconAnchor: [PIN_WIDTH / 2, PIN_HEIGHT],
});

// Leaflet calls this once per polygon as it draws, so the color comes straight from that feature's waterScore.
function styleFeature(feature?: Feature): PathOptions {
  const color = scoreColor(feature?.properties?.waterScore);
  return {
    color, // outline
    weight: 2, // outline thickness
    fillColor: color, // inside
    fillOpacity: 0.5,
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

// Stops zooming out past the zoom where all of LA County fits. That zoom depends on the map's size,
// so it's worked out on load and again whenever the map resizes (phone rotation, window resize).
// Leaflet's own zoom control greys out its "−" button once the map reaches this minimum.
function LockToCounty() {
  const map = useMap();
  useEffect(() => {
    const update = () => map.setMinZoom(map.getBoundsZoom(LA_COUNTY));
    update();
    map.on("resize", update);
    return () => {
      map.off("resize", update);
    };
  }, [map]);
  return null;
}

// Crosshair button under the + / − buttons that brings the map back to its starting view.
// Built as a real Leaflet control so it lines up and looks like the zoom buttons above it.
function RecenterControl() {
  const map = useMap();
  useEffect(() => {
    const control = new Control({ position: "topleft" });
    control.onAdd = () => {
      const bar = DomUtil.create("div", "leaflet-bar recenter-control");
      const button = DomUtil.create("a", "", bar) as HTMLAnchorElement;
      button.href = "#";
      button.title = "Recenter map";
      button.setAttribute("role", "button");
      button.setAttribute("aria-label", "Recenter map");
      button.innerHTML =
        '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" ' +
        'stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="1.5" ' +
        'fill="currentColor"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>';
      // Keep clicks on the button from also counting as clicks (or drags) on the map underneath
      DomEvent.disableClickPropagation(bar);
      DomEvent.on(button, "click", (e) => {
        DomEvent.preventDefault(e);
        map.flyTo(START_CENTER, START_ZOOM);
      });
      return bar;
    };
    // Added after MapContainer's zoom control, so it stacks right below it
    control.addTo(map);
    return () => {
      control.remove();
    };
  }, [map]);
  return null;
}

// Bottom left: what the polygon colors mean. The bar is built from the same color stops as the polygons.
function MapLegend() {
  return (
    <div className="map-legend-box" aria-label="Map legend">
      <strong>Water score</strong>
      <span className="legend-bar" style={{ background: scoreGradient() }} />
      <div className="legend-ends">
        <span>0 · Higher concern</span>
        <span>100 · Lower concern</span>
      </div>
      <div className="legend-extra">
        <span>
          <i className="legend-swatch" style={{ background: NO_SCORE_COLOR }} /> No score
        </span>
        <span>
          <i className="legend-swatch legend-pin" style={{ background: PIN_COLOR }} /> Your address
        </span>
      </div>
      <small>CalEnviroScreen 5.0 (July 2026)</small>
    </div>
  );
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
    // The wrapper lets the legend sit on top of the map without being part of Leaflet
    <div className="map-view">
      <MapContainer
        className={className}
        center={START_CENTER}
        zoom={START_ZOOM}
        maxBounds={LA_COUNTY} // can't pan away from LA County
        maxBoundsViscosity={1} // 1 = a hard edge, not a rubber band
        style={{ height: "500px", width: "100%" }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; OpenStreetMap contributors"
        />
        <LockToCounty />
        <RecenterControl />
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
        {/* Our own divIcon pin, not Leaflet's default pin image (that one breaks with Vite bundling). */}
        {marker && (
          <>
            <Marker position={marker} icon={addressPin} keyboard={false} />
            <FlyTo position={marker} />
          </>
        )}
      </MapContainer>
      <MapLegend />
    </div>
  );
}
