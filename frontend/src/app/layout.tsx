import { Outlet } from "react-router";
import { Navbar } from "./components/navbar";
import { CookieConsentBanner } from "./components/cookies/CookieConsentBanner";
import { CookieDashboard } from "./components/cookies/CookieDashboard";
import { useActivityTracker } from "./hooks/useActivityTracker";
import { OfflineBanner } from "./components/OfflineBanner";

export function Layout() {
  useActivityTracker();

  return (
    <div className="size-full flex flex-col">
      <Navbar />
      <Outlet />
      <CookieConsentBanner />
      <CookieDashboard />
      <OfflineBanner />
    </div>
  );
}
