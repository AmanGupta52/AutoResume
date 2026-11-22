'use strict';
require('dotenv').config();
const mongoose = require('mongoose');
const { loadConfig, configProblems, configWarnings } = require('./config');
const { createApp } = require('./app');

async function main() {
  const config = loadConfig(process.env);
  const problems = configProblems(config);
  if (problems.length) {
    console.error('Cannot start:\n - ' + problems.join('\n - '));
    process.exit(1);
  }
  configWarnings(config).forEach((w) => console.warn('Warning: ' + w));
  mongoose.set('strictQuery', true);
  await mongoose.connect(config.mongoUri, { serverSelectionTimeoutMS: 10000 });
  console.log('MongoDB connected');
  const cl = config.cloudinary;
  console.log(cl.cloudName && cl.apiKey && cl.apiSecret ? `PDF storage: Cloudinary (${cl.cloudName})` : 'PDF storage: OFF (set the CLOUDINARY_* variables to turn it on)');

  console.log(config.ai.apiKey ? `AI: ON (NVIDIA, model ${config.ai.model || 'chosen automatically'}, ${config.ai.dailyLimit} requests per person per day)` : 'AI: OFF (set NVIDIA_API_KEY to turn it on)');

  const app = createApp(config);
  const server = app.listen(config.port, () => console.log(`AutoResume API and web app on http://localhost:${config.port}`));

  const stop = async () => {
    server.close();
    await mongoose.disconnect();
    process.exit(0);
  };
  process.on('SIGTERM', stop);
  process.on('SIGINT', stop);
}

main().catch((e) => {
  console.error('Failed to start:', e.message);
  process.exit(1);
});
