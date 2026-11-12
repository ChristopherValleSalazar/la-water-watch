export interface CircularScoreProps {
  score: number; //current score
  maxScore?: number; //maximum score can only be 100
  size?: number;
  strokeWidth?: number; //thickness of the rim of the circle
  colorScore?: number; //0-100, higher is better; picks the ring color (defaults to score)
  displayText?: string;
}

// The ring color now comes from scoreColor.ts, shared with the map and the rest of the panel,
// instead of the three fixed colors that used to be here.
