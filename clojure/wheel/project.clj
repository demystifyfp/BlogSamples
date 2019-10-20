(defproject wheel "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [aero "1.1.3"]
                 [mount "0.1.16"]
                 [hikari-cp "2.8.0"]
                 [org.postgresql/postgresql "42.2.6"]
                 [org.flywaydb/flyway-core "5.2.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [cheshire "5.9.0"]
                 [toucan "1.14.0"]
                 [clj-http "3.10.0"]
                 [com.ibm.mq/com.ibm.mq.allclient "9.1.0.0"]
                 [clojurewerkz/quartzite "2.1.0"]
                 [funcool/cuerdas "2.0.5"]
                 [org.clojure/data.xml "0.0.8"]]
  :main ^:skip-aot wheel.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths ["dev"]
                       :dependencies [[org.clojure/tools.namespace "0.3.1"]]}})
