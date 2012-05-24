(ns peano.tokens
  (:require [clojure.core.logic :as l]))

(defn query-symbol
  ([symbol-or-string]
     (symbol (str (name symbol-or-string) "??")))
  ([prefix-symbol suffix-key]
     (query-symbol (str (name prefix-symbol) "-" (name suffix-key)))))

(defn typelike-symbol [key]
  ;; Why can I not create a symbol from a key?
  key)

(defn data-symbol [symbol-or-string]
  (symbol (str (name symbol-or-string) "-data")))
