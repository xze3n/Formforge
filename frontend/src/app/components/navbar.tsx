import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import { Menu, X, Shield } from "lucide-react";
import { Logo } from "./logo";
import { useAuth } from "../hooks/useAuth";
import { fetchUnresolvedCount } from "../services/adminService";

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [threatCount, setThreatCount] = useState(0);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    fetchUnresolvedCount().then(r => setThreatCount(r.unresolved)).catch(() => {});
    const id = setInterval(() => {
      fetchUnresolvedCount().then(r => setThreatCount(r.unresolved)).catch(() => {});
    }, 30_000);
    return () => clearInterval(id);
  }, [user]);

  const primaryLinks = [
    { href: "/scholarship-applications", label: "Applications" },
  ];

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="border-b bg-white relative overflow-x-clip">
      <div className="mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          {/* Left side - Logo, App Name, and Navigation */}
          <div className="flex items-center gap-3 sm:gap-8 min-w-0">
            {/* Logo and App Name */}
            <a href="/" className="flex items-center">
              <Logo />
            </a>

            {/* Navigation Links */}
            <div className="hidden md:flex items-center gap-6">
              {primaryLinks.map((link) => (
                <a key={link.href} href={link.href} className="text-gray-700 hover:text-purple-600 transition-colors">
                  {link.label}
                </a>
              ))}
            </div>
          </div>

          {/* Right side - auth controls */}
          <div className="flex items-center gap-2 sm:gap-4 shrink-0">
            {user ? (
              <>
                <span className="hidden sm:inline text-sm text-gray-600">
                  <span className="font-medium text-gray-900">{user.username}</span>
                  <span className="ml-1 text-xs bg-purple-100 text-purple-700 rounded px-1.5 py-0.5">{user.role}</span>
                </span>
                {user.role === "ADMIN" && (
                  <a href="/admin"
                    className="relative hidden sm:inline-flex items-center gap-1 text-sm px-2.5 py-1 rounded-md border border-purple-300 text-purple-700 hover:bg-purple-50 transition-colors">
                    <Shield className="w-3.5 h-3.5" />
                    Admin
                    {threatCount > 0 && (
                      <span className="absolute -top-1.5 -right-1.5 flex items-center justify-center w-4 h-4 rounded-full bg-red-500 text-white text-[9px] font-bold">
                        {threatCount > 9 ? "9+" : threatCount}
                      </span>
                    )}
                  </a>
                )}
                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline text-gray-700 hover:text-red-600 transition-colors text-sm px-2 py-1 rounded-md border border-gray-200 hover:border-red-300"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <a
                  href="/login"
                  className="inline text-gray-700 hover:text-purple-600 transition-colors text-base sm:text-base px-2 py-1 rounded-md"
                >
                  Login
                </a>
                <a
                  href="/register"
                  className="hidden md:inline bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 transition-colors"
                >
                  Register
                </a>
              </>
            )}
            <button
              type="button"
              className="md:hidden inline-flex items-center justify-center rounded-lg border border-gray-200 p-2 text-gray-700 hover:text-purple-600 hover:border-purple-300 transition-colors"
              onClick={() => setMobileMenuOpen((open) => !open)}
              aria-label={mobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
              aria-expanded={mobileMenuOpen}
              aria-controls="mobile-nav-menu"
            >
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div id="mobile-nav-menu" className="md:hidden border-t border-gray-100 py-3">
            <div className="flex flex-col gap-1">
              {primaryLinks.map((link) => (
                <a
                  key={link.href}
                  href={link.href}
                  className="px-2 py-2 rounded-md text-sm text-gray-700 hover:text-purple-600 hover:bg-purple-50 transition-colors"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {link.label}
                </a>
              ))}
              <div className="my-1 h-px bg-gray-100" />
              {user ? (
                <button
                  type="button"
                  onClick={() => { setMobileMenuOpen(false); handleLogout(); }}
                  className="px-2 py-2 rounded-md text-sm font-medium text-red-600 hover:bg-red-50 transition-colors text-left"
                >
                  Logout ({user.username})
                </button>
              ) : (
                <a
                  href="/register"
                  className="px-2 py-2 rounded-md text-sm font-medium text-purple-700 hover:text-purple-800 hover:bg-purple-50 transition-colors"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Register
                </a>
              )}
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}