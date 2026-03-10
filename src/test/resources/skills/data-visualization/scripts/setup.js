#!/usr/bin/env node
// Setup script for data visualization environment

const fs = require('fs');
const path = require('path');

console.log('Setting up data visualization environment...');

// Create output directory
const outputDir = path.join(process.cwd(), 'output');
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

console.log('✓ Setup complete');
