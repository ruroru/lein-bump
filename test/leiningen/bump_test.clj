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

(defn- verify-project-clj-write [versions]
  (let [args [(list "project.clj" (get-mock-project-clj (first versions)))]]
    (is (= (mock/calls spit) args))
    (is (= (mock/call-count spit) (count versions)))))

(deftest prepare-for-release
  (testing "Stepping patch version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write (list "5.4.9")))))

  (testing "Stepping patch snapshot version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write (list "5.4.8")))))

  (testing "Stepping major version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "major")
        (verify-project-clj-write (list "6.0.0")))))

  (testing "Stepping minor version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "minor")
        (verify-project-clj-write (list "5.5.0"))))))

(deftest prepare-for-new-dev-iteration
  (testing "Prepare for new development iteration for 6.0.0"
    (let [project {:version "6.0.0"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project dev)
        (verify-project-clj-write (list "6.0.1-SNAPSHOT")))))

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
        (verify-project-clj-write (list "5.4.9-SNAPSHOT"))))))

(deftest set-version
  (testing "set version to 6.0.0"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write (list "1.0.0"))))))

(deftest get-version
  (testing "get version for 5.4.8"
    (let [project {:version "5.4.8"}]
      (is (= "5.4.8" (string/trim-newline (with-out-str (plugin/bump project)))))))
  (testing "get version for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (plugin/bump project)
      (is (= "5.4.9-SNAPSHOT" (string/trim-newline (with-out-str (plugin/bump project))))))))


(deftest bump-multi-project-version

  (testing "get version for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write (list "1.0.0"))))))