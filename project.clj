(defproject bashimbot "0.1.0-SNAPSHOT"
  :description "Bot for posting bash.im/abyssbest quotes to twitter"
  :url "https://twitter.com/bashimabyssbest"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.11.0"]
                 [enlive "1.1.6"]
                 [twitter-api "0.7.8"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [log4j/log4j "1.2.17"]]
  :main ^:skip-aot bashimbot.core
  :profiles {:dev {:source-paths ["dev" "src" "test"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [proto-repl "0.1.2"]]}
             :uberjar {:aot :all}}
  :uberjar-name "bashimbot.jar")
