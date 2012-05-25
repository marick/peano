(ns peano.selectors
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))

(defn make-selector-functions* [base-relation]
  `(do
     (defn ~(selector-symbol base-relation) []
       (l/run false [q#]
         (l/fresh [animal#]
           (~(query-symbol base-relation) animal#)
           (l/== q# animal#)))))
  )
