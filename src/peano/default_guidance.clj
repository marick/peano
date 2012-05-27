(ns peano.default-guidance
  (:require [clojure.core.logic :as l])
  (:use peano.tokens))

(defn default-classification [form]
  (cond (= '- form) :unconstrained-blank
        (string? form) :blank-that-identifies
        (symbol? form) :presupplied-lvar
        (map? form) :blank-with-properties))
        
