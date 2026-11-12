import { useEffect, useRef, useState } from "react";
import { CircularScore } from "./CircularScore";
import { displayName, shortName } from "./systemName";
import { PIN_COLOR, PIN_DOT, PIN_PATH, PIN_VIEWBOX } from "./pinIcon";
import { BETTER_COLOR, WORSE_COLOR, scoreColor, scoreTint } from "./scoreColor";

// Shape of GET /api/systems/info-panel/{pwsid} (SystemInfoPanel on the backend). Only the fields we show.
type Violation = {
  violationId: string;
  isHealthBasedInd: string; // "Y" | "N"
  complPerBeginDate: string | null;
  complPerEndDate?: string | null;
  violationCategoryCode?: string;
  contaminantName?: string;
  complianceStatusCode?: string;
  publicNotificationTier?: number | null;
};

type SystemInfo = {
  pwsid: string;
  name: string;
  county: string | null;
  population: number | null;
  serviceConnections: number | null;
  phone: string | null;
  regulatingAgency: string | null;
  waterScore: number | null;
  burdenScore: number | null;
  tractCount: number | null;
  tractPopulation: number | null;
  avgAsthmaPctl: number | null;
  avgPovertyPctl: number | null;
  avgChildLeadPctl: number | null;
  avgToxReleasesPctl: number | null;
  avgGwThreatsPctl: number | null;
  avgCleanupSitesPctl: number | null;
  avgAirPm25Pctl: number | null;
  violations: Violation[];
};

// Shape of GET /api/systems/avg-score: the average over every tracked system.
type AvgScore = { avgWaterScore: number | null; avgBurdenScore: number | null };

const API_BASE_URL = "http://localhost:8080/api";

type Tab = "overview" | "waterRecord" | "people";

// Asks the panel to scroll itself into view once `pwsid` has loaded. App bumps `id` for every address
// search (any screen size) and every map click (mobileOnly: on desktop the panel is already beside the map).
export type ScrollRequest = { id: number; mobileOnly: boolean };

// The system serving the address the visitor searched, and that address as Census matched it
export type HomeSystem = { pwsid: string; address: string };

type InfoPanelProps = {
  pwsid: string;
  scrollRequest?: ScrollRequest | null;
  home?: HomeSystem | null;
  onSelect?: (pwsid: string) => void; // used by "Back to your system"
  onClose?: () => void;
};

// CalEnviroScreen percentiles, averaged over the census tracts each system serves. Higher = worse for all of them.
// topic = which explanation the (i) button opens (scoreExplanations.ts)
type TopicKey = "asthma" | "poverty" | "childLead" | "pm25" | "toxReleases" | "gwThreats" | "cleanupSites";

const PERCENTILES: { key: keyof SystemInfo; label: string; topic: TopicKey }[] = [
  { key: "avgAsthmaPctl", label: "Asthma", topic: "asthma" },
  { key: "avgPovertyPctl", label: "Poverty", topic: "poverty" },
  { key: "avgChildLeadPctl", label: "Child lead risk", topic: "childLead" },
  { key: "avgAirPm25Pctl", label: "Air PM2.5", topic: "pm25" },
  { key: "avgToxReleasesPctl", label: "Toxic releases", topic: "toxReleases" },
  { key: "avgGwThreatsPctl", label: "Groundwater threats", topic: "gwThreats" },
  { key: "avgCleanupSitesPctl", label: "Cleanup sites", topic: "cleanupSites" },
];

// Where each tab's numbers come from, shown at the bottom left of the panel. Links checked 2026-11-12.
const SOURCES = {
  ces: { label: "CalEnviroScreen 5.0 (July 2026), OEHHA", href: "https://oehha.ca.gov/calenviroscreen" },
  sdwis: {
    label: "EPA Safe Drinking Water Information System (SDWIS)",
    href: "https://www.epa.gov/ground-water-and-drinking-water/safe-drinking-water-information-system-sdwis-federal-reporting",
  },
  sabl: {
    label: "CA State Water Board, System Area Boundaries (SABL)",
    href: "https://gispublic.waterboards.ca.gov/portal/home/item.html?id=fbba842bf134497c9d611ad506ec48cc",
  },
};

