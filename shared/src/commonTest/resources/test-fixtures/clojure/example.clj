;; SPDX-FileCopyrightText: 2026 Milos Vasic
;; SPDX-License-Identifier: Apache-2.0
;; iter-58 F2 Phase 6 fixture: Clojure.

(ns yole.fixtures.example)

(defrecord Greeter [name])

(defn greet [g]
  (str "Hello, " (:name g) "!"))

(defn -main [& _args]
  (let [g (->Greeter "Yole")]
    (println (greet g))))
