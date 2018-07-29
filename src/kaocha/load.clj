(ns kaocha.load
  (:require [kaocha.core-ext :refer :all]
            [kaocha.classpath :as classpath]
            [kaocha.testable :as testable]
            [clojure.java.io :as io]
            [clojure.tools.namespace.find :as ctn.find]
            [kaocha.output :as out]))

(defn- ns-match? [ns-patterns ns-sym]
  (some #(re-find % (name ns-sym)) ns-patterns))

(defn- find-test-nss [test-paths ns-patterns]
  (sequence (comp
             (map io/file)
             (map ctn.find/find-namespaces-in-dir)
             cat
             (filter (partial ns-match? ns-patterns)))
            test-paths))

(defn load-test-namespaces [testable ns-testable-fn]
  (let [{:kaocha.suite/keys [test-paths ns-patterns]} testable
        ns-patterns                                   (map regex ns-patterns)]

    (classpath/maybe-add-dynamic-classloader)

    (doseq [path test-paths]
      (when-not (.exists (io/file path))
        (out/warn "In :test-paths, no such file or directory: " path))
      (classpath/add-classpath path))

    (let [ns-names  (find-test-nss test-paths ns-patterns)
          testables (map ns-testable-fn ns-names)]
      (assoc testable :kaocha.test-plan/tests
             (testable/load-testables testables)))))
