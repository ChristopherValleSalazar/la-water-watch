import { useEffect, useRef, useState } from "react";
import "./Leaderboard.css";
import { shortName } from "./systemName";
import { scoreShade, scoreTint } from "./scoreColor";

const LEADERBOARD_URL = "http://localhost:8080/api/systems/leaderboard";

// Mirrors the backend's LeaderboardRow record. Scores can be null when a system matched no tracts.
type LeaderboardRow = {
  pwsid: string;
  name: string;
  waterScore: number | null;
  burdenScore: number | null;
  population: number | null;
  tracts: number | null;
  violations: number;
};

// Mirrors the backend's Sort enum. Any other value gets a 400 from the backend.
type Sort = "NONE" | "WATERSCORE" | "BURDENSCORE" | "VIOLATIONS" | "POPULATION" | "TRACT";

// Labels say which direction the backend sorts in (see WaterSystemService.leaderboard).
const sortOptions: { value: Sort; label: string }[] = [
  { value: "NONE", label: "Default order" },
  { value: "WATERSCORE", label: "Water score (highest first)" },
  { value: "BURDENSCORE", label: "Burden score (lowest first)" },
  { value: "VIOLATIONS", label: "Violations (fewest first)" },
  { value: "POPULATION", label: "Population (largest first)" },
  { value: "TRACT", label: "Neighborhoods scored (fewest first)" },
];

// Placeholder rows while the first response is on its way (there are 11 tracked systems)
const SKELETON_ROWS = 11;

type LeaderboardProps = {
  selectedPwsid: string | null; // the system picked on the map or found by the address search
  onSelectSystem: (pwsid: string) => void; // same handler as a map click
};

// Soft pills in the site's muted style: a light tint of the map color, with the number in a dark shade of it
const chipStyle = (goodness: number | null) => ({
  background: scoreTint(goodness),
  color: scoreShade(goodness),
  boxShadow: `inset 0 0 0 1px ${scoreTint(goodness, 0.55)}`,
});

// null shows as a dash so a missing score never looks like a real 0.
function show(value: number | null) {
  return value == null ? "—" : value.toLocaleString();
}

export default function Leaderboard({ selectedPwsid, onSelectSystem }: LeaderboardProps) {
  const [sort, setSort] = useState<Sort>("NONE");
  const [rows, setRows] = useState<LeaderboardRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Refetch whenever the sort changes; the backend does the sorting.
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");

    fetch(`${LEADERBOARD_URL}?sort=${sort}`, { signal: controller.signal })
      .then(async (res) => {
        const body = await res.json();
        // Errors come back from GlobalExceptionHandler with a message (e.g. an invalid sort value).
        if (!res.ok) throw new Error(body?.message ?? `Request failed (${res.status})`);
        return body as LeaderboardRow[];
      })
      .then((data) => {
        setRows(data);
        setLoading(false);
      })
      .catch((err) => {
        if (err.name === "AbortError") return; // a newer sort replaced this request
        console.error("Leaderboard fetch failed:", err);
        setError(err.message || "Could not load the leaderboard.");
        setLoading(false);
      });

    return () => controller.abort();
  }, [sort]);

  // Close the sort menu on a click outside it or on Escape.
  useEffect(() => {
    if (!menuOpen) return;
    function handleMouseDown(e: MouseEvent) {
      if (!menuRef.current?.contains(e.target as Node)) setMenuOpen(false);
    }
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") setMenuOpen(false);
    }
    document.addEventListener("mousedown", handleMouseDown);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("mousedown", handleMouseDown);
      document.removeEventListener("keydown", handleKey);
    };
  }, [menuOpen]);

  const currentLabel = sortOptions.find((o) => o.value === sort)?.label;

  return (
    <section className="leaderboard" aria-labelledby="leaderboard-title">
      <div className="leaderboard-head">
        <div>
          <h2 id="leaderboard-title">Water system leaderboard</h2>
          <p>Scores from CalEnviroScreen 5.0 (July 2026). Violations are EPA SDWIS history.</p>
        </div>

        <div className="sort-menu" ref={menuRef}>
          <button
            className="sort-button"
            onClick={() => setMenuOpen((open) => !open)}
            aria-haspopup="listbox"
            aria-expanded={menuOpen}
          >
            <span className="sort-button-label">Sort: {currentLabel}</span>
            <span aria-hidden="true">{menuOpen ? "▴" : "▾"}</span>
          </button>
          {menuOpen && (
            <ul className="sort-options" role="listbox" aria-label="Sort leaderboard by">
              {sortOptions.map((option) => (
                <li key={option.value} role="option" aria-selected={option.value === sort}>
                  <button
                    className={option.value === sort ? "active" : ""}
                    onClick={() => {
                      setSort(option.value);
                      setMenuOpen(false);
                    }}
                  >
                    {option.label}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {error ? (
        <p className="leaderboard-message">{error}</p>
      ) : (
        // The wrapper scrolls sideways on phones so the table never squeezes.
        <div className="leaderboard-scroll" aria-busy={loading}>
          <table>
            <thead>
              <tr>
                <th scope="col">Water system</th>
                <th scope="col">Population</th>
                <th scope="col">Water score</th>
                <th scope="col">Burden score</th>
                <th scope="col">Violations</th>
                <th scope="col" title="Census tracts are small areas the U.S. Census Bureau draws, about 4,000 people each. CalEnviroScreen scores every tract; this is how many make up this system's score.">
                  Neighborhoods
                  <span className="th-note">census tracts</span>
                </th>
              </tr>
            </thead>
            {/* Re-sorting keeps the old rows (dimmed by .is-loading) until the new order arrives */}
            <tbody className={loading && rows.length > 0 ? "is-loading" : ""}>
              {/* First load only: shimmering placeholder rows, one per tracked system */}
              {loading &&
                rows.length === 0 &&
                Array.from({ length: SKELETON_ROWS }, (_, i) => (
                  <tr key={`skeleton-${i}`} className="skeleton-row" aria-hidden="true">
                    <th scope="row">
                      <span className="sk sk-name" />
                    </th>
                    {[0, 1, 2, 3, 4].map((c) => (
                      <td key={c}>
                        <span className="sk sk-cell" />
                      </td>
                    ))}
                  </tr>
                ))}
              {rows.map((row) => {
                const selected = row.pwsid === selectedPwsid;
                return (
                  <tr
                    key={row.pwsid}
                    className={`clickable ${selected ? "selected" : ""}`}
                    aria-current={selected ? "true" : undefined}
                    tabIndex={0}
                    onClick={() => onSelectSystem(row.pwsid)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        onSelectSystem(row.pwsid);
                      }
                    }}
                  >
                    <th scope="row">
                      {/* Same short name as the info panel title; the full official name shows on hover */}
                      <span className="system-name" title={row.name}>
                        {shortName(row.name)}
                      </span>
                      <span className="system-pwsid">{row.pwsid}</span>
                    </th>
                    <td>{show(row.population)}</td>
                    <td>
                      <span className="score-chip" style={chipStyle(row.waterScore)}>
                        {show(row.waterScore)}
                      </span>
                    </td>
                    <td>
                      {/* Higher burden is worse, so its color is flipped */}
                      <span
                        className="score-chip"
                        style={chipStyle(row.burdenScore == null ? null : 100 - row.burdenScore)}
                      >
                        {show(row.burdenScore)}
                      </span>
                    </td>
                    <td>{show(row.violations)}</td>
                    <td>{show(row.tracts)}</td>
                  </tr>
                );
              })}
              {!loading && rows.length === 0 && (
                <tr>
                  <td colSpan={6} className="leaderboard-message">
                    No water systems loaded yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
