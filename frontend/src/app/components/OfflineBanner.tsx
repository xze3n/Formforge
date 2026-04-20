import { WifiOff, RefreshCw, Loader2, CheckCircle } from "lucide-react";
import { useNetworkStatus } from "../hooks/useNetworkStatus";
import { useEffect, useState } from "react";
import { networkService } from "../services/networkService";

export function OfflineBanner() {
  const { online, pendingCount, refreshPendingCount } = useNetworkStatus();
  const [syncing, setSyncing] = useState(false);
  const [showSyncDone, setShowSyncDone] = useState(false);

  // Re-check pending count periodically when offline
  useEffect(() => {
    if (online) {
      refreshPendingCount();
      return;
    }
    const id = setInterval(refreshPendingCount, 2_000);
    return () => clearInterval(id);
  }, [online, refreshPendingCount]);

  // Listen for syncing state via a simple DOM event
  useEffect(() => {
    const onSyncStart = () => {
      setSyncing(true);
      setShowSyncDone(false);
    };
    const onSyncEnd = () => {
      setSyncing(false);
      refreshPendingCount();
      setShowSyncDone(true);
      setTimeout(() => setShowSyncDone(false), 2500);
    };
    window.addEventListener("ff:sync:start", onSyncStart);
    window.addEventListener("ff:sync:end", onSyncEnd);
    return () => {
      window.removeEventListener("ff:sync:start", onSyncStart);
      window.removeEventListener("ff:sync:end", onSyncEnd);
    };
  }, [refreshPendingCount]);

  if (online && !syncing && !showSyncDone && pendingCount === 0) return null;

  return (
    <div
      className={`fixed top-4 right-4 w-max sm:top-auto sm:right-auto sm:bottom-4 sm:left-1/2 sm:-translate-x-1/2 z-50 flex items-center gap-2 sm:gap-3 rounded-lg px-3 py-2 sm:px-4 sm:py-2.5 text-xs sm:text-sm font-medium shadow-lg transition-colors whitespace-nowrap ${
        !online
          ? "bg-amber-500 text-white"
          : showSyncDone && !syncing
            ? "bg-green-600 text-white"
            : "bg-blue-600 text-white"
      }`}
    >
      {!online && (
        <>
          <WifiOff className="h-4 w-4 shrink-0" />
          <span>You are offline</span>
          {pendingCount > 0 && (
            <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs">
              {pendingCount} pending
            </span>
          )}
          <button
            onClick={() => networkService.check()}
            className="ml-1 rounded p-1 hover:bg-white/20 transition-colors"
            title="Retry connection"
          >
            <RefreshCw className="h-3.5 w-3.5" />
          </button>
        </>
      )}

      {online && syncing && (
        <>
          <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
          <span>Syncing changes…</span>
        </>
      )}

      {online && !syncing && showSyncDone && (
        <>
          <CheckCircle className="h-4 w-4 shrink-0" />
          <span>All changes synced</span>
        </>
      )}

      {online && !syncing && !showSyncDone && pendingCount > 0 && (
        <>
          <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
          <span>Syncing {pendingCount} pending change(s)…</span>
        </>
      )}
    </div>
  );
}
