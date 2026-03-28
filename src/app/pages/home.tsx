/// <reference types="vite/client" />

import heroImage from '../../assets/219766762b942b2bfd94f339de3368a632f311f8.png';

export function Home() {
  return (
    <main className="flex-1 w-full bg-gradient-to-br from-purple-50 via-purple-100 to-yellow-50">
      <div className="max-w-7xl mx-auto px-6 sm:px-8 lg:px-10 py-10 sm:py-12 lg:py-16 w-full h-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 lg:gap-12 items-center h-full">
          
          {/* Left side */}
          <div className="text-left flex flex-col justify-center">
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 mb-4 sm:mb-6">
              Helping students apply smarter
            </h1>
            <p className="text-base sm:text-lg lg:text-xl text-gray-600 mb-4 sm:mb-6 lg:mb-8 leading-relaxed max-w-xl">
              FormForge helps you complete your
              scholarship applications in minutes. Enter your
              information once, upload documents, and
              automatically generate a ready-to-submit PDF
              dossier.
            </p>
          </div>

          {/* Right side */}
          <div className="flex flex-col items-center justify-center gap-4 sm:gap-5 lg:gap-6">
            <img 
              src={heroImage} 
              alt="Stack of documents" 
              className="w-full max-w-sm sm:max-w-md rounded-lg shadow-lg"
            />

            <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center w-full sm:w-auto">
              <a
                href="/register"
                className="bg-purple-600 text-white px-8 py-3 hover:bg-purple-700 transition-colors text-base sm:text-lg font-medium text-center"
              >
                Get Started
              </a>

              <a
                href="#learn-more"
                className="bg-white text-purple-600 px-8 py-3 border-2 border-purple-600 hover:bg-purple-50 transition-colors text-base sm:text-lg font-medium text-center"
              >
                Learn More
              </a>
            </div>
          </div>

        </div>
      </div>
    </main>
  );
}