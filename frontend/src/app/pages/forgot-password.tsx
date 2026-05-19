import { useState } from "react";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { useAuth } from "../hooks/useAuth";

export function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [devToken, setDevToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { forgotPassword } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const result = await forgotPassword(email);
      setSubmitted(true);
      // Dev mode: backend returns the token directly (no SMTP configured)
      if (result.resetToken) setDevToken(result.resetToken);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex-1 flex items-center justify-center bg-gradient-to-bl from-purple-50 via-purple-100 to-yellow-50 py-8">
      <div className="w-full max-w-md mx-auto px-4">
        <div className="bg-white rounded-lg shadow-lg p-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2 text-center">
            Reset Password
          </h1>
          <p className="text-gray-600 mb-8 text-center">
            Enter your email and we'll send you a reset link.
          </p>

          {submitted ? (
            <div className="space-y-4">
              <div className="rounded-md bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
                If that email is registered, a reset link has been generated.
              </div>
              {devToken && (
                <div className="rounded-md bg-yellow-50 border border-yellow-300 px-4 py-3 text-sm">
                  <p className="font-semibold text-yellow-800 mb-1">Dev mode — use this token:</p>
                  <a
                    href={`/reset-password?token=${devToken}`}
                    className="text-purple-600 hover:underline break-all"
                  >
                    /reset-password?token={devToken}
                  </a>
                </div>
              )}
              <div className="text-center">
                <a href="/login" className="text-sm text-purple-600 hover:text-purple-700 font-medium">
                  Back to login
                </a>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              {error && (
                <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}
              <div className="space-y-2">
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  Email address
                </label>
                <Input
                  id="email"
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" className="w-full" size="lg" disabled={loading}>
                {loading ? "Sending…" : "Send reset link"}
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
