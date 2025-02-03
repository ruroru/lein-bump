(ns leiningen.bump
  (:require
    [clojure.string :as string]
    [leiningen.core.project :as lein-project]))

(def ^:private snapshot-suffix "-SNAPSHOT")

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

(defn- get-next-project-version [command current-version]
  (cond
    (= command "patch") (get-increased-patch-version current-version)
    (= command "minor") (get-increased-minor-version current-version)
    (= command "major") (get-increased-major-version current-version)
    (= command "dev") (get-increased-patch-snapshot-version current-version)
    :else command))

(defn- update-version-in-project-clj [file-config old-version new-version]
  (let [pattern-str (format "(?<=\\\")%s(?=\\\"\\s+)" old-version)
        match (re-pattern pattern-str)]
    (string/replace file-config match new-version)))

(defn- update-config [file-config command old-version]
  (let [new-version (get-next-project-version command old-version)]
    (update-version-in-project-clj file-config old-version new-version)))

(defn- write-to-file [content project-file]
  (spit project-file content))

(defn- update-subs-with-new-values [file-config regexes-to-update]
  (if (empty? regexes-to-update)
    file-config
    (let [update-config (first regexes-to-update)
          rest-regexes (rest regexes-to-update)]
      (update-subs-with-new-values (string/replace file-config (first update-config) (second update-config)) rest-regexes))))

(defn- update-subs [file-config current-file command old-version]
  (if (and (= current-file "project.clj")
           (:sub (lein-project/read)))
    (let [new-version (get-next-project-version command old-version)
          regexes-to-update (map
                              (fn [project-id]
                                (let [replace-pattern (format "(?<=\\[)\\s*%s\\s+\\\"%s(?=\\\"\\s*\\])" project-id old-version)]
                                  [(re-pattern replace-pattern) (format "%s \"%s" project-id new-version)]))
                              (map (fn [subproject]
                                     (let [project-pattern #"(?<=defproject )\s*([a-zA-Z0-9\-\.]+\/)?[a-zA-Z0-9\-]+"
                                           project-id (first (re-find project-pattern (slurp (format "%s/project.clj" subproject))))]
                                       project-id))
                                   (:sub (lein-project/read))))]
      (update-subs-with-new-values file-config regexes-to-update))
    file-config))

(defn bump
  "Allows stepping version from lein"
  [project & args]
  (let [command (first args)
        project-files (cons "project.clj"
                            (map (fn [item]
                                   (str item "/project.clj"))
                                 (:sub (lein-project/read))))]
    (when (not (and (= command "dev")
                    (string/ends-with? (:version project) snapshot-suffix)))
      (if command
        (doseq [project-file project-files]
          (-> project-file
              slurp
              (update-config command (:version project))
              (update-subs project-file command (:version project))
              (write-to-file project-file)))
        (println (:version project))))))
