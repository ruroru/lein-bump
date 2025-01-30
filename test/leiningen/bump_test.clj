(ns leiningen.bump-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [leiningen.bump :as plugin]
    [mock-clj.core :as mock]))

(def ^:private project-name "lein-bump")
(defn- get-mock-project-clj
  ([version]
   (format "(defproject %s \"%s\" :description \"F\" :url \"h\" :dependencies [[dependency-1 \"5.4.8\"] [dependency-2 \"5.4.5\"]])\n"
           project-name
           version))
  ([version sub-projects]
   (format "(defproject %s \"%s\"
   :description \"F\"
   :url \"h\"
   :sub %s
   :dependencies [[dependency-1 \"5.4.8\"] [dependency-2 \"5.4.5\"]])\n"
           project-name
           version
           (string/join sub-projects)))
  )
(def ^:private dev "dev")

(defn- verify-not-written []
  (is (= (mock/call-count spit) 0)))

(defn- verify-project-clj-write
  ([_ version]
   (let [args [(list "project.clj" (get-mock-project-clj version))]]
     (is (= (mock/calls spit) (into [] args)))
     (is (= (mock/call-count spit) 1))))
  ([project-name version subprojects]
   (let [args (map (fn [project-name]
                     (list (if (string/starts-with? project-name "sub")
                             (str project-name "/project.clj")
                             "project.clj")
                           (get-mock-project-clj version)))
                   (merge subprojects "project.clj"))]
     (is (= (mock/calls spit) (into [] args)))
     (is (= (mock/call-count spit) (+ 1 (count subprojects)))))))



(deftest prepare-for-release
  (testing "Stepping patch version"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.9"))))

  (testing "Stepping patch snapshot version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.8"))))

  (testing "Stepping major version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "major")
        (verify-project-clj-write (list project-name) "6.0.0"))))

  (testing "Stepping minor version"
    (let [project {:version "5.4.8-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "minor")
        (verify-project-clj-write (list project-name) "5.5.0")))))

(deftest prepare-for-new-dev-iteration
  (testing "Prepare for new development iteration for 6.0.0"
    (let [project {:version "6.0.0"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project dev)
        (verify-project-clj-write (list project-name) "6.0.1-SNAPSHOT"))))

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
        (verify-project-clj-write (list project-name) "5.4.9-SNAPSHOT")))))

(deftest set-version
  (testing "set version to 6.0.0"
    (let [project {:version "5.4.8"}]
      (mock/with-mock
        [spit nil
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write (list project-name) "1.0.0")))))

(deftest get-version
  (testing "get version for 5.4.8"
    (let [project {:version "5.4.8"}]
      (is (= "5.4.8" (string/trim-newline (with-out-str (plugin/bump project)))))))
  (testing "get version for 5.4.9-SNAPSHOT"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (plugin/bump project)
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
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "patch")
        (verify-project-clj-write project-name "5.4.9" (list "sub1" "sub2")))))

  (testing "step multi project minor version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "minor")
        (verify-project-clj-write project-name "5.5.0" (list "sub1" "sub2")))))

  (testing "step multi project major version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "major")
        (verify-project-clj-write project-name "6.0.0" (list "sub1" "sub2")))))

  (testing "multi project set version"
    (let [project {:version "5.4.9-SNAPSHOT"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "1.0.0")
        (verify-project-clj-write project-name "1.0.0" (list "sub1" "sub2")))))

  (testing "step multi project version to snapshot"
    (let [project {:version "5.4.9"}]
      (mock/with-mock
        [spit nil
         leiningen.core.project/read {:sub ["sub1" "sub2"]}
         slurp (get-mock-project-clj (:version project))]
        (plugin/bump project "dev")
        (verify-project-clj-write project-name "5.4.10-SNAPSHOT" (list "sub1" "sub2"))))))