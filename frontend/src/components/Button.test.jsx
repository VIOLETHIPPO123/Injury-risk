import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import Button from "./Button";

describe("Button", () => {
  it("renders its label", () => {
    render(<Button>Roster</Button>);

    expect(screen.getByRole("button", { name: "Roster" })).toBeInTheDocument();
  });

  it("calls onClick when clicked", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Roster</Button>);

    await user.click(screen.getByRole("button", { name: "Roster" }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("does not call onClick before it is clicked", () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Roster</Button>);

    expect(onClick).not.toHaveBeenCalled();
  });
});
