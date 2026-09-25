import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import NavMenu from "./NavMenu";

const items = [
  { label: "Roster", onClick: vi.fn() },
  { label: "Search", onClick: vi.fn() },
];

function toggle() {
  return screen.getByRole("button", { name: "Toggle navigation menu" });
}

describe("NavMenu", () => {
  it("hides the dropdown items until the toggle is clicked", () => {
    render(<NavMenu items={items} />);

    expect(screen.queryByRole("menuitem")).not.toBeInTheDocument();
    expect(toggle()).toHaveAttribute("aria-expanded", "false");
  });

  it("shows all item labels after the toggle is clicked", async () => {
    const user = userEvent.setup();
    render(<NavMenu items={items} />);

    await user.click(toggle());

    expect(toggle()).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("menuitem", { name: "Roster" })).toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: "Search" })).toBeInTheDocument();
  });

  it("closes when the toggle is clicked again", async () => {
    const user = userEvent.setup();
    render(<NavMenu items={items} />);

    await user.click(toggle());
    await user.click(toggle());

    expect(screen.queryByRole("menuitem")).not.toBeInTheDocument();
  });

  it("calls the item's onClick and closes the menu when an item is selected", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<NavMenu items={[{ label: "Roster", onClick }]} />);

    await user.click(toggle());
    await user.click(screen.getByRole("menuitem", { name: "Roster" }));

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menuitem")).not.toBeInTheDocument();
  });

  it("closes when clicking outside the menu", async () => {
    const user = userEvent.setup();
    render(
      <div>
        <NavMenu items={items} />
        <button type="button">Outside</button>
      </div>,
    );

    await user.click(toggle());
    expect(screen.getByRole("menuitem", { name: "Roster" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Outside" }));

    expect(screen.queryByRole("menuitem")).not.toBeInTheDocument();
  });
});
