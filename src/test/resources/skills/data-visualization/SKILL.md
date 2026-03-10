---
name: data-visualization
description: Data visualization toolkit for creating charts, graphs, and interactive dashboards
tags: [data, visualization, charts, d3, plotly]
author: Data Team
version: 1.5.0
requires: [nodejs, npm]
---

# Data Visualization Skill

Complete toolkit for data visualization.

## Supported Chart Types

- Bar charts
- Line charts
- Scatter plots
- Pie charts
- Heatmaps
- Geographic maps

## Quick Start

```javascript
const viz = require('data-viz');

viz.create({
  type: 'bar',
  data: [...],
  container: '#chart'
});
```

## References

- [references/chart-config.md](references/chart-config.md) - Configuration guide
- [references/api-reference.md](references/api-reference.md) - API documentation
- [references/examples.md](references/examples.md) - Code examples

## Scripts

- [scripts/setup.js](scripts/setup.js) - Environment setup
- [scripts/build-charts.js](scripts/build-charts.js) - Build all charts
- [scripts/export-png.js](scripts/export-png.js) - Export to PNG

## Assets

- [assets/color-palettes.json](assets/color-palettes.json) - Color schemes
- [assets/templates.svg](assets/templates.svg) - SVG templates
