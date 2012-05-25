(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens
        peano.data))

(defn remaining-keys [complete-list to-remove]
  (remove (set to-remove) complete-list))

(defn narrower-clauses [data-map]
  (map (fn [[key value]] `(l/== ~(key-to-lvar key) ~value)) data-map))

(defn generate-no-argument-selector [base-relation]
  `(l/run false [q#]
          (~(query-symbol base-relation) q#)))

(defn generate-one-argument-selector [base-relation [key value]]
  `(l/run false [q#]
          (~(query-symbol base-relation key) q# ~value)))

(defn generate-two-argument-selector [base-relation did kvs]
  (let [as-map (apply hash-map kvs)
        did-value (as-map did)
        [other-key other-value] (first (dissoc as-map did))]
  `(not (empty? (l/run false [q#]
                       (~(query-symbol base-relation other-key) ~did-value ~other-value))))))

(defn did-selector [base-relation did]
  `(defmacro ~(selector-symbol base-relation) [& kvs#]
     (case (count kvs#)
           0 (generate-no-argument-selector '~base-relation)

           2 (generate-one-argument-selector '~base-relation kvs#)

           4 (generate-two-argument-selector '~base-relation ~did kvs#))))
           

(defn make-selector-functions* [base-relation did]
  `(do
     ~(did-selector base-relation did)))
