import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AcwrInfoPage from "./AcwrInfoPage";

describe("AcwrInfoPage", () => {
  it("renders a heading and the plain-language explainer text", () => {
    render(<AcwrInfoPage onBack={vi.fn()} />);

    expect(
      screen.getByRole("heading", { name: "Understanding ACWR" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Acute:Chronic Workload Ratio/i),
    ).toBeInTheDocument();
  });

  it("renders the risk scale with all four tiers", () => {
    render(<AcwrInfoPage onBack={vi.fn()} />);

    expect(screen.getByText("Ramping up")).toBeInTheDocument();
    expect(screen.getByText("Normal workload")).toBeInTheDocument();
    expect(screen.getByText("Slightly elevated")).toBeInTheDocument();
    expect(screen.getByText("Spiking — high risk")).toBeInTheDocument();
  });

  it("renders a data-source list disclosing where the data comes from", () => {
    render(<AcwrInfoPage onBack={vi.fn()} />);

    expect(
      screen.getByRole("heading", { name: "Where our data comes from" }),
    ).toBeInTheDocument();
    expect(screen.getByText("nflverse")).toBeInTheDocument();
    expect(screen.getByText("Public NFL injury reports")).toBeInTheDocument();
  });

  it("calls onBack when the back link is clicked", async () => {
    const onBack = vi.fn();
    render(<AcwrInfoPage onBack={onBack} />);

    screen.getByRole("button", { name: /back to home/i }).click();

    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
