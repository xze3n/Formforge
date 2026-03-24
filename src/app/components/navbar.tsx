import { GraduationCap } from "lucide-react";
import { Logo } from "./logo";

export function Navbar() {
  return (
    <nav className="border-b bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Left side - Logo, App Name, and Navigation */}
          <div className="flex items-center gap-8">
            {/* Logo and App Name */}
            <a href="/" className="flex items-center">
              <Logo />
            </a>

            {/* Navigation Links */}
            <div className="hidden md:flex items-center gap-6">
              <a href="/scholarship-applications" className="text-gray-700 hover:text-purple-600 transition-colors">
                Applications
              </a>
              <a href="/dashboard" className="text-gray-700 hover:text-purple-600 transition-colors">
                Dashboard
              </a>
              <a href="/personal-info" className="text-gray-700 hover:text-purple-600 transition-colors">
                Personal Info
              </a>
              <a href="/documents" className="text-gray-700 hover:text-purple-600 transition-colors">
                Documents
              </a>
            </div>
          </div>

          {/* Right side - Login and Register */}
          <div className="flex items-center gap-4">
            <a href="/login" className="text-gray-700 hover:text-purple-600 transition-colors">
              Login
            </a>
            <a 
              href="/register" 
              className="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 transition-colors"
            >
              Register
            </a>
          </div>
        </div>
      </div>
    </nav>
  );
}