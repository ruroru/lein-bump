(ns leiningen.bump
  (:require
    [leiningen.core.project :as lein-project]
    [clojure.string :as string])
  (:import (java.util.regex Matcher Pattern)))

(def ^:private snapshot-suffix "-SNAPSHOT")

(defn- set-version-in-project-clj [project-clj old-version new-version]
  (let [proj-file (slurp project-clj)
        matcher (.matcher
                  (Pattern/compile (format "(\\(defproject .+? )\"\\Q%s\\E\"" old-version))
                  proj-file)]
    (if-not (.find matcher)
      (println "Error: unable to find version string %s in project.clj file!" old-version)
      (do
        (spit project-clj (.replaceFirst ^Matcher matcher ^String (format "%s\"%s\"" (.group matcher 1) new-version)))
        (println (format "Updated %s version from %s to %s" project-clj old-version new-version))))))

(defn- get-increased-major-version [version]
  (let [major-version (-> version (string/split #"\.") first Integer/parseInt inc)]
    (string/join "." [major-version "0.0"])))

(defn- get-increased-minor-version [current-version]
  (let [sem-ver-list (-> current-version (string/split #"\."))
        minor-version (-> sem-ver-list rest first Integer/parseInt inc)]

    (string/join "." [(first sem-ver-list) minor-version "0"])))

(defn- get-increased-patch-version [current-version]
  (if (string/ends-with? current-version snapshot-suffix)
    (string/replace current-version snapshot-suffix "")
    (let [sem-ver-list (-> current-version (string/split #"\."))
          patch-version (-> sem-ver-list last Integer/parseInt inc)]
      (string/join "." [(first sem-ver-list) (second sem-ver-list) patch-version]))))


(defn- get-increased-patch-snapshot-version [current-version]
  (str (get-increased-patch-version current-version) snapshot-suffix))

(defn- update-version [project-clj current-version increase-version-function]
  (let [new-project-version (increase-version-function current-version)]
    (set-version-in-project-clj project-clj current-version new-project-version)))

(defn- handle-release-version [project-clj current-project-version command]
  (cond
    (.equals "patch" command) (update-version project-clj current-project-version get-increased-patch-version)
    (.equals "minor" command) (update-version project-clj current-project-version get-increased-minor-version)
    (.equals "major" command) (update-version project-clj current-project-version get-increased-major-version)
    :else "Invalid option"))

(defn- handle-prep-for-new-iteration [project-clj current-project-version]
  (when-not (string/ends-with? current-project-version "-SNAPSHOT")
    (update-version project-clj current-project-version get-increased-patch-snapshot-version)))

(defn- update-project-clj [project-clj project-version arg]
  (cond
    (= arg "patch") (handle-release-version project-clj project-version arg)
    (= arg "minor") (handle-release-version project-clj project-version arg)
    (= arg "major") (handle-release-version project-clj project-version arg)
    (= arg "dev") (handle-prep-for-new-iteration project-clj project-version)
    (not (nil? arg)) (set-version-in-project-clj project-clj project-version arg)
    :else (when (= "project.clj" project-clj)
            (println project-version))))

(defn bump
  "Allows stepping version from lein"
  [project & args]
  (let [command (first args)
        project-version (:version project)]
    (let [project-files (cons "project.clj"
                              (map (fn [item]
                                     (str item "/project.clj"))
                                   (:sub (lein-project/read))))]
      (doseq [project-file project-files]
        (update-project-clj project-file project-version command)))))
