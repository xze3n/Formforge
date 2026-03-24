import { Outlet } from "react-router";
import { Navbar } from "./components/navbar";

export function Layout() {
  return (
    <div className="size-full flex flex-col">
      <Navbar />
      <Outlet />
    </div>
  );
}
