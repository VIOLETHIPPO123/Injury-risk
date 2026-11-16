import { apiFetch } from "./apiFetch";

// Admin only. Tells the backend to reload players and snap counts from nflverse now, instead
// of waiting for Tuesday's run. Resolves once both have finished (a few seconds) with what
// each did: { players: { added, updated, ... }, workloads: { added, updated, ... } }.
// The backend checks the role itself: anyone else gets a 403, which rejects here.
export async function refreshPlayerData() {
  const response = await apiFetch("/admin/players/refresh", { method: "POST" });
  if (!response.ok) {
    // The backend explains a rejection in { error }, e.g. "Admins only".
    let reason;
    try {
      reason = (await response.json())?.error;
    } catch {
      // No JSON body: the status alone will have to do.
    }
    throw new Error(reason ?? `Refresh failed with status ${response.status}`);
  }
  return response.json();
}
