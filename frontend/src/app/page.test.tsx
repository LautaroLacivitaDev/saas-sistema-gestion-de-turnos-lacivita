import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { messages } from "@/messages/es-AR";

import Home from "./page";

// next/form necesita el router de Next; en la prueba alcanza con un formulario común.
vi.mock("next/form", () => ({
  default: (props: React.FormHTMLAttributes<HTMLFormElement>) => <form {...props} />,
}));

describe("Home", () => {
  it("muestra el título principal en español", () => {
    render(<Home />);

    expect(screen.getByRole("heading", { level: 1, name: messages.home.title })).toBeInTheDocument();
  });

  it("tiene un buscador que manda a /buscar con lo escrito", () => {
    render(<Home />);

    const search = screen.getByRole("search");
    expect(search).toHaveAttribute("action", "/buscar");
    expect(screen.getByRole("searchbox", { name: messages.search.label })).toHaveAttribute("name", "q");
  });
});
