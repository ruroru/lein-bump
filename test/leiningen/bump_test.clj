(ns leiningen.bump-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [leiningen.bump :as plugin]
    [mock-clj.core :as mock]))

(defn- get-mock-project-clj
  ([project-name version]
   (format "(defproject %s \"%s\" :description \"F\" :url \"h\" :dependencies [[dependency-1 \"5.4.8\"] [dependency-2 \"5.4.5\"]])\n"
           project-name
           version))
  ([project-name version sub-projects]
   (let [sub-project-string (format "[%s] " (string/join " " (map (fn [sp]
                                                                    (format "\"%s\"" sp))
                                                                  sub-projects)))
         dependency-string (string/join " " (map (fn [sp]
                                                   (format "[%s \"%s\"]" sp version))
                                                 sub-projects))]
     (format "(defproject %s \"%s\" :description \"F\" :url \"h\" :sub %s :dependencies [[dependency-1 \"5.4.8\"] %s [dependency-2 \"5.4.5\"]])\n"
             project-name
             version
             sub-project-string
             dependency-string))))
(def ^:private dev "dev")
(def ^:private project-name "lein-bump")

(defn- verify-not-written []
  (is (= (mock/call-count spit) 0)))

(defn- verify-project-clj-write
  ([_ version]
   (let [args [(list "project.clj" (get-mock-project-clj project-name version))]]
     (is (= (mock/calls spit) (into [] args)))
     (is (= (mock/call-count spit) 1))))
  ([_ version subprojects]
   (let [args (map (fn [sub-project]
                     (if (= "project.clj" sub-project)
                       (list (format "project.clj" sub-project) (get-mock-project-clj project-name version subprojects))
                       (list (format "%s/project.clj" sub-project)
                             (get-mock-project-clj sub-project version))))

                   (conj subprojects "project.clj"))]
     (is (= (mock/calls spit) (into [] args)))
     (is (= (mock/call-count spit) (+ 1 (count subprojects)))))))

(deftest prepare-for-release
  (testing "Stepping patch version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.9"))))

  (testing "Stepping patch snapshot version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.8"))))

  (testing "Stepping major version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "major")
        (verify-project-clj-write (list project-name) "6.0.0"))))

  (testing "Stepping minor version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "minor")
        (verify-project-clj-write (list project-name) "5.5.0")))))

(deftest prepare-for-new-dev-iteration
  (testing "Prepare for new development iteration for 6.0.0"
    (let [project {:version "6.0.0"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project dev)
        (verify-project-clj-write (list project-name) "6.0.1-SNAPSHOT"))))

  (testing "Prepare for new development iteration for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project dev)
        (verify-not-written))))

  (testing "Prepare for new development iteration for 5.4.8"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project dev)
        (verify-project-clj-write (list project-name) "5.4.9-SNAPSHOT")))))

(deftest set-version
  (testing "set version to 1.0.0"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write (list project-name) "1.0.0"))))

  (testing "set version to 1.0.0-SNAPSHOT"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj project-name (:version project))]
        (plugin/bump project "1.0.0-SNAPSHOT")
        (verify-project-clj-write (list project-name) "1.0.0-SNAPSHOT")
        )))

  (testing "set invalid version"
    (let [project {:version "5.4.8"}]
      (is (= "New version is not legal." (string/trim-newline (with-out-str (plugin/bump project "..."))))))))

(deftest  invalid-version
  (let [project {:version "5.4.8"}]
    (= "Unable to set version" (string/trim-newline (with-out-str (plugin/bump project "..."))))))

(deftest get-version
  (testing "get version for 5.4.8"
    (let [project {:version "5.4.8"}]
      (is (= "5.4.8" (string/trim-newline (with-out-str (plugin/bump project)))))))
  (testing "get version for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]

      (is (= "5.4.9-SNAPSHOT" (string/trim-newline (with-out-str (plugin/bump project)))))))
  (testing "get multi project version"
    (mock/with-mock
      [leiningen.core.project/read {:sub ["sub1" "sub2"]}]
      (plugin/bump {:version "5.4.9-SNAPSHOT"})
      (is (= "5.4.9-SNAPSHOT" (string/trim-newline (with-out-str (plugin/bump {:version "5.4.9-SNAPSHOT"}))))))))


(deftest bump-multi-project-version
  (testing "step multi project patch version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (fn [file-name]
                 (if (= file-name "project.clj")
                   (get-mock-project-clj project-name (:version project) ["sub1" "sub2"])
                   (get-mock-project-clj (first (string/split file-name #"/")) (:version project))))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.9" (list "sub1" "sub2")))))

  (testing "step multi project minor version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (fn [file-name]
                 (if (= file-name "project.clj")
                   (get-mock-project-clj project-name (:version project) ["sub1" "sub2"])
                   (get-mock-project-clj (first (string/split file-name #"/")) (:version project))))]
        (plugin/bump project "minor")
        (verify-project-clj-write project-name "5.5.0" (list "sub1" "sub2")))))

  (testing "step multi project major version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (fn [file-name]
                 (if (= file-name "project.clj")
                   (get-mock-project-clj project-name (:version project) ["sub1" "sub2"])
                   (get-mock-project-clj (first (string/split file-name #"/")) (:version project))))]
        (plugin/bump project "major")
        (verify-project-clj-write project-name "6.0.0" (list "sub1" "sub2")))))

  (testing "multi project set version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (fn [file-name]
                 (if (= file-name "project.clj")
                   (get-mock-project-clj project-name (:version project) ["sub1" "sub2"])
                   (get-mock-project-clj (first (string/split file-name #"/")) (:version project))))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write project-name "1.0.0" (list "sub1" "sub2")))))

  (testing "step multi project version to snapshot"
    (let [project {:version "5.4.9"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (fn [file-name]
                 (if (= file-name "project.clj")
                   (get-mock-project-clj project-name (:version project) ["sub1" "sub2"])
                   (get-mock-project-clj (first (string/split file-name #"/")) (:version project))))]
        (plugin/bump project "dev")
        (verify-project-clj-write project-name "5.4.10-SNAPSHOT" (list "sub1" "sub2"))))))
