(ns peano.core
  (:require [clojure.core.logic :as l])
  (:use [peano.data :only [data*]])
  (:use [peano.selectors :only [make-did-selector*]]))


(defmacro data [data-type & data-maps]
  (data* data-type data-maps))

(defmacro make-did-selector [relation]
  (make-did-selector* relation))
  
