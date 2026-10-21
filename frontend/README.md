# LA Water Watch frontend mockup

This folder contains an early, interactive visual mockup based on the team's Figma design. It is meant to make the layout and basic interactions easy to review and discuss. It is not a finished product and does not look up real water-quality data.

## What you can try

- Search the built-in sample neighborhoods by name or ZIP code.
- Select the **90026** shortcut or click one of the map markers.
- Change the map layer, zoom the illustrative map, and open the sample water overview.
- Switch between Explore, Environmental context, and Resources & action.
- Resize the browser to see the compact layout.
- Use Tab to reach suggestion buttons and map markers, then Enter or Space to select one. Escape closes search suggestions.

The displayed scores, systems, and concerns are illustrative sample content. The location button does not ask for your location. Search, navigation, and map selection only update the page in your browser; they do not call the backend or send a search to a service.

The map-layer buttons currently demonstrate selection and update the legend text; they do not yet show different real map datasets. The resources and context cards are also placeholders for design review.

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
| **SVG**        | Draws the illustrative map as scalable shapes and markers. A marker is an interactive SVG group, so it can update the selected neighborhood.                                             |

## Requirements

- Node.js **22.13 or newer**. This is required by pnpm 11; this mockup was built with Node.js 22.21.0.
- pnpm **11.25.0**, the package-manager version used for this mockup.
- A modern browser such as Microsoft Edge, Chrome, or Firefox.
- Internet access for the first package install, so pnpm can download dependencies. The mockup itself does not fetch water data or remote fonts.

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

This downloads React, Vite, TypeScript, and the other packages into `node_modules`, using the exact versions in `pnpm-lock.yaml`. You do not need to install those packages individually. The mockup runs independently of the Java backend and requires no API keys or database.

If your terminal is already inside `frontend`, skip `cd frontend`.

### 3. Start the development server

From the `frontend` folder, run:

```sh
pnpm dev
```

Leave that terminal open and visit **http://localhost:3000** in your browser. Saving a source-file edit automatically updates the preview. Press **Ctrl+C** in the terminal to stop the server.

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

This first runs TypeScript's checks (`tsc -b`), then asks Vite to make the browser-ready files in `dist/`. That folder can be hosted by a static web host when the team is ready to deploy. A Docker development container is not needed for this small front-end-only mockup; it may be useful later if the team wants a consistent setup for both the React app and Spring Boot backend.

## A guided tour of the code

Start with `src/App.tsx`. The comments there explain the main TypeScript and React ideas as they appear in the mockup:

1. `type Area` describes the fields in one sample neighborhood. `type Page` is a TypeScript union, which limits navigation to the three page-name strings written there.
2. `areas` is a normal JavaScript array containing the seven sample neighborhoods. Its `Area[]` annotation asks TypeScript to check each item.
3. `useState(...)` stores values that can change while the app is open, such as the selected neighborhood, typed search, or active page. It returns a pair: the current value and a setter function. Calling its setter (for example, `setSelectedName`) tells React to update the screen. In `useState<Page>(...)`, `<Page>` is the value's TypeScript type.
4. `filter(...)` makes the suggestions from that small local array. `find(...)` locates one matching area. There is no network request behind either operation.
5. The returned JSX looks like HTML, with a few differences: dynamic JavaScript values go inside `{braces}`, and CSS classes are written as `className`.
6. Event handlers such as `onClick`, `onChange`, and `onSubmit` run when the user interacts. The same `selectArea` function is used by search results, the ZIP shortcut, and map markers so the map and detail card stay in sync.

The score comparison is calculated from the rounded average of the seven sample scores. `reduce(...)` adds those scores together; dividing by the number of records gives the average. This keeps the displayed difference consistent when sample scores change.

Other useful files:

- `src/main.tsx` connects the React app to the empty `#root` element in `index.html` and loads the stylesheet.
- `src/styles.css` controls the visual design. The `@media` sections adjust the layout for narrower screens.
- `vite.config.ts` sets the local server to port 3000.
- `package.json` lists the run/build commands and packages, pins the pnpm version, and declares the minimum Node.js version.
- `tsconfig*.json` tells TypeScript how strictly to check the code.

The map, score, and resource screens are intentionally simple and open to revision. The sample system assignments, scores, map geography, and resource descriptions should not be treated as verified facts.
