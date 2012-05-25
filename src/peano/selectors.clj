(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens
        peano.data))

(defn generate-run-form [base-relation kvs]
  (let [q (gensym "q")
        selector-for-did `(~(query-symbol base-relation) ~q)
        narrowing-selectors (map (fn [[key value]]
                                   `( ~(query-symbol base-relation key) ~q ~value))
                                 (partition 2 kvs))]
  `(l/run false [~q] ~selector-for-did ~@narrowing-selectors)))

(defn make-did-selector* [base-relation]
  `(defmacro ~(selector-symbol base-relation) [& kvs#]
     (generate-run-form '~base-relation kvs#)))
