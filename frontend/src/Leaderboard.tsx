import { useEffect, useRef, useState } from "react";
import "./Leaderboard.css";

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
  { value: "TRACT", label: "Tracts (fewest first)" },
];

type LeaderboardProps = {
  selectedPwsid: string | null; // the system picked on the map or found by the address search
};

// null shows as a dash so a missing score never looks like a real 0.
function show(value: number | null) {
  return value == null ? "—" : value.toLocaleString();
}

export default function Leaderboard({ selectedPwsid }: LeaderboardProps) {
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
                <th scope="col">Tracts</th>
              </tr>
            </thead>
            <tbody className={loading ? "is-loading" : ""}>
              {rows.map((row) => {
                const selected = row.pwsid === selectedPwsid;
                return (
                  <tr
                    key={row.pwsid}
                    className={selected ? "selected" : ""}
                    aria-current={selected ? "true" : undefined}
                  >
                    <th scope="row">
                      <span className="system-name">{row.name}</span>
                      <span className="system-pwsid">{row.pwsid}</span>
                    </th>
                    <td>{show(row.population)}</td>
                    <td>{show(row.waterScore)}</td>
                    <td>{show(row.burdenScore)}</td>
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
