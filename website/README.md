# Yole Website

Marketing and documentation website for Yole, built with Next.js 14, TypeScript, and Tailwind CSS.

## Setup

```bash
cd website
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Build

```bash
npm run build
```

This produces a static export in `out/` (configured via `output: 'export'` in `next.config.mjs`). Deploy to any static hosting provider.

## Structure

```
website/
  app/
    layout.tsx          Root layout with header and footer
    page.tsx            Homepage (hero, formats, platforms, features)
    globals.css         Tailwind directives and custom styles
    docs/page.tsx       Documentation (18 format guides)
    download/page.tsx   Download page with platform cards
    about/page.tsx      About page with history and license
  public/               Static assets
  next.config.mjs       Next.js config (MDX, static export)
  tailwind.config.ts    Tailwind config (colors, fonts)
  postcss.config.mjs    PostCSS config
  tsconfig.json         TypeScript config
```

## License

Apache-2.0
