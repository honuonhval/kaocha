(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as kaocha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as default-test-suite]
            [kaocha.type :as type]
            [kaocha.spec-test-check :as k-stc]
            [kaocha.type.clojure.spec.test.check :as type.stc]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]))

(alias 'stc 'clojure.spec.test.check)
(alias 'type.stc 'kaocha.type.clojure.spec.test.check)

(defn tests-with-overridden-stc-opts [{:kaocha/keys [tests] :as config}]
  (map (fn [test]
         (if (k-stc/is-stc? test)
           (update test merge (k-stc/opts config))
           test))
       tests))

(defn default-test-suite [config]
  (-> {:kaocha.testable/type    :kaocha.type/clojure.spec.test.check
       :kaocha.testable/id      :generative-fdef-checks
       :kaocha.filter/skip-meta [:kaocha/skip]
       :kaocha/source-paths     ["src"],
       ::type.stc/syms          :all-fdefs}
      (merge (k-stc/opts config))))

(defn overide-stc-settings [config]
  (assoc config :kaocha/tests (tests-with-overridden-stc-opts config)))

(defn add-default-test-suite [config]
  (update config :kaocha/tests conj (default-test-suite config)))

(defplugin kaocha.plugin.alpha/spec-test-check
  (pre-load [{:kaocha/keys [tests] :as config}]
            (if (k-stc/has-stc? tests)
              (overide-stc-settings config)
              (add-default-test-suite config)))
  (cli-options [opts]
               (conj opts
                     [nil  "--num-tests NUM" "Test iterations per fdef"
                      :parse-fn #(Integer/parseInt %)]
                     [nil  "--max-size SIZE" "Maximum length of generated collections"
                      :parse-fn #(Integer/parseInt %)]
                     [nil  "--ns-pattern PATTERN" "Regex matching your project's namespaces"]))
  (config [config]
          (let [num-tests  (get-in config [:kaocha/cli-options :num-tests])
                max-size   (get-in config [:kaocha/cli-options :max-size])
                ns-pattern (get-in config [:kaocha/cli-options :ns-pattern])]
            (assoc config
                   ::ns-patterns [ns-pattern]
                   ::stc/num-tests num-tests
                   ::stc/max-size max-size))))

(s/def ::ns-patterns :kaocha/ns-patterns)