// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import "context"

// PlatformAdapter defines the contract for driving apps on different platforms.
// Implementations exist for Android (ADB/Robolectric), Desktop (Playwright),
// and Web (Playwright).
type PlatformAdapter interface {
	// Name returns the platform name (e.g., "android", "desktop", "web").
	Name() string

	// Build builds the application for this platform.
	Build(ctx context.Context) error

	// Launch starts the application.
	Launch(ctx context.Context) error

	// IsRunning checks if the application is currently running.
	IsRunning(ctx context.Context) (bool, error)

	// OpenFile opens a file in the application.
	OpenFile(ctx context.Context, path string) error

	// GetDisplayedContent returns the currently visible content.
	GetDisplayedContent(ctx context.Context) (string, error)

	// EditContent enters content into the editor.
	EditContent(ctx context.Context, content string) error

	// SaveFile saves the current document.
	SaveFile(ctx context.Context) error

	// NavigateTo navigates to a named screen/tab.
	NavigateTo(ctx context.Context, screen string) error

	// NavigateToSettings opens the settings screen.
	NavigateToSettings(ctx context.Context) error

	// SetSetting modifies a setting value.
	SetSetting(ctx context.Context, key, value string) error

	// SwitchTheme changes the application theme.
	SwitchTheme(ctx context.Context, theme string) error

	// TakeScreenshot captures the current screen.
	TakeScreenshot(ctx context.Context) ([]byte, error)

	// Close terminates the application.
	Close(ctx context.Context) error
}
