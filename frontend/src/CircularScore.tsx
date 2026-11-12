import React from "react";
import { getScoreColorClass, type CircularScoreProps } from './circularScoreConfig';
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
    const dynamicColorClass = getScoreColorClass(percentage);
    const colorPerentage = (colorScore ?? score) / maxScore;
    const dynamicColor = getScoreColorClass(
        Math.max(0, Math.min(1, colorPerentage))
    );

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
                className={"circular-score-progress " + dynamicColorClass}
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
