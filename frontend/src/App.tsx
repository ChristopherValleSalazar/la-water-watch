import { useState } from "react";
import MapView from "./MapView";
import AddressSearch from "./AddressSearch";
import SystemInfoPanel from "./SystemInfoPanel";

// This is a TypeScript "union": activeNav can only be one of these three exact strings.
type Page = "explore" | "context" | "resources";

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
  // App shares one selection: MapView changes it, and SystemInfoPanel reads it.
  const [selectedSystemId, setSelectedSystemId] = useState("CA1910067");
  // Shared between AddressSearch (writes it) and MapView (reads it), so it lives here in the parent.
  const [location, setLocation] = useState<[number, number] | null>(null);

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
                {/* Address autocomplete lives in AddressSearch.tsx; it reports coordinates up to App. */}
                <AddressSearch onLocate={(lat, lon) => setLocation([lat, lon])} />
              </section>

              <section className="city-section" aria-labelledby="city-title">
                <div className="section-head">
                  <div>
                    <div className="title-row">
                      <h2 id="city-title">The citywide picture</h2>
                    </div>
                    <p>Different water stories. A shared landscape.</p>
                  </div>
                </div>

                <div className="map-and-card">
                  {/* The Leaflet map lives in MapView.tsx. */}
                  <div className="map-frame">
                    <MapView
                      marker={location}
                      selectedSystemId={selectedSystemId}
                      onSelectSystem={setSelectedSystemId}
                    />
                  </div>

                  {/* A new key resets the old details and overview when selection changes. */}
                  <SystemInfoPanel key={selectedSystemId} systemId={selectedSystemId} />
                </div>
              </section>
              <p className="demo-disclaimer">
                <span>ⓘ</span> Independent community tool · Scores summarize public
                datasets and are not measurements at your tap.
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
