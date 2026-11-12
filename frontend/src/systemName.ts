// Name formatting shared by the info panel and the leaderboard, so a system reads the same in both.
// (Moved here from InfoPanel.tsx.)

// "LOS ANGELES-CITY, DEPT. OF WATER & POWER" -> "Los Angeles-City, Dept. of Water & Power"
export function displayName(raw: string): string {
  const small = new Set(["of", "and", "&"]);
  return raw
    .toLowerCase()
    .replace(/\s*-\s*/g, "-")
    .split(" ")
    .map((word, i) =>
      i > 0 && small.has(word) ? word : word.replace(/(^|-)([a-z])/g, (_, sep, ch) => sep + ch.toUpperCase()),
    )
    .join(" ");
}

// "SANTA MONICA-CITY, WATER DIVISION" -> "Santa Monica-City"
export const shortName = (raw: string) => displayName(raw.split(",")[0]);
