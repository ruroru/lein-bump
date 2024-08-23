(ns leiningen.versions-plugin
  (:require
    [clojure.core.match :as match]
    [clojure.string :as string])
  (:import (java.util.regex Pattern)))

(def project-clj "project.clj")

(defn- set-version-in-project-clj [old-version new-version]
  (let [proj-file (slurp project-clj)
        matcher (.matcher
                  (Pattern/compile (format "(\\(defproject .+? )\"\\Q%s\\E\"" old-version))
                  proj-file)]
    (if-not (.find matcher)
      (println "Error: unable to find version string %s in project.clj file!" old-version)
      (do
        (spit project-clj (.replaceFirst matcher (format "%s\"%s\"" (.group matcher 1) new-version)))
        (println (format "Stepped project version from %s to %s" old-version new-version) )))))

(defn- get-increased-major-version [version]
  (let [major-version (-> version (string/split #"\.") first Integer/parseInt inc)]
    (string/join "." [major-version "0.0"])))

(defn- get-increased-minor-version [current-version]
  (let [sem-ver-list (-> current-version (string/split #"\."))
        minor-version (-> sem-ver-list rest first Integer/parseInt inc)]

    (string/join "." [(first sem-ver-list) minor-version "0"])))

(defn- get-increased-patch-version [current-version]
  (let [sem-ver-list (-> current-version (string/split #"\."))
        patch-version (-> sem-ver-list last Integer/parseInt inc)]

    (string/join "." [(first sem-ver-list) (second sem-ver-list) patch-version])))

(defn update-version [current-version increase-version-function]
  (let [new-project-version (increase-version-function current-version)]
    (set-version-in-project-clj current-version new-project-version)))



(defn- handle-stepping [current-project-version args]
  (match/match
    [(first args)]
    [nil] (update-version current-project-version get-increased-patch-version)
    ["minor"] (update-version current-project-version get-increased-minor-version)
    ["major"] (update-version current-project-version get-increased-major-version)
    :else "Invalid option"))

(defn versions-plugin
  "Allows stepping version from lein"
  [project & args]
  (match/match
    [(first args)]
    ["step"] (handle-stepping (:version project) (-> args rest))
    ["set"] (set-version-in-project-clj (:version project) (-> args rest first))
    :else (println (:version project))))
