# LA Water Watch frontend mockup

This folder contains an early React frontend based on the team's Figma design. The Leaflet map loads water-system boundaries from the Java backend, and the side panel defaults to LADWP (`CA1910067`) using real backend records. Other areas of the experience remain prototypes.

## What you can try

- Pan and zoom the water-system boundary map.
- View LADWP's details on initial load, then click a water-system polygon to show that system's name, water score, population served, and service connections.
- Identify the selected system by its thicker outline, which matches the region's color. Disconnected pieces of the same system are selected together.
- Open **View water overview** for environmental indicators and violation history.
- Use **Try again** if the panel cannot load its data.
- Switch between Explore, Environmental context, and Resources & action.
- Resize the browser to see the compact layout.
- Use Tab to reach the overview control, then Enter or Space to expand it.

The water score comes from the backend's CalEnviroScreen calculation, not a direct tap-water measurement. Missing values display as “No data” or “—”; zero is a valid score. Unresolved contaminant names display their codes. The panel no longer uses the sample Silver Lake score, ZIP, concern, or comparison.

The former map-layer switch has been removed. Polygon clicks now update the panel; numbered score markers will be added in a later step. Address suggestions call ArcGIS, but selecting an address still calls an unfinished backend route. The resources and context cards remain placeholders.

## Tools used

| Tool           | What it does here                                                                                                                                                                        |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **HTML**       | Provides the browser document and the root element where React displays the app.                                                                                                         |
| **CSS**        | Controls colors, spacing, responsive layout, map appearance, and keyboard focus styles.                                                                                                  |
| **JavaScript** | Runs the interaction logic in the browser.                                                                                                                                               |
| **TypeScript** | JavaScript with type checks while developing. Types help catch mistakes such as leaving a required field out of a neighborhood record. The browser runs the generated JavaScript.        |
| **React**      | A UI library (rather than a complete framework) for making the page from reusable pieces and redrawing it when its state changes. The main page is the `App` component in `src/App.tsx`. |
| **Vite**       | Starts the local development server with fast refresh and makes the static files for a production build.                                                                                 |
| **Node.js**    | Runs development tools on your computer. Visitors do not need Node.js to view a deployed static site.                                                                                    |
| **pnpm**       | Downloads and records the project's JavaScript packages. It is similar to npm; this project includes `pnpm-lock.yaml` to keep installs consistent.                                       |
| **Leaflet**    | Draws the interactive map. `react-leaflet` connects it to React; `@types/leaflet` and `@types/geojson` provide its TypeScript types.                                                      |
| **SVG**        | Leaflet uses SVG to draw the water-system boundaries over the map tiles. |

## Requirements

- Node.js **22.13 or newer**. This is required by pnpm 11; this mockup was built with Node.js 22.21.0.
- pnpm **11.25.0**, the package-manager version used for this mockup.
- A modern browser such as Microsoft Edge, Chrome, or Firefox.
- Internet access for the first package install, map tiles, and address suggestions. Water-system records come from the local backend, which downloads public datasets when needed.
- The Java backend running at `http://localhost:8080` to populate the map and side panel. See the repository-root README for backend setup.

Follow the setup steps below if these tools are not installed yet.

## Run it in your browser

These instructions work in PowerShell on Windows or a terminal on macOS/Linux. Each teammate runs the app on their own computer.

### 1. Install Node.js and pnpm

