import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { useAuth } from "../hooks/useAuth";

export function ResetPassword() {
  const [searchParams] = useSearchParams();
  const tokenFromUrl = searchParams.get("token") ?? "";
  const [token, setToken] = useState(tokenFromUrl);
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { resetPassword } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await resetPassword(token, newPassword);
      setDone(true);
      setTimeout(() => navigate("/login", { replace: true }), 2500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Reset failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex-1 flex items-center justify-center bg-gradient-to-bl from-purple-50 via-purple-100 to-yellow-50 py-8">
      <div className="w-full max-w-md mx-auto px-4">
        <div className="bg-white rounded-lg shadow-lg p-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2 text-center">
            New Password
          </h1>
          <p className="text-gray-600 mb-8 text-center">
            Enter your reset token and choose a new password.
          </p>

          {done ? (
            <div className="rounded-md bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700 text-center">
              Password updated! Redirecting to login…
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              {error && (
                <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              {/* Token field — pre-filled when coming from the link */}
              <div className="space-y-2">
                <label htmlFor="token" className="block text-sm font-medium text-gray-700">
                  Reset token
                </label>
                <Input
                  id="token"
                  type="text"
                  placeholder="Paste your reset token"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
                  New password
                </label>
                <Input
                  id="newPassword"
                  type="password"
                  placeholder="At least 8 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  minLength={8}
                  required
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="confirm" className="block text-sm font-medium text-gray-700">
                  Confirm password
                </label>
                <Input
                  id="confirm"
                  type="password"
                  placeholder="Repeat your new password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  minLength={8}
                  required
                />
              </div>

              <Button type="submit" className="w-full" size="lg" disabled={loading}>
                {loading ? "Updating…" : "Set new password"}
              </Button>

              <div className="text-center">
                <a href="/login" className="text-sm text-purple-600 hover:text-purple-700 font-medium">
                  Back to login
                </a>
              </div>
            </form>
          )}
        </div>
      </div>
    </main>
  );
}
