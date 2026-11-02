// A body part injured this many times or more counts as a repeat injury.
export const REPEAT_THRESHOLD = 2;

const plural = (count, word, words = `${word}s`) => (count === 1 ? word : words);
const seasonCount = (injuries) => new Set(injuries.map((i) => i.season)).size;

// Body parts injured REPEAT_THRESHOLD or more times, matched ignoring case. Each comes with
// how many times and across how many seasons. Most frequent first; ties keep the order the
// body parts first appear in (newest first, as the API sends them).
export function repeatedInjuries(injuries) {
  const byPart = new Map();
  for (const injury of injuries) {
    const key = injury.bodyPart.toLowerCase();
    if (!byPart.has(key)) byPart.set(key, { bodyPart: injury.bodyPart, injuries: [] });
    byPart.get(key).injuries.push(injury);
  }

  return [...byPart.values()]
    .filter((part) => part.injuries.length >= REPEAT_THRESHOLD)
    .map((part) => ({
      bodyPart: part.bodyPart,
      count: part.injuries.length,
      seasons: seasonCount(part.injuries),
    }))
    .sort((a, b) => b.count - a.count);
}

// "2 hamstring injuries in 2 seasons."
export function describeRepeat({ bodyPart, count, seasons }) {
  return `${count} ${bodyPart.toLowerCase()} injuries in ${seasons} ${plural(seasons, "season")}.`;
}

// The line shown when nothing repeats: "3 injuries in 2 seasons, none to the same body part twice."
export function describeHistory(injuries) {
  const count = injuries.length;
  const seasons = seasonCount(injuries);
  const tail = count === 1 ? "." : ", none to the same body part twice.";
  return `${count} ${plural(count, "injury", "injuries")} in ${seasons} ${plural(seasons, "season")}${tail}`;
}
