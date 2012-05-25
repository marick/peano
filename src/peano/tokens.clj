(ns peano.tokens
  (:require [clojure.core.logic :as l]))

(defn- symbol-maker [prefix suffix]
  (symbol (str (name prefix) suffix)))

(defn query-symbol
  ([symbol-or-string]
     (symbol-maker symbol-or-string "??"))
  ([prefix-symbol suffix-key]
     (query-symbol (str (name prefix-symbol) "-" (name suffix-key)))))

(defn selector-symbol
  ([symbol-or-string]
     (symbol-maker symbol-or-string "?>")))

(defn typelike-symbol [key]
  ;; Why can I not create a symbol from a key?
  key)

(defn data-symbol [symbol-or-string]
  (symbol (str (name symbol-or-string) "-data")))
