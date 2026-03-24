import { createBrowserRouter } from "react-router";
import { Layout } from "./layout";
import { Home } from "./pages/home";
import { Login } from "./pages/login";
import { Register } from "./pages/register";
import { ScholarshipApplications } from "./pages/scholarship-applications";
import { AddApplication } from "./pages/add-application";
import { ApplicationDetail } from "./pages/application-detail";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, Component: Home },
      { path: "login", Component: Login },
      { path: "register", Component: Register },
      { path: "scholarship-applications", Component: ScholarshipApplications },
      { path: "add-application", Component: AddApplication },
      { path: "application/:id", Component: ApplicationDetail },
    ],
  },
]);