function PanelSources({ sources }: { sources: { label: string; href: string }[] }) {
  return (
    <p className="panel-sources">
      <span>Source{sources.length > 1 ? "s" : ""}:</span>
      {sources.map((s) => (
        <a key={s.href} href={s.href} target="_blank" rel="noreferrer">
          {s.label} ↗
        </a>
      ))}
    </p>
  );
}

const fmt = (n: number | null | undefined) => (n == null ? "–" : n.toLocaleString("en-US"));

// 3,875,566 -> "3.9M", 712,357 -> "712K"
const compact = (n: number | null | undefined) =>
  n == null ? "–" : new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 }).format(n);

// "Water score vs. average (43)": a bar filled to this system's score, a tick at the average of all systems,
// and the difference on the right, colored by whether it's better or worse than average.
function AverageBar({
  label,
  value,
  avg,
  higherIsBetter,
}: {
  label: string;
  value: number | null;
  avg: number | null | undefined;
  higherIsBetter: boolean;
}) {
  if (value == null || avg == null) return null;
  const diff = value - avg;
  const better = higherIsBetter ? diff > 0 : diff < 0;
  const sign = diff > 0 ? "+" : diff < 0 ? "−" : "±";
  // Better than average is green, worse is red (darker shades than the bar, so the number doesn't blend in)
  const diffColor = diff === 0 ? undefined : better ? BETTER_COLOR : WORSE_COLOR;
  return (
    <div className="avg-bar">
      <div className="avg-bar-head">
        <span>
          {label} vs. average ({avg})
        </span>
        <b style={{ color: diffColor }}>
          {sign}
          {Math.abs(diff)}
        </b>
      </div>
      <div className="avg-bar-track">
        {/* The bar uses the score's own color from the shared scale (burden is flipped: higher = worse) */}
        <i style={{ width: `${value}%`, background: scoreColor(higherIsBetter ? value : 100 - value) }} />
        <span className="avg-bar-tick" style={{ left: `${avg}%` }} title={`Average: ${avg}`} />
      </div>
    </div>
  );
}

// Ring gauge with the number in the middle. badness drives the color, so "higher is better" scores pass 100 - value.
function Gauge({ value, badness, size }: { value: number | null; badness: number; size: "large" | "small" }) {
  return (
    <div className={`gauge gauge-${size}`}>
      <CircularScore
        score={value ?? 0}
        colorScore={100 - badness}
        displayText={value == null ? "–" : String(Math.round(value))}
        size={size === "large" ? 100 : 48}
        strokeWidth={size === "large" ? 9 : 11}
      />
    </div>
  );
}

function InfoButton({ topic, onOpen }: { topic: TopicKey; onOpen: (topic: TopicKey) => void }) {
  return (
    <button
      type="button"
      className="info-button"
      aria-label={`More information about ${topic}`}
      onClick={() => onOpen(topic)}
    >
      i
    </button>
  );
}

// Systems already loaded this session, by PWSID. The data only changes when the backend re-downloads it
// (at most daily), so going back to a system is instant and also works offline.
const infoCache = new Map<string, SystemInfo>();

// "1600 MAIN ST, SANTA MONICA, CA, 90401" -> "1600 Main St, Santa Monica, CA 90401"
function formatAddress(raw: string): string {
  const parts = raw.split(",").map((p) => p.trim());
  const [state, zip] = parts.slice(-2);
  const street = parts.slice(0, -2).map((p) => p.toLowerCase().replace(/\b[a-z]/g, (c) => c.toUpperCase()));
  return state && zip ? `${street.join(", ")}, ${state} ${zip}` : raw;
}

// The same pin as the address marker on the map (pinIcon.ts), so the two read as one thing
function PinIcon() {
  return (
    <svg viewBox={PIN_VIEWBOX} width="11" height="15" aria-hidden="true">
      <path d={PIN_PATH} fill={PIN_COLOR} />
      <circle {...PIN_DOT} fill="#fff" />
    </svg>
  );
}

