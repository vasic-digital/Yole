// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

import createMDX from "@next/mdx";

const withMDX = createMDX({
  extension: /\.mdx?$/,
});

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
  pageExtensions: ["ts", "tsx", "md", "mdx"],
  images: {
    unoptimized: true,
  },
};

export default withMDX(nextConfig);
