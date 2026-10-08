import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { messages } from "@/messages/es-AR";

import Home from "./page";

describe("Home", () => {
  it("muestra el título principal en español", () => {
    render(<Home />);

    expect(
      screen.getByRole("heading", { level: 1, name: messages.home.title }),
    ).toBeInTheDocument();
  });
});
