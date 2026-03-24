import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";

class ApplicationRepository {
  private applications: Application[] = [];
  private nextId: number = 1;

  constructor() {
    this.initialize();
  }

  private initialize(): void {
    // Initialize with mock data
    const types = ["Merit", "Social", "Performance"] as const;
    const statuses = ["Draft", "Pending Action", "Approved"] as const;

    const baseApplications: Application[] = [
      // 2023/2024 - 1 application
      {
        id: 1,
        type: types[0],
        academicYear: "2023/2024",
        semester: "I",
        createdAt: new Date(2023, 8, 15).toLocaleDateString(),
        status: statuses[2],
      },
      // 2024/2025 - 3 applications
      {
        id: 2,
        type: types[1],
        academicYear: "2024/2025",
        semester: "I",
        createdAt: new Date(2024, 7, 20).toLocaleDateString(),
        status: statuses[1],
      },
      {
        id: 3,
        type: types[0],
        academicYear: "2024/2025",
        semester: "II",
        createdAt: new Date(2025, 0, 10).toLocaleDateString(),
        status: statuses[2],
      },
      {
        id: 4,
        type: types[2],
        academicYear: "2024/2025",
        semester: "II",
        createdAt: new Date(2025, 1, 5).toLocaleDateString(),
        status: statuses[0],
      },
      // 2025/2026 - 4 applications
      {
        id: 5,
        type: types[1],
        academicYear: "2025/2026",
        semester: "I",
        createdAt: new Date(2025, 8, 5).toLocaleDateString(),
        status: statuses[2],
      },
      {
        id: 6,
        type: types[0],
        academicYear: "2025/2026",
        semester: "II",
        createdAt: new Date(2026, 1, 15).toLocaleDateString(),
        status: statuses[1],
      },
      {
        id: 7,
        type: types[2],
        academicYear: "2025/2026",
        semester: "I",
        createdAt: new Date(2025, 9, 10).toLocaleDateString(),
        status: statuses[0],
      },
      {
        id: 8,
        type: types[1],
        academicYear: "2025/2026",
        semester: "II",
        createdAt: new Date(2026, 2, 1).toLocaleDateString(),
        status: statuses[1],
      },
    ];

    this.applications = baseApplications;
    this.nextId = Math.max(...baseApplications.map(a => a.id)) + 1;
  }

  /**
   * Get all applications
   */
  getAll(): Application[] {
    return [...this.applications];
  }

  /**
   * Get a single application by ID
   */
  getById(id: number): Application | undefined {
    return this.applications.find(app => app.id === id);
  }

  /**
   * Create a new application
   */
  create(input: CreateApplicationInput): Application {
    const newApplication: Application = {
      id: this.nextId++,
      ...input,
      createdAt: new Date().toLocaleDateString(),
      status: "Draft",
    };

    this.applications.push(newApplication);
    return newApplication;
  }

  /**
   * Update an existing application
   */
  update(id: number, input: UpdateApplicationInput): Application | undefined {
    const application = this.applications.find(app => app.id === id);
    
    if (!application) {
      return undefined;
    }

    // Update only provided fields
    if (input.type !== undefined) application.type = input.type;
    if (input.academicYear !== undefined) application.academicYear = input.academicYear;
    if (input.semester !== undefined) application.semester = input.semester;
    if (input.status !== undefined) application.status = input.status;

    return application;
  }

  /**
   * Delete an application
   */
  delete(id: number): boolean {
    const index = this.applications.findIndex(app => app.id === id);
    
    if (index === -1) {
      return false;
    }

    this.applications.splice(index, 1);
    return true;
  }

  /**
   * Clear all applications and reset to initial state
   */
  reset(): void {
    this.applications = [];
    this.nextId = 1;
    this.initialize();
  }
}

// Export singleton instance
export const applicationRepository = new ApplicationRepository();
