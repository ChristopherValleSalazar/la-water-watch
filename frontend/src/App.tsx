import { useState } from "react";
import MapView from "./MapView";
import AddressSearch from "./AddressSearch";

// A type describes the allowed shape of one neighborhood record.
// TypeScript checks these fields while we work, then removes the type information for the browser.
type Area = {
  name: string;
  zip: string;
  score: number;
  concern: string;
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
  },
  {
    name: "Hollywood",
    zip: "90028",
    score: 85,
    concern: "Filtering lead",
  },
  {
    name: "Silver Lake",
    zip: "90026",
    score: 82,
    concern: "Disinfection byproducts",
  },
  {
    name: "Pasadena",
    zip: "91101",
    score: 72,
    concern: "Water system overview",
  },
  {
    name: "Downtown",
    zip: "90012",
    score: 76,
    concern: "Disinfection byproducts",
  },
  {
    name: "Boyle Heights",
    zip: "90023",
    score: 64,
    concern: "Filtering lead",
  },
  {
    name: "South LA",
    zip: "90037",
    score: 61,
    concern: "Historical site context",
  },
];

// Keep the initial selection and the ZIP shortcut tied to the same Silver Lake record.
const defaultArea = areas[2];

// Add the sample scores and divide by their count to get a rounded citywide demo average.
const demoAverage = Math.round(
  areas.reduce((total, area) => total + area.score, 0) / areas.length,
);

// The [] means this is a list. Each item must have an allowed page id, a label, and an icon glyph.
const navItems: { id: Page; label: string; icon: string }[] = [
  { id: "explore", label: "Explore the water", icon: "map" },
  { id: "context", label: "Environmental context", icon: "history" },
  { id: "resources", label: "Resources & action", icon: "resource" },
];

// This small React component avoids repeating the same decorative-icon markup.
// The { children: string } part is a TypeScript type for the text passed between its tags.
function Icon({ name, size = 25 }: { name: string; size?: number }) {
  const paths: Record<string, React.ReactNode> = {
    drop: <path d="M12 3C9 8 5 11 5 15a7 7 0 0 0 14 0c0-4-4-7-7-12Z" />,
    map: <><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3Z"/><path d="M9 3v15M15 6v15"/></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6"/>,
    pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/></>,
    history: <><path d="M3 11a9 9 0 1 1 2 7M3 4v7h7M12 7v5l3 2"/></>,
    resource: <><path d="M4 4h7a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H4Zm16 0h-3a3 3 0 0 0-3 3M20 4v15h-2"/></>,
    info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v1"/></>,
    globe: <><circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18"/></>,
    locate: <><circle cx="12" cy="12" r="5"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/></>,
    civic: <><path d="M3 21h18M5 18h14M6 9v9M10 9v9M14 9v9M18 9v9M3 6l9-4 9 4Z"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] || paths.info}</svg>
}

function App() {
  // useState returns a current value and a setter: [value, updateFunction].
  // React recalculates this component and updates the screen when a setter is called.
  const [activeNav, setActiveNav] = useState<Page>("explore");
  // These values are temporary page memory: map layer and the overview toggle.
  const [activeLayer, setActiveLayer] = useState("Water quality");
  const [expanded, setExpanded] = useState(false);
  // Shared between AddressSearch (writes it) and MapView (reads it), so it lives here in the parent.
  const [location, setLocation] = useState<[number, number] | null>(null);

  // The detail card still shows the Silver Lake demo record until real data is wired in.
  const selected = defaultArea;
  const scoreDifference = selected.score - demoAverage;
  const comparisonLabel =
    scoreDifference === 0
      ? "At demo average"
      : `${scoreDifference > 0 ? "↑" : "↓"} ${Math.abs(scoreDifference)} points`;

  // JSX is HTML-like page markup. Curly braces insert JavaScript values and expressions.
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#top"
          onClick={() => setActiveNav("explore")}
          aria-label="Wellwater Home"
        >
          <span className="brand-drop"><Icon name="drop" size={29}/></span>
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
               <Icon name={item.icon}/>
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
          <Icon name="notice"/> About the data <span>↗</span>
        </button>
      </aside>

      <main id="top" className="main-panel">
        <header className="topbar">
          <span className="eyebrow">THE WATER WE SHARE</span>
          <div className="topbar-right">
            <span className="location-pin"><Icon name="pin" size={17}/></span> Los Angeles, CA{" "}
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
                {/* Address autocomplete lives in AddressSearch.tsx; it reports coordinates up to App. */}
                <div  role="search">
                  <AddressSearch onLocate={(lat, lon) => setLocation([lat, lon])} />
                  <span className="search-icon" aria-hidden="true" >
                    {/*<Icon name="search" size={20}/>*/}
                  </span>
                </div>
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

                <div className="map-and-card">
                  {/* The Leaflet map lives in MapView.tsx. */}
                  <div className="map-frame">
                    <MapView marker={location} />
                  </div>

                  <aside
                    className="area-card"
                    aria-label="Selected neighborhood details"
                  >
                    <div className="card-kicker">
                      <span><Icon name="pin" size={14}/></span> YOUR SELECTED NEIGHBORHOOD
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
                    text: "See how 1930s redlining maps graded LA neighborhoods, and how those decisions shaped where industry and pollution ended up.",
                    href:"https://dsl.richmond.edu/panorama/redlining/"
                  },
                  {
                    title: "Environmental sites",
                    text: "Find Superfund and other cleanup sites near you, including contaminated groundwater that some water systems draw from.",
                    href: "https://www.epa.gov/superfund/search-superfund-sites-where-you-live"
                  },
                  {
                    title: "Community perspective",
                    text: "See how your neighborhood's pollution burden compares across California with CalEnviroScreen.",
                    href: "https://oehha.ca.gov/calenviroscreen/about-calenviroscreen"
                  },
                ].map((item) => (
                  <article className="info-card" key={item.title}>
                    <span className="info-icon">⌁</span>
                    <h2>{item.title}</h2>
                    <p>{item.text}</p>
                    <a className="coming-pill" href={item.href} target="_blank" rel="noreferrer">
                     Learn more ↗
                    </a>
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
                    href: "https://www.waterboards.ca.gov/drinking_water/certlic/labs/"
                  },
                  {
                    title: "Understand your report",
                    text: "Find plain-language help for reading a water system quality report.",
                    href: "https://www.epa.gov/ccr/understanding-your-annual-water-quality-report"
                  },
                  {
                    title: "Connect with local groups",
                    text: "Discover environmental justice organizations and neighborhood advocates.",
                    href: "https://ceja.org/"
                  },
                  {
                    title: "Ask a question",
                    text: "Prepare useful questions for your water provider or local representative.",
                    href: "https://nationalwaterservice.com/7-questions-to-ask-your-local-water-company/"

                  },
                ].map((item) => (
                  <article className="info-card" key={item.title}>
                    <span className="info-icon">↗</span>
                    <h2>{item.title}</h2>
                    <p>{item.text}</p>
                    <a className="coming-pill" href={item.href} target="_blank" rel="noreferrer">
                         Open resource ↗
                    </a>
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