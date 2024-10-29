(ns leiningen.bump
  (:require
    [clojure.core.match :as match]
    [clojure.string :as string])
  (:import (java.util.regex Matcher Pattern)))

(def ^:private project-clj "project.clj")
(def ^:private snapshot-suffix "-SNAPSHOT")

(defn- set-version-in-project-clj [old-version new-version]
  (let [proj-file (slurp project-clj)
        matcher (.matcher
                  (Pattern/compile (format "(\\(defproject .+? )\"\\Q%s\\E\"" old-version))
                  proj-file)]
    (if-not (.find matcher)
      (println "Error: unable to find version string %s in project.clj file!" old-version)
      (do
        (spit project-clj (.replaceFirst ^Matcher matcher ^String (format "%s\"%s\"" (.group matcher 1) new-version)))
        (println (format "Updated project version from %s to %s" old-version new-version))))))

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

(defn- update-version [current-version increase-version-function]
  (let [new-project-version (increase-version-function current-version)]
    (set-version-in-project-clj current-version new-project-version)))

(defn- handle-release-version [current-project-version command]
  (match/match
    [command]
    ["patch"] (update-version current-project-version get-increased-patch-version)
    ["minor"] (update-version current-project-version get-increased-minor-version)
    ["major"] (update-version current-project-version get-increased-major-version)
    :else "Invalid option"))

(defn- handle-prep-for-new-iteration [current-project-version args]
  (update-version current-project-version get-increased-patch-snapshot-version))

(defn bump
  "Allows stepping version from lein"
  [project & args]
  (let [command (first args)]
    (match/match
      [command]
      ["patch"] (handle-release-version (:version project) command)
      ["minor"] (handle-release-version (:version project) command)
      ["major"] (handle-release-version (:version project) command)
      ["dev"] (handle-prep-for-new-iteration (:version project) command)
      ["set"] (set-version-in-project-clj (:version project) (-> args rest first))
      :else (println (:version project)))))
