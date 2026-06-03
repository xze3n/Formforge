import { useState, useCallback, useRef, useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { Button } from "../components/ui/button";
import { FileText, Trash2, ArrowLeft, TrendingUp, DollarSign, Award, Plus, Pencil, CheckCircle, Circle, X, Check } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../components/ui/alert-dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { useApplicationRepository } from "../hooks/useApplicationRepository";
import { useDocumentRepository } from "../hooks/useDocumentRepository";
import { useEnums } from "../hooks/useEnums";
import { applicationApi } from "../services/applicationApi";
import { offlineStorage } from "../services/offlineStorage";
import { networkService } from "../services/networkService";
import type { Application } from "../types/application";
import type { UpdateDocumentInput } from "../types/document";

export function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { update, delete: deleteApplication } = useApplicationRepository();
  const { enums } = useEnums();

  const [application, setApplication] = useState<Application | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const loadedOnlineRef = useRef(false);

  const fetchApplication = useCallback(() => {
    if (!id) return;
    applicationApi.getById(Number(id))
      .then(app => {
        setApplication(app);
        loadedOnlineRef.current = true;
      })
      .catch(() => {
        // ignore — keep existing state
      });
  }, [id]);

  useEffect(() => {
    if (!id) return;
    loadedOnlineRef.current = false;
    setLoading(true);
    applicationApi.getById(Number(id))
      .then(app => {
        setApplication(app);
        loadedOnlineRef.current = true;
      })
      .catch(() => {
        const cached = offlineStorage.getApplications().find((a) => String(a.id) === String(id));
        setApplication(cached);
      })
      .finally(() => setLoading(false));
  }, [id]);

  // Re-fetch from server after sync completes or when back online
  useEffect(() => {
    if (!id) return;
    const onSyncEnd = () => fetchApplication();
    window.addEventListener("ff:sync:end", onSyncEnd);
    const unsubNetwork = networkService.subscribe((online) => {
      if (online && !loadedOnlineRef.current && offlineStorage.pendingCount === 0) fetchApplication();
    });
    return () => {
      window.removeEventListener("ff:sync:end", onSyncEnd);
      unsubNetwork();
    };
  }, [id, fetchApplication]);

  const { documents, loading: docsLoading, error: docsError, add: addDocument, update: updateDocument, remove: removeDocument } = useDocumentRepository(Number(id));

  const [academicYear, setAcademicYear] = useState("");
  const [semester, setSemester] = useState("");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [moneyParticles, setMoneyParticles] = useState<{ id: number; x: number; y: number; rotation: number; startX: number }[]>([]);
  const buttonRef = useRef<HTMLDivElement>(null);
  const [initialized, setInitialized] = useState(false);

  // Document form state
  const [showAddForm, setShowAddForm] = useState(false);
  const [newDocName, setNewDocName] = useState("");
  const [newDocType, setNewDocType] = useState("");
  const [newDocDescription, setNewDocDescription] = useState("");
  const [newDocNotes, setNewDocNotes] = useState("");
  const [addingDoc, setAddingDoc] = useState(false);

  // Per-document edit state
  const [editingDocId, setEditingDocId] = useState<number | null>(null);
  const [editDocName, setEditDocName] = useState("");
  const [editDocType, setEditDocType] = useState("");
  const [editDocDescription, setEditDocDescription] = useState("");
  const [editDocNotes, setEditDocNotes] = useState("");
  const [savingDocId, setSavingDocId] = useState<number | null>(null);
  const [deleteDocId, setDeleteDocId] = useState<number | null>(null);

  // Sync form state when application loads
  if (application && !initialized) {
    setAcademicYear(application.academicYear);
    setSemester(application.semester);
    setInitialized(true);
  }

  const handleAddDocument = async () => {
    if (!newDocName.trim() || !newDocType) return;
    setAddingDoc(true);
    try {
      await addDocument({
        name: newDocName.trim(),
        type: newDocType as import("../types/document").DocumentType,
        description: newDocDescription.trim() || undefined,
        notes: newDocNotes.trim() || undefined,
      });
      setNewDocName("");
      setNewDocType("");
      setNewDocDescription("");
      setNewDocNotes("");
      setShowAddForm(false);
    } finally {
      setAddingDoc(false);
    }
  };

  const startEditDoc = (doc: import("../types/document").Document) => {
    setEditingDocId(doc.id);
    setEditDocName(doc.name);
    setEditDocType(doc.type);
    setEditDocDescription(doc.description ?? "");
    setEditDocNotes(doc.notes ?? "");
  };

  const handleSaveDoc = async (id: number) => {
    setSavingDocId(id);
    try {
      const input: UpdateDocumentInput = {};
      if (editDocName.trim()) input.name = editDocName.trim();
      if (editDocType) input.type = editDocType as import("../types/document").DocumentType;
      if (editDocDescription.trim() !== "") input.description = editDocDescription.trim();
      if (editDocNotes.trim() !== "") input.notes = editDocNotes.trim();
      await updateDocument(id, input);
      setEditingDocId(null);
    } finally {
      setSavingDocId(null);
    }
  };

  const handleToggleVerified = async (doc: import("../types/document").Document) => {
    await updateDocument(doc.id, { verified: !doc.verified });
  };

  const handleDeleteDoc = async () => {
    if (deleteDocId === null) return;
    await removeDocument(deleteDocId);
    setDeleteDocId(null);
  };

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

  if (loading) {
    return (
      <main className="flex-1 bg-gradient-to-tr from-purple-50 via-purple-100 to-yellow-50 flex items-center justify-center">
        <p className="text-gray-600 text-lg">Loading...</p>
      </main>
    );
  }

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

  const handleSaveChanges = async () => {
    if (application) {
      const updated = await update(Number(application.id), {
        academicYear,
        semester: semester as "I" | "II",
      });
      setApplication(updated);
      setSuccessMessage("Changes saved successfully!");
      setTimeout(() => setSuccessMessage(null), 3000);
    }
  };

  const handleGenerateDossier = () => {
    spawnMoney();
    console.log("Generate dossier for application:", application?.id);
  };

  const handleDelete = async () => {
    if (application) {
      await deleteApplication(Number(application.id));
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
                      {enums.academicYears.map(year => (
                        <SelectItem key={year} value={year}>{year}</SelectItem>
                      ))}
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
                      {enums.semesters.map(sem => (
                        <SelectItem key={sem} value={sem}>{sem}</SelectItem>
                      ))}
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
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      variant="destructive"
                      size="lg"
                      className="w-full gap-2 rounded-none"
                    >
                      <Trash2 className="size-5" />
                      Delete Application
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Delete Application</AlertDialogTitle>
                      <AlertDialogDescription>
                        Are you sure you want to delete application #{application.id}? This action cannot be undone.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Delete</AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
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

            {/* Document Statistics removed — moved below Supporting Documents heading */}
          </div>
        </div>

        {/* Documents Section */}
        <div className="mt-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-bold text-gray-900">Supporting Documents</h2>
            <Button onClick={() => setShowAddForm(v => !v)} className="gap-2">
              <Plus className="size-4" />
              Add Document
            </Button>
          </div>

          {/* Document Statistics — full width, under heading */}
          {!docsLoading && documents.length > 0 && (
            <div className="bg-white rounded-none shadow p-6 mb-6">
              <div className="flex flex-wrap gap-6 items-start">
                <div className="flex gap-4 shrink-0">
                  <div className="bg-purple-50 px-6 py-3 rounded-none text-center">
                    <p className="text-2xl font-bold text-purple-700">{documents.length}</p>
                    <p className="text-xs text-gray-500 mt-1">Total</p>
                  </div>
                  <div className="bg-green-50 px-6 py-3 rounded-none text-center">
                    <p className="text-2xl font-bold text-green-700">{documents.filter(d => d.verified).length}</p>
                    <p className="text-xs text-gray-500 mt-1">Verified</p>
                  </div>
                  <div className="bg-gray-50 px-6 py-3 rounded-none text-center">
                    <p className="text-2xl font-bold text-gray-700">{documents.filter(d => !d.verified).length}</p>
                    <p className="text-xs text-gray-500 mt-1">Unverified</p>
                  </div>
                </div>
                <div className="flex-1 min-w-[200px] space-y-2">
                  <p className="text-sm font-medium text-gray-700 mb-2">By Type</p>
                  {Array.from(new Set(documents.map(d => d.type))).map(type => {
                    const count = documents.filter(d => d.type === type).length;
                    const pct = Math.round((count / documents.length) * 100);
                    return (
                      <div key={type} className="space-y-1">
                        <div className="flex justify-between text-xs text-gray-600">
                          <span>{type}</span>
                          <span className="font-medium">{count}</span>
                        </div>
                        <div className="h-1.5 bg-gray-100 rounded-none">
                          <div className="h-1.5 bg-purple-500 rounded-none" style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* Delete document confirmation */}
          <AlertDialog open={deleteDocId !== null} onOpenChange={open => { if (!open) setDeleteDocId(null); }}>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Delete Document</AlertDialogTitle>
                <AlertDialogDescription>
                  Are you sure you want to delete this document? This action cannot be undone.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction onClick={handleDeleteDoc} className="bg-red-600 hover:bg-red-700">Delete</AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>

          {/* Add document inline form */}
          {showAddForm && (
            <div className="bg-white shadow p-6 mb-4 rounded-none border-l-4 border-purple-500">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">New Document</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name <span className="text-red-500">*</span></label>
                  <input
                    className="w-full border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                    value={newDocName}
                    onChange={e => setNewDocName(e.target.value)}
                    placeholder="e.g. Income Certificate 2026"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Type <span className="text-red-500">*</span></label>
                  <select
                    className="w-full border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 bg-white"
                    value={newDocType}
                    onChange={e => setNewDocType(e.target.value)}
                  >
                    <option value="">Select type…</option>
                    {enums.documentTypes.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <input
                    className="w-full border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                    value={newDocDescription}
                    onChange={e => setNewDocDescription(e.target.value)}
                    placeholder="Brief description of the document"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                  <input
                    className="w-full border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                    value={newDocNotes}
                    onChange={e => setNewDocNotes(e.target.value)}
                    placeholder="Optional reviewer notes"
                  />
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Button onClick={handleAddDocument} disabled={addingDoc || !newDocName.trim() || !newDocType} className="gap-2">
                  <Check className="size-4" />
                  {addingDoc ? "Adding…" : "Add"}
                </Button>
                <Button variant="outline" onClick={() => setShowAddForm(false)}>
                  <X className="size-4" />
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {docsError && (
            <p className="text-sm text-red-600 mb-4">{docsError}</p>
          )}

          {docsLoading && (
            <p className="text-sm text-gray-500">Loading documents…</p>
          )}

          {!docsLoading && documents.length === 0 && !showAddForm && (
            <div className="bg-white shadow p-8 text-center text-gray-500 rounded-none">
              <FileText className="size-10 mx-auto mb-2 text-gray-300" />
              <p>No documents attached yet.</p>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {documents.map(doc => (
              <div key={doc.id} className="bg-white shadow rounded-none border-l-4 border-gray-200 hover:border-purple-400 transition-colors">
                <div className="p-5">
                  {editingDocId === doc.id ? (
                    <div className="space-y-3">
                      <input
                        className="w-full border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                        value={editDocName}
                        onChange={e => setEditDocName(e.target.value)}
                        placeholder="Name"
                      />
                      <select
                        className="w-full border border-gray-300 px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                        value={editDocType}
                        onChange={e => setEditDocType(e.target.value)}
                      >
                        {enums.documentTypes.map(t => <option key={t} value={t}>{t}</option>)}
                      </select>
                      <input
                        className="w-full border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                        value={editDocDescription}
                        onChange={e => setEditDocDescription(e.target.value)}
                        placeholder="Description"
                      />
                      <input
                        className="w-full border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                        value={editDocNotes}
                        onChange={e => setEditDocNotes(e.target.value)}
                        placeholder="Notes"
                      />
                      <div className="flex gap-2">
                        <Button size="sm" onClick={() => handleSaveDoc(doc.id)} disabled={savingDocId === doc.id} className="gap-1">
                          <Check className="size-3" />{savingDocId === doc.id ? "Saving…" : "Save"}
                        </Button>
                        <Button size="sm" variant="outline" onClick={() => setEditingDocId(null)}>
                          <X className="size-3" /> Cancel
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="flex items-start justify-between mb-2">
                        <p className="font-semibold text-gray-900 text-sm leading-tight pr-2">{doc.name}</p>
                        <div className="flex gap-1 shrink-0">
                          <button
                            onClick={() => handleToggleVerified(doc)}
                            title={doc.verified ? "Mark unverified" : "Mark verified"}
                            className="text-gray-400 hover:text-green-600 transition-colors"
                          >
                            {doc.verified
                              ? <CheckCircle className="size-4 text-green-500" />
                              : <Circle className="size-4" />}
                          </button>
                          <button onClick={() => startEditDoc(doc)} className="text-gray-400 hover:text-purple-600 transition-colors">
                            <Pencil className="size-4" />
                          </button>
                          <button onClick={() => setDeleteDocId(doc.id)} className="text-gray-400 hover:text-red-600 transition-colors">
                            <Trash2 className="size-4" />
                          </button>
                        </div>
                      </div>
                      <span className="inline-flex items-center px-2 py-0.5 text-xs font-medium bg-purple-100 text-purple-800 mb-2">
                        {doc.type}
                      </span>
                      {doc.description && (
                        <p className="text-xs text-gray-600 mb-1">{doc.description}</p>
                      )}
                      {doc.notes && (
                        <p className="text-xs text-amber-700 italic">Note: {doc.notes}</p>
                      )}
                      <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
                        <span className="text-xs text-gray-400">{doc.dateAdded}</span>
                        <span className={`text-xs font-medium ${doc.verified ? "text-green-600" : "text-gray-400"}`}>
                          {doc.verified ? "Verified" : "Unverified"}
                        </span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </main>
  );
}