(ns peano.sweet
  (:require [clojure.zip])
  (:use [peano.data :only [data*]]
        [peano.selectors :only [make-did-selector* make-seq-selector*]]
        [peano.blank-filling :only [fill-in-the-zipper def-forest-fillers*]]))

(defmacro data [data-type & data-maps]
  (data* data-type data-maps))

(defmacro make-did-selector [relation]
  (make-did-selector* relation))
  
(defmacro make-seq-selector [relation & keys]
  (make-seq-selector* relation keys))

(defn fill-in-the-blanks [guidance vector-structure]
  (fill-in-the-zipper guidance (clojure.zip/vector-zip vector-structure)))
  
(defmacro def-forest-fillers [basename guidance]
  (def-forest-fillers* basename guidance))

(defn suggested-classifier [form]
  (cond (= '- form) :unconstrained-blank
        (string? form) :blank-that-identifies
        (symbol? form) :presupplied-lvar
        (map? form) :blank-with-properties))
