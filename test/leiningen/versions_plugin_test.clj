(ns leiningen.versions-plugin_test
  (:require
    [clojure.test :refer [deftest testing is]]
    [leiningen.versions-plugin :as plugin]
    [mock-clj.core :as mock]))

(defn- get-mock-project-clj [version] (str "(defproject lein-versions-plugin \"" version "\" :description \"F\" :url \"h\" :dependencies [[dependency-1 \"5.4.8-SNAPSHOT\"] [dependency-2 \"5.4.5\"]])\n"))

(defn- verify-spit [args call-count]
  (is (= (mock/call-count spit) call-count))
  (is (= (mock/calls spit) args)))

(defn- verify-spit-call [version]
  (let [expected-spit-arg [(list "project.clj" (get-mock-project-clj version))]]
    (verify-spit expected-spit-arg 1)))

(deftest versions-plugin-release

  (testing "prepare-dev does not update file, when version is snapshot"
    (let [project {:version "5.4.5-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "prepare-dev")
        (verify-spit (list) 0))))

  (testing "prepare-dev updates version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "prepare-dev")
        (verify-spit-call "5.4.9-SNAPSHOT"))))

  (testing "release"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "release")
        (verify-spit-call "5.4.8"))))

  (testing "updates version  minor version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "release" "minor")
        (verify-spit-call "5.5.0"))))

  (testing "release major "
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/versions-plugin project "release" "major")
        (verify-spit-call "6.0.0")))))