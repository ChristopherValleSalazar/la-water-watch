import { useEffect, useRef, useState } from "react";
import "./AddressSearch.css";

const ESRI = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
// xmin,ymin,xmax,ymax (LA County box): only narrows suggestions, backend is the real check
const LA_BOUNDS = "-118.9448,33.7036,-117.6463,34.8233";

// Spring endpoint: receives { address }, geocodes with Census, runs the polygon check
const BACKEND_URL = "http://localhost:8080/api/address";

// Shape of one Esri suggestion (text + magicKey, no coordinates yet)
type Suggestion = { text: string; magicKey: string };

// Mirrors the backend's AddressLookupResponse.
// Everything but inServiceArea is null when the geocoder can't place the address.
type CheckResult = {
  matchedAddress: string | null;
  lat: number | null;
  lng: number | null;
  pwsid: string | null; // null when the point is outside every system
  inServiceArea: boolean;
};

// The parent passes a function in; we call it once the backend confirms the address.
type AddressSearchProps = {
  // address is the geocoder's matched address; the info panel shows it as "Serves ..."
  onLocate: (lat: number, lng: number, pwsid: string | null, address: string) => void;
};

// Esri suggestion text: "1572 Bloomfield Ave, Lakewood, CA, 90712, USA"
function formatSuggestion(text: string) {
  const i = text.indexOf(",");
  return i === -1
    ? { line1: text, line2: "" }
    : { line1: text.slice(0, i), line2: text.slice(i + 2) };
}

export default function AddressSearch({ onLocate }: AddressSearchProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState("");

  // useRef holds a value that survives re-renders WITHOUT causing one (like a plain field).
  const containerRef = useRef<HTMLDivElement>(null);
  // Picking a suggestion sets the input text; this flag stops that from triggering a new search.
  const skipNextSearch = useRef(false);

  // Runs every time query changes. This replaces the vanilla 'input' listener,
  // the debounceTimer variable, and the controller variable.
  useEffect(() => {
    if (skipNextSearch.current) {
      skipNextSearch.current = false;
      return;
    }

    const q = query.trim();
    if (q.length < 3) {
      setOpen(false);
      setLoading(false);
      return;
    }

    // Instant feedback: show skeleton rows while debouncing + fetching
    setLoading(true);
    setError("");
    setOpen(true);

    const controller = new AbortController();
    const timer = setTimeout(() => {
      const url =
        `${ESRI}/suggest?f=json&text=${encodeURIComponent(q)}` +
        `&searchExtent=${LA_BOUNDS}&location=-118.2437,34.0522` +
        `&category=Address&countryCode=USA&maxSuggestions=5`;

      fetch(url, { signal: controller.signal })
        .then((res) => res.json())
        .then((data) => {
          setSuggestions(data.suggestions ?? []);
          setLoading(false);
        })
        .catch((err) => {
          if (err.name === "AbortError") return; // superseded by newer typing
          console.error("Geocoding error:", err);
          setError("Something went wrong. Try again.");
          setLoading(false);
        });
    }, 300);

    // Cleanup: React runs this before the effect runs again (the next keystroke).
    // That cancels the pending timer and aborts the old request, which is the debounce.
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  // Close the dropdown when clicking anywhere outside this component.
  useEffect(() => {
    function handleMouseDown(e: MouseEvent) {
      if (!containerRef.current?.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", handleMouseDown);
    // Cleanup removes the listener if the component disappears.
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, []);

  function selectSuggestion(s: Suggestion) {
    skipNextSearch.current = true;
    setQuery(s.text);
    setOpen(false);

    // Census onelineaddress doesn't need the trailing country
    const address = s.text.replace(/,\s*USA$/, "");
    setStatus("Checking address...");

    // Backend is the source of truth: it geocodes and validates
    fetch(BACKEND_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ address }),
    })
      .then(async (res) => {
        const body = await res.json();
        // 400 / 429 / 504 come back from the backend with a message
        if (!res.ok) throw new Error(body?.message ?? `Request failed (${res.status})`);
        return body as CheckResult;
      })
      .then((data) => {
        if (data.matchedAddress == null || data.lat == null || data.lng == null) {
          setStatus("Could not verify that address.");
          return;
        }
        onLocate(data.lat, data.lng, data.pwsid, data.matchedAddress);
        setStatus(
          data.inServiceArea
            ? `In service area: ${data.matchedAddress}`
            : `Outside our service area: ${data.matchedAddress}`,
        );
      })
      .catch((err) => {
        console.error("Backend error:", err);
        setStatus(err instanceof TypeError ? "Could not reach the server." : err.message);
      });
  }

  // Decides what goes inside the dropdown: skeleton, error, empty, or results.
  function renderItems() {
    if (loading) {
      return [75, 88, 70].map((width) => (
        <li key={width} className="skeleton">
          <div className="skel-line" style={{ width: `${width}%` }} />
          <div className="skel-line short" />
        </li>
      ));
    }
    if (error) return <li className="message">{error}</li>;
    if (suggestions.length === 0) return <li className="message">No matches</li>;

    return suggestions.map((s) => {
      const { line1, line2 } = formatSuggestion(s.text);
      // React escapes text automatically, so no innerHTML/textContent juggling needed.
      return (
        <li key={s.magicKey} className="result" onClick={() => selectSuggestion(s)}>
          <div className="line1">{line1}</div>
          <div className="line2">{line2}</div>
        </li>
      );
    });
  }

  return (
    <div className="address-search" ref={containerRef}>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        // Reopen results when refocusing a filled input
        onFocus={() => {
          if (query.trim().length >= 3 && (suggestions.length > 0 || error)) setOpen(true);
        }}
        placeholder="Type an address in Los Angeles..."
        autoComplete="off"
      />
      {open && <ul className="dropdown">{renderItems()}</ul>}
      {status && <p className="address-status">{status}</p>}
    </div>
  );
}

/*
  Usage in App.tsx:

  1. Imports at the top:
       import AddressSearch from "./AddressSearch";

  2. Inside App(), with the other useState lines:
       const [location, setLocation] = useState<[number, number] | null>(null);

  3. Wherever you want the search box:
       <AddressSearch onLocate={(lat, lng) => setLocation([lat, lng])} />

  4. Pass the location to the map:
       <MapView marker={location} />
*/