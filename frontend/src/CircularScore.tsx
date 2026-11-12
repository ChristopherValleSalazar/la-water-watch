import React from "react";
import { type CircularScoreProps } from './circularScoreConfig';
import { scoreColor } from './scoreColor';
import './CircularScore.css'; 

export const CircularScore: React.FC<CircularScoreProps> = ({ 
    score, 
    colorScore,
    displayText,
    maxScore = 100, 
    size = 100, 
    strokeWidth = 10
}) => {
    const center = size / 2;
    const radius = center - strokeWidth / 2;
    const circumference = 2 * Math.PI * radius;
    //const offset = circumference - (score / maxScore) * circumference;

    const percentage = score / maxScore;
    const strokeDashoffset = circumference - percentage * circumference;
    // Same color as this score on the map (scoreColor.ts expects 0-100, higher is better)
    const dynamicColor = scoreColor(((colorScore ?? score) / maxScore) * 100);

    return (
        <div className="circular-score-container" style={{ width: size, height: size }}>
            <svg className="circular-score-svg" width={size} height={size}>
                {/* Track Circle */}
                <circle
                className="circular-score-track"
                cx={center}
                cy={center}
                r={radius}
                strokeWidth={strokeWidth}
                fill="transparent"
                />
                {/* Dynamic Progress Circle */}
                <circle
                className="circular-score-progress"
                cx={center}
                cy={center}
                r={radius}
                strokeWidth={strokeWidth}
                fill="transparent"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                style={{stroke: dynamicColor}}
                />
            </svg>
            {/* Centered Number */}
            <div className="circular-score-text-wrapper">
                <span className="circular-score-number">{displayText ?? score}</span>
            </div>
        </div>
    )
};
