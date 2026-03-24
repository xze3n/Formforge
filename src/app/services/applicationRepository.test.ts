import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { applicationRepository } from './applicationRepository';
import { CreateApplicationInput, UpdateApplicationInput } from '../types/application';

describe('ApplicationRepository', () => {
  beforeEach(() => {
    // Reset to initial state before each test
    applicationRepository.reset();
  });

  describe('getAll', () => {
    it('should return all applications', () => {
      const apps = applicationRepository.getAll();
      expect(apps).toHaveLength(8);
    });

    it('should return a copy of the applications array', () => {
      const apps1 = applicationRepository.getAll();
      const apps2 = applicationRepository.getAll();
      expect(apps1).not.toBe(apps2);
      expect(apps1).toEqual(apps2);
    });

    it('should not allow mutations to affect the repository', () => {
      const apps = applicationRepository.getAll();
      apps.push({
        id: 999,
        type: 'Merit',
        academicYear: '2999/3000',
        semester: 'I',
        createdAt: '1/1/1900',
        status: 'Draft',
      });

      const appsAfter = applicationRepository.getAll();
      expect(appsAfter).toHaveLength(8);
    });
  });

  describe('getById', () => {
    it('should return an application by id', () => {
      const app = applicationRepository.getById(1);
      expect(app).toBeDefined();
      expect(app?.id).toBe(1);
      expect(app?.type).toBe('Merit');
      expect(app?.academicYear).toBe('2023/2024');
    });

    it('should return undefined for non-existent id', () => {
      const app = applicationRepository.getById(999);
      expect(app).toBeUndefined();
    });

    it('should return undefined for invalid id', () => {
      const app = applicationRepository.getById(-1);
      expect(app).toBeUndefined();
    });

    it('should return undefined for id zero', () => {
      const app = applicationRepository.getById(0);
      expect(app).toBeUndefined();
    });

    it('should handle multiple getById calls consistently', () => {
      const app1 = applicationRepository.getById(2);
      const app2 = applicationRepository.getById(2);
      expect(app1).toEqual(app2);
      expect(app1?.id).toBe(app2?.id);
    });

    it('should return all initial applications by their ids', () => {
      for (let i = 1; i <= 8; i++) {
        const app = applicationRepository.getById(i);
        expect(app).toBeDefined();
        expect(app?.id).toBe(i);
      }
    });
  });

  describe('create', () => {
    it('should create a new application with default status', () => {
      const input: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      };

      const created = applicationRepository.create(input);

      expect(created).toBeDefined();
      expect(created.id).toBe(9);
      expect(created.type).toBe('Merit');
      expect(created.academicYear).toBe('2026/2027');
      expect(created.semester).toBe('I');
      expect(created.status).toBe('Draft');
      expect(created.createdAt).toBeDefined();
      expect(typeof created.createdAt).toBe('string');
    });

    it('should increment id for each created application', () => {
      const input1: CreateApplicationInput = {
        type: 'Social',
        academicYear: '2026/2027',
        semester: 'II',
      };

      const input2: CreateApplicationInput = {
        type: 'Performance',
        academicYear: '2027/2028',
        semester: 'I',
      };

      const app1 = applicationRepository.create(input1);
      const app2 = applicationRepository.create(input2);

      expect(app1.id).toBe(9);
      expect(app2.id).toBe(10);
      expect(app2.id).toBeGreaterThan(app1.id);
    });

    it('should add application to the repository', () => {
      const initialCount = applicationRepository.getAll().length;

      const input: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      };

      applicationRepository.create(input);

      const finalCount = applicationRepository.getAll().length;
      expect(finalCount).toBe(initialCount + 1);
    });

    it('should make created application retrievable by getById', () => {
      const input: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      };

      const created = applicationRepository.create(input);
      const retrieved = applicationRepository.getById(created.id);

      expect(retrieved).toEqual(created);
    });

    it('should create applications with different types', () => {
      const types = ['Merit', 'Social', 'Performance'] as const;

      types.forEach((type) => {
        const input: CreateApplicationInput = {
          type,
          academicYear: '2026/2027',
          semester: 'I',
        };

        const created = applicationRepository.create(input);
        expect(created.type).toBe(type);
      });
    });

    it('should create applications with different semesters', () => {
      const input1: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      };

      const input2: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'II',
      };

      const app1 = applicationRepository.create(input1);
      const app2 = applicationRepository.create(input2);

      expect(app1.semester).toBe('I');
      expect(app2.semester).toBe('II');
    });

    it('should use current date for createdAt', () => {
      const now = new Date();
      const input: CreateApplicationInput = {
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      };

      const created = applicationRepository.create(input);
      const createdDate = new Date(created.createdAt);

      // Allow for small time differences (within same day)
      expect(createdDate.toLocaleDateString()).toBe(now.toLocaleDateString());
    });
  });

  describe('update', () => {
    it('should update type field', () => {
      const input: UpdateApplicationInput = { type: 'Performance' };
      const updated = applicationRepository.update(1, input);

      expect(updated).toBeDefined();
      expect(updated?.type).toBe('Performance');
      expect(updated?.academicYear).toBe('2023/2024');
    });

    it('should update academicYear field', () => {
      const input: UpdateApplicationInput = { academicYear: '2099/2100' };
      const updated = applicationRepository.update(1, input);

      expect(updated).toBeDefined();
      expect(updated?.academicYear).toBe('2099/2100');
      expect(updated?.type).toBe('Merit');
    });

    it('should update semester field', () => {
      const input: UpdateApplicationInput = { semester: 'II' };
      const updated = applicationRepository.update(1, input);

      expect(updated).toBeDefined();
      expect(updated?.semester).toBe('II');
    });

    it('should update status field', () => {
      const input: UpdateApplicationInput = { status: 'Approved' };
      const updated = applicationRepository.update(1, input);

      expect(updated).toBeDefined();
      expect(updated?.status).toBe('Approved');
    });

    it('should update multiple fields at once', () => {
      const input: UpdateApplicationInput = {
        type: 'Social',
        academicYear: '2030/2031',
        semester: 'II',
        status: 'Pending Action',
      };

      const updated = applicationRepository.update(1, input);

      expect(updated).toBeDefined();
      expect(updated?.type).toBe('Social');
      expect(updated?.academicYear).toBe('2030/2031');
      expect(updated?.semester).toBe('II');
      expect(updated?.status).toBe('Pending Action');
    });

    it('should return undefined for non-existent id', () => {
      const input: UpdateApplicationInput = { type: 'Merit' };
      const updated = applicationRepository.update(999, input);

      expect(updated).toBeUndefined();
    });

    it('should return undefined for invalid id', () => {
      const input: UpdateApplicationInput = { type: 'Merit' };
      const updated = applicationRepository.update(-1, input);

      expect(updated).toBeUndefined();
    });

    it('should handle empty update object', () => {
      const original = applicationRepository.getById(1);
      const updated = applicationRepository.update(1, {});

      expect(updated).toBeDefined();
      expect(updated?.id).toBe(original?.id);
      expect(updated?.type).toBe(original?.type);
      expect(updated?.academicYear).toBe(original?.academicYear);
      expect(updated?.semester).toBe(original?.semester);
      expect(updated?.status).toBe(original?.status);
    });

    it('should persist changes to getAll', () => {
      applicationRepository.update(1, { type: 'Social' });
      
      const app = applicationRepository.getById(1);
      expect(app?.type).toBe('Social');

      const allApps = applicationRepository.getAll();
      const app1 = allApps.find(a => a.id === 1);
      expect(app1?.type).toBe('Social');
    });

    it('should update all fields independently', () => {
      const statuses = ['Draft', 'Pending Action', 'Approved'] as const;

      statuses.forEach((status) => {
        applicationRepository.update(2, { status });
        const updated = applicationRepository.getById(2);
        expect(updated?.status).toBe(status);
      });
    });

    it('should preserve createdAt when updating', () => {
      const original = applicationRepository.getById(1);
      const originalDate = original?.createdAt;

      applicationRepository.update(1, { type: 'Social' });

      const updated = applicationRepository.getById(1);
      expect(updated?.createdAt).toBe(originalDate);
    });

    it('should preserve id when updating', () => {
      applicationRepository.update(1, { type: 'Social' });

      const updated = applicationRepository.getById(1);
      expect(updated?.id).toBe(1);
    });
  });

  describe('delete', () => {
    it('should delete an application by id', () => {
      const initialCount = applicationRepository.getAll().length;
      const deleted = applicationRepository.delete(1);

      expect(deleted).toBe(true);
      expect(applicationRepository.getAll().length).toBe(initialCount - 1);
    });

    it('should make deleted application unavailable by getById', () => {
      applicationRepository.delete(1);
      const app = applicationRepository.getById(1);

      expect(app).toBeUndefined();
    });

    it('should return false for non-existent id', () => {
      const deleted = applicationRepository.delete(999);
      expect(deleted).toBe(false);
    });

    it('should return false for invalid id', () => {
      const deleted = applicationRepository.delete(-1);
      expect(deleted).toBe(false);
    });

    it('should return false for id zero', () => {
      const deleted = applicationRepository.delete(0);
      expect(deleted).toBe(false);
    });

    it('should delete multiple applications independently', () => {
      applicationRepository.delete(1);
      applicationRepository.delete(3);

      expect(applicationRepository.getById(1)).toBeUndefined();
      expect(applicationRepository.getById(3)).toBeUndefined();
      expect(applicationRepository.getById(2)).toBeDefined();
    });

    it('should return false when deleting already deleted application', () => {
      applicationRepository.delete(1);
      const secondDelete = applicationRepository.delete(1);

      expect(secondDelete).toBe(false);
    });

    it('should not affect getAll length', () => {
      const initialCount = applicationRepository.getAll().length;
      
      applicationRepository.delete(1);
      applicationRepository.delete(2);

      expect(applicationRepository.getAll().length).toBe(initialCount - 2);
    });

    it('should preserve other applications after deletion', () => {
      const app5Before = applicationRepository.getById(5);
      
      applicationRepository.delete(1);
      applicationRepository.delete(2);

      const app5After = applicationRepository.getById(5);
      expect(app5After).toEqual(app5Before);
    });
  });

  describe('reset', () => {
    it('should reset to initial state', () => {
      // Modify the repository
      applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      });
      applicationRepository.delete(1);

      // Reset
      applicationRepository.reset();

      // Verify initial state
      expect(applicationRepository.getAll().length).toBe(8);
      expect(applicationRepository.getById(1)).toBeDefined();
      expect(applicationRepository.getById(9)).toBeUndefined();
    });

    it('should restore deleted applications', () => {
      applicationRepository.delete(1);
      expect(applicationRepository.getById(1)).toBeUndefined();

      applicationRepository.reset();

      expect(applicationRepository.getById(1)).toBeDefined();
      expect(applicationRepository.getById(1)?.type).toBe('Merit');
    });

    it('should remove created applications', () => {
      const created = applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      });

      applicationRepository.reset();

      expect(applicationRepository.getById(created.id)).toBeUndefined();
    });

    it('should revert updates', () => {
      const originalType = applicationRepository.getById(1)?.type;
      
      applicationRepository.update(1, { type: 'Social' });
      expect(applicationRepository.getById(1)?.type).toBe('Social');

      applicationRepository.reset();

      expect(applicationRepository.getById(1)?.type).toBe(originalType);
    });

    it('should restore correct count of applications', () => {
      applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      });
      applicationRepository.create({
        type: 'Social',
        academicYear: '2026/2027',
        semester: 'II',
      });
      applicationRepository.delete(3);

      applicationRepository.reset();

      expect(applicationRepository.getAll().length).toBe(8);
    });

    it('should reset next id counter', () => {
      // Create applications to advance the counter
      for (let i = 0; i < 5; i++) {
        applicationRepository.create({
          type: 'Merit',
          academicYear: '2026/2027',
          semester: 'I',
        });
      }

      const lastId = applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      }).id;

      applicationRepository.reset();

      const nextCreated = applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      });

      // After reset, next id should be 9 (max id 8 + 1)
      expect(nextCreated.id).toBe(9);
    });
  });

  describe('Integration Tests', () => {
    it('should support CRUD operations workflow', () => {
      // Create
      const created = applicationRepository.create({
        type: 'Merit',
        academicYear: '2026/2027',
        semester: 'I',
      });

      // Read
      const read = applicationRepository.getById(created.id);
      expect(read).toEqual(created);

      // Update
      const updated = applicationRepository.update(created.id, {
        type: 'Social',
        status: 'Approved',
      });
      expect(updated?.type).toBe('Social');
      expect(updated?.status).toBe('Approved');

      // Delete
      const deleted = applicationRepository.delete(created.id);
      expect(deleted).toBe(true);
      expect(applicationRepository.getById(created.id)).toBeUndefined();
    });

    it('should handle concurrent operations', () => {
      const ids: number[] = [];

      // Create multiple applications
      for (let i = 0; i < 3; i++) {
        const app = applicationRepository.create({
          type: 'Merit',
          academicYear: '2026/2027',
          semester: 'I',
        });
        ids.push(app.id);
      }

      // Update all
      ids.forEach(id => {
        applicationRepository.update(id, { status: 'Pending Action' });
      });

      // Verify all updates
      ids.forEach(id => {
        expect(applicationRepository.getById(id)?.status).toBe('Pending Action');
      });

      // Delete all
      ids.forEach(id => {
        applicationRepository.delete(id);
      });

      // Verify all deleted
      ids.forEach(id => {
        expect(applicationRepository.getById(id)).toBeUndefined();
      });
    });

    it('should maintain data integrity across operations', () => {
      const app1 = applicationRepository.getById(1);
      const app2 = applicationRepository.getById(2);

      // Create
      const created = applicationRepository.create({
        type: 'Performance',
        academicYear: '2030/2031',
        semester: 'II',
      });

      // Update one of the initial apps
      applicationRepository.update(1, { status: 'Approved' });

      // Delete another
      applicationRepository.delete(3);

      // Verify original apps are still accessible
      expect(applicationRepository.getById(1)?.id).toBe(app1?.id);
      expect(applicationRepository.getById(2)?.id).toBe(app2?.id);
      expect(applicationRepository.getById(created.id)).toBeDefined();
    });

    it('should have correct initial data structure', () => {
      const apps = applicationRepository.getAll();

      apps.forEach(app => {
        expect(app).toHaveProperty('id');
        expect(app).toHaveProperty('type');
        expect(app).toHaveProperty('academicYear');
        expect(app).toHaveProperty('semester');
        expect(app).toHaveProperty('createdAt');
        expect(app).toHaveProperty('status');

        expect(typeof app.id).toBe('number');
        expect(['Merit', 'Social', 'Performance']).toContain(app.type);
        expect(typeof app.academicYear).toBe('string');
        expect(['I', 'II']).toContain(app.semester);
        expect(typeof app.createdAt).toBe('string');
        expect(['Draft', 'Pending Action', 'Approved']).toContain(app.status);
      });
    });
  });
});
