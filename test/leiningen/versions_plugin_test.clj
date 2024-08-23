(ns leiningen.versions-plugin_test
  (:require
    [clojure.test :refer [deftest testing is]]
    [leiningen.versions-plugin :as plugin]
    [mock-clj.core :as mock]))

(defn- get-mock-project-clj [version] (str "(defproject lein-versions-plugin \"" version "\" :description \"F\" :url \"h\" :dependencies [[dependency-1 \"5.4.8\"] [dependency-2 \"5.4.5\"]])\n"))

(defn- verify-spit [args call-count]
  (is (= (mock/call-count spit) call-count))
  (is (= (mock/calls spit) args)))

(defn- verify-spit-call [version]
  (let [expected-spit-arg [(list "project.clj" (get-mock-project-clj version))]]
    (verify-spit expected-spit-arg 1)))

(deftest versions-plugin-release
  (testing "Stepping patch version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "step")
        (verify-spit-call "5.4.9"))))

  (testing "Stepping minor version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "step" "minor")
        (verify-spit-call "5.5.0"))))

  (testing "Stepping major version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "step" "major")
        (verify-spit-call "6.0.0")))))