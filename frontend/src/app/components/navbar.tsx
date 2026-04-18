import { useState } from "react";
import { Menu, X } from "lucide-react";
import { Logo } from "./logo";

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const primaryLinks = [
    { href: "/scholarship-applications", label: "Applications" },
    { href: "/dashboard", label: "Dashboard" },
    { href: "/personal-info", label: "Personal Info" },
    { href: "/documents", label: "Documents" },
  ];

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

          {/* Right side - Login and Register */}
          <div className="flex items-center gap-2 sm:gap-4 shrink-0">
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
              <a
                href="/register"
                className="px-2 py-2 rounded-md text-sm font-medium text-purple-700 hover:text-purple-800 hover:bg-purple-50 transition-colors"
                onClick={() => setMobileMenuOpen(false)}
              >
                Register
              </a>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}