// The line above the system name. Viewing the searched address's system: "YOUR WATER SYSTEM" plus the
// address it serves. Viewing another one after a search: the usual label plus a link back to yours.
function PanelHeader({
  pwsid,
  home,
  onSelect,
  onClose,
}: {
  pwsid: string;
  home?: HomeSystem | null;
  onSelect?: (pwsid: string) => void;
  onClose?: () => void;
}) {
  const isHome = home?.pwsid === pwsid;
  const homeName = home ? infoCache.get(home.pwsid)?.name : undefined;
  return (
    <div className="selected-water-header">
      <div className="header-left">
        {/* Your own system gets the same green highlight as its leaderboard row */}
        <span className={`field-label ${isHome ? "is-home" : ""}`}>
          {isHome ? "YOUR WATER SYSTEM" : "SELECTED WATER SYSTEM"}
        </span>
        {isHome && home && <p className="home-address">Serves {formatAddress(home.address)}</p>}
        {!isHome && home && onSelect && (
          <button type="button" className="back-home" onClick={() => onSelect(home.pwsid)}>
            <PinIcon /> Back to your system{homeName ? `: ${shortName(homeName)}` : ""}
          </button>
        )}
      </div>
    </div>
  );
}

// How long a switch between systems may take before the old panel is replaced by the skeleton
const SKELETON_DELAY_MS = 300;

