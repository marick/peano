(ns peano.guidance
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))

(defn with-guaranteed-key
  ([associative key starting-value]
     (merge-with (fn[x,_]x) associative {key starting-value}))
  ([associative key]
     (with-guaranteed-key associative key [])))
                                   

(defn assoc-into-vector [associative key value]
  (-> associative
      (with-guaranteed-key key)
      ((partial merge-with conj) {key value})))

(defn with-lvar [guidance lvar]
  ; Todo: use the ordered jar
  (if (some #{lvar} (:lvars-needed guidance))
    guidance
    (assoc-into-vector guidance :lvars-needed lvar)))

(defn with-new-lvar [guidance identifier]
  (let [where [:lvars identifier :counts]
        current-count (get-in guidance where 0)
        new-symbol (symbol (str (name identifier) "-" current-count))]
    (vector 
     (-> guidance
         (assoc-in where (inc current-count))
         (with-lvar new-symbol))
     new-symbol)))

(defn property-narrower [[relation property] lvar required-value]
  `(l/== (~(query-symbol relation property) ~lvar ~required-value)))

(defn with-narrower [guidance narrower]
  (assoc-into-vector guidance :narrowers narrower))


