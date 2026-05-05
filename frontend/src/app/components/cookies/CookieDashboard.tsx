import { useState } from "react";
import { useCookieConsent } from "../../hooks/useCookieConsent";
import { activityTracker, type ActivitySummary } from "../../services/activityTracker";
import { preferencesService, type UserPreferences } from "../../services/preferencesService";
import { Button } from "../ui/button";
import { Activity, Settings, Trash2, Eye, EyeOff } from "lucide-react";

export function CookieDashboard() {
  const { hasConsent, revoke } = useCookieConsent();
  const [visible, setVisible] = useState(false);
  const [summary, setSummary] = useState<ActivitySummary | null>(null);
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);

  if (!hasConsent) return null;

  const refresh = () => {
    setSummary(activityTracker.getSummary());
    setPrefs(preferencesService.getAll());
  };

  const toggle = () => {
    if (!visible) refresh();
    setVisible((v) => !v);
  };

  const handleClearActivity = () => {
    activityTracker.clearAll();
    refresh();
  };

  const handleRevokeConsent = () => {
    revoke();
    setVisible(false);
  };

  const formatTime = (ts: number | null) =>
    ts ? new Date(ts).toLocaleString() : "—";

  return (
    <div className="relative z-40">
      {/* Toggle button */}
      <Button
        onClick={toggle}
        size="sm"
        variant="outline"
        className="rounded-full shadow-lg gap-2"
        aria-label="Toggle cookie dashboard"
      >
        {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        Cookies
      </Button>

      {visible && summary && prefs && (
        <div className="absolute bottom-12 right-0 w-80 sm:w-96 bg-white border border-gray-200 rounded-xl shadow-2xl p-4 max-h-[70vh] overflow-y-auto">
          <h3 className="font-semibold text-gray-900 flex items-center gap-2 mb-3">
            <Activity className="h-4 w-4 text-purple-600" />
            Cookie Dashboard
          </h3>

          {/* Activity section */}
          <section className="mb-4">
            <h4 className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-1">
              <Activity className="h-3 w-3" /> Activity Tracking
            </h4>
            <dl className="text-xs space-y-1 text-gray-600">
              <div className="flex justify-between">
                <dt>Total sessions</dt>
                <dd className="font-medium">{summary.totalSessions}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Session started</dt>
                <dd className="font-medium">{formatTime(summary.currentSessionStart)}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Last active</dt>
                <dd className="font-medium">{formatTime(summary.lastActiveTimestamp)}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Pages visited (recent)</dt>
                <dd className="font-medium">{summary.pageVisits.length}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Click events (recent)</dt>
                <dd className="font-medium">{summary.clickEvents.length}</dd>
              </div>
            </dl>

            {summary.mostVisitedPages.length > 0 && (
              <div className="mt-2">
                <p className="text-xs font-medium text-gray-700 mb-1">Most visited:</p>
                <ul className="text-xs text-gray-500 space-y-0.5">
                  {summary.mostVisitedPages.slice(0, 5).map((p) => (
                    <li key={p.path} className="flex justify-between">
                      <span className="truncate">{p.path}</span>
                      <span className="font-medium ml-2">{p.count}×</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </section>

          {/* Preferences section */}
          <section className="mb-4">
            <h4 className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-1">
              <Settings className="h-3 w-3" /> Saved Preferences
            </h4>
            <dl className="text-xs space-y-1 text-gray-600">
              <div className="flex justify-between">
                <dt>View mode</dt>
                <dd className="font-medium">{prefs.viewMode}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Items per page</dt>
                <dd className="font-medium">{prefs.itemsPerPage}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Theme</dt>
                <dd className="font-medium">{prefs.theme}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Last visited</dt>
                <dd className="font-medium">{prefs.lastVisitedPage || "—"}</dd>
              </div>
            </dl>
          </section>

          {/* Actions */}
          <div className="flex flex-col gap-2 pt-2 border-t border-gray-100">
            <Button
              onClick={handleClearActivity}
              variant="outline"
              size="sm"
              className="gap-2 text-xs"
            >
              <Trash2 className="h-3 w-3" />
              Clear Activity Data
            </Button>
            <Button
              onClick={handleRevokeConsent}
              variant="destructive"
              size="sm"
              className="gap-2 text-xs"
            >
              <Trash2 className="h-3 w-3" />
              Revoke Consent & Delete All
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
