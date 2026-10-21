import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles.css";

// Find the empty <div id="root"> in index.html and let React draw the app inside it.
// The ! tells TypeScript that this element exists; index.html defines it for us.
ReactDOM.createRoot(document.getElementById("root")!).render(
  // StrictMode adds helpful development checks; it does not add visible UI.
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
