import { createBrowserRouter } from "react-router";
import { Layout } from "./layout";
import { Home } from "./pages/home";
import { Login } from "./pages/login";
import { Register } from "./pages/register";
import { ForgotPassword } from "./pages/forgot-password";
import { ResetPassword } from "./pages/reset-password";
import { ScholarshipApplications } from "./pages/scholarship-applications";
import { AddApplication } from "./pages/add-application";
import { ApplicationDetail } from "./pages/application-detail";
import { AdminPage } from "./pages/admin";
import { ProtectedRoute } from "./components/ProtectedRoute";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, Component: Home },
      { path: "login",            Component: Login },
      { path: "register",         Component: Register },
      { path: "forgot-password",  Component: ForgotPassword },
      { path: "reset-password",   Component: ResetPassword },
      { path: "admin",            Component: AdminPage },
      {
        // All application-related routes require authentication
        Component: ProtectedRoute,
        children: [
          { path: "scholarship-applications", Component: ScholarshipApplications },
          { path: "add-application",          Component: AddApplication },
          { path: "application/:id",          Component: ApplicationDetail },
        ],
      },
    ],
  },
]);