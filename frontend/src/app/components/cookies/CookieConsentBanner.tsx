import { useCookieConsent } from "../../hooks/useCookieConsent";
import { Cookie } from "lucide-react";
import { Button } from "../ui/button";

export function CookieConsentBanner() {
  const { isPending, grant, deny } = useCookieConsent();

  if (!isPending) return null;

  return (
    <div className="fixed bottom-0 inset-x-0 z-50 p-3 sm:p-4" role="dialog" aria-label="Cookie consent">
      <div className="max-w-xl mx-auto bg-white border border-gray-200 rounded-lg shadow-2xl px-4 py-3">
        <div className="flex items-center gap-3">
          <Cookie className="h-5 w-5 shrink-0 text-purple-600" />

          <p className="flex-1 text-sm text-gray-600">
            We use cookies to remember your preferences and improve your experience.
          </p>

          <div className="flex shrink-0 gap-2">
            <Button
              onClick={grant}
              size="sm"
              className="bg-purple-600 hover:bg-purple-700 text-white"
            >
              Accept
            </Button>
            <Button
              onClick={deny}
              variant="outline"
              size="sm"
            >
              Decline
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
