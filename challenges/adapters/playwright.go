// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"fmt"

	"github.com/playwright-community/playwright-go"
)

// PlaywrightAdapter wraps playwright-go for browser and desktop automation.
type PlaywrightAdapter struct {
	pw      *playwright.Playwright
	browser playwright.Browser
	page    playwright.Page
	BaseURL string
}

// NewPlaywrightAdapter creates a new Playwright adapter.
func NewPlaywrightAdapter(baseURL string) *PlaywrightAdapter {
	return &PlaywrightAdapter{
		BaseURL: baseURL,
	}
}

// Initialize sets up the Playwright browser instance.
func (p *PlaywrightAdapter) Initialize(
	ctx context.Context, browserType string,
) error {
	pw, err := playwright.Run()
	if err != nil {
		return fmt.Errorf("could not start playwright: %w", err)
	}
	p.pw = pw

	launchOpts := playwright.BrowserTypeLaunchOptions{
		Headless: playwright.Bool(true),
	}

	var browser playwright.Browser
	switch browserType {
	case "firefox":
		browser, err = pw.Firefox.Launch(launchOpts)
	case "webkit":
		browser, err = pw.WebKit.Launch(launchOpts)
	default:
		browser, err = pw.Chromium.Launch(launchOpts)
	}
	if err != nil {
		return fmt.Errorf("could not launch browser: %w", err)
	}
	p.browser = browser

	page, err := browser.NewPage()
	if err != nil {
		return fmt.Errorf("could not create page: %w", err)
	}
	p.page = page

	return nil
}

// Navigate goes to the specified URL.
func (p *PlaywrightAdapter) Navigate(ctx context.Context, url string) error {
	_, err := p.page.Goto(url, playwright.PageGotoOptions{
		WaitUntil: playwright.WaitUntilStateNetworkidle,
	})
	return err
}

// Click clicks an element matching the selector.
func (p *PlaywrightAdapter) Click(ctx context.Context, selector string) error {
	return p.page.Click(selector)
}

// ClickByText clicks an element containing the specified text.
func (p *PlaywrightAdapter) ClickByText(
	ctx context.Context, text string,
) error {
	locator := p.page.GetByText(text)
	return locator.Click()
}

// Fill types text into an input element.
func (p *PlaywrightAdapter) Fill(
	ctx context.Context, selector string, value string,
) error {
	return p.page.Fill(selector, value)
}

// GetTextContent returns the text content of an element.
func (p *PlaywrightAdapter) GetTextContent(
	ctx context.Context, selector string,
) (string, error) {
	content, err := p.page.TextContent(selector)
	if err != nil {
		return "", err
	}
	return content, nil
}

// IsVisible checks if an element is visible on the page.
func (p *PlaywrightAdapter) IsVisible(
	ctx context.Context, selector string,
) (bool, error) {
	return p.page.IsVisible(selector)
}

// Screenshot takes a screenshot of the current page.
func (p *PlaywrightAdapter) Screenshot(ctx context.Context) ([]byte, error) {
	return p.page.Screenshot()
}

// Close shuts down the browser and Playwright.
func (p *PlaywrightAdapter) Close(ctx context.Context) error {
	if p.browser != nil {
		if err := p.browser.Close(); err != nil {
			return err
		}
	}
	if p.pw != nil {
		if err := p.pw.Stop(); err != nil {
			return err
		}
	}
	return nil
}

// WaitForSelector waits for an element to appear on the page.
func (p *PlaywrightAdapter) WaitForSelector(
	ctx context.Context, selector string,
) error {
	_, err := p.page.WaitForSelector(selector)
	return err
}

// Page returns the underlying Playwright page for advanced operations.
func (p *PlaywrightAdapter) Page() playwright.Page {
	return p.page
}