Install a supported LTS version of [Node.js](https://nodejs.org/en/download) that meets the requirement above. The standard installer includes **npm**, which we use to install pnpm. Close and reopen your terminal after installing Node.js, then check:

```sh
node --version
npm --version
```

Both commands should print a version number. If Node.js is older than 22.13, update it before continuing.

Install the project's pnpm version and confirm it is available:

```sh
npm install --global pnpm@11.25.0
pnpm --version
```

If pnpm 11.25.0 is already installed, skip its installation command. For other installation methods, see the [official pnpm instructions](https://pnpm.io/installation).

### 2. Get the project and install its packages

Clone the team's repository using Git or GitHub Desktop, or download and extract its ZIP from GitHub. Use the branch containing this `frontend` folder.

Open a terminal in the **repository root**: the folder containing both `frontend` and `water-api`. Then run:

```sh
cd frontend
pnpm install --frozen-lockfile
```

This downloads React, Vite, TypeScript, and the other packages into `node_modules`, using the exact versions in `pnpm-lock.yaml`. You do not need to install those packages individually. No frontend API key is required; the backend maintains its own local SQLite database.

If your terminal is already inside `frontend`, skip `cd frontend`.

#### Install Leaflet and its types

The map and address search use [Leaflet](https://leafletjs.com/) through `react-leaflet`. These packages are listed in `package.json`, so `pnpm install --frozen-lockfile` downloads them. If they are missing from `node_modules` (for example, `pnpm dev` reports that it cannot resolve `leaflet`), download Leaflet, the React bindings, and their TypeScript types from the `frontend` folder:

```sh
pnpm add leaflet react-leaflet
pnpm add -D @types/leaflet @types/geojson
```

`leaflet` draws the interactive map, `react-leaflet` lets React components control it, and `@types/leaflet` and `@types/geojson` give TypeScript the type definitions for Leaflet and the GeoJSON boundaries. Without the types, `pnpm build` fails its TypeScript checks.

### 3. Start the development server

From the `frontend` folder, run:

```sh
pnpm dev
```

Leave that terminal open and visit **http://127.0.0.1:3000** in your browser. Use this exact address because it is the frontend origin allowed by the backend. Saving a source-file edit automatically updates the preview. Press **Ctrl+C** in the terminal to stop the server.

For later sessions, open a terminal in `frontend` and run `pnpm dev`. Repeat `pnpm install --frozen-lockfile` after pulling changes to `package.json` or `pnpm-lock.yaml`, or if `node_modules` is missing.

### Common setup issues

- **A command is not recognized:** reopen your terminal after installing the tool, then check its version again.
- **Cannot find `package.json`:** make sure the terminal is inside the repository's `frontend` folder.
- **Port 3000 is already in use:** stop the other development server before starting this one. Vite is configured to keep port 3000 instead of automatically choosing another port.
- **The browser cannot connect:** confirm `pnpm dev` is still running and shows its local URL. `localhost` refers to the computer running the server.

## Build the static site

```sh
pnpm build
```

This first runs TypeScript's checks (`tsc -b`), then asks Vite to make the browser-ready files in `dist/`. Backend URLs currently point to `localhost:8080`; they will need deployment configuration before the team hosts the frontend remotely.

## A guided tour of the code

Start with these components:

1. `src/App.tsx` owns navigation, the address location, and the selected system ID, initially LADWP (`CA1910067`). It passes that ID and its update function to the map, and the same ID to the panel.
2. `src/SystemInfoPanel.tsx` accepts a system ID and requests `/api/systems/info-panel/{pwsid}`. It handles loading, non-success responses, retry, and cancellation of obsolete requests. Its expandable overview shows backend environmental indicators and dated violation records.
3. `src/MapView.tsx` requests `/api/systems/map` once and draws a GeoJSON layer for each system. A polygon click calls `onSelectSystem(pwsid)`, updating App's selection. The selected ID determines the outline and fill emphasis; selection does not re-fetch the boundaries. Map loading failures offer a retry button.
4. `src/AddressSearch.tsx` handles ArcGIS address suggestions. Its final address-check route is still unfinished.

Other useful files:

- `src/main.tsx` connects the React app to the empty `#root` element in `index.html` and loads the stylesheet.
- `src/styles.css` controls the visual design. The `@media` sections adjust the layout for narrower screens.
- `vite.config.ts` sets the local server to port 3000.
- `package.json` lists the run/build commands and packages, pins the pnpm version, and declares the minimum Node.js version.
- `tsconfig*.json` tells TypeScript how strictly to check the code.

The context and resource screens remain design previews. The panel displays the backend's current dataset, including its missing fields and unresolved contaminant names; it does not invent replacement values.
