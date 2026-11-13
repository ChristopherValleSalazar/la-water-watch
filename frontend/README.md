# LA Water Watch frontend

This React frontend connects the team's Figma design to the Java backend. The Leaflet map loads water-system boundaries and scores, and the side panel defaults to LADWP (`CA1910067`) using backend records. The address field starts empty.

## What you can try

- Pan and zoom the water-system boundary map, use its score legend, and return to the starting view with the recenter control. The starting view also limits how far out the map can zoom or pan.
- View LADWP's details on initial load, then click a water-system polygon or numbered badge to show that system's name, water score, population served, and service connections.
- Read each badge's water score; its border matches the region's color. Zoom in to separate nearby badges. The selected badge stays visible and has a stronger border.
- Hover over a polygon or badge to see the system name in one shared tooltip. Hovering does not change the selected panel.
- Identify the selected system by its thicker outline, which matches the region's color. Disconnected pieces of the same system are selected together.
- Explore the panel's **Overview**, **Water record**, and **People & place** tabs for scores, environmental indicators, violations, population, and connections.
- Use the panel's retry control if its data cannot load.
- Type an address, choose a suggestion, and see the matching water system, address pin, and panel. For a manual check, try `200 N Spring St, Los Angeles, CA 90012`; it is an example, not a prefilled address.
- Compare systems in the leaderboard; its selected row follows the same selection as the map and panel.
- Switch between Explore, Environmental context, and Resources & action.
- Resize the browser to see the compact layout.
- Use Tab to focus a score badge and show its name; press Enter or Space to select it.

The water score comes from the backend's CalEnviroScreen calculation, not a direct tap-water measurement. Missing values display as “No data” or “—”; zero is a valid score. Map regions, badges, and panel scores use the shared color scale in `src/scoreColor.ts`.

The former map-layer switch has been removed. Polygon and badge clicks use the same system-selection function. Badges use scores and interior label coordinates from the existing map response; a missing score displays “—”, and a missing position omits the badge while leaving polygon selection available. Address suggestions come from ArcGIS. Selecting a suggestion sends its address to `POST /api/address`; the backend geocodes it with Census and checks which water-system polygon covers the returned coordinates.

Check the displayed matched address: Census can return an approximate or different address. During verification, the `200 N Spring St` example matched `200 S SPRING ST`; those returned coordinates fall in LADWP's service area.

## Tools used

| Tool           | What it does here                                                                                                                                                                        |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **HTML**       | Provides the browser document and the root element where React displays the app.                                                                                                         |
| **CSS**        | Controls colors, spacing, responsive layout, map appearance, and keyboard focus styles.                                                                                                  |
| **JavaScript** | Runs the interaction logic in the browser.                                                                                                                                               |
| **TypeScript** | JavaScript with type checks while developing. Types help catch mistakes such as leaving a required field out of a water-system record. The browser runs the generated JavaScript.        |
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

Leave that terminal open and visit **http://127.0.0.1:3000** in your browser. The backend allows this origin and **http://localhost:3000**. Saving a source-file edit automatically updates the preview. Press **Ctrl+C** in the terminal to stop the server.

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

1. `src/App.tsx` owns navigation, the address location, and `selectedPwsid`, initially LADWP (`CA1910067`). It passes that ID and its update function to the map, and the same ID to the panel and leaderboard. `useState` stores the current value; calling its setter updates the screen.
2. `src/InfoPanel.tsx` replaces the earlier `SystemInfoPanel`. It requests `/api/systems/info-panel/{pwsid}` and system averages, handles loading and retry, caches viewed systems, and cancels obsolete panel requests. Its tabs show the selected system's scores, environmental indicators, water record, and people and system details.
3. `src/MapView.tsx` requests `/api/systems/map` and passes the data and selection callback to `SystemMapLayers`. It also owns the address pin, starting-view limits, recenter control, and score legend.
4. `src/SystemMapLayers.tsx` draws each system's polygons and HTML/CSS score badge. Both call `onSelectSystem(pwsid)` to update App's selection. It owns the single hover tooltip and adjusts badge spacing after zooming; it makes no data requests. Native buttons provide Enter/Space support, and their DOM elements are preserved when selection changes so keyboard focus stays in place.
5. `src/AddressSearch.tsx` handles ArcGIS suggestions, then posts `{ address }` to `/api/address`. The response supplies `matchedAddress`, `lat`, `lng`, `pwsid`, and `inServiceArea`. Its `onLocate` callback gives App the coordinates, matching ID, and address so the map and panel update together. A match outside the available polygons has coordinates but a null `pwsid`; an address Census cannot place has null address, coordinates, and ID.
6. `src/Leaderboard.tsx` compares backend system records and highlights the ID selected in App. `src/scoreColor.ts` supplies the common score colors used by the map, badges, legend, and panel.

Other useful files:

- `src/main.tsx` connects the React app to the empty `#root` element in `index.html` and loads the stylesheet.
- `src/styles.css` controls the visual design. The `@media` sections adjust the layout for narrower screens.
- `vite.config.ts` sets the local server to port 3000.
- `package.json` lists the run/build commands and packages, pins the pnpm version, and declares the minimum Node.js version.
- `tsconfig*.json` tells TypeScript how strictly to check the code.

The context and resource screens remain design previews. The panel displays the backend's current dataset, including its missing fields and unresolved contaminant names; it does not invent replacement values.
