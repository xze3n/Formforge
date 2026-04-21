import { useState, useCallback, useEffect, useRef } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Trash2, Plus, ChevronLeft, ChevronRight, Table as TableIcon, PieChart, LayoutGrid, Play, Square } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../components/ui/alert-dialog";
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
import { useEnums } from "../hooks/useEnums";
import { useApplicationWebSocket } from "../hooks/useApplicationWebSocket";
import { generatorApi } from "../services/generatorApi";
import { applicationApi, type ApplicationPage } from "../services/applicationApi";
import type { Application } from "../types/application";

const mergeUniqueById = (current: Application[], incoming: Application[]): Application[] => {
  const seen = new Set(current.map((app) => app.id));
  const merged = [...current];
  for (const app of incoming) {
    if (!seen.has(app.id)) {
      merged.push(app);
      seen.add(app.id);
    }
  }
  return merged;
};

export function ScholarshipApplications() {
  const navigate = useNavigate();
  const { applications, remove, addFromServer } = useApplicationRepository();
  const { enums } = useEnums();
  const { preferences, setViewMode: persistViewMode } = usePreferences();
  const [isPopulating, setIsPopulating] = useState(false);
  const [tableCurrentPage, setTableCurrentPage] = useState(1);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [viewMode, setViewModeLocal] = useState<"table" | "statistics" | "cards">(preferences.viewMode);

  const itemsPerPage = preferences.itemsPerPage;

  // ── Table pagination state ────────────────────────────────────────────
  const [tableApplications, setTableApplications] = useState<Application[]>([]);
  const [tableTotalPages, setTableTotalPages] = useState(0);
  const [tableTotalElements, setTableTotalElements] = useState(0);
  const [tableLoading, setTableLoading] = useState(false);
  const [tableLoadError, setTableLoadError] = useState<string | null>(null);
  const tablePrefetchCacheRef = useRef<Map<number, ApplicationPage>>(new Map());
  const tablePrefetchInFlightRef = useRef<Set<number>>(new Set());

  // ── Cards pagination state ────────────────────────────────────────────
  const [cardsApplications, setCardsApplications] = useState<Application[]>([]);
  const [cardsLastLoadedPage, setCardsLastLoadedPage] = useState(-1);
  const [cardsTotalPages, setCardsTotalPages] = useState(0);
  const [cardsTotalElements, setCardsTotalElements] = useState(0);
  const [cardsInitialLoading, setCardsInitialLoading] = useState(false);
  const [cardsAppending, setCardsAppending] = useState(false);
  const [cardsLoadError, setCardsLoadError] = useState<string | null>(null);
  const cardsPrefetchCacheRef = useRef<Map<number, ApplicationPage>>(new Map());
  const cardsInFlightPagesRef = useRef<Set<number>>(new Set());
  const cardsPrefetchInFlightRef = useRef<Set<number>>(new Set());
  const cardsSentinelRef = useRef<HTMLDivElement | null>(null);

  const cardsHasMore = cardsLastLoadedPage + 1 < cardsTotalPages;

  const setViewMode = (mode: "table" | "statistics" | "cards") => {
    setViewModeLocal(mode);
    persistViewMode(mode);
  };

  const getPageFromLocalCache = useCallback((page: number): ApplicationPage | null => {
    if (applications.length === 0) return null;
    const start = page * itemsPerPage;
    if (start >= applications.length) return null;
    return {
      content: applications.slice(start, start + itemsPerPage),
      page,
      size: itemsPerPage,
      totalElements: applications.length,
      totalPages: Math.ceil(applications.length / itemsPerPage),
    };
  }, [applications, itemsPerPage]);

  const prefetchTablePage = useCallback(async (page: number, totalPages: number) => {
    if (page < 0 || page >= totalPages) return;
    if (tablePrefetchCacheRef.current.has(page)) return;
    if (tablePrefetchInFlightRef.current.has(page)) return;
    tablePrefetchInFlightRef.current.add(page);
    try {
      const prefetched = await applicationApi.getPage(page, itemsPerPage);
      tablePrefetchCacheRef.current.set(page, prefetched);
    } catch {
      // ignore prefetch failures
    } finally {
      tablePrefetchInFlightRef.current.delete(page);
    }
  }, [itemsPerPage]);

  const loadTablePage = useCallback(async (page: number) => {
    setTableLoading(true);
    setTableLoadError(null);
    tablePrefetchCacheRef.current.delete(page); // consume
    try {
      const cached = tablePrefetchCacheRef.current.get(page);
      const result = cached ?? await applicationApi.getPage(page, itemsPerPage);
      setTableApplications(result.content);
      setTableTotalPages(result.totalPages);
      setTableTotalElements(result.totalElements);
      // prefetch neighbours
      void prefetchTablePage(page + 1, result.totalPages);
      if (page > 0) void prefetchTablePage(page - 1, result.totalPages);
    } catch {
      const fallback = getPageFromLocalCache(page);
      if (fallback) {
        setTableApplications(fallback.content);
        setTableTotalPages(fallback.totalPages);
        setTableTotalElements(fallback.totalElements);
      } else {
        setTableLoadError("Failed to load applications.");
      }
    } finally {
      setTableLoading(false);
    }
  }, [getPageFromLocalCache, itemsPerPage, prefetchTablePage]);

  const resetCardsFeed = useCallback(() => {
    setCardsApplications([]);
    setCardsLastLoadedPage(-1);
    setCardsTotalPages(0);
    setCardsTotalElements(0);
    setCardsLoadError(null);
    cardsPrefetchCacheRef.current.clear();
    cardsInFlightPagesRef.current.clear();
    cardsPrefetchInFlightRef.current.clear();
  }, []);

  const getCardsPageFromLocalCache = getPageFromLocalCache;

  const prefetchCardsPage = useCallback(async (page: number, totalPages: number) => {
    if (page < 0 || page >= totalPages) return;
    if (cardsPrefetchCacheRef.current.has(page)) return;
    if (cardsPrefetchInFlightRef.current.has(page)) return;

    cardsPrefetchInFlightRef.current.add(page);
    try {
      const prefetched = await applicationApi.getPage(page, itemsPerPage);
      cardsPrefetchCacheRef.current.set(page, prefetched);
    } catch {
      // Ignore prefetch errors; explicit load path will surface failures.
    } finally {
      cardsPrefetchInFlightRef.current.delete(page);
    }
  }, [itemsPerPage]);

  const loadNextCardsPage = useCallback(async () => {
    const nextPage = cardsLastLoadedPage + 1;
    if (cardsAppending || cardsInFlightPagesRef.current.has(nextPage)) return;
    if (cardsTotalPages > 0 && nextPage >= cardsTotalPages) return;

    const isInitialPage = nextPage === 0;
    if (isInitialPage) {
      setCardsInitialLoading(true);
    } else {
      setCardsAppending(true);
    }
    setCardsLoadError(null);
    cardsInFlightPagesRef.current.add(nextPage);

    try {
      const cachedPage = cardsPrefetchCacheRef.current.get(nextPage);
      if (cachedPage) {
        cardsPrefetchCacheRef.current.delete(nextPage);
      }

      const pageResult = cachedPage ?? await applicationApi.getPage(nextPage, itemsPerPage);
      setCardsLastLoadedPage(pageResult.page);
      setCardsTotalPages(pageResult.totalPages);
      setCardsTotalElements(pageResult.totalElements);
      setCardsApplications((prev) => mergeUniqueById(prev, pageResult.content));

      // Keep one page ahead ready to reduce latency and duplicate requests.
      void prefetchCardsPage(pageResult.page + 1, pageResult.totalPages);
    } catch {
      const fallbackPage = getCardsPageFromLocalCache(nextPage);
      if (fallbackPage) {
        setCardsLastLoadedPage(fallbackPage.page);
        setCardsTotalPages(fallbackPage.totalPages);
        setCardsTotalElements(fallbackPage.totalElements);
        setCardsApplications((prev) => mergeUniqueById(prev, fallbackPage.content));
        setCardsLoadError(null);
      } else {
        setCardsLoadError("Failed to load applications.");
      }
    } finally {
      cardsInFlightPagesRef.current.delete(nextPage);
      if (isInitialPage) {
        setCardsInitialLoading(false);
      } else {
        setCardsAppending(false);
      }
    }
  }, [cardsAppending, cardsLastLoadedPage, cardsTotalPages, getCardsPageFromLocalCache, itemsPerPage, prefetchCardsPage]);

  const onApplicationCreated = useCallback((app: Application) => {
    addFromServer(app);
    if (viewMode === "table") {
      // Invalidate prefetch cache and reload the current page so the new item appears
      tablePrefetchCacheRef.current.clear();
      void loadTablePage(tableCurrentPage - 1);
    }
    if (viewMode === "cards") {
      setCardsApplications((prev) => mergeUniqueById(prev, [app]));
      setCardsTotalElements((prev) => {
        const nextTotal = prev + 1;
        setCardsTotalPages((prevPages) => Math.max(prevPages, Math.ceil(nextTotal / itemsPerPage)));
        return nextTotal;
      });
      cardsPrefetchCacheRef.current.clear();
    }
  }, [addFromServer, itemsPerPage, loadTablePage, tableCurrentPage, viewMode]);

  const onGeneratorStopped = useCallback(() => {
    console.log("Generator stopped signal received via WebSocket");
    setIsPopulating(false);
  }, []);

  useApplicationWebSocket({ onApplicationCreated, onGeneratorStopped });

  const handleStartPopulating = useCallback(async () => {
    setIsPopulating(true);
    try {
      await generatorApi.start();
    } catch (err) {
      console.error("Failed to start generator:", err);
      setIsPopulating(false);
    }
  }, []);

  const handleStopPopulating = useCallback(async () => {
    try {
      await generatorApi.stop();
    } catch {
      // ignore
    }
    setIsPopulating(false);
  }, []);

  // Table pagination — driven by API state
  const totalPages = tableTotalPages || Math.ceil(applications.length / itemsPerPage);
  const currentApplications = tableApplications;

  // Load table page on mount and when page/itemsPerPage changes
  useEffect(() => {
    if (viewMode !== "table") return;
    tablePrefetchCacheRef.current.clear();
    tablePrefetchInFlightRef.current.clear();
    void loadTablePage(tableCurrentPage - 1);
  }, [viewMode, tableCurrentPage, itemsPerPage, loadTablePage]);

  // Offline recovery for table: if API load failed but cache arrived later
  useEffect(() => {
    if (viewMode !== "table") return;
    if (tableLoading || tableApplications.length > 0 || applications.length === 0) return;
    const fallback = getPageFromLocalCache(tableCurrentPage - 1);
    if (!fallback) return;
    setTableApplications(fallback.content);
    setTableTotalPages(fallback.totalPages);
    setTableTotalElements(fallback.totalElements);
  }, [applications, getPageFromLocalCache, tableApplications.length, tableCurrentPage, tableLoading, viewMode]);

  useEffect(() => {
    if (viewMode !== "cards") return;
    resetCardsFeed();

    const loadInitialCardsPage = async () => {
      setCardsInitialLoading(true);
      setCardsLoadError(null);
      cardsInFlightPagesRef.current.add(0);
      try {
        const firstPage = await applicationApi.getPage(0, itemsPerPage);
        setCardsApplications(firstPage.content);
        setCardsLastLoadedPage(firstPage.page);
        setCardsTotalPages(firstPage.totalPages);
        setCardsTotalElements(firstPage.totalElements);
        void prefetchCardsPage(firstPage.page + 1, firstPage.totalPages);
      } catch {
        const fallbackFirstPage = getCardsPageFromLocalCache(0);
        if (fallbackFirstPage) {
          setCardsApplications(fallbackFirstPage.content);
          setCardsLastLoadedPage(fallbackFirstPage.page);
          setCardsTotalPages(fallbackFirstPage.totalPages);
          setCardsTotalElements(fallbackFirstPage.totalElements);
          setCardsLoadError(null);
        } else {
          setCardsLoadError("Failed to load applications.");
        }
      } finally {
        cardsInFlightPagesRef.current.delete(0);
        setCardsInitialLoading(false);
      }
    };

    void loadInitialCardsPage();
  }, [viewMode, itemsPerPage, prefetchCardsPage, resetCardsFeed, getCardsPageFromLocalCache]);

  useEffect(() => {
    if (viewMode !== "cards") return;
    if (cardsInitialLoading || cardsApplications.length > 0 || applications.length === 0) return;

    const fallbackFirstPage = getCardsPageFromLocalCache(0);
    if (!fallbackFirstPage) return;

    setCardsApplications(fallbackFirstPage.content);
    setCardsLastLoadedPage(fallbackFirstPage.page);
    setCardsTotalPages(fallbackFirstPage.totalPages);
    setCardsTotalElements(fallbackFirstPage.totalElements);
    setCardsLoadError(null);
  }, [applications, cardsApplications.length, cardsInitialLoading, getCardsPageFromLocalCache, viewMode]);

  useEffect(() => {
    if (viewMode !== "cards") return;
    if (!cardsHasMore) return;

    const target = cardsSentinelRef.current;
    if (!target) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry?.isIntersecting && !cardsAppending) {
          void loadNextCardsPage();
        }
      },
      { rootMargin: "300px 0px" }
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [viewMode, cardsHasMore, cardsAppending, loadNextCardsPage]);

  const handleDelete = (id: number) => {
    setDeleteId(id);
  };

  const confirmDelete = async () => {
    if (deleteId === null) return;
    await remove(deleteId);
    if (viewMode === "table") {
      // If we deleted the last item on this page, go back one page then reload
      const newPage = tableApplications.length === 1 && tableCurrentPage > 1
        ? tableCurrentPage - 1
        : tableCurrentPage;
      setTableCurrentPage(newPage);
      tablePrefetchCacheRef.current.clear();
      void loadTablePage(newPage - 1);
    }
    if (viewMode === "cards") {
      setCardsApplications((prev) => prev.filter((app) => app.id !== deleteId));
      setCardsTotalElements((prev) => {
        const nextTotal = Math.max(0, prev - 1);
        setCardsTotalPages(Math.ceil(nextTotal / itemsPerPage));
        return nextTotal;
      });
      cardsPrefetchCacheRef.current.clear();
    }
    setDeleteId(null);
  };

  const handleAddNew = () => {
    navigate("/add-application");
  };

  const goToPage = (page: number) => {
    const clamped = Math.max(1, Math.min(page, totalPages));
    setTableCurrentPage(clamped);
    // loadTablePage is triggered by the useEffect watching tableCurrentPage
  };

  // Color palettes for dynamic chart data
  const statusColors: Record<string, string> = {
    "Draft": "#9ca3af", "Pending Action": "#fbbf24", "Approved": "#10b981",
  };
  const typeColors: Record<string, string> = {
    "Merit": "#7c3aed", "Social": "#06b6d4", "Performance": "#fbbf24",
  };
  const fallbackColors = ["#7c3aed", "#06b6d4", "#fbbf24", "#10b981", "#f43f5e", "#9ca3af"];

  // Calculate statistics
  const totalApplications = applications.length;

  // Status distribution data — driven by backend enums
  const statusData = enums.statuses.map((status, i) => ({
    name: status,
    value: applications.filter(app => app.status === status).length,
    color: statusColors[status] ?? fallbackColors[i % fallbackColors.length],
  }));

  // Type distribution data — driven by backend enums
  const typeData = enums.types.map((type, i) => ({
    name: type,
    value: applications.filter(app => app.type === type).length,
    color: typeColors[type] ?? fallbackColors[i % fallbackColors.length],
  }));

  return (
    <main className="flex-1 bg-gradient-to-tr from-purple-50 via-purple-100 to-yellow-50">
      <AlertDialog open={deleteId !== null} onOpenChange={(open) => { if (!open) setDeleteId(null); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Application</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete application #{deleteId}? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDelete} className="bg-red-600 hover:bg-red-700">Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
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
          <div className="bg-white rounded-lg shadow-lg overflow-x-auto" aria-busy={tableLoading}>
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

              {/* Dynamic Status Cards */}
              {statusData.map((status) => (
                <div key={status.name} className="bg-white shadow p-6 border-l-4" style={{ borderLeftColor: status.color }}>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-600 mb-1">{status.name} Applications</p>
                      <p className="text-3xl font-bold text-gray-900">{status.value}</p>
                    </div>
                    <div className="size-12 flex items-center justify-center" style={{ backgroundColor: status.color + "20" }}>
                      <PieChart className="size-6" style={{ color: status.color }} />
                    </div>
                  </div>
                </div>
              ))}
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
          <div className="overflow-x-auto pb-2 space-y-4">
            <div className="flex gap-6 md:grid md:grid-cols-2 lg:grid-cols-3">
              {cardsApplications.map((app) => (
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

            {cardsInitialLoading && (
              <div className="text-sm text-gray-700">Loading applications...</div>
            )}

            {cardsLoadError && (
              <div className="text-sm text-red-700">{cardsLoadError}</div>
            )}

            <div ref={cardsSentinelRef} className="h-1" aria-hidden="true" />

            {cardsAppending && (
              <div className="text-sm text-gray-700">Loading more applications...</div>
            )}

            {!cardsHasMore && cardsApplications.length > 0 && (
              <div className="text-sm text-gray-700 whitespace-nowrap">
                Showing <span className="font-medium">{cardsApplications.length}</span> of{" "}
                <span className="font-medium">{cardsTotalElements}</span> results
              </div>
            )}
          </div>
        )}

        {/* Pagination - Table view only */}
        {viewMode === "table" && (
          <div className="mt-6 space-y-3">
            <div className="flex items-center gap-2 w-full">
              <Button
                variant="outline"
                size="sm"
                onClick={() => goToPage(tableCurrentPage - 1)}
                disabled={tableCurrentPage === 1}
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
                  } else if (tableCurrentPage <= 3) {
                    pageNum = i + 1;
                  } else if (tableCurrentPage >= totalPages - 2) {
                    pageNum = totalPages - 4 + i;
                  } else {
                    pageNum = tableCurrentPage - 2 + i;
                  }
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={tableCurrentPage === pageNum ? "default" : "outline"}
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
                onClick={() => goToPage(tableCurrentPage + 1)}
                disabled={tableCurrentPage >= totalPages}
                className="flex-1 sm:flex-none"
              >
                Next
                <ChevronRight className="size-4" />
              </Button>
            </div>

            <div className="text-sm text-gray-700 whitespace-nowrap">
              {tableLoadError
                ? <span className="text-red-700">{tableLoadError}</span>
                : tableLoading
                  ? <span>Loading…</span>
                  : <>
                      Showing{" "}
                      <span className="font-medium">{(tableCurrentPage - 1) * itemsPerPage + 1}</span> to{" "}
                      <span className="font-medium">{Math.min(tableCurrentPage * itemsPerPage, tableTotalElements)}</span> of{" "}
                      <span className="font-medium">{tableTotalElements}</span> results
                    </>
              }
            </div>
          </div>
        )}
      </div>
    </main>
  );
}