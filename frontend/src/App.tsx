import { useState, type FormEvent } from "react";

// A type describes the allowed shape of one neighborhood record.
// TypeScript checks these fields while we work, then removes the type information for the browser.
type Area = {
  name: string;
  zip: string;
  score: number;
  concern: string;
  // x and y are coordinates inside the SVG map's 680 × 340 viewBox.
  x: number;
  y: number;
};

// This is a TypeScript "union": activeNav can only be one of these three exact strings.
type Page = "explore" | "context" | "resources";

// These illustrative records make the prototype interactive without requesting real data.
const areas: Area[] = [
  {
    name: "Santa Monica",
    zip: "90401",
    score: 91,
    concern: "Water system overview",
    x: 110,
    y: 136,
  },
  {
    name: "Hollywood",
    zip: "90028",
    score: 85,
    concern: "Filtering lead",
    x: 274,
    y: 115,
  },
  {
    name: "Silver Lake",
    zip: "90026",
    score: 82,
    concern: "Disinfection byproducts",
    x: 394,
    y: 146,
  },
  {
    name: "Pasadena",
    zip: "91101",
    score: 72,
    concern: "Water system overview",
    x: 525,
    y: 88,
  },
  {
    name: "Downtown",
    zip: "90012",
    score: 76,
    concern: "Disinfection byproducts",
    x: 443,
    y: 236,
  },
  {
    name: "Boyle Heights",
    zip: "90023",
    score: 64,
    concern: "Filtering lead",
    x: 565,
    y: 227,
  },
  {
    name: "South LA",
    zip: "90037",
    score: 61,
    concern: "Historical site context",
    x: 322,
    y: 271,
  },
];

// Keep the initial selection and the ZIP shortcut tied to the same Silver Lake record.
const defaultArea = areas[2];

// Add the sample scores and divide by their count to get a rounded citywide demo average.
const demoAverage = Math.round(
  areas.reduce((total, area) => total + area.score, 0) / areas.length,
);

// The [] means this is a list. Each item must have an allowed page id, a label, and an icon glyph.
const navItems: { id: Page; label: string; glyph: string }[] = [
  { id: "explore", label: "Explore the water", glyph: "⌖" },
  { id: "context", label: "Environmental context", glyph: "◷" },
  { id: "resources", label: "Resources & action", glyph: "▤" },
];

// This small React component avoids repeating the same decorative-icon markup.
// The { children: string } part is a TypeScript type for the text passed between its tags.
function Icon({ children }: { children: string }) {
  return (
    <span className="icon" aria-hidden="true">
      {children}
    </span>
  );
}

