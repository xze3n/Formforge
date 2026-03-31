import { useState, useRef, useCallback } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Trash2, Plus, ChevronLeft, ChevronRight, Table as TableIcon, PieChart, LayoutGrid, Play, Square } from "lucide-react";
import { usePreferences } from "../hooks/usePreferences";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";
import { PieChart as RechartsPie, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts";
import { useApplicationRepository } from "../hooks/useApplicationRepository";
import { faker } from "@faker-js/faker";
import type { ApplicationType, ApplicationStatus } from "../types/application";

export function ScholarshipApplications() {
  const navigate = useNavigate();
  const { applications, remove, add } = useApplicationRepository();
  const { preferences, setViewMode: persistViewMode, setItemsPerPage: persistItemsPerPage } = usePreferences();
  const [isPopulating, setIsPopulating] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const generateRandomApplication = useCallback(() => {
    const types: ApplicationType[] = ["Merit", "Social", "Performance"];
    const semesters: ("I" | "II")[] = ["I", "II"];
    const startYear = faker.number.int({ min: 2023, max: 2026 });
    const statuses: ApplicationStatus[] = ["Draft", "Pending Action", "Approved"];
    add({
      type: faker.helpers.arrayElement(types),
      academicYear: `${startYear}/${startYear + 1}`,
      semester: faker.helpers.arrayElement(semesters),
      status: faker.helpers.arrayElement(statuses),
    });
  }, [add]);

  const handleStartPopulating = useCallback(() => {
    if (intervalRef.current) return;
    setIsPopulating(true);
    intervalRef.current = setInterval(() => {
      generateRandomApplication();
    }, 1000);
  }, [generateRandomApplication]);

  const handleStopPopulating = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setIsPopulating(false);
  }, []);
  const [currentPage, setCurrentPage] = useState(1);
  const [viewMode, setViewModeLocal] = useState<"table" | "statistics" | "cards">(preferences.viewMode);
  const itemsPerPage = preferences.itemsPerPage;

  const setViewMode = (mode: "table" | "statistics" | "cards") => {
    setViewModeLocal(mode);
    persistViewMode(mode);
  };

  // Calculate pagination
  const totalPages = Math.ceil(applications.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentApplications = applications.slice(startIndex, endIndex);

  const handleDelete = (id: number) => {
    remove(id);
    // Adjust current page if necessary
    if (currentApplications.length === 1 && currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  const handleAddNew = () => {
    navigate("/add-application");
  };

  const goToPage = (page: number) => {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));
  };

  // Calculate statistics
  const totalApplications = applications.length;
  const draftApplications = applications.filter(app => app.status === "Draft").length;
  const approvedApplications = applications.filter(app => app.status === "Approved").length;
  const pendingApplications = applications.filter(app => app.status === "Pending Action").length;

  // Status distribution data
  const statusData = [
    { name: "Draft", value: draftApplications, color: "#9ca3af" },
    { name: "Pending Action", value: pendingApplications, color: "#fbbf24" },
    { name: "Approved", value: approvedApplications, color: "#10b981" },
  ];

  // Type distribution data
  const typeData = [
    { name: "Merit", value: applications.filter(app => app.type === "Merit").length, color: "#7c3aed" },
    { name: "Social", value: applications.filter(app => app.type === "Social").length, color: "#06b6d4" },
    { name: "Performance", value: applications.filter(app => app.type === "Performance").length, color: "#fbbf24" },
  ];

  return (
    <main className="flex-1 bg-gradient-to-tr from-purple-50 via-purple-100 to-yellow-50">
      <div className="max-w-screen-2xl mx-auto px-4 sm:px-6 lg:px-10 py-8">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Scholarship Applications
          </h1>
          <p className="text-gray-600">
            Manage and track all your scholarship applications
          </p>
        </div>

        {/* Add New Button */}
        <div className="mb-6 flex gap-2 flex-wrap">
          <Button onClick={handleAddNew} className="gap-2">
            <Plus className="size-4" />
            Add New Application
          </Button>
          {!isPopulating ? (
            <Button onClick={handleStartPopulating} variant="outline" className="gap-2">
              <Play className="size-4" />
              Populate Table
            </Button>
          ) : (
            <Button onClick={handleStopPopulating} variant="destructive" className="gap-2">
              <Square className="size-4" />
              Stop Populating
            </Button>
          )}
        </div>

        {/* View Mode Toggle */}
        <div className="mb-6 overflow-x-auto pb-1">
          <div className="flex w-max gap-2">
            <Button
              variant={viewMode === "table" ? "default" : "outline"}
              size="sm"
              onClick={() => setViewMode("table")}
              className="gap-2 shrink-0"
            >
              <TableIcon className="size-4" />
              Table View
            </Button>
            <Button
              variant={viewMode === "statistics" ? "default" : "outline"}
              size="sm"
              onClick={() => setViewMode("statistics")}
              className="gap-2 shrink-0"
            >
              <PieChart className="size-4" />
              Statistics View
            </Button>
            <Button
              variant={viewMode === "cards" ? "default" : "outline"}
              size="sm"
              onClick={() => setViewMode("cards")}
              className="gap-2 shrink-0"
            >
              <LayoutGrid className="size-4" />
              Cards View
            </Button>
          </div>
        </div>

        {/* Table */}
        {viewMode === "table" && (
          <div className="bg-white rounded-lg shadow-lg overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="min-w-16 text-base text-center px-6 sm:px-8">ID</TableHead>
                  <TableHead className="min-w-44 text-base px-6 sm:px-8">Scholarship Type</TableHead>
                  <TableHead className="min-w-36 text-base px-6 sm:px-8">Academic Year</TableHead>
                  <TableHead className="min-w-28 text-base px-6 sm:px-8">Semester</TableHead>
                  <TableHead className="min-w-36 text-base px-6 sm:px-8">Created At</TableHead>
                  <TableHead className="min-w-40 text-base px-6 sm:px-8">Status</TableHead>
                  <TableHead className="text-right min-w-24 text-base px-6 sm:px-8">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {currentApplications.map((app) => (
                  <TableRow 
                    key={app.id}
                    onClick={() => navigate(`/application/${app.id}`)}
                    className="cursor-pointer"
                  >
                    <TableCell className="font-medium text-base text-center py-4 px-6 sm:px-8">{app.id}</TableCell>
                    <TableCell className="px-6 sm:px-8">
                      <span className={`inline-flex items-center px-3 py-1 text-sm font-medium ${
                        app.type === "Merit" 
                          ? "bg-blue-100 text-blue-800" 
                          : app.type === "Social"
                            ? "bg-green-100 text-green-800"
                            : "bg-yellow-100 text-yellow-800"
                      }`}>
                        {app.type}
                      </span>
                    </TableCell>
                    <TableCell className="text-base py-4 px-6 sm:px-8">{app.academicYear}</TableCell>
                    <TableCell className="text-base py-4 px-6 sm:px-8">{app.semester}</TableCell>
                    <TableCell className="text-base py-4 px-6 sm:px-8">{app.createdAt}</TableCell>
                    <TableCell className="px-6 sm:px-8">
                      <span className={`inline-flex items-center px-3 py-1 text-sm font-medium ${
                        app.status === "Approved" 
                          ? "bg-green-100 text-green-800" 
                          : app.status === "Pending Action"
                            ? "bg-yellow-100 text-yellow-800"
                            : "bg-gray-100 text-gray-800"
                      }`}>
                        {app.status}
                      </span>
                    </TableCell>
                    <TableCell className="text-right py-4 px-6 sm:px-8">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(app.id);
                        }}
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      >
                        <Trash2 className="size-4" />
                        <span className="sr-only">Delete application {app.id}</span>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}

        {/* Statistics */}
        {viewMode === "statistics" && (
          <div className="space-y-6">
            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Total Applications Card */}
              <div className="bg-white shadow p-6 border-l-4 border-purple-600">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Total Applications</p>
                    <p className="text-3xl font-bold text-gray-900">{totalApplications}</p>
                  </div>
                  <div className="size-12 bg-purple-100 flex items-center justify-center">
                    <PieChart className="size-6 text-purple-600" />
                  </div>
                </div>
              </div>

              {/* Draft Applications Card */}
              <div className="bg-white shadow p-6 border-l-4 border-gray-400">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Draft Applications</p>
                    <p className="text-3xl font-bold text-gray-900">{draftApplications}</p>
                  </div>
                  <div className="size-12 bg-gray-100 flex items-center justify-center">
                    <PieChart className="size-6 text-gray-600" />
                  </div>
                </div>
              </div>

              {/* Approved Applications Card */}
              <div className="bg-white shadow p-6 border-l-4 border-green-500">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">Approved Applications</p>
                    <p className="text-3xl font-bold text-gray-900">{approvedApplications}</p>
                  </div>
                  <div className="size-12 bg-green-100 flex items-center justify-center">
                    <PieChart className="size-6 text-green-600" />
                  </div>
                </div>
              </div>
            </div>

            {/* Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Status Distribution Chart */}
              <div className="bg-white shadow p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Applications by Status</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <RechartsPie>
                    <Pie
                      data={statusData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => percent > 0 ? `${name}: ${(percent * 100).toFixed(0)}%` : ''}
                      outerRadius={100}
                      dataKey="value"
                    >
                      {statusData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </RechartsPie>
                </ResponsiveContainer>
              </div>

              {/* Type Distribution Chart */}
              <div className="bg-white shadow p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Applications by Type</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <RechartsPie>
                    <Pie
                      data={typeData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => percent > 0 ? `${name}: ${(percent * 100).toFixed(0)}%` : ''}
                      outerRadius={100}
                      dataKey="value"
                    >
                      {typeData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </RechartsPie>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        )}

        {/* Cards */}
        {viewMode === "cards" && (
          <div className="overflow-x-auto pb-2">
            <div className="flex gap-6 md:grid md:grid-cols-2 lg:grid-cols-3">
              {currentApplications.map((app) => (
                <div 
                  key={app.id} 
                  className="min-w-[300px] md:min-w-0 bg-white rounded-none shadow-lg hover:shadow-xl transition-shadow cursor-pointer"
                  onClick={() => navigate(`/application/${app.id}`)}
                >
                  <div className="p-6">
                  {/* Header with ID and Status */}
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-1">Application ID</p>
                      <p className="text-2xl font-bold text-gray-900">#{app.id}</p>
                    </div>
                    <span className={`inline-flex items-center px-3 py-1 rounded-none text-xs font-medium ${
                      app.status === "Approved" 
                        ? "bg-green-100 text-green-800" 
                        : app.status === "Pending Action"
                          ? "bg-yellow-100 text-yellow-800"
                          : "bg-gray-100 text-gray-800"
                    }`}>
                      {app.status}
                    </span>
                  </div>

                  {/* Scholarship Type */}
                  <div className="mb-4 pb-4 border-b border-gray-200">
                    <p className="text-sm font-medium text-gray-500 mb-2">Scholarship Type</p>
                    <span className={`inline-flex items-center px-3 py-1 rounded-none text-sm font-medium ${
                      app.type === "Merit" 
                        ? "bg-blue-100 text-blue-800" 
                        : app.type === "Social"
                          ? "bg-green-100 text-green-800"
                          : "bg-yellow-100 text-yellow-800"
                    }`}>
                      {app.type}
                    </span>
                  </div>

                  {/* Details Grid */}
                  <div className="space-y-3 mb-6">
                    <div>
                      <p className="text-xs font-medium text-gray-500 mb-1">Academic Year</p>
                      <p className="text-sm font-semibold text-gray-900">{app.academicYear}</p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-gray-500 mb-1">Semester</p>
                      <p className="text-sm font-semibold text-gray-900">{app.semester}</p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-gray-500 mb-1">Created Date</p>
                      <p className="text-sm font-semibold text-gray-900">{app.createdAt}</p>
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1 rounded-none"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/application/${app.id}`);
                      }}
                    >
                      View Details
                    </Button>
                    <Button
                      variant="destructive"
                      size="sm"
                      className="rounded-none"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(app.id);
                      }}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Pagination - Show in table and cards view */}
        {(viewMode === "table" || viewMode === "cards") && (
          <div className="mt-6 space-y-3">
            <div className="flex items-center gap-2 w-full">
              <Button
                variant="outline"
                size="sm"
                onClick={() => goToPage(currentPage - 1)}
                disabled={currentPage === 1}
                className="flex-1 sm:flex-none"
              >
                <ChevronLeft className="size-4" />
                Previous
              </Button>
              
              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let pageNum;
                  if (totalPages <= 5) {
                    pageNum = i + 1;
                  } else if (currentPage <= 3) {
                    pageNum = i + 1;
                  } else if (currentPage >= totalPages - 2) {
                    pageNum = totalPages - 4 + i;
                  } else {
                    pageNum = currentPage - 2 + i;
                  }
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={currentPage === pageNum ? "default" : "outline"}
                      size="sm"
                      onClick={() => goToPage(pageNum)}
                      className="min-w-9"
                    >
                      {pageNum}
                    </Button>
                  );
                })}
              </div>
              
              <Button
                variant="outline"
                size="sm"
                onClick={() => goToPage(currentPage + 1)}
                disabled={currentPage === totalPages}
                className="flex-1 sm:flex-none"
              >
                Next
                <ChevronRight className="size-4" />
              </Button>
            </div>

            <div className="text-sm text-gray-700 whitespace-nowrap">
              Showing <span className="font-medium">{startIndex + 1}</span> to{" "}
              <span className="font-medium">{Math.min(endIndex, applications.length)}</span> of{" "}
              <span className="font-medium">{applications.length}</span> results
            </div>
          </div>
        )}
      </div>
    </main>
  );
}