const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 9000;

// MIME types
const mimeTypes = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.css': 'text/css'
};

function serveFile(filePath, res) {
  const ext = path.extname(filePath).toLowerCase();
  const contentType = mimeTypes[ext] || 'application/octet-stream';
  
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('File not found');
      return;
    }
    
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  });
}

const server = http.createServer((req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  
  let filePath = req.url === '/' ? '/index.html' : req.url;
  
  // Try different locations for the files
  const possiblePaths = [
    path.join(__dirname, 'webApp/build/dist/wasmJs/productionExecutable', filePath),
    path.join(__dirname, 'webApp/build/compileSync/wasmJs/main/productionExecutable', filePath),
    path.join(__dirname, 'webApp/src/wasmJsMain/resources', filePath),
    path.join(__dirname, 'build/js/packages/yole-web', filePath),
    path.join(__dirname, 'webApp/build/compose/skiko-for-web-runtime', filePath)
  ];
  
  // Check each possible path
  let found = false;
  for (const tryPath of possiblePaths) {
    if (fs.existsSync(tryPath)) {
      serveFile(tryPath, res);
      found = true;
      break;
    }
  }
  
  if (!found) {
    // Try to serve a default HTML file
    if (filePath === '/index.html') {
      const defaultHtml = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Yole - Web Text Editor</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .error { color: red; }
    </style>
</head>
<body>
    <h1>Yole Web Application</h1>
    <p class="error">Development server is running, but the WASM module is not yet built.</p>
    <p>Build the web app using: <code>./gradlew :webApp:compileKotlinWasmJs</code></p>
    <p>Then refresh this page.</p>
</body>
</html>`;
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(defaultHtml);
    } else {
      res.writeHead(404);
      res.end('File not found: ' + filePath);
    }
  }
});

server.listen(PORT, () => {
  console.log(`Yole Web Development Server running at http://localhost:${PORT}`);
  console.log('Serving files from:');
  const possiblePaths = [
    path.join(__dirname, 'webApp/build/dist/wasmJs/productionExecutable'),
    path.join(__dirname, 'webApp/build/compileSync/wasmJs/main/productionExecutable'),
    path.join(__dirname, 'webApp/src/wasmJsMain/resources'),
    path.join(__dirname, 'build/js/packages/yole-web'),
    path.join(__dirname, 'webApp/build/compose/skiko-for-web-runtime')
  ];
  possiblePaths.forEach(p => console.log('  - ' + p));
});