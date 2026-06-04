import { useState } from "react";
import { useNavigate } from "react-router";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";
import { Checkbox } from "../components/ui/checkbox";
import { useAuth } from "../hooks/useAuth";

export function Login() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [step, setStep] = useState<"credentials" | "2fa">("credentials");
  const [twoFaCode, setTwoFaCode] = useState("");
  const [twoFaMessage, setTwoFaMessage] = useState("");
  const [twoFaDevCode, setTwoFaDevCode] = useState<string | null>(null);
  const { login, verifyTwoFa, loading, error } = useAuth();
  const navigate = useNavigate();

  const handleCredentials = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await login({ identifier, password });
      setTwoFaMessage(res.message);
      if (res.devCode) setTwoFaDevCode(res.devCode);
      setStep("2fa");
    } catch {
      // error already set in useAuth
    }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await verifyTwoFa(twoFaCode.trim());
      navigate("/scholarship-applications", { replace: true });
    } catch {
      // error already set in useAuth
    }
  };

  return (
    <main className="flex-1 flex items-center justify-center bg-gradient-to-bl from-purple-50 via-purple-100 to-yellow-50 py-8">
      <div className="w-full max-w-md mx-auto px-4">
        <div className="bg-white rounded-lg shadow-lg p-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2 text-center">
            Welcome Back
          </h1>
          <p className="text-gray-600 mb-8 text-center">
            {step === "credentials"
              ? "Sign in to your account to continue"
              : "Two-factor verification"}
          </p>

          {/* ── Step 1: credentials ── */}
          {step === "credentials" && (
            <form onSubmit={handleCredentials} className="space-y-6">
              {error && (
                <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="space-y-2">
                <label htmlFor="identifier" className="block text-sm font-medium text-gray-700">
                  Email or username
                </label>
                <Input
                  id="identifier"
                  type="text"
                  placeholder="Enter your email or username"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <Input
                  id="password"
                  type="password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="remember"
                    checked={rememberMe}
                    onCheckedChange={(checked) => setRememberMe(checked === true)}
                  />
                  <label htmlFor="remember" className="text-sm text-gray-700 cursor-pointer">
                    Remember me
                  </label>
                </div>
                <a href="/forgot-password" className="text-sm text-purple-600 hover:text-purple-700 font-medium">
                  Forgot password?
                </a>
              </div>

              <Button type="submit" className="w-full" size="lg" disabled={loading}>
                {loading ? "Signing in…" : "Login"}
              </Button>

              <div className="text-center">
                <p className="text-sm text-gray-600">
                  Don't have an account?{" "}
                  <a href="/register" className="text-purple-600 hover:text-purple-700 font-medium">
                    Register here
                  </a>
                </p>
              </div>
            </form>
          )}

          {/* ── Step 2: 2FA code ── */}
          {step === "2fa" && (
            <form onSubmit={handleVerify} className="space-y-6">
              <div className="rounded-md bg-blue-50 border border-blue-200 px-4 py-3 text-sm text-blue-800">
                {twoFaMessage}
              </div>

              {twoFaDevCode && (
                <div className="rounded-md bg-yellow-50 border border-yellow-300 px-4 py-3 text-sm">
                  <p className="font-semibold text-yellow-800 mb-1">Dev mode — your 2FA code:</p>
                  <button
                    type="button"
                    onClick={() => setTwoFaCode(twoFaDevCode)}
                    className="font-mono text-lg tracking-widest text-purple-700 hover:text-purple-900 font-bold"
                  >
                    {twoFaDevCode}
                  </button>
                  <p className="text-yellow-700 text-xs mt-1">Click the code to auto-fill</p>
                </div>
              )}

              {error && (
                <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="space-y-2">
                <label htmlFor="twoFaCode" className="block text-sm font-medium text-gray-700">
                  Verification code
                </label>
                <Input
                  id="twoFaCode"
                  type="text"
                  placeholder="Enter the 6-digit code from the server logs"
                  value={twoFaCode}
                  onChange={(e) => setTwoFaCode(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <Button type="submit" className="w-full" size="lg" disabled={loading}>
                {loading ? "Verifying…" : "Verify"}
              </Button>

              <div className="text-center">
                <button
                  type="button"
                  onClick={() => { setStep("credentials"); setTwoFaCode(""); }}
                  className="text-sm text-purple-600 hover:text-purple-700 font-medium"
                >
                  ← Back to login
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </main>
  );
}