// Every score color in the app comes from this file, so the map, the score ring, the concern pill and the
// average bars all agree: the same score is always the same color.
//
// All functions take a "goodness" score from 0 to 100: higher is better. The water score already is one;
// for a burden score or a percentile (higher = worse), pass 100 - value.

type Rgb = [number, number, number];

// Color stops along the score. The hues are clearly different (red, orange, gold, green), but balanced:
// the reds and oranges are toned down from neon and the greens are deep instead of pale, so neither end
// of the scale shouts over the other and a mid green (Santa Monica) still reads as green.
const STOPS: [score: number, rgb: Rgb][] = [
  [0, [166, 36, 58]], //  #a6243a crimson
  [20, [201, 68, 54]], // #c94436 brick red
  [40, [221, 122, 58]], // #dd7a3a soft orange
  [52, [217, 164, 65]], // #d9a441 gold
  [62, [143, 178, 83]], // #8fb253 leaf green
  [75, [79, 154, 85]], // #4f9a55 green
  [100, [43, 115, 72]], // #2b7348 deep green
];

const NO_SCORE: Rgb = [141, 149, 141]; // gray: a system with no matched tracts has a null score

// "Better / worse than average" text. Darker than any fill on the scale, so the +/- number stands out
// against the bar right below it instead of blending into it.
export const BETTER_COLOR = "#1f6b3a";
export const WORSE_COLOR = "#b02a2a";

function scoreRgb(score: number | null | undefined): Rgb {
  if (score == null || Number.isNaN(score)) return NO_SCORE;

  // Clamp so a bad value can't leave the gradient
  const s = Math.min(Math.max(score, 0), 100);

  // Find the two stops around the score and blend between them
  for (let i = 1; i < STOPS.length; i++) {
    const [hiScore, hi] = STOPS[i];
    if (s <= hiScore) {
      const [loScore, lo] = STOPS[i - 1];
      const t = (s - loScore) / (hiScore - loScore);
      return lo.map((c, k) => Math.round(c + (hi[k] - c) * t)) as Rgb;
    }
  }
  return STOPS[STOPS.length - 1][1];
}

const css = ([r, g, b]: Rgb) => `rgb(${r}, ${g}, ${b})`;

// The full color: map fill, score ring, the concern pill's dot, the average bars
export function scoreColor(score: number | null | undefined): string {
  return css(scoreRgb(score));
}

// The whole scale as a CSS gradient (worst on the left, best on the right), for the map legend.
// Built from the same STOPS, so the legend can never drift from the colors on the map.
export function scoreGradient(): string {
  return `linear-gradient(to right, ${STOPS.map(([score, rgb]) => `${css(rgb)} ${score}%`).join(", ")})`;
}

export const NO_SCORE_COLOR = css(NO_SCORE);

// A light tint of the same color (mixed 80% with white), for backgrounds like the concern pill,
// where the full color would be too heavy behind dark text
export function scoreTint(score: number | null | undefined): string {
  return css(scoreRgb(score).map((c) => Math.round(c + (255 - c) * 0.8)) as Rgb);
}
