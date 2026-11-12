import type { ReactNode } from "react";

export function Icon({ name, size = 25 }: { name: string; size?: number }) {
  const paths: Record<string, ReactNode> = {
    drop: <path d="M12 3C9 8 5 11 5 15a7 7 0 0 0 14 0c0-4-4-7-7-12Z" />,
    map: <><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3Z"/><path d="M9 3v15M15 6v15"/></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6"/>,
    pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/></>,
    history: <path d="M3 11a9 9 0 1 1 2 7M3 4v7h7M12 7v5l3 2"/>,
    resource: <path d="M4 4h7a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H4Zm16 0h-3a3 3 0 0 0-3 3M20 4v15h-2"/>,
    info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v1"/></>,
    notice: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v1"/></>,
    globe: <><circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18"/></>,
    locate: <><circle cx="12" cy="12" r="5"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/></>,
    civic: <path d="M3 21h18M5 18h14M6 9v9M10 9v9M14 9v9M18 9v9M3 6l9-4 9 4Z"/>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></>,
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] || paths.info}
    </svg>
  );
}