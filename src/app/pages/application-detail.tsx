import { useState, useCallback, useRef } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { Button } from "../components/ui/button";
import { FileText, Trash2, ArrowLeft, TrendingUp, DollarSign, Award } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { useApplicationRepository } from "../hooks/useApplicationRepository";

export function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { getById, update, delete: deleteApplication } = useApplicationRepository();
  
  const application = getById(Number(id));
  const [academicYear, setAcademicYear] = useState(application?.academicYear || "");
  const [semester, setSemester] = useState(application?.semester || "");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [moneyParticles, setMoneyParticles] = useState<{ id: number; x: number; y: number; rotation: number; startX: number }[]>([]);
  const buttonRef = useRef<HTMLDivElement>(null);

  const spawnMoney = useCallback(() => {
    const halfWidth = buttonRef.current ? buttonRef.current.offsetWidth / 2 : 150;
    const particles = Array.from({ length: 24 }, (_, i) => ({
      id: Date.now() + i,
      startX: (Math.random() - 0.5) * 2 * halfWidth,
      x: (Math.random() - 0.5) * 400,
      y: -(Math.random() * 300 + 100),
      rotation: (Math.random() - 0.5) * 720,
    }));
    setMoneyParticles(particles);
    setTimeout(() => setMoneyParticles([]), 1500);
  }, []);

  if (!application) {
    return (
      <main className="flex-1 bg-gradient-to-tr from-purple-50 via-purple-100 to-yellow-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Application Not Found</h1>
          <p className="text-gray-600 mb-4">The application you're looking for doesn't exist.</p>
          <Button onClick={() => navigate("/scholarship-applications")}>
            Back to Applications
          </Button>
        </div>
      </main>
    );
  }

  const handleSaveChanges = () => {
    if (application) {
      update(application.id, {
        academicYear,
        semester: semester as "I" | "II",
      });
      setSuccessMessage("Changes saved successfully!");
      setTimeout(() => setSuccessMessage(null), 3000);
    }
  };

  const handleGenerateDossier = () => {
    spawnMoney();
    console.log("Generate dossier for application:", application?.id);
  };

  const handleDelete = () => {
    if (application && confirm("Are you sure you want to delete this application?")) {
      deleteApplication(application.id);
      navigate("/scholarship-applications");
    }
  };

  // Calculate eligibility score based on application type
  const eligibilityScore = application.type === "Merit" ? 82 : 68;
  const eligibilityLabel = eligibilityScore >= 75 ? "Strong candidate" : "Moderate candidate";

  return (
    <main className="flex-1 bg-gradient-to-br from-purple-50 via-purple-100 to-yellow-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Back Button */}
        <Button
          variant="ghost"
          onClick={() => navigate("/scholarship-applications")}
          className="mb-6 gap-2"
        >
          <ArrowLeft className="size-4" />
          Back to Applications
        </Button>

        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Application #{application.id}
          </h1>
          <p className="text-gray-600">
            View and manage your scholarship application
          </p>
        </div>

        {/* Two-column layout */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left Column - Application Details */}
          <div className="space-y-6">
            <div className="bg-white rounded-none shadow p-6">
              {successMessage && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-green-800">{successMessage}</p>
                </div>
              )}
              <h2 className="text-xl font-semibold text-gray-900 mb-6">
                Application Details
              </h2>
              
              <div className="space-y-4">
                {/* ID */}
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Application ID
                  </label>
                  <p className="text-lg text-gray-900">#{application.id}</p>
                </div>

                {/* Scholarship Type */}
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Scholarship Type
                  </label>
                  <span className={`inline-flex items-center px-3 py-1 rounded-none text-sm font-medium ${
                    application.type === "Merit" 
                      ? "bg-blue-100 text-blue-800" 
                      : "bg-green-100 text-green-800"
                  }`}>
                    {application.type}
                  </span>
                </div>

                {/* Academic Year - Editable */}
                <div>
                  <label htmlFor="academic-year" className="block text-sm font-medium text-gray-700 mb-2">
                    Academic Year
                  </label>
                  <Select value={academicYear} onValueChange={setAcademicYear}>
                    <SelectTrigger id="academic-year">
                      <SelectValue placeholder="Select academic year" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="2023/2024">2023/2024</SelectItem>
                      <SelectItem value="2024/2025">2024/2025</SelectItem>
                      <SelectItem value="2025/2026">2025/2026</SelectItem>
                      <SelectItem value="2026/2027">2026/2027</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Semester - Editable */}
                <div>
                  <label htmlFor="semester" className="block text-sm font-medium text-gray-700 mb-2">
                    Semester
                  </label>
                  <Select value={semester} onValueChange={setSemester}>
                    <SelectTrigger id="semester">
                      <SelectValue placeholder="Select semester" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="I">I</SelectItem>
                      <SelectItem value="II">II</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Created At */}
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Created Date
                  </label>
                  <p className="text-lg text-gray-900">{application.createdAt}</p>
                </div>

                {/* Status */}
                <div>
                  <label className="block text-sm font-medium text-gray-500 mb-1">
                    Status
                  </label>
                  <span className={`inline-flex items-center px-3 py-1 rounded-none text-sm font-medium ${
                    application.status === "Approved" 
                      ? "bg-green-100 text-green-800" 
                      : application.status === "Pending Action"
                        ? "bg-yellow-100 text-yellow-800"
                        : "bg-gray-100 text-gray-800"
                  }`}>
                    {application.status}
                  </span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="space-y-3 mt-6 pt-6 border-t border-gray-200">
                <Button
                  onClick={handleSaveChanges}
                  size="lg"
                  className="w-full rounded-none bg-purple-600 hover:bg-purple-700"
                >
                  Save Changes
                </Button>
                <Button
                  onClick={handleDelete}
                  variant="destructive"
                  size="lg"
                  className="w-full gap-2 rounded-none"
                >
                  <Trash2 className="size-5" />
                  Delete Application
                </Button>
              </div>
            </div>
          </div>

          {/* Right Column - Eligibility Score Card */}
          <div>
            <div className="bg-purple-600 text-white rounded-none shadow-lg p-8">
              <h2 className="text-2xl font-semibold mb-6">
                Eligibility Score
              </h2>

              {/* Large percentage */}
              <div className="text-center mb-6">
                <div className="text-7xl font-bold mb-2">
                  {eligibilityScore}%
                </div>
                <p className="text-xl text-purple-100">
                  {eligibilityLabel}
                </p>
              </div>

              {/* Breakdown of factors */}
              <div className="space-y-4 mb-8">
                <h3 className="text-lg font-semibold mb-4">
                  Score Breakdown
                </h3>

                {/* Academic Performance */}
                <div className="bg-purple-700 rounded-none p-4">
                  <div className="flex items-center gap-3 mb-2">
                    <TrendingUp className="size-5" />
                    <span className="font-medium">Academic Performance</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-purple-900 rounded-none h-2">
                      <div 
                        className="bg-yellow-400 h-2 rounded-none" 
                        style={{ width: '85%' }}
                      ></div>
                    </div>
                    <span className="text-sm font-medium">85%</span>
                  </div>
                </div>

                {/* Family Income */}
                <div className="bg-purple-700 rounded-none p-4">
                  <div className="flex items-center gap-3 mb-2">
                    <DollarSign className="size-5" />
                    <span className="font-medium">Family Income</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-purple-900 rounded-none h-2">
                      <div 
                        className="bg-yellow-400 h-2 rounded-none" 
                        style={{ width: application.type === "Social" ? '90%' : '70%' }}
                      ></div>
                    </div>
                    <span className="text-sm font-medium">{application.type === "Social" ? '90%' : '70%'}</span>
                  </div>
                </div>

                {/* Scholarship Type Match */}
                <div className="bg-purple-700 rounded-none p-4">
                  <div className="flex items-center gap-3 mb-2">
                    <Award className="size-5" />
                    <span className="font-medium">Scholarship Type Match</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-purple-900 rounded-none h-2">
                      <div 
                        className="bg-yellow-400 h-2 rounded-none" 
                        style={{ width: application.type === "Merit" ? '90%' : '75%' }}
                      ></div>
                    </div>
                    <span className="text-sm font-medium">{application.type === "Merit" ? '90%' : '75%'}</span>
                  </div>
                </div>
              </div>

              {/* Action Button */}
              <div className="relative" ref={buttonRef}>
                <Button
                  onClick={handleGenerateDossier}
                  size="lg"
                  className="w-full gap-2 bg-yellow-400 text-purple-900 hover:bg-yellow-500 rounded-none"
                >
                  <FileText className="size-5" />
                  Generate Dossier
                </Button>
                <AnimatePresence>
                  {moneyParticles.map((p) => (
                    <motion.span
                      key={p.id}
                      initial={{ opacity: 1, x: p.startX, y: 0, scale: 2.5, rotate: 0 }}
                      animate={{ opacity: 0, x: p.x, y: p.y, scale: 0.6, rotate: p.rotation }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 2.0, ease: "easeOut" }}
                      className="pointer-events-none absolute left-1/2 top-1/2 text-2xl"
                    >
                      💸
                    </motion.span>
                  ))}
                </AnimatePresence>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}