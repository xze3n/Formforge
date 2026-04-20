
  import { createRoot } from "react-dom/client";
  import App from "./app/App.tsx";
  import "./styles/index.css";
  import { networkService } from "./app/services/networkService.ts";

  networkService.start();

  createRoot(document.getElementById("root")!).render(<App />);
  