function App() {
  // useState returns a current value and a setter: [value, updateFunction].
  // React recalculates this component and updates the screen when a setter is called.
  const [activeNav, setActiveNav] = useState<Page>("explore");
  // These values are temporary page memory: layer, selection, search, notice, and controls.
  const [activeLayer, setActiveLayer] = useState("Water quality");
  const [selectedName, setSelectedName] = useState(defaultArea.name);
  const [query, setQuery] = useState("");
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const [expanded, setExpanded] = useState(false);
  const [zoom, setZoom] = useState(1);

  // Find the selected record so the map and detail card read from the same sample data.
  const selected = areas.find((area) => area.name === selectedName) ?? defaultArea;
  const scoreDifference = selected.score - demoAverage;
  const comparisonLabel =
    scoreDifference === 0
      ? "At demo average"
      : `${scoreDifference > 0 ? "↑" : "↓"} ${Math.abs(scoreDifference)} points`;

  // Shrinking the visible SVG rectangle zooms every map shape together around its center.
  const mapWidth = 680 / zoom;
  const mapHeight = 340 / zoom;
  const mapViewBox = `${(680 - mapWidth) / 2} ${(340 - mapHeight) / 2} ${mapWidth} ${mapHeight}`;
  // There are only seven demo records, so a simple filter is quick and easier to follow than a cache.
  const searchTerm = query.trim().toLowerCase();
  const suggestions = searchTerm
    ? areas.filter(
        (area) =>
          area.name.toLowerCase().includes(searchTerm) ||
          area.zip.startsWith(searchTerm),
      )
    : [];

  function selectArea(area: Area) {
    // Search results, map markers, and the ZIP shortcut all update the same selected area.
    setSelectedName(area.name);
    setQuery(area.zip);
    setSuggestionsOpen(false);
    setNotice(`Showing the illustrative ${area.name} demo area.`);
    setExpanded(false);
  }

  // FormEvent is a type only: it helps TypeScript understand this browser form event.
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    // Keep the browser from reloading the page when the form is submitted.
    event.preventDefault();
    setSuggestionsOpen(false);
    if (!query.trim()) {
      setNotice("Enter a Los Angeles area name or try ZIP code 90026.");
      return;
    }
    const match = areas.find(
      (area) =>
        area.zip === query.trim() ||
        area.name.toLowerCase() === query.trim().toLowerCase(),
    );
    if (match) selectArea(match);
    else
      setNotice(
        "Demo only: try 90026, 90028, 90023, or a neighborhood name shown on the map.",
      );
  }

  // JSX is HTML-like page markup. Curly braces insert JavaScript values and expressions.
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#top"
          onClick={() => setActiveNav("explore")}
          aria-label="wellwater home"
        >
          <span className="brand-drop">◉</span>
          <span>
            wellwater<span className="brand-period">.</span>
          </span>
        </a>
        <p className="brand-caption">LOS ANGELES, CONNECTED.</p>

        <nav className="primary-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`nav-link ${activeNav === item.id ? "active" : ""}`}
              onClick={() => setActiveNav(item.id)}
              aria-current={activeNav === item.id ? "page" : undefined}
            >
              <Icon>{item.glyph}</Icon>
              <span>{item.label}</span>
              {activeNav === item.id && <i aria-hidden="true" />}
            </button>
          ))}
        </nav>

        <div className="sidebar-story">
          <span className="story-mark">⌁</span>
          <strong>
            Different neighborhoods.
            <br />
            One connected story.
          </strong>
          <p>Water flows beyond ZIP codes. Understanding it should, too.</p>
          <span className="story-rule" />
          <small>
            <span className="status-dot" /> An independent community tool
          </small>
        </div>
        <button className="about-link" onClick={() => setActiveNav("context")}>
          <Icon>ⓘ</Icon> About the data <span>↗</span>
        </button>
      </aside>

      <main id="top" className="main-panel">
        <header className="topbar">
          <span className="eyebrow">THE WATER WE SHARE</span>
          <div className="topbar-right">
            <span className="location-pin">⌖</span> Los Angeles, CA{" "}
            <span className="top-divider" />
            <span className="demo-pill">Demo edition</span>
          </div>
        </header>

        <div className="content">
          {activeNav === "explore" ? (
            <>
              <section className="intro">
                <p className="eyebrow">LOCAL WATER. BIGGER PICTURE.</p>
                <h1>
                  Know your water.
                  <br className="mobile-break" /> See the connections.
                </h1>
                <p className="intro-copy">
                  Explore what’s in your water, how your neighborhood compares,
                  and the stories behind the numbers.
                </p>
                <form
                  className="search-box"
                  onSubmit={submitSearch}
                  // Leave suggestions open while tabbing to their buttons; dismiss them when leaving the form.
                  onBlur={(event) => {
                    if (!event.currentTarget.contains(event.relatedTarget)) {
                      setSuggestionsOpen(false);
                    }
                  }}
                >
                  <span className="search-icon" aria-hidden="true">
                    ⌕
                  </span>
                  <label className="sr-only" htmlFor="area-search">
                    Enter your address or ZIP code
                  </label>
                  {/* This input is "controlled": its value comes from React state, and typing updates that state. */}
                  <input
                    id="area-search"
                    value={query}
                    onFocus={() => setSuggestionsOpen(true)}
                    // Escape closes suggestions without changing the search text.
                    onKeyDown={(event) => {
                      if (event.key === "Escape") setSuggestionsOpen(false);
                    }}
                    // event.target.value is the text currently in the input box.
                    onChange={(event) => {
                      setQuery(event.target.value);
                      setSuggestionsOpen(true);
                    }}
                    placeholder="Enter your address or ZIP code"
                    autoComplete="off"
                  />
                  <button
                    type="button"
                    className="location-button"
                    aria-label="Use my current location"
                    onClick={() =>
                      setNotice(
                        "Location access is not enabled in this demo. Try ZIP code 90026.",
                      )
                    }
                    title="Location demo"
                  >
                    <span>⌖</span>
                  </button>
                  <button type="submit" className="search-submit">
                    Explore my water <span>→</span>
                  </button>
                  {suggestionsOpen && suggestions.length > 0 && (
                    <div
                      className="suggestions"
                      role="group"
                      aria-label="Matching demo areas"
                    >
                      {/* map() makes one suggestion button for each matching sample area. */}
                      {suggestions.slice(0, 5).map((area) => (
                        <button
                          key={area.zip}
                          type="button"
                          onClick={() => selectArea(area)}
                        >
                          <span>{area.name}</span>
                          <small>{area.zip}</small>
                        </button>
                      ))}
                    </div>
                  )}
                </form>
                <div className="search-hints">
                  <button
                    onClick={() =>
                      setNotice(
                        "Location access is not enabled in this demo. Try ZIP code 90026.",
                      )
                    }
                  >
                    ⌖ &nbsp;Use my current location
                  </button>
                  <span>·</span>
                  <span>Your location is never stored.</span>
                  <span>·</span>
                  <button onClick={() => selectArea(defaultArea)}>
                    Try <u>90026</u>
                  </button>
                </div>
                {notice && (
                  <p className="notice" role="status">
                    {notice}
                  </p>
                )}
              </section>

              <section className="city-section" aria-labelledby="city-title">
                <div className="section-head">
                  <div>
                    <div className="title-row">
                      <h2 id="city-title">The citywide picture</h2>
                      <span className="count-pill">7 demo neighborhoods</span>
                    </div>
                    <p>Different water stories. A shared landscape.</p>
                  </div>
                  <div
                    className="layer-tabs"
                    role="group"
                    aria-label="Map layer"
                  >
                    {["Water quality", "Water systems", "Context"].map(
                      (layer) => (
                        <button
                          aria-pressed={activeLayer === layer}
                          className={activeLayer === layer ? "selected" : ""}
                          key={layer}
                          onClick={() => setActiveLayer(layer)}
                        >
                          {layer}
                        </button>
                      ),
                    )}
                  </div>
                </div>

                {/* The inline SVG is the demo map; its coordinates and shapes are drawn in this file. */}
                <div className="map-and-card">
                  <div className="map-frame">
                    <div className="map-label">
                      <span className="map-dot" /> Los Angeles County{" "}
                      <span>/</span> Illustrative map
                    </div>
                    <div className="map-controls">
                      <button
                        aria-label="Zoom in"
                        disabled={zoom >= 1.4}
                        onClick={() =>
                          setZoom((value) => Math.min(value + 0.1, 1.4))
                        }
                      >
                        +
                      </button>
                      <button
                        aria-label="Zoom out"
                        disabled={zoom <= 0.8}
                        onClick={() =>
                          setZoom((value) => Math.max(value - 0.1, 0.8))
                        }
                      >
                        −
                      </button>
                    </div>
                    <svg
                      className="map-svg"
                      viewBox={mapViewBox}
                      role="group"
                      aria-label={`Illustrative Los Angeles ${activeLayer.toLowerCase()} map; select a neighborhood marker`}
                    >
                      <defs>
                        <pattern
                          id="grid"
                          width="25"
                          height="25"
                          patternTransform="rotate(-13)"
                          patternUnits="userSpaceOnUse"
                        >
                          <path
                            d="M 25 0 L 0 0 0 25"
                            fill="none"
                            stroke="#fffdf5"
                            strokeWidth="1.1"
                            opacity=".85"
                          />
                        </pattern>
                        <clipPath id="land">
                          <path d="M0 76 135 24 270 42 390 9 555 28 680 0 680 286 548 322 410 300 258 342 90 304 0 254Z" />
                        </clipPath>
                      </defs>
                      <rect width="680" height="340" fill="#e9eee4" />
                      <path
                        d="M0 76 135 24 270 42 390 9 555 28 680 0 680 286 548 322 410 300 258 342 90 304 0 254Z"
                        fill="#d9e0d0"
                      />
                      <g clipPath="url(#land)">
                        <rect width="680" height="340" fill="url(#grid)" />
                        <path
                          d="m0 182 119-72 99 19 36 57-74 64-138 6Z"
                          fill="#bdcda3"
                          opacity=".84"
                        />
                        <path
                          d="m217 136 111-48 101 31 11 69-98 45-99-40Z"
                          fill="#d3d7a0"
                          opacity=".85"
                        />
                        <path
                          d="m407 106 116-47 99 26 15 76-86 54-116-34Z"
                          fill="#dce0b0"
                          opacity=".88"
                        />
                        <path
                          d="m333 220 141-40 118 31 23 68-126 47-154-44Z"
                          fill="#dabd8e"
                          opacity=".75"
                        />
                        <path
                          d="m123 235 119-30 104 43-48 88-145-22Z"
                          fill="#cbd4a3"
                          opacity=".88"
                        />
                        <path
                          d="M-20 218Q130 288 253 249T700 198"
                          fill="none"
                          stroke="#fffdf3"
                          strokeWidth="8"
                        />
                        <path
                          d="M165 -20Q224 102 304 166T444 370"
                          fill="none"
                          stroke="#fffdf3"
                          strokeWidth="7"
                        />
                        <path
                          d="M566 -10Q525 91 570 189T615 360"
                          fill="none"
                          stroke="#91bac0"
                          strokeWidth="4"
                          opacity=".86"
                        />
                        <path
                          d="M-10 150 690 81"
                          fill="none"
                          stroke="#fffdf3"
                          strokeWidth="3"
                        />
                        <text x="70" y="64" className="map-landmark">
                          SANTA MONICA MOUNTAINS
                        </text>
                        <text x="325" y="53" className="map-landmark">
                          BURBANK
                        </text>
                        <text x="595" y="214" className="map-landmark">
                          ALHAMBRA
                        </text>
                        <text x="55" y="309" className="map-landmark">
                          WEST LOS ANGELES
                        </text>
                        {/* One marker per record: the shared click handler updates the selected card. */}
                        <g>
                          {areas.map((area) => (
                            <g
                              key={area.name}
                              className="map-marker"
                              role="button"
                              tabIndex={0}
                              aria-pressed={selected.name === area.name}
                              aria-label={`${area.name}, demo score ${area.score}`}
                              onClick={() => selectArea(area)}
                              onKeyDown={(event) => {
                                if (event.key === "Enter" || event.key === " ") {
                                  // Space would otherwise scroll the page instead of only selecting this marker.
                                  event.preventDefault();
                                  selectArea(area);
                                }
                              }}
                            >
                              <circle
                                cx={area.x}
                                cy={area.y}
                                r={selected.name === area.name ? 15 : 12}
                                fill={
                                  selected.name === area.name
                                    ? "#254c3d"
                                    : "#fbfcf7"
                                }
                                stroke={
                                  selected.name === area.name
                                    ? "#fff"
                                    : "#75836e"
                                }
                                strokeWidth="2"
                              />
                              <text
                                x={area.x}
                                y={area.y + 4}
                                textAnchor="middle"
                                className={
                                  selected.name === area.name
                                    ? "marker-score active-marker"
                                    : "marker-score"
                                }
                              >
                                {area.score}
                              </text>
                              <text
                                x={area.x}
                                y={area.y + 30}
                                textAnchor="middle"
                                className="marker-name"
                              >
                                {area.name}
                              </text>
                            </g>
                          ))}
                        </g>
                      </g>
                      <text x="548" y="321" className="map-landmark">
                        Illustrative · not to scale
                      </text>
                    </svg>
                    <div className="map-legend">
                      <span className="legend-chip">
                        <i />{" "}
                        {activeLayer === "Water quality"
                          ? "Illustrative score"
                          : activeLayer === "Water systems"
                            ? "Water systems"
                            : "Local context"}
                      </span>
                      <span>Demo neighborhood markers</span>
                    </div>
                  </div>

                  <aside
                    className="area-card"
                    aria-label="Selected neighborhood details"
                  >
                    <div className="card-kicker">
                      <span>⌖</span> YOUR SELECTED NEIGHBORHOOD
                    </div>
                    <div className="area-heading">
                      <h3>{selected.name}</h3>
                      <span className="zip-badge">{selected.zip}</span>
                    </div>
                    <p className="system-line">LADWP · Location match</p>
                    <div className="score-line">
                      <strong>{selected.score}</strong>
                      <span>/ 100</span>
                      <span className="score-tag">
                        {selected.score >= 80
                          ? "Lower concern"
                          : "Explore context"}
                      </span>
                    </div>
                    <div className="score-bar">
                      <i style={{ width: `${selected.score}%` }} />
                    </div>
                    <small className="disclaimer">
                      Illustrative score · not a safety determination
                    </small>
                    <div className="compare-line">
                      <span>Compared with demo average</span>
                      <b>{comparisonLabel}</b>
                    </div>
                    <div className="concern-block">
                      <div className="concern-title">
                        WHAT TO LOOK INTO{" "}
                        <span title="A conversation starter, not a measurement">
                          ⓘ
                        </span>
                      </div>
                      <div className="concern-row">
                        <i />
                        {selected.concern}
                        <span className="review-badge">Review</span>
                      </div>
                      <p>
                        A starting point for questions—not a measurement at your
                        tap.
                      </p>
                    </div>
                    <button
                      className="overview-button"
                      onClick={() => setExpanded((value) => !value)}
                      aria-expanded={expanded}
                    >
                      {expanded ? "Hide water overview" : "View water overview"}{" "}
                      <span>{expanded ? "↑" : "→"}</span>
                    </button>
                    {expanded && (
                      <div className="overview-detail">
                        <strong>Sample overview</strong>
                        <p>
                          This concept uses illustrative neighborhood values. A
                          future version would show the water system’s published
                          report, date, and source.
                        </p>
                      </div>
                    )}
                  </aside>
                </div>
              </section>
              <p className="demo-disclaimer">
                <span>ⓘ</span> Independent community tool · Demo values are
                illustrative and are not water quality measurements.
              </p>
            </>
          ) : activeNav === "context" ? (
            <section className="secondary-view">
              <p className="eyebrow">PLACE, HISTORY, AND WATER</p>
              <h1>Environmental context</h1>
              <p className="intro-copy">
                Water stories are shaped by the places and histories around us.
                This section is a preview of how local context could appear
                alongside water-system information.
              </p>
              <div className="context-grid">
                {[
                  {
                    title: "Industrial history",
                    text: "Explore how land use and historical industry may shape questions residents ask about nearby water.",
                  },
                  {
                    title: "Environmental sites",
                    text: "A future version could connect verified public site records with neighborhood context.",
                  },
                  {
                    title: "Community perspective",
                    text: "Pair public records with local organizations and community knowledge.",
                  },
                ].map((item) => (
                  <article className="info-card" key={item.title}>
                    <span className="info-icon">⌁</span>
                    <h2>{item.title}</h2>
                    <p>{item.text}</p>
                    <span className="coming-pill">Concept preview</span>
                  </article>
                ))}
              </div>
              <button
                className="return-button"
                onClick={() => setActiveNav("explore")}
              >
                ← Back to the citywide map
              </button>
            </section>
          ) : (
            <section className="secondary-view">
              <p className="eyebrow">NEXT STEPS FOR YOUR COMMUNITY</p>
              <h1>Resources & action</h1>
              <p className="intro-copy">
                A starting point for questions, testing, and community action.
                Resource links are placeholders in this visual mockup.
              </p>
              <div className="context-grid">
                {[
                  {
                    title: "Request water testing",
                    text: "Learn which local agency can answer questions about testing and sampling.",
                  },
                  {
                    title: "Understand your report",
                    text: "Find plain-language help for reading a water system quality report.",
                  },
                  {
                    title: "Connect with local groups",
                    text: "Discover environmental justice organizations and neighborhood advocates.",
                  },
                  {
                    title: "Ask a question",
                    text: "Prepare useful questions for your water provider or local representative.",
                  },
                ].map((item) => (
                  <article className="info-card" key={item.title}>
                    <span className="info-icon">↗</span>
                    <h2>{item.title}</h2>
                    <p>{item.text}</p>
                    <span className="coming-pill">Resource preview</span>
                  </article>
                ))}
              </div>
              <button
                className="return-button"
                onClick={() => setActiveNav("explore")}
              >
                ← Back to the citywide map
              </button>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}

export default App;
