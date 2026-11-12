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

// Where the map starts, and where the recenter button brings it back to.
// This starting view is also the furthest out a user can zoom or pan (see LockToStartView).
// The center of the 11 systems' bounding box (lng -118.668 to -118.066, lat 33.704 to 34.337),
// so the polygons sit in the middle of the map
const START_CENTER: [number, number] = [34.0205, -118.3669];
// Tiles only exist at whole zoom levels; Leaflet uses the nearest one and scales it. At 9.25 it stretched
// zoom-9 tiles (blurry); at 9.5 it uses zoom-10 tiles shrunk to ~71%, which stays sharp.
const START_ZOOM = 9.5;

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

// Makes the starting view the limit: no zooming out past START_ZOOM, and no panning outside the area that
// view shows at the map's current pixel size. Once zoomed in, you can pan anywhere inside that area.
// The area depends on the map's size, so it's worked out on load and again on every resize.
// Leaflet's own zoom control greys out its "−" button at the minimum zoom.
function LockToStartView() {
  const map = useMap();
  useEffect(() => {
    const update = () => {
      // The start view's corners: half the map's size in pixels either side of the start center
      const center = map.project(START_CENTER, START_ZOOM);
      const half = map.getSize().divideBy(2);
      const startView = latLngBounds(
        map.unproject(center.subtract(half), START_ZOOM),
        map.unproject(center.add(half), START_ZOOM),
      );
      map.setMinZoom(START_ZOOM);
      map.setMaxBounds(startView);
    };
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
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    fetch("http://localhost:8080/api/systems/map")
      .then((res) => {
        if (!res.ok) throw new Error(`Backend responded ${res.status}`);
        return res.json();
      })
      .then((json: GeoJsonObject) => setData(json))
      .catch((err) => {
        console.error("Map data fetch failed:", err);
        setFailed(true);
      });
  }, []);

  return (
    // The wrapper lets the legend sit on top of the map without being part of Leaflet
    <div className="map-view">
      <MapContainer
        className={className}
        center={START_CENTER}
        zoom={START_ZOOM}
        zoomSnap={0.25} // allow quarter zoom levels, so START_ZOOM can sit between Leaflet's whole steps
        maxBoundsViscosity={1} // 1 = a hard edge, not a rubber band (the bounds are set by LockToStartView)
        style={{ height: "100%", width: "100%" }} // fills .map-frame, which the layout sizes
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; OpenStreetMap contributors"
        />
        <LockToStartView />
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
      {/* Centered over the map until the water system shapes arrive (the base map shows underneath) */}
      {!data && (
        <div className="map-loading" role="status">
          {failed ? (
            <span>Couldn't load the water systems.</span>
          ) : (
            <>
              <span className="map-spinner" aria-hidden="true" />
              <span>Map is loading…</span>
            </>
          )}
        </div>
      )}
    </div>
  );
}
