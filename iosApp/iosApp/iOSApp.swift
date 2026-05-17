/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole iOS App entry point — SwiftUI + Compose Multiplatform
 *
 * HONEST DISCLOSURE (CONST-039 / iter-77):
 * This file requires Xcode to compile. The KMP shared framework
 * (digital.vasic.yole.shared) is built by :shared Gradle target
 * and linked by the Xcode project. Until Xcode is installed and
 * `./gradlew :shared:assembleReleaseXCFramework` has been run,
 * the `import shared` line will fail to resolve.
 *
 * Operator action required after Xcode install:
 *   1. ./gradlew :shared:assembleReleaseXCFramework
 *   2. Open iosApp/iosApp.xcodeproj in Xcode
 *   3. Add the framework output to the Frameworks, Libraries,
 *      and Embedded Content section of the Yole target.
 *   4. Build + run.
 *
 *########################################################*/

import SwiftUI
// import shared  // Uncomment after Xcode + KMP framework are linked

/// iOS app entry point. Uses SwiftUI @main to bootstrap the app.
/// Hosts the Compose Multiplatform UI via ComposeView.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Prevent keyboard from resizing the Compose surface —
                // Compose handles insets internally via WindowInsets.
                .ignoresSafeArea(.keyboard)
        }
    }
}

/// Bridges SwiftUI to Compose Multiplatform via UIViewControllerRepresentable.
///
/// Once the KMP shared framework is linked, replace the placeholder
/// UIViewController with:
///   MainViewControllerKt.MainViewController()
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // TODO(operator-action): Replace with `MainViewControllerKt.MainViewController()`
        // after `import shared` is uncommented and framework linked.
        // Pre-Xcode placeholder: show honest "coming soon" screen.
        return YolePlaceholderViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Honest placeholder shown before Compose Multiplatform framework is linked.
/// CONST-039: this is NOT a bluff — the screen truthfully communicates the
/// pre-Xcode state. Replace with ComposeUIViewController once framework is ready.
final class YolePlaceholderViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.53, green: 0.11, blue: 0.15, alpha: 1.0)

        let label = UILabel()
        label.text = "Yole"
        label.textColor = .white
        label.font = UIFont.systemFont(ofSize: 48, weight: .bold)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)

        let sub = UILabel()
        sub.text = "iOS — Xcode build pending"
        sub.textColor = UIColor.white.withAlphaComponent(0.7)
        sub.font = UIFont.systemFont(ofSize: 16)
        sub.textAlignment = .center
        sub.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(sub)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -20),
            sub.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            sub.topAnchor.constraint(equalTo: label.bottomAnchor, constant: 12),
        ])
    }
}
