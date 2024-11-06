(defproject org.clojars.jj/bump "1.0.1-SNAPSHOT"
  :description "Lein plugin that helps updating version in project.clj"
  :url "https://github.com/ruroru/lein-bump"
  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/core.match "1.1.0"]
                 [leiningen-core "2.11.2"]]

  :plugins [[org.clojars.jj/bump "1.0.0"]]
  :deploy-repositories [["clojars" {:url      "https://repo.clojars.org"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass}]]

  :profiles {:test {:global-vars  {*warn-on-reflection* true}
                    :dependencies [[mock-clj "0.2.1"]]}})