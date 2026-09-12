const fs = require('fs');
const path = require('path');

const modulesPath = path.join(process.env.GITHUB_WORKSPACE, '.github', 'modules.json');
const modules = JSON.parse(fs.readFileSync(modulesPath, 'utf8'));

// Derive IDs from module name
const all = modules.map(m => ({
  module: m.module,
  module_id: `anvillib-${m.module}`,
  mod_id: `anvillib_${m.module.replace(/-/g, '_')}`,
  needs: m.needs || []
}));

// ── Topological sort (Kahn's algorithm) ──────────────────────────
const deps = new Map(all.map(m => [m.module, m.needs]));
const inDegree = new Map(all.map(m => [m.module, m.needs.length]));
const queue = all.filter(m => inDegree.get(m.module) === 0).map(m => m.module);
const levels = [];

while (queue.length > 0) {
  const size = queue.length;
  const current = [];
  for (let i = 0; i < size; i++) {
    const name = queue.shift();
    current.push(name);
    for (const m of all) {
      if (m.needs.includes(name)) {
        const deg = inDegree.get(m.module) - 1;
        inDegree.set(m.module, deg);
        if (deg === 0) queue.push(m.module);
      }
    }
  }
  levels.push(current);
}

// ── Outputs ──────────────────────────────────────────────────────
const out = process.env.GITHUB_OUTPUT;

levels.forEach((names, i) => {
  const entries = all.filter(m => names.includes(m.module));
  const matrix = JSON.stringify({
    include: entries.map(m => ({
      module: m.module,
      module_id: m.module_id,
      mod_id: m.mod_id
    }))
  });
  fs.appendFileSync(out, `level_${i}=${matrix}\n`);
});

fs.appendFileSync(out, `level_count=${levels.length}\n`);

// Flat list for roseau_comment.yml
const names = JSON.stringify(all.map(m => m.module));
fs.appendFileSync(out, `module_names=${names}\n`);
