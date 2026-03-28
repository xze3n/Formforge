import { useState } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { useApplicationRepository } from "../hooks/useApplicationRepository";
import { ApplicationType } from "../types/application";

export function AddApplication() {
  const navigate = useNavigate();
  const { add } = useApplicationRepository();
  const [scholarshipType, setScholarshipType] = useState<ApplicationType | "">("");
  const [academicYear, setAcademicYear] = useState("");
  const [semester, setSemester] = useState<"I" | "II" | "">("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!scholarshipType || !academicYear || !semester) {
      setError("Please fill in all required fields");
      return;
    }
    
    try {
      add({
        type: scholarshipType as ApplicationType,
        academicYear,
        semester: semester as "I" | "II",
      });
      
      // Navigate back to applications page
      navigate("/scholarship-applications");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create application");
    }
  };

  return (
    <main className="flex-1 bg-gradient-to-bl from-purple-50 via-purple-100 to-yellow-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            New Scholarship Application
          </h1>
          <p className="text-gray-600">
            Create a new scholarship application
          </p>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow p-8">
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-800">{error}</p>
            </div>
          )}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Scholarship Type */}
            <div className="space-y-2">
              <label htmlFor="scholarship-type" className="block text-sm font-medium text-gray-700">
                Scholarship Type
              </label>
              <Select value={scholarshipType} onValueChange={(value) => setScholarshipType(value as ApplicationType)}>
                <SelectTrigger id="scholarship-type">
                  <SelectValue placeholder="Select scholarship type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Merit">Merit</SelectItem>
                  <SelectItem value="Social">Social</SelectItem>
                  <SelectItem value="Performance">Performance</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Academic Year */}
            <div className="space-y-2">
              <label htmlFor="academic-year" className="block text-sm font-medium text-gray-700">
                Academic Year
              </label>
              <Select value={academicYear} onValueChange={setAcademicYear} required>
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

            {/* Semester */}
            <div className="space-y-2">
              <label htmlFor="semester" className="block text-sm font-medium text-gray-700">
                Semester
              </label>
              <Select value={semester} onValueChange={setSemester} required>
                <SelectTrigger id="semester">
                  <SelectValue placeholder="Select semester" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="I">I</SelectItem>
                  <SelectItem value="II">II</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Buttons */}
            <div className="flex gap-4 pt-4">
              <Button type="submit" size="lg" className="flex-1">
                Fill in my data
              </Button>
              <Button
                type="button"
                variant="outline"
                size="lg"
                onClick={() => navigate("/scholarship-applications")}
              >
                Cancel
              </Button>
            </div>
          </form>
        </div>
      </div>
    </main>
  );
}
