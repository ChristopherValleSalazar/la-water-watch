import { useEffect, useId, useState } from "react";

type Violation = {
  violationId: string | null;
  violationCategoryCode: string | null;
  contaminantCode: string | null;
  contaminantName: string | null;
  complianceStatusCode: string | null;
  complPerBeginDate: string | null;
  complPerEndDate: string | null;
  rtcDate: string | null;
};

type SystemDetails = {
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
  avgAsthmaPctl: number | null;
  avgPovertyPctl: number | null;
  avgChildLeadPctl: number | null;
  avgToxReleasesPctl: number | null;
  avgGwThreatsPctl: number | null;
  avgCleanupSitesPctl: number | null;
  avgAirPm25Pctl: number | null;
  violations: Violation[];
};

const countFormat = new Intl.NumberFormat("en-US");
const percentileFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 1 });

function count(value: number | null) {
  return value == null ? "No data" : countFormat.format(value);
}

function percentile(value: number | null) {
  return value == null ? "No data" : `${percentileFormat.format(value)} / 100`;
}

function date(value: string | null) {
  if (!value) return "Not recorded";
  const parsed = new Date(`${value}T00:00:00Z`);
  return Number.isNaN(parsed.getTime()) ? "Not recorded" : parsed.toLocaleDateString("en-US", {
    month: "short", day: "numeric", year: "numeric", timeZone: "UTC",
  });
}

function contaminant(violation: Violation) {
  const name = violation.contaminantName?.trim();
  // The current API has a placeholder for unresolved contaminant names.
  if (name && !/^(unknown|unkown)$/i.test(name)) return name;
  return violation.contaminantCode ? `Contaminant code ${violation.contaminantCode}` : "Contaminant not recorded";
}

// App passes the selected ID as a prop; this component owns its details request and display.
export default function SystemInfoPanel({ systemId }: { systemId: string }) {
  const headingId = useId();
  const [details, setDetails] = useState<SystemDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);

  // Re-run the request when App selects another system or the user presses Try again.
  useEffect(() => {
    const controller = new AbortController();
    let current = true;
    setDetails(null);
    setLoading(true);
    setError(false);

    async function load() {
      try {
        const response = await fetch(
          `http://localhost:8080/api/systems/info-panel/${encodeURIComponent(systemId)}`,
          { signal: controller.signal },
        );
        if (!response.ok) throw new Error("System information unavailable");
        const result: SystemDetails = await response.json();
        if (result.pwsid !== systemId || typeof result.name !== "string" || !Array.isArray(result.violations)) {
          throw new Error("Unexpected system information");
        }
        if (current) setDetails(result);
      } catch {
        if (current) setError(true);
      } finally {
        if (current) setLoading(false);
      }
    }

    void load();
    // Ignore old responses when a different system is selected or the panel closes.
    return () => {
      current = false;
      controller.abort();
    };
  }, [systemId, attempt]);

  const displayName = systemId === "CA1910067" ? "LADWP" : details?.name ?? "Water system";
  const indicators: [string, number | null][] = details ? [
    ["Overall environmental burden", details.burdenScore],
    ["Asthma", details.avgAsthmaPctl],
    ["Poverty", details.avgPovertyPctl],
    ["Child lead indicator", details.avgChildLeadPctl],
    ["Toxic releases", details.avgToxReleasesPctl],
    ["Groundwater threats", details.avgGwThreatsPctl],
    ["Cleanup sites", details.avgCleanupSitesPctl],
    ["Air particulate matter (PM2.5)", details.avgAirPm25Pctl],
  ] : [];

  return (
    <aside className="area-card" aria-labelledby={headingId} aria-busy={loading}>
      <div className="card-kicker"><span aria-hidden="true">⌖</span> WATER SYSTEM DETAILS</div>
      <div className="area-heading"><h3 id={headingId}>{displayName}</h3></div>
      <p className="system-line">System ID: {systemId}</p>

      {loading && <p className="panel-message" role="status">Loading water system information…</p>}
      {error && (
        <div className="panel-error">
          <p role="alert">We couldn’t load this water system’s information. Check that the water data service is running, then try again.</p>
          <button className="panel-retry" onClick={() => setAttempt((value) => value + 1)}>Try again</button>
        </div>
      )}

      {details && (
        <>
          {details.name !== displayName && <p className="system-full-name">{details.name}</p>}
          <p className="score-label">Water score</p>
          <div className="score-line">
            <strong>{details.waterScore ?? "—"}</strong>
            <span>{details.waterScore == null ? "No data" : "/ 100"}</span>
          </div>
          {details.waterScore != null && (
            <div className="score-bar" aria-hidden="true"><i style={{ width: `${Math.max(0, Math.min(100, details.waterScore))}%` }} /></div>
          )}
          <p className="score-explanation">Calculated from CalEnviroScreen drinking-water percentiles. Higher scores indicate a lower relative burden, not a measurement at your tap.</p>

          <dl className="system-facts">
            <div><dt>Population served</dt><dd>{count(details.population)}</dd></div>
            <div><dt>Service connections</dt><dd>{count(details.serviceConnections)}</dd></div>
          </dl>

          <details className="system-overview">
            <summary>View water overview</summary>
            <div className="overview-detail" role="region" aria-label="Water system overview" tabIndex={0}>
              <h4>System information</h4>
              <dl className="system-facts">
                <div><dt>County</dt><dd>{details.county || "Not recorded"}</dd></div>
                <div><dt>Phone</dt><dd>{details.phone || "Not recorded"}</dd></div>
                <div><dt>Regulating agency</dt><dd>{details.regulatingAgency || "Not recorded"}</dd></div>
                <div><dt>Matched census tracts</dt><dd>{count(details.tractCount)}</dd></div>
              </dl>
              <h4>Environmental indicators</h4>
              <p>Population-weighted CalEnviroScreen percentiles. Higher values indicate greater relative burden for each measure.</p>
              <dl className="system-facts indicator-facts">
                {indicators.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{percentile(value)}</dd></div>)}
              </dl>
              <h4>Violation history</h4>
              <p>{details.violations.length === 0 ? "No violation records in this dataset." : `${count(details.violations.length)} recorded violations in this dataset.`}</p>
              <ul className="violation-list">
                {details.violations.map((violation, index) => (
                  <li key={`${violation.violationId ?? "record"}-${index}`}>
                    <h5>{contaminant(violation)}</h5>
                    <dl className="system-facts">
                      <div><dt>Record ID</dt><dd>{violation.violationId || "Not recorded"}</dd></div>
                      <div><dt>Period begins</dt><dd>{date(violation.complPerBeginDate)}</dd></div>
                      <div><dt>Period ends</dt><dd>{date(violation.complPerEndDate)}</dd></div>
                      <div><dt>Return to compliance</dt><dd>{date(violation.rtcDate)}</dd></div>
                      <div><dt>Category code</dt><dd>{violation.violationCategoryCode || "Not recorded"}</dd></div>
                      <div><dt>Compliance status code</dt><dd>{violation.complianceStatusCode || "Not recorded"}</dd></div>
                    </dl>
                  </li>
                ))}
              </ul>
            </div>
          </details>
        </>
      )}
    </aside>
  );
}
