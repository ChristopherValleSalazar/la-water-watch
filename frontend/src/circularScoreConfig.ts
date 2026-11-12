export interface CircularScoreProps {
  score: number; //current score
  maxScore?: number; //maximum score can only be 100
  size?: number; 
  strokeWidth?: number; //thickness of the rim of the circle
  colorScore?: number;
  displayText?: string;
}

export const getScoreColorClass = (pct: number) : string => {
    if(pct < 0.4){
        return '#ef4444';
    }
    if(pct < 0.7){
        return '#facc15e5';
    }
    return '#22c55e';
}