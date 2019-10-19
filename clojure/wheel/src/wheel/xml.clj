(ns wheel.xml
  (:require [cuerdas.core :as str]
            [clojure.xml :as xml])
  (:import [java.io StringBufferInputStream]))

(defn- xml-element-merger [x y]
  (if (vector? x)
    (conj x y)
    (vector x y)))

(defn- merge-if [map source-map]
  (if (empty? map)
    source-map
    (merge map source-map)))

(defn hyphenate-keys [m]
  (reduce
   (fn [state x]
     (let [value (get m x)]
       (assoc (dissoc state x)
              (str/keyword x)
              (cond
                (map? value) (hyphenate-keys value)
                (sequential? value) (map #(hyphenate-keys %) value)
                :else value))))
   m (keys m)))

(defn xml-element->map [{:keys [tag attrs content]}]
  (let [attrs (hyphenate-keys attrs)]
    (if (= 1 (count content))
      (if (map? (first content))
        (hash-map (str/keyword tag) (->> (first content)
                                         xml-element->map
                                         (merge attrs)))
        (if (empty? attrs)
          (hash-map (str/keyword tag) (first content))
          (hash-map (str/keyword tag) attrs)))
      (hash-map (str/keyword tag) (->> (map xml-element->map content)
                                       (apply (partial merge-with xml-element-merger))
                                       (merge-if attrs))))))

(defn xml-str->map [raw-xml-str]
  (-> (StringBufferInputStream. raw-xml-str)
      xml/parse
      xml-element->map))