// Gray placeholder shapes in the Scores tab's layout (title, tabs, two gauges, two bars, 7 small gauges),
// shown on first load instead of "Loading…" so the panel doesn't jump when the data arrives.
function PanelSkeleton() {
  return (
    <div className="panel-skeleton" aria-hidden="true">
      <span className="sk sk-title" />
      <span className="sk sk-line" />
      <span className="sk sk-tabs" />
      <div className="score-pair">
        {[0, 1].map((i) => (
          <div className="score-cell" key={i}>
            <span className="sk sk-circle-lg" />
            <div>
              <span className="sk sk-word" />
              <span className="sk sk-word short" />
            </div>
          </div>
        ))}
      </div>
      <span className="sk sk-bar" />
      <span className="sk sk-bar" />
      <ul className="pctl-grid">
        {PERCENTILES.map((p) => (
          <li key={p.key}>
            <span className="sk sk-circle-sm" />
            <span className="sk sk-word short" />
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function InfoPanel({ pwsid, scrollRequest, home, onSelect, onClose }: InfoPanelProps) {
  const [info, setInfo] = useState<SystemInfo | null>(null);
  const [avg, setAvg] = useState<AvgScore | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>("overview");
  // Switching systems keeps the old panel (dimmed) at first; only a load slower than SKELETON_DELAY_MS
  // swaps it for the skeleton. Fast switches then don't flash gray, slow ones clearly show loading.
  const [slowLoad, setSlowLoad] = useState(false);
  // Which explanation modal is open (null = none). One InfoModal below serves every (i) button.
  const [topic, setTopic] = useState<TopicKey | null>(null);
  // Bumped by the Retry button (and by coming back online) to run the fetches again
  const [attempt, setAttempt] = useState(0);
  const panelRef = useRef<HTMLElement>(null);
  const handledScroll = useRef(0);

  // Scroll the panel into view for a new request, but only once the requested system has loaded, so the
  // page scrolls to the finished panel and not to the old one that's about to change height.
  useEffect(() => {
    if (!scrollRequest || scrollRequest.id === handledScroll.current) return;
    if (loading || info?.pwsid !== pwsid) return;
    handledScroll.current = scrollRequest.id;
    if (scrollRequest.mobileOnly && !window.matchMedia("(max-width: 940px)").matches) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    // "nearest": scroll only as far as needed to show the whole panel; no movement if it's already visible
    panelRef.current?.scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth", block: "nearest" });
  }, [scrollRequest, loading, info, pwsid]);

  // The averages never change while the app runs, so fetch them once (again on retry if that first try failed).
  useEffect(() => {
    if (avg) return;
    fetch(`${API_BASE_URL}/systems/avg-score`)
      .then((res) => {
        if (!res.ok) throw new Error(`Backend responded ${res.status}`);
        return res.json();
      })
      .then((json: AvgScore) => setAvg(json))
      .catch((err) => console.error("Average score fetch failed:", err));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attempt]);

  // After a failure, retry by itself as soon as the device is back online
  useEffect(() => {
    if (!error) return;
    const retry = () => setAttempt((n) => n + 1);
    window.addEventListener("online", retry);
    return () => window.removeEventListener("online", retry);
  }, [error]);

  // Refetch whenever the selected system changes. The old panel stays on screen (dimmed) until the new one
  // arrives, and the abort stops a slow earlier response from overwriting a newer selection.
  useEffect(() => {
    setError("");
    const cached = infoCache.get(pwsid);
    if (cached) {
      setInfo(cached);
      setLoading(false);
      setSlowLoad(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    const slowTimer = setTimeout(() => setSlowLoad(true), SKELETON_DELAY_MS);
    const done = () => {
      clearTimeout(slowTimer);
      setSlowLoad(false);
    };
    fetch(`${API_BASE_URL}/systems/info-panel/${encodeURIComponent(pwsid)}`, { signal: controller.signal })
      .then((res) => {
        if (!res.ok) throw new Error(`Backend responded ${res.status}`);
        return res.json();
      })
      .then((json: SystemInfo) => {
        done();
        infoCache.set(json.pwsid, json);
        setInfo(json);
        setLoading(false);
      })
      .catch((err) => {
        if (err.name === "AbortError") return;
        done();
        console.error("Info panel fetch failed:", err);
        setError(
          navigator.onLine
            ? "Couldn't load this water system."
            : "You're offline. This system hasn't been loaded yet, so it isn't saved on this device.",
        );
        setLoading(false);
      });
    return () => {
      clearTimeout(slowTimer);
      controller.abort();
    };
  }, [pwsid, attempt]);

  // The requested system failed to load. Don't fall back to the previous system's data: the map already
  // highlights the new one, and showing the old name and scores next to it would be wrong.
  if (error) {
    return (
      <aside ref={panelRef} className="area-card info-panel" aria-label="Selected water system">
        <p className="panel-kicker">SELECTED WATER SYSTEM</p>
        <div className="panel-error-state" role="alert">
          <strong>{pwsid}</strong>
          <p>{error}</p>
          <button type="button" className="retry-button" onClick={() => setAttempt((n) => n + 1)}>
            Retry
          </button>
        </div>
      </aside>
    );
  }

  // First load, or a switch that's taking a while
  if (!info || slowLoad) {
    return (
      <aside ref={panelRef} className="area-card info-panel" aria-busy={loading} aria-label="Selected water system">
        <p className="panel-kicker">SELECTED WATER SYSTEM</p>
        <PanelSkeleton />
      </aside>
    );
  }

  const healthBased = info.violations.filter((v) => v.isHealthBasedInd === "Y").length;
  const latestViolation = info.violations
    .map((v) => v.complPerBeginDate)
    .filter((d): d is string => d != null)
    .sort()
    .at(-1);
  const concernLabel =
    info.waterScore == null ? "No score" : info.waterScore >= 67 ? "Lower concern" : info.waterScore >= 34 ? "Moderate concern" : "Higher concern";
  const overviewMetrics = [
    { value: compact(info.population), label: "People served" },
    { value: compact(info.serviceConnections), label: "Service connections" },
    { value: fmt(info.tractCount), label: "Census tracts" },
    { value: compact(info.tractPopulation), label: "Tract population" },
    { value: String(info.violations.length), label: "EPA violations" },
    { value: String(healthBased), label: "Health-based" },
  ];

  return (
    <aside
      ref={panelRef}
      className={`area-card info-panel ${loading ? "is-loading" : ""}`}
      aria-busy={loading}
      aria-label="Selected water system"
    >
      <PanelHeader pwsid={info.pwsid} home={home} onSelect={onSelect} onClose={onClose} />
      <h3 className="water-system-name panel-title">{shortName(info.name)}</h3>
      <p className="system-meta panel-sub">
        {info.pwsid} · {fmt(info.population)} people served
      </p>

      <div className="panel-tabs" role="tablist">
        <button role="tab" aria-selected={tab === "overview"} onClick={() => setTab("overview")}>
          Overview
        </button>
        <button role="tab" aria-selected={tab === "waterRecord"} onClick={() => setTab("waterRecord")}>
          Water record
        </button>
        <button role="tab" aria-selected={tab === "people"} onClick={() => setTab("people")}>
          People & place
        </button>
      </div>

      {tab === "overview" ? (
        <div className="panel-body" role="tabpanel">
          <div className="water-overview">
            <div className="score-ring">
              <Gauge value={info.waterScore} badness={100 - (info.waterScore ?? 100)} size="large" />
            </div>
            <div className="score-copy">
              {/* Dot and tint follow the water score's color on the map */}
              <div
                className="concern-pill"
                style={
                  {
                    background: scoreTint(info.waterScore),
                    "--concern-dot": scoreColor(info.waterScore),
                  } as React.CSSProperties
                }
              >
                {concernLabel}
              </div>
              <p>Water score: {info.waterScore == null ? "–" : `${info.waterScore} of 100`}</p>
              <p>Burden score: {info.burdenScore == null ? "–" : `${info.burdenScore} of 100`}</p>
            </div>
          </div>

          <p className="water-note">
            Water score = 100 minus the CalEnviroScreen drinking water percentile. Illustrative, not a safety determination.
          </p>

          <div className="water-comparison avg-bars">
            <AverageBar label="Water score" value={info.waterScore} avg={avg?.avgWaterScore} higherIsBetter />
            <AverageBar label="Burden score" value={info.burdenScore} avg={avg?.avgBurdenScore} higherIsBetter={false} />
          </div>
          <PanelSources sources={[SOURCES.ces]} />
        </div>
      ) : tab === "waterRecord" ? (
        <div className="panel-body" role="tabpanel">
          <div className="glance-grid record-summary">
            <div className="metric-tile"><strong>{info.violations.length}</strong><span>Recorded violations</span></div>
            <div className="metric-tile"><strong>{healthBased}</strong><span>Health-based</span></div>
            <div className="metric-tile">
              {latestViolation ? (
                <strong>{latestViolation}</strong>
              ) : (
                <strong className="metric-empty">No violations on record</strong>
              )}
              <span>Latest start date</span>
            </div>
          </div>
          <ul className="water-record-list">
            {info.violations.length === 0 && (
              <li className="record-empty">EPA has no violations on record for this water system.</li>
            )}
            {info.violations.map((violation) => (
              <li key={violation.violationId}>
                <div>
                  <strong>{violation.contaminantName || "Unspecified contaminant"}</strong>
                  <span>{violation.violationCategoryCode || "Violation"} · {violation.isHealthBasedInd === "Y" ? "Health-based" : "Not health-based"}</span>
                </div>
                <time>{violation.complPerBeginDate ?? "Date unavailable"}</time>
              </li>
            ))}
          </ul>
          <p className="water-note">Records are reported by the water system and regulatory sources; they do not alone determine current tap-water safety.</p>
          <PanelSources sources={[SOURCES.sdwis]} />
        </div>
      ) : (
        <div className="panel-body" role="tabpanel">
          <div className="glance-section">
            <span className="glance-label">PEOPLE & SYSTEM</span>
            <div className="glance-grid">
              {overviewMetrics.slice(0, 4).map((item) => (
                <div key={item.label} className="metric-tile">
                  <strong>{item.value}</strong>
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>
          <dl className="detail-list">
            <div>
              <dt>County</dt>
              <dd>{info.county ? displayName(info.county) : "–"}</dd>
            </div>
            <div>
              <dt>Regulator</dt>
              <dd>{info.regulatingAgency ? displayName(info.regulatingAgency) : "–"}</dd>
            </div>
            <div>
              <dt>Latest violation</dt>
              <dd>{latestViolation ?? "No violations on record"}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{info.phone ? <a href={`tel:${info.phone}`}>{info.phone}</a> : "–"}</dd>
            </div>
          </dl>
          <PanelSources sources={[SOURCES.sabl, SOURCES.ces]} />
        </div>
      )}
    </aside>
  );
}

