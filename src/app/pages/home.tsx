import heroImage from 'figma:asset/219766762b942b2bfd94f339de3368a632f311f8.png';

export function Home() {
  return (
    <main className="flex-1 flex items-center justify-center bg-gradient-to-br from-purple-50 via-purple-100 to-yellow-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Left side - Text content */}
          <div className="text-left">
            <h1 className="text-5xl font-bold text-gray-900 mb-6">
              Helping students apply smarter
            </h1>
            <p className="text-xl text-gray-600 mb-8 leading-relaxed">
              FormForge helps you complete your
              scholarship applications in minutes. Enter your
              information once, upload documents, and
              automatically generate a ready-to-submit PDF
              dossier.
            </p>
          </div>

          {/* Right side - Image and buttons */}
          <div className="flex flex-col items-center gap-6">
            <img 
              src={heroImage} 
              alt="Stack of documents" 
              className="w-full max-w-md rounded-lg shadow-lg"
            />
            <div className="flex gap-4 justify-center w-full">
              <a
                href="/register"
                className="bg-purple-600 text-white px-8 py-3 rounded-none hover:bg-purple-700 transition-colors text-lg font-medium"
              >
                Get Started
              </a>
              <a
                href="#learn-more"
                className="bg-white text-purple-600 px-8 py-3 rounded-none border-2 border-purple-600 hover:bg-purple-50 transition-colors text-lg font-medium"
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