import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import FadeIn from "./FadeIn";

describe("FadeIn", () => {
  it("mounts and renders its children inside the animated wrapper", () => {
    render(
      <FadeIn>
        <p>Hello</p>
      </FadeIn>,
    );

    expect(screen.getByText("Hello").parentElement).toHaveClass("fade-in");
  });

  it("starts the animation after the given delay", () => {
    render(
      <FadeIn delay={250}>
        <p>Hello</p>
      </FadeIn>,
    );

    expect(screen.getByText("Hello").parentElement).toHaveStyle({
      animationDelay: "250ms",
    });
  });

  it("lets its children be clicked straight away, even before the animation starts", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(
      <FadeIn delay={5000}>
        <button onClick={onClick}>Click me</button>
      </FadeIn>,
    );

    await user.click(screen.getByRole("button", { name: "Click me" }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
