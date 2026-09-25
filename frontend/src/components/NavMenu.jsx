import { useEffect, useRef, useState } from "react";
import Button from "./Button";
import "./NavMenu.css";

function NavMenu({ items }) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event) {
      if (!containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  function selectItem(onClick) {
    onClick();
    setIsOpen(false);
  }

  return (
    <div className="nav-menu" ref={containerRef}>
      <Button
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label="Toggle navigation menu"
        onClick={() => setIsOpen((open) => !open)}
      >
        <span className="hamburger-icon" aria-hidden="true">
          <span />
          <span />
          <span />
        </span>
      </Button>

      {isOpen && (
        <ul className="nav-menu-dropdown" role="menu">
          {items.map(({ label, onClick }) => (
            <li key={label} role="none">
              <Button role="menuitem" onClick={() => selectItem(onClick)}>
                {label}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default NavMenu;
