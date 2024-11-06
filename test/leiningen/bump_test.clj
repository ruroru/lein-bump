(ns leiningen.bump-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [leiningen.bump :as plugin]
    [mock-clj.core :as mock]))

(defn- get-mock-project-clj [version] (str "(defproject lein-versions-plugin \"" version "\" :description \"F\" :url \"h\" :dependencies [[dependency-1 \"5.4.8\"] [dependency-2 \"5.4.5\"]])\n"))

(def dev "dev")

(defn- verify-not-written []
  (is (= (mock/call-count spit) 0)))

(defn- verify-spit [args call-count]
  (is (= (mock/call-count spit) call-count))
  (is (= (mock/calls spit) args)))

(defn- verify-spit-call [version]
  (let [expected-spit-arg [(list "project.clj" (get-mock-project-clj version))]]
    (verify-spit expected-spit-arg 1)))

(deftest prepare-for-release
  (testing "Stepping patch version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-spit-call "5.4.9"))))

  (testing "Stepping patch snapshot version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-spit-call "5.4.8"))))

  (testing "Stepping major version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "major")
        (verify-spit-call "6.0.0"))))

  (testing "Stepping minor version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "minor")
        (verify-spit-call "5.5.0")))))

(deftest prepare-for-new-dev-iteration
  (testing "Prepare for new development iteration for 6.0.0"
    (let [project {:version "6.0.0"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project dev)
        (verify-spit-call "6.0.1-SNAPSHOT"))))

  (testing "Prepare for new development iteration for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project dev)
        (verify-not-written))))

  (testing "Prepare for new development iteration for 5.4.8"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project dev)
        (verify-spit-call "5.4.9-SNAPSHOT")))))

(deftest set-version
  (testing "set version to 6.0.0"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "set" "1.0.0")
        (verify-spit-call "1.0.0")))))

(deftest get-version
  (testing "get version for 5.4.8"
    (let [project {:version "5.4.8"}]
      (is (= "5.4.8" (string/trim-newline (with-out-str (plugin/bump project)))))))
  (testing "get version for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (plugin/bump project)
      (is (= "5.4.9-SNAPSHOT" (string/trim-newline (with-out-str (plugin/bump project))))))))