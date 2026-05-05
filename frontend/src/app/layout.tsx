import { Outlet } from "react-router";
import { Navbar } from "./components/navbar";
import { CookieConsentBanner } from "./components/cookies/CookieConsentBanner";
import { CookieDashboard } from "./components/cookies/CookieDashboard";
import { useActivityTracker } from "./hooks/useActivityTracker";
import { OfflineBanner } from "./components/OfflineBanner";
import { ChatPanel } from "./components/ChatPanel";
import { useAuth } from "./hooks/useAuth";

export function Layout() {
  useActivityTracker();
  const { user } = useAuth();

  return (
    <div className="size-full flex flex-col">
      <Navbar />
      <Outlet />
      <CookieConsentBanner />
      <OfflineBanner />
      <div className="fixed bottom-5 right-5 z-50 flex items-center gap-3">
        <CookieDashboard />
        {user && <ChatPanel user={user} />}
      </div>
    </div>
  );
}
