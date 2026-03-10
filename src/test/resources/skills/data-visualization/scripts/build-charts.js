#!/usr/bin/env node
// Build all charts from config

const configs = require('./chart-configs.json');

console.log(`Building ${configs.length} charts...`);

configs.forEach(config => {
  console.log(`Building: ${config.name}`);
  // Build logic here
});

console.log('✓ Build complete');
